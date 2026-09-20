package fr.corentin.biblioscan.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import fr.corentin.biblioscan.AppContainer
import fr.corentin.biblioscan.ui.common.LambdaViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(isbn: String, onBack: () -> Unit) {
    val viewModel: BookDetailViewModel = viewModel(
        key = isbn,
        factory = LambdaViewModelFactory { BookDetailViewModel(AppContainer.repository, isbn) }
    )
    val book by viewModel.book.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détail du livre") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    book?.let {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val current = book
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Livre introuvable")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            AsyncImage(
                model = current.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .width(140.dp)
                    .height(200.dp)
                    .background(Color.LightGray, RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.height(16.dp))
            Text(current.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            current.subtitle?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            if (current.authors.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(current.authors.joinToString(", "), style = MaterialTheme.typography.bodyLarge)
            }
            current.seriesName?.let { series ->
                Spacer(Modifier.height(8.dp))
                val index = current.seriesIndex?.let {
                    if (it == it.toLong().toDouble()) " — Tome ${it.toLong()}" else " — Tome $it"
                }.orEmpty()
                AssistChip(onClick = {}, label = { Text("$series$index") })
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            DetailRow("ISBN", current.isbn)
            current.publisher?.let { DetailRow("Éditeur", it) }
            current.publishedDate?.let { DetailRow("Date de publication", it) }
            current.pageCount?.let { DetailRow("Pages", it.toString()) }
            if (current.categories.isNotEmpty()) DetailRow("Catégories", current.categories.joinToString(", "))

            current.description?.let {
                Spacer(Modifier.height(16.dp))
                Text("Résumé", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Retirer ce livre ?") },
                text = { Text("« ${current.title} » sera retiré de votre collection locale.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.delete(current, onBack) }) { Text("Retirer") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") }
                }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(160.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
