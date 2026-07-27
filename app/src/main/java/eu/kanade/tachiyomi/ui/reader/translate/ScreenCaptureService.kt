package eu.kanade.tachiyomi.ui.reader.translate

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

class ScreenCaptureService : Service() {

    override fun onCreate() {
        super.onCreate()
        logcat { "ScreenCaptureService: onCreate" }
        val notification = notificationBuilder(Notifications.CHANNEL_SCREEN_CAPTURE) {
            setSmallIcon(R.drawable.ic_mihon)
            setContentTitle("Screen Capture Active")
            setContentText("Running screen capture for translation")
            setOngoing(true)
            setAutoCancel(false)
            setShowWhen(false)
            setSilent(true)
        }.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        logcat { "ScreenCaptureService: foreground started" }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logcat { "ScreenCaptureService: onStartCommand" }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        logcat { "ScreenCaptureService: onDestroy" }
    }

    companion object {
        private const val NOTIFICATION_ID = Notifications.ID_SCREEN_CAPTURE

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ScreenCaptureService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenCaptureService::class.java))
        }
    }
}
