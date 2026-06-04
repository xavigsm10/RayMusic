package com.mrtdk.liquid_glass.playback

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.mrtdk.liquid_glass.R

class ExoDownloadService : DownloadService(
    NOTIFICATION_ID,
    1000L,
    CHANNEL_ID,
    R.string.descargando_ellipsis,
    0
) {
    private lateinit var downloadUtil: DownloadUtil

    override fun onCreate() {
        downloadUtil = DownloadUtil.getInstance(this)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelName = getString(R.string.descargando_ellipsis)
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                channelName,
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de descarga"
            }
            nm.createNotificationChannel(channel)
        }
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == REMOVE_ALL_PENDING_DOWNLOADS) {
            downloadManager.currentDownloads.forEach { download ->
                downloadManager.removeDownload(download.request.id)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun getDownloadManager(): DownloadManager = downloadUtil.downloadManager

    override fun getScheduler(): Scheduler = PlatformScheduler(this, JOB_ID)

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        val notification = downloadUtil.downloadNotificationHelper.buildProgressNotification(
            this,
            R.drawable.download,
            null,
            if (downloads.size == 1) {
                try {
                    // Try to get title from request data (split custom payload)
                    val payload = Util.fromUtf8Bytes(downloads[0].request.data)
                    payload.split("||").firstOrNull() ?: payload
                } catch (e: Exception) {
                    downloads[0].request.id
                }
            } else {
                resources.getQuantityString(R.plurals.n_song, downloads.size, downloads.size)
            },
            downloads,
            notMetRequirements
        )
        
        return Notification.Builder.recoverBuilder(this, notification)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    getString(android.R.string.cancel),
                    PendingIntent.getService(
                        this,
                        0,
                        Intent(this, ExoDownloadService::class.java).setAction(
                            REMOVE_ALL_PENDING_DOWNLOADS
                        ),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
            ).build()
    }

    class TerminalStateNotificationHelper(
        private val context: Context,
        private val notificationHelper: DownloadNotificationHelper,
        private var nextNotificationId: Int,
    ) : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) {
            if (download.state == Download.STATE_FAILED) {
                val title = try {
                    val payload = Util.fromUtf8Bytes(download.request.data)
                    payload.split("||").firstOrNull() ?: payload
                } catch (e: Exception) {
                    download.request.id
                }
                val notification = notificationHelper.buildDownloadFailedNotification(
                    context,
                    R.drawable.download,
                    null,
                    title
                )
                NotificationUtil.setNotification(context, nextNotificationId++, notification)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 999
        const val JOB_ID = 999
        const val REMOVE_ALL_PENDING_DOWNLOADS = "REMOVE_ALL_PENDING_DOWNLOADS"
    }
}
