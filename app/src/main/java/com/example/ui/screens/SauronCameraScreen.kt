package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.audio.LanguageAutoDetector
import com.example.audio.LanguageMeta
import com.example.audio.SupportedLanguages
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DeepPurpleOnPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlowLavender
import com.example.ui.theme.LavenderContainer
import com.example.ui.theme.LavenderOnContainer
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.ListeningCoral
import com.example.ui.theme.ListeningCoralContainer
import com.example.ui.theme.SophisticatedBackground
import com.example.ui.theme.SophisticatedOutline
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.TranscriberViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private const val TAG = "SauronCamera"

data class DetectedTextItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalText: String,
    val translatedText: String = "",
    val boundingBox: Rect? = null,
    val sourceLang: String = "auto"
)

@Composable
fun SauronCameraScreen(
    viewModel: TranscriberViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val ttsManager = viewModel.ttsManager

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Permissão de câmara necessária para o Olho de Sauron.", Toast.LENGTH_LONG).show()
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFrozen by remember { mutableStateOf(false) }

    var detectedItems by remember { mutableStateOf<List<DetectedTextItem>>(emptyList()) }
    var targetLanguage by remember { mutableStateOf(SupportedLanguages.findByCode("pt-PT") ?: SupportedLanguages.ALL.first()) }
    var isTranslating by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            textRecognizer.close()
        }
    }

    if (!hasCameraPermission) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SophisticatedBackground)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ListeningCoral.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = ListeningCoral,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "OLHO DE SAURON",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = ListeningCoral,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Tradução com a Câmara",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "O Olho de Sauron necessita de acesso à câmara do telemóvel para ler e traduzir texto impresso ou digital em tempo real diretamente no ecrã.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ListeningCoral),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Conceder Acesso à Câmara", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isFrozen) {
                            processImageProxy(
                                imageProxy = imageProxy,
                                recognizer = textRecognizer,
                                targetLang = targetLanguage.code,
                                viewModel = viewModel,
                                scope = scope,
                                onDetected = { items ->
                                    detectedItems = items
                                }
                            )
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay glowing viewfinder effect & bounding box indicators
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = GlowLavender.copy(alpha = 0.5f)
            val cornerLength = 40.dp.toPx()
            val strokeWidth = 3.dp.toPx()

            // Center targeting brackets
            val cx = size.width / 2f
            val cy = size.height / 2.3f
            val boxW = size.width * 0.85f
            val boxH = size.height * 0.45f
            val left = cx - boxW / 2f
            val top = cy - boxH / 2f
            val right = left + boxW
            val bottom = top + boxH

            // Top-left
            drawLine(strokeColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
            drawLine(strokeColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)
            // Top-right
            drawLine(strokeColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
            drawLine(strokeColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)
            // Bottom-left
            drawLine(strokeColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
            drawLine(strokeColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)
            // Bottom-right
            drawLine(strokeColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
            drawLine(strokeColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)
        }

        // Top HUD Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ListeningCoral.copy(alpha = 0.25f))
                            .border(1.dp, ListeningCoral, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = ListeningCoral,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "OLHO DE SAURON",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontSize = 9.sp),
                            color = ListeningCoral,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFrozen) "Imagem Fixada (Pausa)" else "Tradução em Tempo Real",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Flash Toggle
                    IconButton(
                        onClick = {
                            isFlashOn = !isFlashOn
                            camera?.cameraControl?.enableTorch(isFlashOn)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isFlashOn) AmberGold else Color.White
                        )
                    }

                    // Freeze Frame Toggle
                    IconButton(
                        onClick = { isFrozen = !isFrozen },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isFrozen) AmberGold.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isFrozen) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Fixar imagem",
                            tint = if (isFrozen) AmberGold else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Target Language Selector Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Traduzir p/:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val popularTargets = listOf("pt-PT", "en-US", "es-ES", "fr-FR", "de-DE", "it-IT", "zh-CN")
                    items(popularTargets) { code ->
                        val meta = SupportedLanguages.findByCode(code) ?: return@items
                        val isSelected = targetLanguage.code == code
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) LavenderPrimary else Color.Black.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) LavenderPrimary else Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable {
                                targetLanguage = meta
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = meta.flag, fontSize = 12.sp)
                                Text(
                                    text = meta.namePt.split(" ").first(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = if (isSelected) DeepPurpleOnPrimary else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Results HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Live Detected & Translated Cards Preview
            if (detectedItems.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = SophisticatedSurface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, GlowLavender.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess)
                                )
                                Text(
                                    text = "${detectedItems.size} Blocos de Texto Detetados",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Speak entire translation
                                IconButton(
                                    onClick = {
                                        val combined = detectedItems.joinToString(" ") { it.translatedText.ifBlank { it.originalText } }
                                        if (combined.isNotBlank()) {
                                            ttsManager.speak(combined, targetLanguage.code)
                                            Toast.makeText(context, "A ler tradução...", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Ouvir",
                                        tint = LavenderPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Copy all
                                IconButton(
                                    onClick = {
                                        val combined = detectedItems.joinToString("\n") {
                                            "Original: ${it.originalText}\nTradução: ${it.translatedText}"
                                        }
                                        if (combined.isNotBlank()) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Olho de Sauron Tradução", combined))
                                            Toast.makeText(context, "Texto copiado!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar tudo",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(detectedItems) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SophisticatedSurfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(0.8.dp, SophisticatedOutline.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = item.originalText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${targetLanguage.flag} ${item.translatedText.ifBlank { item.originalText }}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                ),
                                                color = GlowLavender,
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(
                                                onClick = {
                                                    ttsManager.speak(
                                                        item.translatedText.ifBlank { item.originalText },
                                                        targetLanguage.code
                                                    )
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = "Ouvir",
                                                    tint = LavenderPrimary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = ListeningCoral,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Aponte a câmara para qualquer texto, cartaz ou livro para traduzir instantaneamente...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    targetLang: String,
    viewModel: TranscriberViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    onDetected: (List<DetectedTextItem>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            val blocks = visionText.textBlocks
            if (blocks.isEmpty()) {
                onDetected(emptyList())
                imageProxy.close()
                return@addOnSuccessListener
            }

            scope.launch(Dispatchers.IO) {
                val items = mutableListOf<DetectedTextItem>()
                for (block in blocks.take(6)) {
                    val raw = block.text.trim()
                    if (raw.length >= 2) {
                        val detection = LanguageAutoDetector.detect(raw)
                        val translationResult = viewModel.speechService.translateText(
                            text = raw,
                            sourceLang = detection.languageCode,
                            targetLang = targetLang,
                            apiKeyOverride = viewModel.customApiKey.value.ifBlank { null }
                        )
                        val translated = translationResult.getOrDefault(raw)
                        items.add(
                            DetectedTextItem(
                                originalText = raw,
                                translatedText = translated,
                                boundingBox = block.boundingBox,
                                sourceLang = detection.languageCode
                            )
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    onDetected(items)
                }
                imageProxy.close()
            }
        }
        .addOnFailureListener {
            imageProxy.close()
        }
}
