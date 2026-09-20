package fr.corentin.biblioscan.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import fr.corentin.biblioscan.AppContainer
import fr.corentin.biblioscan.data.Book
import fr.corentin.biblioscan.ui.common.LambdaViewModelFactory
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onOpenLibrary: () -> Unit) {
    val context = LocalContext.current
    val viewModel: ScanViewModel = viewModel(
        factory = LambdaViewModelFactory { ScanViewModel(AppContainer.repository, AppContainer.lookupService) }
    )
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner un livre") },
                actions = {
                    IconButton(onClick = onOpenLibrary) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = "Ma bibliothèque")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (hasCameraPermission) {
                CameraPreview(
                    paused = uiState !is ScanUiState.Scanning,
                    onIsbnDetected = viewModel::onIsbnDetected
                )
                ScanOverlayGuide(active = uiState is ScanUiState.Scanning)
            } else {
                PermissionRationale(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
            }

            when (val state = uiState) {
                is ScanUiState.Loading -> LoadingCard(isbn = state.isbn)
                is ScanUiState.Found -> ScanResultCard(
                    book = state.book,
                    alreadyOwned = state.alreadyOwned,
                    onAdd = { viewModel.addToCollection(state.book) },
                    onRemove = { viewModel.removeFromCollection(state.book) },
                    onScanNext = viewModel::resumeScanning
                )
                is ScanUiState.NotFound -> NotFoundCard(isbn = state.isbn, onDismiss = viewModel::resumeScanning)
                is ScanUiState.Error -> ErrorCard(message = state.message, onDismiss = viewModel::resumeScanning)
                ScanUiState.Scanning -> Unit
            }
        }
    }
}

@Composable
private fun CameraPreview(paused: Boolean, onIsbnDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analyzer = remember { BarcodeAnalyzer(onIsbnDetected) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(paused) {
        if (!paused) analyzer.resetLastDetected()
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(cameraExecutor, analyzer) }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun ScanOverlayGuide(active: Boolean) {
    if (!active) return
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(120.dp)
                .background(Color.Transparent, RoundedCornerShape(12.dp))
                .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
        )
        Text(
            "Visez le code-barres au dos du livre",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PermissionRationale(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("L'accès à la caméra est nécessaire pour scanner les codes-barres.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Autoriser la caméra") }
    }
}

@Composable
private fun BoxScope.LoadingCard(isbn: String) {
    Card(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text("Recherche du livre $isbn…")
        }
    }
}

@Composable
private fun BoxScope.ScanResultCard(
    book: Book,
    alreadyOwned: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onScanNext: () -> Unit
) {
    Card(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.width(64.dp).height(96.dp).background(Color.LightGray, RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (book.authors.isNotEmpty()) {
                        Text(book.authors.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                    }
                    book.seriesName?.let { series ->
                        val index = book.seriesIndex?.let { if (it == it.toLong().toDouble()) " #${it.toLong()}" else " #$it" }.orEmpty()
                        Text("$series$index", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                OwnershipBadge(alreadyOwned)
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (alreadyOwned) {
                    OutlinedButton(onClick = onRemove, modifier = Modifier.weight(1f)) {
                        Text("Retirer de ma collection")
                    }
                } else {
                    Button(onClick = onAdd, modifier = Modifier.weight(1f)) {
                        Text("Ajouter à ma collection")
                    }
                }
                OutlinedButton(onClick = onScanNext) { Text("Scanner suivant") }
            }
        }
    }
}

@Composable
private fun OwnershipBadge(owned: Boolean) {
    Icon(
        imageVector = if (owned) Icons.Default.CheckCircle else Icons.Default.Cancel,
        contentDescription = if (owned) "Déjà dans ma collection" else "Absent de ma collection",
        tint = if (owned) Color(0xFF2E7D32) else Color(0xFFC62828),
        modifier = Modifier.size(28.dp)
    )
}

@Composable
private fun BoxScope.NotFoundCard(isbn: String, onDismiss: () -> Unit) {
    Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Aucune information trouvée pour l'ISBN $isbn", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Vérifiez votre connexion ou ajoutez ce livre manuellement depuis la bibliothèque.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("OK") }
        }
    }
}

@Composable
private fun BoxScope.ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Erreur", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Réessayer") }
        }
    }
}
