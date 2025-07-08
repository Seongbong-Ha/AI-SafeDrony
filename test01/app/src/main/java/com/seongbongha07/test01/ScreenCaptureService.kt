package com.seongbongha07.test01

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.webrtc.EglBase
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var capturer: ScreenCapturerAndroid? = null

    private val eglBase by lazy { EglBase.create() }

    override fun onCreate() {
        super.onCreate()

        // 알림 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "screen_capture"
            val channel = NotificationChannel(
                channelId,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            val notif = NotificationCompat.Builder(this, channelId)
                .setContentTitle("화면 캡처 중")
                .setContentText("드론 화면이 송출되고 있습니다.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

            // Q 이상에서는 ServiceInfo 타입 지정
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    1,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(1, notif)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // resultCode와 data 받아오기
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY

        // MediaProjection 생성
        val mgr = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = mgr.getMediaProjection(resultCode, data)

        // WebRTC VideoSource 생성 예시
        val factory = buildPeerFactory(eglBase, this)
        val videoSource: VideoSource = factory.createVideoSource(false)

        capturer = ScreenCapturerAndroid(data, object : MediaProjection.Callback() {})
        val helper = SurfaceTextureHelper.create("ScreenCap", eglBase.eglBaseContext)

        capturer!!.initialize(helper, this, videoSource.capturerObserver)
        capturer!!.startCapture(720, 1280, 15)

        // TODO: 여기서 PeerConnection에 videoSource를 연결해서 Offer 송출
        // (SafetyScanActivity에서 하던 로직과 동일하게 작성)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        capturer?.stopCapture()
        mediaProjection?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
