package eu.kanade.tachiyomi.ui.reader.translate

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder

class ScreenCaptureService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NOTIFICATION_ID,
            notificationBuilder(Notifications.CHANNEL_COMMON) {
                setSmallIcon(R.drawable.ic_mihon)
                setContentTitle("Screen Capture")
                setContentText("Running screen capture for translation")
                setOngoing(true)
                setAutoCancel(false)
                setShowWhen(false)
            }.build(),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ScreenCaptureService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenCaptureService::class.java))
        }
    }
}
