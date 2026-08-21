package com.mrtdk.liquid_glass

import android.app.Application
import android.content.Context
import com.echo.innertube.YouTube
import com.echo.innertube.YouTubeExtractor
import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        
        LibraryManager.init(this)
        YouTubeExtractor.cacheDir = cacheDir
        
        appScope.launch {
            try {
                YouTubeExtractor.ensureInitialized()
            } catch (_: Exception) {}
            
            try {
                if (YouTube.visitorData == null) {
                    YouTube.visitorData = YouTube.visitorData().getOrNull()
                }
            } catch (_: Exception) {}
        }
    }
}
