package com.seongbongha07.test01

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.seongbongha07.test01.ui.theme.Test01Theme

/* ───────────── Activity ───────────── */
class EduUIActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SafetyScanActivity 가 넘겨준 이미지 URI 리스트
        val uriList = intent?.getParcelableArrayListExtra<Uri>("images") ?: arrayListOf()

        enableEdgeToEdge()
        setContent { Test01Theme { SafetyEducationScreen(uriList) } }
    }
}

/* ───────────── Screen ───────────── */
@Composable
fun SafetyEducationScreen(uriList: List<Uri>) {
    Scaffold(
        topBar    = { EduTopBar() },
        bottomBar = { EduBottomNav() }
    ) { inner -> MainContent(Modifier.padding(inner), uriList) }
}

/* ───────────── Top Bar ───────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduTopBar() {
    val context = LocalContext.current
    TopAppBar(
        title = { Text("Safety Education") },
        navigationIcon = {
            IconButton(onClick = { (context as? Activity)?.finish() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { /* 설정 화면으로 이동 */ }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

/* ───────────── Main Content ───────────── */
@Composable
fun MainContent(modifier: Modifier = Modifier, uriList: List<Uri>) {

    /* 현재 사진 파일명 기억 */
    var currentFileName by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        /* 이미지 / 경고 / 파일명 콜백 */
        ImageCarousel(
            uriList          = uriList,
            onFileNameChange = { currentFileName = it }
        )

        Spacer(Modifier.height(24.dp))

        /* 템플릿이 없는 scan_ 파일일 때만 HazardList 노출 */
        val hasTemplate = currentFileName
            ?.let { com.seongbongha07.test01.template.HazardTemplate.fromFileName(it) } != null

        if (!hasTemplate && currentFileName?.startsWith("scan_") == true) {
            HazardList()
            Spacer(Modifier.height(24.dp))
        }

        DescriptionAndButton()
    }
}

/* 이미지 슬라이드 — 화살표로 넘기기 */
@Composable
fun ImageCarousel(
    uriList: List<Uri>,
    onFileNameChange: (String?) -> Unit   // 현재 파일명 보고
) {
    val context = LocalContext.current
    var index by remember { mutableStateOf(0) }

    /* 파일명이 바뀔 때 부모에 보고 */
    LaunchedEffect(index, uriList) {
        onFileNameChange(uriList.getOrNull(index)?.lastPathSegment)
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        /* ◀ 왼쪽 화살표 */
        IconButton(
            onClick = { if (index > 0) index-- },
            enabled = index > 0
        ) { Icon(Icons.Default.ArrowBack, "Prev") }

        /* ---------- 가운데 ---------- */
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            /* 이미지 로드 */
            val bmp: Bitmap? = remember(index, uriList) {
                uriList.getOrNull(index)?.let { uri ->
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
            }
            (bmp ?: BitmapFactory.decodeResource(
                context.resources,
                R.drawable.construction_site_image
            )).let { img ->
                Image(
                    bitmap       = img.asImageBitmap(),
                    contentDescription = null,
                    modifier     = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            /* --------- 조건별 문구 --------- */
            val fileName = uriList.getOrNull(index)
                ?.lastPathSegment
                ?.substringAfterLast('/') ?: ""

            val tpl = com.seongbongha07.test01.template.HazardTemplate
                .fromFileName(fileName)

            when {
                tpl != null -> {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        tpl.title,
                        color = Color.Red,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tpl.desc,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                fileName.startsWith("helmet_") -> {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "헬멧을 착용하지 않았습니다.",
                        color = Color.Red,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                /* scan_ + 템플릿 없음 → 문구 없음 */
            }
        }

        /* ▶ 오른쪽 화살표 */
        IconButton(
            onClick = { if (index < uriList.size - 1) index++ },
            enabled = index < uriList.size - 1
        ) { Icon(Icons.Default.ArrowForward, "Next") }
    }
}

/* ───────────── Hazard List (랜덤 3종) ───────────── */
data class HazardItem(val title: String, val description: String)

private val ALL_HAZARDS = listOf(
    HazardItem("추락 위험",        "난간 없는 개방 구역"),
    HazardItem("걸림·넘어짐 위험", "흩어져 있는 자재"),
    HazardItem("미끄러짐 위험",    "젖은 바닥"),
    HazardItem("낙하물 위험",      "머리 위 작업"),
    HazardItem("감전 위험",        "노출된 전선"),
    HazardItem("질식 위험",        "밀폐 공간"),
    HazardItem("소음 위험",        "120 dB 이상 장비"),
    HazardItem("안전 구역",        "지정된 안전 구역")
)

@Composable
fun HazardList() {
    val items = remember { ALL_HAZARDS.shuffled().take(3) }

    Column {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    item.description,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Divider(Modifier.padding(vertical = 6.dp))
        }
    }
}

/* ───────────── 설명 & 버튼 ───────────── */
@Composable
fun DescriptionAndButton() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "드론이 다양한 위험 요소를 실시간으로 식별하고 표시하여 현장의 안전 의식을 높입니다.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

/* ───────────── Bottom Nav ───────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduBottomNav() {
    val context = LocalContext.current
    val items = listOf(
        R.drawable.home_24px  to "Home",
        R.drawable.photo_scan_24px to "Scan",
        R.drawable.report_24px to "Report",
        R.drawable.settings_24px to "Settings"
    )
    val selected = 2 // Report 탭

    NavigationBar(containerColor = Color.White) {
        items.forEachIndexed { idx, (iconRes, label) ->
            NavigationBarItem(
                selected = idx == selected,
                icon  = { Icon(painterResource(iconRes), contentDescription = label) },
                label = { Text(label) },
                onClick = {
                    when (idx) {
                        0 -> context.startActivity(Intent(context, MainActivity::class.java))
                        1 -> context.startActivity(Intent(context, SafetyScanActivity::class.java))
                        2 -> {} // 현재 화면
                        3 -> context.startActivity(Intent(context, DashboardActivity::class.java))
                    }
                }
            )
        }
    }
}

/* ───────────── Preview ───────────── */
@Preview(showBackground = true)
@Composable
fun EduPreview() {
    Test01Theme { SafetyEducationScreen(emptyList()) }
}
