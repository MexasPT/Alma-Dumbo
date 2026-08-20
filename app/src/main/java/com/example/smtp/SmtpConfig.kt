package com.example.smtp

import android.content.Context
import android.content.SharedPreferences

enum class SmtpSecurityType(val label: String, val defaultPort: Int) {
    SSL("SSL (Porta 465)", 465),
    TLS("TLS / STARTTLS (Porta 587)", 587),
    NONE("Nenhuma (Porta 25)", 25)
}

data class SmtpConfig(
    val senderName: String = "Alma Dumbo",
    val senderEmail: String = "",
    val host: String = "",
    val port: Int = 465,
    val securityType: SmtpSecurityType = SmtpSecurityType.SSL,
    val requireAuth: Boolean = true,
    val username: String = "",
    val password: String = "",
    val defaultRecipient: String = ""
)

class SmtpPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("alma_dumbo_smtp_prefs", Context.MODE_PRIVATE)

    fun getSmtpConfig(): SmtpConfig {
        val securityStr = prefs.getString("security_type", SmtpSecurityType.SSL.name) ?: SmtpSecurityType.SSL.name
        val security = try {
            SmtpSecurityType.valueOf(securityStr)
        } catch (e: Exception) {
            SmtpSecurityType.SSL
        }

        return SmtpConfig(
            senderName = prefs.getString("sender_name", "Alma Dumbo") ?: "Alma Dumbo",
            senderEmail = prefs.getString("sender_email", "") ?: "",
            host = prefs.getString("host", "") ?: "",
            port = prefs.getInt("port", security.defaultPort),
            securityType = security,
            requireAuth = prefs.getBoolean("require_auth", true),
            username = prefs.getString("username", "") ?: "",
            password = prefs.getString("password", "") ?: "",
            defaultRecipient = prefs.getString("default_recipient", "") ?: ""
        )
    }

    fun saveSmtpConfig(config: SmtpConfig) {
        prefs.edit()
            .putString("sender_name", config.senderName.trim())
            .putString("sender_email", config.senderEmail.trim())
            .putString("host", config.host.trim())
            .putInt("port", config.port)
            .putString("security_type", config.securityType.name)
            .putBoolean("require_auth", config.requireAuth)
            .putString("username", config.username.trim())
            .putString("password", config.password)
            .putString("default_recipient", config.defaultRecipient.trim())
            .apply()
    }
}
