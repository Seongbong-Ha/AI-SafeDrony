package com.seongbongha07.test01

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.webrtc.*

class AdminMainActivity : ComponentActivity() {

    private val eglBase by lazy { EglBase.create() }
    val dbRoom = Firebase.database.reference.child("room/demo")


    private var peer: PeerConnection? = null
    private lateinit var surfaceView: SurfaceViewRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase 초기화
        FirebaseApp.initializeApp(this)

        // Compose UI
        setContent {
            AdminScreen()
        }
    }

    @Composable
    fun AdminScreen() {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("대시보드", "라이브 영상")

        Column {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> DashboardTab()
                1 -> LiveVideoTab()
            }
        }
    }

    @Composable
    fun DashboardTab() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("관리자 대시보드입니다.", fontSize = 20.sp)
            // 다른 관리자 기능들...
        }
    }

    @Composable
    fun LiveVideoTab() {
        AndroidView(factory = { ctx ->
            Log.d("FirebaseTest", "LiveVideoTab: AndroidView initialized")
            surfaceView = SurfaceViewRenderer(ctx).apply {
                init(eglBase.eglBaseContext, null)
            }
            startWebRtc()
            surfaceView
        }, modifier = Modifier.fillMaxSize())
    }


    private fun startWebRtc() {
        val factory = buildPeerFactory(eglBase, this)

        peer = factory.createPeerConnection(rtcConfig(), object : PeerConnection.Observer {
            override fun onIceCandidate(c: IceCandidate) {
                val ref = dbRoom.child("candidates").push()
                Log.d("FirebaseTest", "Writing ICE candidate: ${c.sdp}")
                ref.setValue(c.sdp)
                    .addOnSuccessListener { Log.d("FirebaseTest", "ICE candidate write success!") }
                    .addOnFailureListener { e ->
                        Log.e(
                            "FirebaseTest",
                            "ICE candidate write failed",
                            e
                        )
                    }
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<MediaStream>?) {
                (receiver?.track() as? VideoTrack)?.addSink(surfaceView)
            }

            override fun onSignalingChange(s: PeerConnection.SignalingState) {}
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>) {}
            override fun onAddStream(p0: MediaStream) {}
            override fun onRemoveStream(p0: MediaStream) {}
            override fun onDataChannel(p0: DataChannel) {}
            override fun onRenegotiationNeeded() {}
        })

        // Offer 받아서 Answer
        dbRoom.child("offer").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdp = snapshot.getValue(String::class.java) ?: return
                Log.d("FirebaseTest", "Received offer SDP: $sdp")
                val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
                peer!!.setRemoteDescription(SdpObserverEmpty(), offer)

                peer!!.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answer: SessionDescription) {
                        Log.d("FirebaseTest", "Answer created: ${answer.description}")
                        peer!!.setLocalDescription(SdpObserverEmpty(), answer)
                        dbRoom.child("answer").setValue(answer.description)
                            .addOnSuccessListener { Log.d("FirebaseTest", "Answer write success!") }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "FirebaseTest",
                                    "Answer write failed",
                                    e
                                )
                            }
                    }

                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {
                        Log.e("FirebaseTest", "Answer creation failed: $p0")
                    }

                    override fun onSetFailure(p0: String?) {}
                }, MediaConstraints())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseTest", "Offer read cancelled: $error")
            }
        })

        dbRoom.child("candidates").addChildEventListener(
            simpleChildListener { c ->
                Log.d("FirebaseTest", "Received remote candidate: $c")
                peer?.addIceCandidate(IceCandidate(null, -1, c))
            }
        )
    }
}
