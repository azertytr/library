package fr.corentin.biblioscan.ui.library

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import fr.corentin.biblioscan.AppContainer
import fr.corentin.biblioscan.data.Book
import fr.corentin.biblioscan.data.LibraryEntity
import fr.corentin.biblioscan.data.LibrarySortOption
import fr.corentin.biblioscan.ui.common.LambdaViewModelFactory
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onOpenBook: (String) -> Unit,
    pendingSharedLibraryUri: Uri? = null,
    onSharedLibraryConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: LibraryViewModel = viewModel(
        factory = LambdaViewModelFactory {
            LibraryViewModel(AppContainer.repository, AppContainer.libraryRepository, AppContainer.modePreferences)
        }
    )
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var libraryMenuExpanded by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var showManageLibraries by remember { mutableStateOf(false) }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { viewModel.exportLibrary(context, it) } }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importSharedLibrary(context, it) } }

    LaunchedEffect(pendingSharedLibraryUri) {
        pendingSharedLibraryUri?.let {
            viewModel.importSharedLibrary(context, it)
            onSharedLibraryConsumed()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    if (showManageLibraries) {
        ManageLibrariesDialog(
            libraries = state.libraries,
            activeLibraryId = state.activeLibraryId,
            onDismiss = { showManageLibraries = false },
            onSwitch = viewModel::switchLibrary,
            onCreate = viewModel::createLibrary,
            onRename = viewModel::renameLibrary,
            onDelete = viewModel::deleteLibrary
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Row(
                            modifier = Modifier.clickable { libraryMenuExpanded = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${state.activeLibraryName} (${state.allBooks.size})", maxLines = 1)
                            Icon(Icons.Default.ExpandMore, contentDescription = "Changer de bibliothèque")
                        }
                        DropdownMenu(expanded = libraryMenuExpanded, onDismissRequest = { libraryMenuExpanded = false }) {
                            state.libraries.forEach { library ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            library.name,
                                            fontWeight = if (library.id == state.activeLibraryId) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = { viewModel.switchLibrary(library.id); libraryMenuExpanded = false }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Gérer les bibliothèques…") },
                                onClick = { libraryMenuExpanded = false; showManageLibraries = true }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour au scan")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Trier")
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            SortMenuItem(
                                label = "Titre (séries groupées)",
                                selected = state.sortOption == LibrarySortOption.TITLE,
                                onClick = { viewModel.onSortOptionChange(LibrarySortOption.TITLE); sortMenuExpanded = false }
                            )
                            SortMenuItem(
                                label = "Date d'ajout (récent d'abord)",
                                selected = state.sortOption == LibrarySortOption.DATE_ADDED,
                                onClick = { viewModel.onSortOptionChange(LibrarySortOption.DATE_ADDED); sortMenuExpanded = false }
                            )
                            SortMenuItem(
                                label = "Auteur",
                                selected = state.sortOption == LibrarySortOption.AUTHOR,
                                onClick = { viewModel.onSortOptionChange(LibrarySortOption.AUTHOR); sortMenuExpanded = false }
                            )
                        }
                    }
                    IconButton(onClick = {
                        viewModel.shareLibrary(context) { intent -> context.startActivity(intent) }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Partager la bibliothèque")
                    }
                    Box {
                        IconButton(onClick = { overflowMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Plus d'options")
                        }
                        DropdownMenu(expanded = overflowMenuExpanded, onDismissRequest = { overflowMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Exporter (sauvegarde)") },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                                onClick = { overflowMenuExpanded = false; exportLauncher.launch("biblioscan-export.json") }
                            )
                            DropdownMenuItem(
                                text = { Text("Importer un fichier JSON") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                onClick = { overflowMenuExpanded = false; importLauncher.launch(arrayOf("application/json")) }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Rechercher (titre, auteur, série, ISBN)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (state.allBooks.isEmpty()) {
                EmptyLibrary()
            } else if (state.isGrouped) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.series, key = { "series-" + it.name }) { group ->
                        SeriesHeader(group.name, group.books.size)
                        group.books.forEach { book ->
                            BookRow(book, sortOption = state.sortOption, onClick = { onOpenBook(book.isbn) })
                        }
                    }
                    if (state.standalone.isNotEmpty()) {
                        item(key = "standalone-header") {
                            SeriesHeader("Autres livres", state.standalone.size)
                        }
                        items(state.standalone, key = { it.isbn }) { book ->
                            BookRow(book, sortOption = state.sortOption, onClick = { onOpenBook(book.isbn) })
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.flatBooks, key = { it.isbn }) { book ->
                        BookRow(book, sortOption = state.sortOption, onClick = { onOpenBook(book.isbn) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Votre bibliothèque est vide", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Scannez le code-barres d'un livre pour l'ajouter, ou importez un export JSON existant.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageLibrariesDialog(
    libraries: List<LibraryEntity>,
    activeLibraryId: String,
    onDismiss: () -> Unit,
    onSwitch: (String) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var newLibraryName by remember { mutableStateOf("") }
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    pendingDeleteId?.let { id ->
        val name = libraries.find { it.id == id }?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Supprimer « $name » ?") },
            text = { Text("Tous les livres de cette bibliothèque seront définitivement supprimés.") },
            confirmButton = {
                TextButton(onClick = { onDelete(id); pendingDeleteId = null }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Annuler") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mes bibliothèques") },
        text = {
            Column {
                libraries.forEach { library ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (renamingId == library.id) {
                            OutlinedTextField(
                                value = renameValue,
                                onValueChange = { renameValue = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onRename(library.id, renameValue); renamingId = null }) {
                                Text("OK")
                            }
                        } else {
                            Text(
                                library.name,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSwitch(library.id); onDismiss() },
                                fontWeight = if (library.id == activeLibraryId) FontWeight.Bold else FontWeight.Normal
                            )
                            IconButton(onClick = { renamingId = library.id; renameValue = library.name }) {
                                Icon(Icons.Default.Edit, contentDescription = "Renommer ${library.name}")
                            }
                            IconButton(onClick = { pendingDeleteId = library.id }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer ${library.name}")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newLibraryName,
                        onValueChange = { newLibraryName = it },
                        placeholder = { Text("Nouvelle bibliothèque") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onCreate(newLibraryName); newLibraryName = "" }) {
                        Text("Créer")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
private fun SeriesHeader(name: String, count: Int) {
    Text(
        "$name ($count)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SortMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        onClick = onClick
    )
}

@Composable
private fun BookRow(book: Book, sortOption: LibrarySortOption, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = book.coverUrl,
            contentDescription = null,
            modifier = Modifier.width(44.dp).height(64.dp).background(Color.LightGray, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(book.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            if (book.authors.isNotEmpty()) {
                Text(
                    book.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (sortOption == LibrarySortOption.DATE_ADDED && book.dateAdded > 0) {
                Text(
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(book.dateAdded)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        book.seriesIndex?.let {
            val label = if (it == it.toLong().toDouble()) "#${it.toLong()}" else "#$it"
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
