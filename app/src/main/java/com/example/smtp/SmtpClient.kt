package com.example.smtp

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

private const val TAG = "SmtpClient"
private const val TIMEOUT_MS = 20000

data class SmtpSendRequest(
    val recipientEmail: String,
    val subject: String,
    val htmlBody: String,
    val plainTextBody: String,
    val attachmentFile: File? = null,
    val attachmentMimeType: String = "audio/mp4"
)

sealed interface SmtpResult {
    data class Success(val message: String) : SmtpResult
    data class Failure(val errorMessage: String) : SmtpResult
}

class SmtpClient {

    suspend fun sendMail(config: SmtpConfig, request: SmtpSendRequest): SmtpResult = withContext(Dispatchers.IO) {
        if (config.host.isBlank()) {
            return@withContext SmtpResult.Failure("O Host do servidor SMTP não está configurado.")
        }
        if (request.recipientEmail.isBlank() || !request.recipientEmail.contains("@")) {
            return@withContext SmtpResult.Failure("Email de destinatário inválido.")
        }

        var socket: Socket? = null
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null

        try {
            val fromEmail = if (config.senderEmail.isNotBlank()) config.senderEmail else config.username
            if (fromEmail.isBlank() || !fromEmail.contains("@")) {
                return@withContext SmtpResult.Failure("Email do remetente inválido ou não configurado nas Definições SMTP.")
            }

            // Connect to server
            if (config.securityType == SmtpSecurityType.SSL) {
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val sslSocket = sslFactory.createSocket() as SSLSocket
                sslSocket.connect(InetSocketAddress(config.host, config.port), TIMEOUT_MS)
                sslSocket.soTimeout = TIMEOUT_MS
                sslSocket.startHandshake()
                socket = sslSocket
            } else {
                val plainSocket = Socket()
                plainSocket.connect(InetSocketAddress(config.host, config.port), TIMEOUT_MS)
                plainSocket.soTimeout = TIMEOUT_MS
                socket = plainSocket
            }

            reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)

            // Read welcome banner
            val banner = readResponse(reader)
            if (!banner.startsWith("220")) {
                return@withContext SmtpResult.Failure("Resposta inesperada do servidor: $banner")
            }

            // Send EHLO
            writer.print("EHLO localhost\r\n")
            writer.flush()
            var ehloResponse = readResponse(reader)

            // Handle STARTTLS if configured
            if (config.securityType == SmtpSecurityType.TLS) {
                writer.print("STARTTLS\r\n")
                writer.flush()
                val tlsResponse = readResponse(reader)
                if (!tlsResponse.startsWith("220")) {
                    return@withContext SmtpResult.Failure("Falha ao iniciar STARTTLS: $tlsResponse")
                }

                // Upgrade socket to TLS
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val upgradedSslSocket = sslFactory.createSocket(socket, config.host, config.port, true) as SSLSocket
                upgradedSslSocket.soTimeout = TIMEOUT_MS
                upgradedSslSocket.startHandshake()
                socket = upgradedSslSocket

                reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)

                // Re-send EHLO after TLS handshake
                writer.print("EHLO localhost\r\n")
                writer.flush()
                ehloResponse = readResponse(reader)
            }

