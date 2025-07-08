package com.seongbongha07.test01

/*────────────────── import ──────────────────*/
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.seongbongha07.test01.template.HazardTemplate
import com.seongbongha07.test01.tts.TTSManager
import org.webrtc.*
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.exp

/*────────────────── Activity ──────────────────*/
@RequiresApi(21)
class SafetyScanActivity : ComponentActivity() {

    private val CAMERA_CODE = 1001
    private lateinit var mediaProjectionMgr: MediaProjectionManager
    private val eglBase by lazy { EglBase.create() }

    /* ───────── Activity LifeCycle ───────── */
    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)

        mediaProjectionMgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

        if (checkSelfPermission(Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) launchUi()
        else ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA), CAMERA_CODE
        )
    }

    private fun launchUi() = setContent { SafetyScanScreen(::toggleLive) }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) launchUi() else finish()
    }

    /*────────────────── LIVE (screen share) ──────────────────*/
    private var liveOn = false
    private val projLauncher =
        registerForActivityResult(MediaProjectionIntentContract()) { intent ->
            if (intent == null) {
                Toast.makeText(this, "화면 캡처 권한 거부", Toast.LENGTH_SHORT).show()
                liveOn = false
            } else {
                // Foreground Service 시작
                val svcIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra("data", intent)
                }
                ContextCompat.startForegroundService(this, svcIntent)
                Toast.makeText(this, "LIVE 시작", Toast.LENGTH_SHORT).show()
            }
        }

    private fun toggleLive() {
        if (liveOn) stopLive()
        else projLauncher.launch(mediaProjectionMgr.createScreenCaptureIntent())
        liveOn = !liveOn
    }

    private fun stopLive() {
        stopService(Intent(this, ScreenCaptureService::class.java))
        Toast.makeText(this, "LIVE 종료", Toast.LENGTH_SHORT).show()
    }
}

