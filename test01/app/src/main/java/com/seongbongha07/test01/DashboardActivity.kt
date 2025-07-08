package com.seongbongha07.test01  // ← 당신의 패키지명으로 바꿔야 합니다

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seongbongha07.test01.ui.theme.Test01Theme

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Test01Theme {
                DashboardTabScreen()
            }
        }
    }
}

@Composable
fun DashboardTabScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Status", "Drone", "Log")

    Column {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> StatusScreen()
            1 -> CameraScreen()
            2 -> LogScreen()
        }
    }
}

@Composable
fun StatusScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        /* 헬멧 미착용 현황 */
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("오늘 헬멧 미착용", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("3명 / 총 42명", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        /* 경고 알림 요약 */
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("금일 발행 경고", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("5회", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
fun CameraScreen() {
    // 예시용 드론 정보 (실제 데이터 바인딩 가능)
    val droneName     = "DJI Mavic 3 Enterprise"
    val batteryLevel  =  78                    // %
    val flightTimeMin =  12                    // 분
    val signalQuality = "양호"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("드론 정보", style = MaterialTheme.typography.titleMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("모델",          droneName)
                InfoRow("배터리 잔량",   "$batteryLevel %")
                InfoRow("잔여 비행 시간", "${flightTimeMin}분")
                InfoRow("신호 품질",     signalQuality)
            }
        }

        Button(
            onClick = { /* 예: Return-to-Home 명령 */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005BBB), contentColor = Color.White)
        ) {
            Text("복귀")
        }
    }
}

/* 재사용용 한 줄 정보 표시 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}


@Composable
fun LogScreen() {
    val logs = listOf(
        "14:32  Kim Worker  – 헬멧 미착용 경고",
        "15:15  Alex Builder – 헬멧 미착용 경고",
        "16:00  Jordan Smith – 헬멧 착용 확인",
        "16:20  시스템  – 일일 리포트 전송"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        logs.forEach { line ->
            Text(line, style = MaterialTheme.typography.bodyMedium)
            Divider(Modifier.padding(vertical = 8.dp))
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    Test01Theme {
        DashboardTabScreen()
    }
}