            // Authentication
            if (config.requireAuth && config.username.isNotBlank()) {
                writer.print("AUTH LOGIN\r\n")
                writer.flush()
                val authPrompt1 = readResponse(reader)
                if (!authPrompt1.startsWith("334")) {
                    return@withContext SmtpResult.Failure("Falha no início da autenticação SMTP: $authPrompt1")
                }

                val encodedUser = Base64.encodeToString(config.username.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                writer.print("$encodedUser\r\n")
                writer.flush()
                val authPrompt2 = readResponse(reader)
                if (!authPrompt2.startsWith("334")) {
                    return@withContext SmtpResult.Failure("Utilizador SMTP recusado: $authPrompt2")
                }

                val encodedPass = Base64.encodeToString(config.password.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                writer.print("$encodedPass\r\n")
                writer.flush()
                val authResult = readResponse(reader)
                if (!authResult.startsWith("235")) {
                    return@withContext SmtpResult.Failure("Erro de Autenticação SMTP (Password incorreta): $authResult")
                }
            }

            // MAIL FROM
            writer.print("MAIL FROM:<$fromEmail>\r\n")
            writer.flush()
            val mailFromRes = readResponse(reader)
            if (!mailFromRes.startsWith("250")) {
                return@withContext SmtpResult.Failure("Erro no remetente (MAIL FROM): $mailFromRes")
            }

            // RCPT TO
            writer.print("RCPT TO:<${request.recipientEmail.trim()}>\r\n")
            writer.flush()
            val rcptToRes = readResponse(reader)
            if (!rcptToRes.startsWith("250") && !rcptToRes.startsWith("251")) {
                return@withContext SmtpResult.Failure("Erro no destinatário (RCPT TO): $rcptToRes")
            }

            // DATA
            writer.print("DATA\r\n")
            writer.flush()
            val dataPrompt = readResponse(reader)
            if (!dataPrompt.startsWith("354")) {
                return@withContext SmtpResult.Failure("Servidor recusou comando DATA: $dataPrompt")
            }

            // Build MIME message
            val boundaryMixed = "AlmaDumbo_Mixed_" + UUID.randomUUID().toString().replace("-", "")
            val boundaryAlt = "AlmaDumbo_Alt_" + UUID.randomUUID().toString().replace("-", "")
            val dateHeader = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())

            val hasAttachment = request.attachmentFile != null && request.attachmentFile.exists()

            val mimeBuilder = StringBuilder()
            mimeBuilder.append("From: \"${config.senderName}\" <$fromEmail>\r\n")
            mimeBuilder.append("To: <${request.recipientEmail.trim()}>\r\n")
            mimeBuilder.append("Subject: =?UTF-8?B?${Base64.encodeToString(request.subject.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}?=\r\n")
            mimeBuilder.append("Date: $dateHeader\r\n")
            mimeBuilder.append("MIME-Version: 1.0\r\n")
            mimeBuilder.append("X-Mailer: Alma Dumbo Android App (by ITerp)\r\n")

            if (hasAttachment) {
                mimeBuilder.append("Content-Type: multipart/mixed; boundary=\"$boundaryMixed\"\r\n\r\n")
                mimeBuilder.append("--$boundaryMixed\r\n")
                mimeBuilder.append("Content-Type: multipart/alternative; boundary=\"$boundaryAlt\"\r\n\r\n")
            } else {
                mimeBuilder.append("Content-Type: multipart/alternative; boundary=\"$boundaryAlt\"\r\n\r\n")
            }

            // Plain text part
            mimeBuilder.append("--$boundaryAlt\r\n")
            mimeBuilder.append("Content-Type: text/plain; charset=UTF-8\r\n")
            mimeBuilder.append("Content-Transfer-Encoding: base64\r\n\r\n")
            mimeBuilder.append(Base64.encodeToString(request.plainTextBody.toByteArray(Charsets.UTF_8), Base64.DEFAULT))
            mimeBuilder.append("\r\n\r\n")

            // HTML part
            mimeBuilder.append("--$boundaryAlt\r\n")
            mimeBuilder.append("Content-Type: text/html; charset=UTF-8\r\n")
            mimeBuilder.append("Content-Transfer-Encoding: base64\r\n\r\n")
            mimeBuilder.append(Base64.encodeToString(request.htmlBody.toByteArray(Charsets.UTF_8), Base64.DEFAULT))
            mimeBuilder.append("\r\n\r\n")

            mimeBuilder.append("--$boundaryAlt--\r\n")

            // Attachment part if present
            if (hasAttachment && request.attachmentFile != null) {
                val file = request.attachmentFile
                val fileName = file.name
                val bytes = file.readBytes()
                val base64File = Base64.encodeToString(bytes, Base64.DEFAULT)

                mimeBuilder.append("\r\n--$boundaryMixed\r\n")
                mimeBuilder.append("Content-Type: ${request.attachmentMimeType}; name=\"$fileName\"\r\n")
                mimeBuilder.append("Content-Disposition: attachment; filename=\"$fileName\"\r\n")
                mimeBuilder.append("Content-Transfer-Encoding: base64\r\n\r\n")
                mimeBuilder.append(base64File)
                mimeBuilder.append("\r\n--$boundaryMixed--\r\n")
            }

            // End DATA with dot
            mimeBuilder.append("\r\n.\r\n")

            writer.print(mimeBuilder.toString())
            writer.flush()

            val endDataRes = readResponse(reader)
            if (!endDataRes.startsWith("250")) {
                return@withContext SmtpResult.Failure("Falha ao entregar mensagem: $endDataRes")
            }

            // QUIT
            try {
                writer.print("QUIT\r\n")
                writer.flush()
            } catch (e: Exception) {
                // Ignore quit errors
            }

            return@withContext SmtpResult.Success("Email enviado com sucesso via SMTP para ${request.recipientEmail}!")
        } catch (e: Exception) {
            Log.e(TAG, "SMTP Error", e)
            return@withContext SmtpResult.Failure("Erro de conexão SMTP: ${e.localizedMessage ?: e.javaClass.simpleName}")
        } finally {
            try { writer?.close() } catch (e: Exception) {}
            try { reader?.close() } catch (e: Exception) {}
            try { socket?.close() } catch (e: Exception) {}
        }
    }

    suspend fun testConnection(config: SmtpConfig, testRecipient: String): SmtpResult {
        val subject = "[Alma Dumbo] Teste de Conexão SMTP"
        val plain = "Teste de configuração SMTP do Alma Dumbo executado com sucesso!\nDesenvolvido por: Rodolfo Valentim by ITerp"
        val html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; background-color: #f6f7fb; padding: 20px; color: #1a1a1a;">
                <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 24px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e0e0e0;">
                    <div style="text-align: center; border-bottom: 2px solid #6750A4; padding-bottom: 12px; margin-bottom: 20px;">
                        <h2 style="color: #6750A4; margin: 0;">Alma Dumbo AI</h2>
                        <p style="color: #757575; font-size: 13px; margin: 4px 0 0 0;">ITerp - Tecnologias de Informação Lda | Aka Fábrica de Software</p>
                    </div>
                    <h3 style="color: #2e7d32;">✓ Conexão SMTP Bem-Sucedida!</h3>
                    <p>Este email confirma que as definições do seu servidor SMTP estão corretas e prontas para envio direto a partir da aplicação <strong>Alma Dumbo</strong>.</p>
                    <table style="width: 100%; border-collapse: collapse; margin: 16px 0;">
                        <tr style="border-bottom: 1px solid #eeeeee;"><td style="padding: 8px; color: #666;">Host:</td><td style="padding: 8px; font-weight: bold;">${config.host}</td></tr>
                        <tr style="border-bottom: 1px solid #eeeeee;"><td style="padding: 8px; color: #666;">Porta:</td><td style="padding: 8px; font-weight: bold;">${config.port} (${config.securityType.name})</td></tr>
                        <tr style="border-bottom: 1px solid #eeeeee;"><td style="padding: 8px; color: #666;">Remetente:</td><td style="padding: 8px; font-weight: bold;">${config.senderName} &lt;${config.senderEmail}&gt;</td></tr>
                    </table>
                    <div style="font-size: 12px; color: #888888; border-top: 1px solid #eeeeee; padding-top: 12px; text-align: center;">
                        Desenvolvido por: Rodolfo Valentim by ITerp
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        return sendMail(
            config = config,
            request = SmtpSendRequest(
                recipientEmail = testRecipient.ifBlank { config.defaultRecipient.ifBlank { config.senderEmail } },
                subject = subject,
                htmlBody = html,
                plainTextBody = plain
            )
        )
    }

    private fun readResponse(reader: BufferedReader): String {
        val sb = StringBuilder()
        var line = reader.readLine() ?: throw java.io.IOException("Conexão fechada pelo servidor SMTP.")
        sb.append(line)

        // If multiline reply (e.g. 250-something), keep reading until 250 something
        while (line.length >= 4 && line[3] == '-') {
            line = reader.readLine() ?: break
            sb.append("\n").append(line)
        }
        return sb.toString()
    }
}
