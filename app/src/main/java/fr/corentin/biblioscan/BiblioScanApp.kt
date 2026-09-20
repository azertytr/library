package fr.corentin.biblioscan

import android.app.Application

class BiblioScanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
