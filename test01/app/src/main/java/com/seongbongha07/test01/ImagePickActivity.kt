package com.seongbongha07.test01

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.seongbongha07.test01.ui.theme.Test01Theme
import java.io.File

/* ─────────── Activity ─────────── */
class ImagePickActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SafetyScanActivity 가 저장한 /cache/captures
        val dir = File(cacheDir, "captures").apply { mkdirs() }

        enableEdgeToEdge()
        setContent { Test01Theme { PickScreen(dir) } }
    }
}

/* ─────────── Composable Screen ─────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickScreen(imgDir: File) {
    val context = LocalContext.current

    // 디렉터리 스캔 → 최신순 정렬
    val all = remember {
        mutableStateListOf<File>().apply {
            addAll(
                imgDir.listFiles()
                    ?.filter { it.length() > 0 }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()
            )
        }
    }

    var selected by remember { mutableStateOf<Set<File>>(emptySet()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("촬영된 이미지 선택") },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        /* ───────── bottomBar: 두 개의 버튼, 살짝 위로 ───────── */
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)   // ↗︎ 전보다 위쪽에 배치
            ) {
                /* ① 교육 컨텐츠 생성 */
                Button(
                    onClick = {
                        if (selected.isNotEmpty()) {
                            val uris = selected.map {
                                FileProvider.getUriForFile(
                                    context, "${context.packageName}.fileprov", it
                                )
                            }
                            Intent(context, EduUIActivity::class.java)
                                .putParcelableArrayListExtra("images", ArrayList<Uri>(uris))
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                .also(context::startActivity)
                        } else {
                            Toast.makeText(context, "최소 1장을 선택하세요", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("교육 컨텐츠 생성") }

                Spacer(Modifier.height(8.dp))

                /* ② 선택 삭제 */
                /* 선택 삭제 버튼 onClick */
                OutlinedButton(
                    onClick = {
                        if (selected.isEmpty()) {
                            Toast.makeText(context, "삭제할 사진을 선택하세요.", Toast.LENGTH_SHORT).show()
                        } else {
                            selected.forEach { it.delete() }
                            all.removeAll(selected)      // ← 가변 리스트이므로 바로 갱신
                            selected = emptySet()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("선택한 사진 삭제") }

            }
        }

    ) { inner ->

        if (all.isEmpty()) {
            Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("저장된 캡처가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 68.dp),
                modifier = Modifier
                    .fillMaxSize()          // ← fillMaxWidth → fillMaxSize 로 교체
                    .padding(inner)
                    .padding(horizontal = 12.dp)
            ) {
                items(all) { f ->
                    val checked = f in selected

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected =
                                    if (checked) selected - f else selected + f
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        /* ─── 썸네일 (null-safe) ─── */
                        val bmp = remember(f) { BitmapFactory.decodeFile(f.path) }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = "broken",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }

                        Spacer(Modifier.width(12.dp))
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selected =
                                    if (checked) selected - f else selected + f
                            }
                        )
                        Text(f.name, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