/*────────────────── UI ──────────────────*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScanScreen(onLiveToggle: () -> Unit) {

    val ctx        = LocalContext.current
    val ttsManager = remember { TTSManager(ctx) }

    var helmetOk   by remember { mutableStateOf<Boolean?>(null) }
    var lastSpoken by remember { mutableStateOf<Boolean?>(null) }
    var captureBmp by remember { mutableStateOf<Bitmap?>(null) }
    var lastSaveMs by remember { mutableStateOf(0L) }
    var prevOk     by remember { mutableStateOf<Boolean?>(null) }

    var showSheet  by remember { mutableStateOf(false) }
    var liveOn     by remember { mutableStateOf(false) }

    LaunchedEffect(helmetOk) {
        helmetOk?.let { ok ->
            if (ok != lastSpoken) {
                ttsManager.speakOut(
                    if (ok) "헬멧을 착용했습니다." else "헬멧을 착용하지 않았습니다."
                )
                lastSpoken = ok
            }
            val now = System.currentTimeMillis()
            val becameNo = (prevOk == true || prevOk == null) && ok == false
            if (becameNo && now - lastSaveMs > 5_000 && captureBmp != null) {
                saveCapture(ctx, captureBmp!!, "helmet")
                lastSaveMs = now
                Toast.makeText(ctx, "⚠️ 헬멧 미착용 자동 저장", Toast.LENGTH_SHORT).show()
            }
            prevOk = ok
        }
    }

    Scaffold(topBar = { TopBar() }, bottomBar = { BottomNav() }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Text("Safety Scan", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            CameraPreview(
                onResult = { ok, bmp -> helmetOk = ok; captureBmp = bmp },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.height(24.dp))
            Text(
                when (helmetOk) {
                    null  -> "Detecting…"
                    true  -> "Helmet detected – all good!"
                    false -> "Helmet NOT detected!"
                },
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                color = when (helmetOk) {
                    true  -> Color(0xFF4CAF50)
                    false -> Color.Red
                    else  -> Color.Black
                }
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { liveOn = !liveOn; onLiveToggle() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (liveOn) Color.Red else Color(0xFF3B82F6),
                    contentColor = Color.White
                )
            ) { Text(if (liveOn) "STOP LIVE" else "START LIVE") }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (captureBmp == null)
                        Toast.makeText(ctx, "인식 중입니다.", Toast.LENGTH_SHORT).show()
                    else showSheet = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Scan (수동 캡처)") }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { ctx.startActivity(Intent(ctx, ImagePickActivity::class.java)) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                )
            ) { Text("저장된 사진 보기") }
        }

        if (showSheet) {
            ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                Text(
                    "위험 유형 선택",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                HazardTemplate.values().forEach { tpl ->
                    ListItem(
                        headlineContent = { Text(tpl.title) },
                        supportingContent = { Text(tpl.desc) },
                        modifier = Modifier.clickable {
                            saveCapture(ctx, captureBmp!!, tpl.prefix)
                            showSheet = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNav() {
    val ctx = LocalContext.current
    NavigationBar(containerColor = Color.White) {

        /* Home */
        NavigationBarItem(
            selected = false,
            onClick = { ctx.startActivity(Intent(ctx, MainActivity::class.java)) },
            icon  = { Icon(painterResource(R.drawable.home_24px), contentDescription = "Home") },
            label = { Text("Home") }
        )

        /* Scan (현재 화면) */
        NavigationBarItem(
            selected = true,
            onClick = { /* no-op */ },
            icon  = { Icon(painterResource(R.drawable.photo_scan_24px), contentDescription = "Scan") },
            label = { Text("Scan") }
        )

        /* Report */
        NavigationBarItem(
            selected = false,
            onClick = { ctx.startActivity(Intent(ctx, WorkerboardActivity::class.java)) },
            icon  = { Icon(painterResource(R.drawable.report_24px), contentDescription = "Report") },
            label = { Text("Report") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    val ctx = LocalContext.current
    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = { (ctx as? Activity)?.finish() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        title = { Text("Safedrony", fontWeight = FontWeight.Bold) },
        actions = {
            IconButton(onClick = {
                ctx.startActivity(Intent(ctx, DashboardActivity::class.java))
            }) {
                Icon(
                    painterResource(R.drawable.settings_24px),
                    contentDescription = "Settings"
                )
            }
        }
    )
}

/*────────────────── Save Helper ──────────────────*/
private fun saveCapture(ctx: Context, bm: Bitmap, prefix: String) {
    val dir = File(ctx.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { bm.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    Toast.makeText(ctx, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show()
}

/*────────────────── Camera Preview + ML ──────────────────*/
@Composable
private fun CameraPreview(
    onResult: (Boolean, Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val interpreter by remember {
        mutableStateOf(
            Interpreter(FileUtil.loadMappedFile(context, "best_float32.tflite"))
        )
    }
    val exec = remember { Executors.newSingleThreadExecutor() }

    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        ProcessCameraProvider.getInstance(ctx).addListener({
            val provider = ProcessCameraProvider.getInstance(ctx).get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(exec) { proxy ->
                processFrame(interpreter, proxy) { ok, bm ->
                    Handler(Looper.getMainLooper()).post { onResult(ok, bm) }
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(
                owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
        }, ContextCompat.getMainExecutor(ctx))
        previewView
    }, modifier = modifier)
}

private fun processFrame(
    interpreter: Interpreter,
    imageProxy: ImageProxy,
    cb: (Boolean, Bitmap) -> Unit
) {
    try {
        val rot = imageProxy.imageInfo.rotationDegrees
        val bm0 = imageProxy.toBitmap()
        val bm = if (rot != 0) Bitmap.createBitmap(
            bm0, 0, 0, bm0.width, bm0.height,
            Matrix().apply { postRotate(rot.toFloat()) }, true
        ) else bm0

        val input = bitmapToNCHW(bm)
        val out = Array(1) { Array(300) { FloatArray(6) } }
        interpreter.run(input, out)

        var helmet = false
        out[0].forEach { b ->
            val conf = sigmoid(b[4])
            val w = b[2]; val h = b[3]
            if (conf >= 0.55f && w * h >= 0.02f && w <= 0.9f && h <= 0.9f) helmet = true
        }
        cb(helmet, bm)
    } finally {
        imageProxy.close()
    }
}

/*────────────────── Util ──────────────────*/
private fun bitmapToNCHW(bm: Bitmap): ByteBuffer {
    val w = 640; val h = 640; val c = 3
    val buf = ByteBuffer.allocateDirect(c * w * h * 4).order(ByteOrder.nativeOrder())
    val resized = Bitmap.createScaledBitmap(bm, w, h, true)
    val pix = IntArray(w * h); resized.getPixels(pix, 0, w, 0, 0, w, h)
    for (ch in 0 until c)
        for (p in pix) {
            val v = when (ch) {
                0 -> ((p shr 16) and 0xFF) / 255f
                1 -> ((p shr 8) and 0xFF) / 255f
                else -> (p and 0xFF) / 255f
            }
            buf.putFloat(v)
        }
    return buf
}

private fun sigmoid(x: Float) = 1f / (1f + exp(-x))

/*────────────────── MediaProjection Contract ──────────────────*/
class MediaProjectionIntentContract
    : ActivityResultContract<Intent, Intent?>() {

    override fun createIntent(context: Context, input: Intent) = input

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? =
        if (resultCode == Activity.RESULT_OK) intent else null
}

/*────────────────── WebRTC Helpers ──────────────────*/
fun buildPeerFactory(egl: EglBase, ctx: Context): PeerConnectionFactory {
    PeerConnectionFactory.initialize(
        PeerConnectionFactory.InitializationOptions.builder(ctx)
            .setEnableInternalTracer(false).createInitializationOptions()
    )
    return PeerConnectionFactory.builder()
        .setVideoEncoderFactory(
            DefaultVideoEncoderFactory(egl.eglBaseContext, true, true)
        )
        .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
        .createPeerConnectionFactory()
}

fun rtcConfig() = PeerConnection.RTCConfiguration(
    mutableListOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
            .createIceServer()
    )
).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }

/* 빈 SdpObserver */
open class SdpObserverEmpty : SdpObserver {
    override fun onCreateSuccess(d: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
}

/* ChildEventListener 간단 래퍼 – 21.x 대응 */
fun simpleChildListener(cb: (String) -> Unit) =
    object : com.google.firebase.database.ChildEventListener {

        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            snapshot.getValue(String::class.java)?.let(cb)
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildMoved  (snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onCancelled   (error: DatabaseError) {}
    }

