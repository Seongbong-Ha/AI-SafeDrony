package com.seongbongha07.test01

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.seongbongha07.test01.tts.TTSManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

/**  단일 클래스-모델(best_float32.tflite, output:[1,300,6]) +
 *   박스 “유/무”로만 헬멧 착용 여부를 판단한다.                       */
class TestModelActivity : ComponentActivity() {

    /* ───────── 필드 ───────── */
    private lateinit var interpreter: Interpreter
    private lateinit var ttsManager: TTSManager
    private var lastSpoken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* TTS 초기화 */
        ttsManager = TTSManager(this)

        /* Compose 상태 – 추론 결과 박스들 */
        val detections = mutableStateListOf<DetectionResult>()

        /* ───────── UI ───────── */
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    /* 테스트 이미지 한 장 */
                    val bitmap = remember {
                        BitmapFactory.decodeResource(resources, R.drawable.test1)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                        /* 가장 신뢰도 높은 박스 하나를 빨간 테두리로 표시 */
                        Canvas(Modifier.fillMaxSize()) {
                            detections.maxByOrNull { it.confidence }?.let { det ->
                                drawRect(
                                    color = Color.Red,
                                    topLeft = Offset(det.left * size.width, det.top * size.height),
                                    size = androidx.compose.ui.geometry.Size(
                                        det.width * size.width,
                                        det.height * size.height
                                    ),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    /* ───── 메시지는 ‘박스가 있느냐 없느냐’로 즉시 계산 ───── */
                    val message =
                        if (detections.isEmpty())
                            "헬멧을 착용하지 않았습니다."
                        else
                            "헬멧을 착용한 상태입니다."

                    Text(message)

                    /* 메시지가 달라질 때마다 TTS 발화(중복 방지) */
                    LaunchedEffect(message) {
                        if (message != lastSpoken) {
                            speakWithRetry(message)
                            lastSpoken = message
                        }
                    }
                }
            }
        }

        /* ───────── 모델 로드 & 추론 ───────── */
        lifecycleScope.launch(Dispatchers.IO) {
            /* 1) Interpreter 생성 */
            interpreter = Interpreter(
                FileUtil.loadMappedFile(this@TestModelActivity, "best_float32.tflite")
            )

            /* 2) 입력 준비 */
            val inputBitmap = BitmapFactory.decodeResource(resources, R.drawable.test1)
            val inputTensor = bitmapToNCHW(inputBitmap)

            /* 3) 출력 버퍼 shape = [1,300,6]  */
            val output = Array(1) { Array(300) { FloatArray(6) } }
            interpreter.run(inputTensor, output)

            /* 4) 박스 필터링 */
            val results = mutableListOf<DetectionResult>()
            for (i in 0 until 300) {
                val o = output[0][i]
                val cx = o[0]; val cy = o[1]; val w = o[2]; val h = o[3]
                val objScore = sigmoid(o[4])          // 객체 확률

                if (objScore >= 0.7f && w <= 0.7f && h <= 0.7f) {
                    results += DetectionResult(
                        className = "Helmet",          // 단일 클래스 모델
                        confidence = objScore,
                        left  = cx - w / 2f,
                        top   = cy - h / 2f,
                        width = w,
                        height = h
                    )
                }
            }

            /* 5) Compose 상태 갱신 */
            withContext(Dispatchers.Main) {
                detections.clear()
                detections.addAll(results)
            }
        }
    }

    /* ───────── TTS 재시도: 0.5s × 6 = 최대 3초 ───────── */
    private fun speakWithRetry(text: String, attempt: Int = 0) {
        if (ttsManager.isReady) {
            ttsManager.speakOut(text)
        } else if (attempt < 6) {
            Handler(Looper.getMainLooper()).postDelayed(
                { speakWithRetry(text, attempt + 1) }, 500
            )
        } else {
            Log.e("TestModelActivity", "TTS 초기화 지연: $text")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }

    /* ───────── Util: Bitmap ➜ NCHW Float32 ───────── */
    private fun bitmapToNCHW(bitmap: Bitmap): ByteBuffer {
        val w = 640; val h = 640; val c = 3
        val buffer = ByteBuffer.allocateDirect(c * w * h * 4)
            .order(ByteOrder.nativeOrder())

        val resized = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val pixels  = IntArray(w * h)
        resized.getPixels(pixels, 0, w, 0, 0, w, h)

        for (ch in 0 until c) {
            for (p in pixels) {
                val v = when (ch) {
                    0 -> ((p shr 16) and 0xFF) / 255f   // R
                    1 -> ((p shr  8) and 0xFF) / 255f   // G
                    else -> ( p         and 0xFF) / 255f// B
                }
                buffer.putFloat(v)
            }
        }
        return buffer
    }

    private fun sigmoid(x: Float) = (1f / (1f + exp(-x))).toFloat()
}

/* ───────── 데이터 클래스 ───────── */
data class DetectionResult(
    val className: String,   // 현재 로직에선 사용하지 않음
    val confidence: Float,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

/* ───────── 미리보기 (모델·TTS 제외) ───────── */
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewTest() {
    MaterialTheme { Text("UI Preview") }
}
