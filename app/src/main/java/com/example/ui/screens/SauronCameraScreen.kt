package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.ListeningCoral
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private const val TAG = "SauronCamera"
private const val PERSISTENCE_DURATION_MS = 5000L // Keep translated words visible for 5 seconds

data class SauronTrackedWord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalText: String,
    val translatedText: String,
    val normLeft: Float,
    val normTop: Float,
    val normRight: Float,
    val normBottom: Float,
    val lastSeenMs: Long = System.currentTimeMillis(),
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

    // Map of persistent tracked words
    val trackedWordsMap = remember { mutableStateMapOf<String, SauronTrackedWord>() }
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var targetLanguage by remember { mutableStateOf(SupportedLanguages.findByCode("pt-PT") ?: SupportedLanguages.ALL.first()) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Timer loop to cleanly purge expired words after 5 seconds
    LaunchedEffect(isFrozen) {
        while (true) {
            delay(300)
            if (!isFrozen) {
                currentTimeMs = System.currentTimeMillis()
                val expiredKeys = trackedWordsMap.filter { (key, item) ->
                    (currentTimeMs - item.lastSeenMs) > PERSISTENCE_DURATION_MS
                }.keys
                expiredKeys.forEach { trackedWordsMap.remove(it) }
            }
        }
    }

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
                text = "O Olho de Sauron necessita de acesso à câmara para ler e traduzir texto impresso ou digital em tempo real diretamente por cima de cada palavra.",
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

    // Active words currently within 5s persistence window
    val activeVisibleWords = remember(currentTimeMs, trackedWordsMap.values.toList(), isFrozen) {
        if (isFrozen) {
            trackedWordsMap.values.toList()
        } else {
            trackedWordsMap.values.filter { (currentTimeMs - it.lastSeenMs) <= PERSISTENCE_DURATION_MS }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // 1. Camera View
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
                            processImageProxyFocused(
                                imageProxy = imageProxy,
                                recognizer = textRecognizer,
                                targetLang = targetLanguage.code,
                                viewModel = viewModel,
                                scope = scope,
                                onNewWordsDetected = { newWords ->
                                    val now = System.currentTimeMillis()
                                    newWords.forEach { item ->
                                        val existingKey = trackedWordsMap.keys.firstOrNull { k ->
                                            val existing = trackedWordsMap[k]
                                            existing != null && (
                                                existing.originalText.equals(item.originalText, ignoreCase = true) ||
                                                (kotlin.math.abs(existing.normLeft - item.normLeft) < 0.08f &&
                                                 kotlin.math.abs(existing.normTop - item.normTop) < 0.08f)
                                            )
                                        }
                                        if (existingKey != null) {
                                            val old = trackedWordsMap[existingKey]!!
                                            trackedWordsMap[existingKey] = old.copy(
                                                normLeft = (old.normLeft * 0.4f + item.normLeft * 0.6f),
                                                normTop = (old.normTop * 0.4f + item.normTop * 0.6f),
                                                normRight = (old.normRight * 0.4f + item.normRight * 0.6f),
                                                normBottom = (old.normBottom * 0.4f + item.normBottom * 0.6f),
                                                lastSeenMs = now
                                            )
                                        } else {
                                            trackedWordsMap[item.id] = item.copy(lastSeenMs = now)
                                        }
                                    }
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

        // Laser scan line animation
        val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
        val laserProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "laser_y"
        )

        // 2. Focused Scan Area Viewfinder & Dimming Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = ListeningCoral
            val strokeWidth = 3.dp.toPx()
            val cornerLength = 32.dp.toPx()

            // Focused Central Reticle: Width 65%, Height 28%
            val reticleW = size.width * 0.68f
            val reticleH = size.height * 0.28f
            val reticleLeft = (size.width - reticleW) / 2f
            val reticleTop = size.height * 0.32f
            val reticleRight = reticleLeft + reticleW
            val reticleBottom = reticleTop + reticleH

            // Draw 4 distinct corner brackets
            // Top-left
            drawLine(strokeColor, Offset(reticleLeft, reticleTop), Offset(reticleLeft + cornerLength, reticleTop), strokeWidth)
            drawLine(strokeColor, Offset(reticleLeft, reticleTop), Offset(reticleLeft, reticleTop + cornerLength), strokeWidth)
            // Top-right
            drawLine(strokeColor, Offset(reticleRight, reticleTop), Offset(reticleRight - cornerLength, reticleTop), strokeWidth)
            drawLine(strokeColor, Offset(reticleRight, reticleTop), Offset(reticleRight, reticleTop + cornerLength), strokeWidth)
            // Bottom-left
            drawLine(strokeColor, Offset(reticleLeft, reticleBottom), Offset(reticleLeft + cornerLength, reticleBottom), strokeWidth)
            drawLine(strokeColor, Offset(reticleLeft, reticleBottom), Offset(reticleLeft, reticleBottom - cornerLength), strokeWidth)
            // Bottom-right
            drawLine(strokeColor, Offset(reticleRight, reticleBottom), Offset(reticleRight - cornerLength, reticleBottom), strokeWidth)
            drawLine(strokeColor, Offset(reticleRight, reticleBottom), Offset(reticleRight, reticleBottom - cornerLength), strokeWidth)

            // Scanning laser line moving inside the reticle
            if (!isFrozen) {
                val laserY = reticleTop + (reticleH * laserProgress)
                drawLine(
                    color = GlowLavender.copy(alpha = 0.85f),
                    start = Offset(reticleLeft + 4.dp.toPx(), laserY),
                    end = Offset(reticleRight - 4.dp.toPx(), laserY),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }

        // Label above the focused scan reticle
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (screenHeightPx * 0.28f / context.resources.displayMetrics.density).dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(0.8.dp, ListeningCoral.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(ListeningCoral)
                    )
                    Text(
                        text = "ÁREA DE LEITURA FOCADA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }

        // 3. AR OVERLAY: Translations positioned DIRECTLY ON TOP OF DETECTED WORDS
        activeVisibleWords.forEach { word ->
            val xDp = ((word.normLeft * screenWidthPx) / context.resources.displayMetrics.density).dp
            val yDp = ((word.normTop * screenHeightPx) / context.resources.displayMetrics.density).dp

            // AR Translated Tag directly over the word
            Box(
                modifier = Modifier
                    .offset(x = xDp.coerceAtLeast(8.dp), y = (yDp - 14.dp).coerceAtLeast(70.dp))
                    .clickable {
                        ttsManager.speak(word.translatedText, targetLanguage.code)
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.88f),
                    border = BorderStroke(1.2.dp, GlowLavender),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = targetLanguage.flag,
                            fontSize = 11.sp
                        )
                        Text(
                            text = word.translatedText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = GlowLavender
                        )
                    }
                }
            }
        }

        // 4. TOP HUD (Controls & Language Selector)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.88f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
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
                            text = if (isFrozen) "Imagem Fixada (Pausa)" else "Tradução Sobreposta (AR)",
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isFrozen) AmberGold.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (isFrozen) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Fixar imagem",
                            tint = if (isFrozen) AmberGold else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Target Language Selector Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Traduzir p/:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
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
                            color = if (isSelected) LavenderPrimary else Color.Black.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, if (isSelected) LavenderPrimary else Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable {
                                targetLanguage = meta
                                trackedWordsMap.clear() // Clear cache on language change
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

        // 5. BOTTOM PERSISTENT RESULTS HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.98f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (activeVisibleWords.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = SophisticatedSurface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, GlowLavender.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
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
                                    text = "${activeVisibleWords.size} Palavras/Frases Traduzidas",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Speak entire translation
                                IconButton(
                                    onClick = {
                                        val combined = activeVisibleWords.joinToString(" ") { it.translatedText }
                                        if (combined.isNotBlank()) {
                                            ttsManager.speak(combined, targetLanguage.code)
                                            Toast.makeText(context, "A ler tradução...", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Ouvir",
                                        tint = LavenderPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Copy all
                                IconButton(
                                    onClick = {
                                        val combined = activeVisibleWords.joinToString("\n") {
                                            "Original: ${it.originalText} -> Tradução: ${it.translatedText}"
                                        }
                                        if (combined.isNotBlank()) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Olho de Sauron Tradução", combined))
                                            Toast.makeText(context, "Traduções copiadas!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar tudo",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(activeVisibleWords) { item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SophisticatedSurfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(0.6.dp, SophisticatedOutline.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.originalText,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color.LightGray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${targetLanguage.flag} ${item.translatedText}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = GlowLavender,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                ttsManager.speak(item.translatedText, targetLanguage.code)
                                            },
                                            modifier = Modifier.size(26.dp)
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
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = ListeningCoral,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Aponte o retículo central para a palavra ou frase que deseja traduzir...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxyFocused(
    imageProxy: ImageProxy,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    targetLang: String,
    viewModel: TranscriberViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    onNewWordsDetected: (List<SauronTrackedWord>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val rotation = imageProxy.imageInfo.rotationDegrees
    val image = InputImage.fromMediaImage(mediaImage, rotation)

    val imageWidth = if (rotation == 90 || rotation == 270) imageProxy.height.toFloat() else imageProxy.width.toFloat()
    val imageHeight = if (rotation == 90 || rotation == 270) imageProxy.width.toFloat() else imageProxy.height.toFloat()

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            val blocks = visionText.textBlocks
            if (blocks.isEmpty()) {
                imageProxy.close()
                return@addOnSuccessListener
            }

            scope.launch(Dispatchers.IO) {
                val newWordsList = mutableListOf<SauronTrackedWord>()

                // Focused scan area bounds: normalized [0.15 .. 0.85] width, [0.30 .. 0.62] height
                val focusMinX = 0.14f
                val focusMaxX = 0.86f
                val focusMinY = 0.28f
                val focusMaxY = 0.64f

                for (block in blocks) {
                    for (line in block.lines) {
                        val box = line.boundingBox ?: continue
                        val normLeft = (box.left / imageWidth).coerceIn(0f, 1f)
                        val normTop = (box.top / imageHeight).coerceIn(0f, 1f)
                        val normRight = (box.right / imageWidth).coerceIn(0f, 1f)
                        val normBottom = (box.bottom / imageHeight).coerceIn(0f, 1f)

                        val centerX = (normLeft + normRight) / 2f
                        val centerY = (normTop + normBottom) / 2f

                        // Only capture words whose center falls inside the central focus reticle!
                        if (centerX in focusMinX..focusMaxX && centerY in focusMinY..focusMaxY) {
                            val rawText = line.text.trim()
                            if (rawText.length >= 2) {
                                val detection = LanguageAutoDetector.detect(rawText)
                                val translationResult = viewModel.speechService.translateText(
                                    text = rawText,
                                    sourceLang = detection.languageCode,
                                    targetLang = targetLang,
                                    apiKeyOverride = viewModel.customApiKey.value.ifBlank { null }
                                )
                                val translated = translationResult.getOrDefault(rawText)

                                newWordsList.add(
                                    SauronTrackedWord(
                                        originalText = rawText,
                                        translatedText = translated,
                                        normLeft = normLeft,
                                        normTop = normTop,
                                        normRight = normRight,
                                        normBottom = normBottom,
                                        sourceLang = detection.languageCode
                                    )
                                )
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    onNewWordsDetected(newWordsList)
                }
                imageProxy.close()
            }
        }
        .addOnFailureListener {
            imageProxy.close()
        }
}
