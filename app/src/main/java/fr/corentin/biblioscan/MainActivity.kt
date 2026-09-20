package fr.corentin.biblioscan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fr.corentin.biblioscan.ui.navigation.AppNavHost
import fr.corentin.biblioscan.ui.theme.BiblioScanTheme

class MainActivity : ComponentActivity() {

    private var sharedLibraryUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedLibraryUri = uriFrom(intent)
        setContent {
            BiblioScanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        sharedLibraryUri = sharedLibraryUri,
                        onSharedLibraryConsumed = { sharedLibraryUri = null }
                    )
                }
            }
        }
    }

    /** singleTask means a file opened while the app is already running arrives here, not in a new onCreate. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        uriFrom(intent)?.let { sharedLibraryUri = it }
    }

    private fun uriFrom(intent: Intent?): Uri? =
        intent?.data?.takeIf { intent.action == Intent.ACTION_VIEW }
}
