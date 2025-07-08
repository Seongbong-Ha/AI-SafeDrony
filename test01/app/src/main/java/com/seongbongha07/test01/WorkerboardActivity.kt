package com.seongbongha07.test01

import android.app.Activity
import android.content.Intent
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seongbongha07.test01.R
import com.seongbongha07.test01.ui.theme.Test01Theme

/** MainActivity · SafetyScanActivity · DashboardActivity
 *  는 이미 프로젝트에 존재한다고 가정합니다. */
class WorkerboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Test01Theme { MainScreen() } }
    }
}

/* ---------- NAVIGATION HELPERS ---------- */

private fun Activity.goTo(target: Class<*>, finishSelf: Boolean = false) {
    startActivity(Intent(this, target))
    if (finishSelf) finish()
}

/* ---------- COMPOSABLES ---------- */

@Composable
fun MainScreen() {
    WorkerScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerScreen() {
    val context = LocalContext.current                                    // 현재 Context

    Scaffold(
        /* ---------- TOP BAR ---------- */
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                ) {
                    /* Back to MainActivity */
                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(context, MainActivity::class.java)
                            )
                            (context as? Activity)?.finish()               // 스택에서 제거
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }

                    Text(
                        text = "Safedrony",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    IconButton(
                        onClick = { /* Settings 메뉴 필요 시 구현 */ },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        },

        /* ---------- BOTTOM NAVIGATION ---------- */
        bottomBar = {
            NavigationBar(containerColor = Color.White) {

                /* 1. Home -> MainActivity */
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, MainActivity::class.java))
                        (context as? Activity)?.finish()
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.home_24px),
                            contentDescription = "Home",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    },
                    label = { Text("Home", color = Color.Black) }
                )

                /* 2. Scan -> SafetyScanActivity */
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, SafetyScanActivity::class.java))
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.photo_scan_24px),
                            contentDescription = "Scan",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    },
                    label = { Text("Scan", color = Color.Black) }
                )

                /* 3. Report -> 현재 화면 (Workerboard) */
                NavigationBarItem(
                    selected = true,     // 현재 화면
                    onClick = { /* Do nothing */ },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.report_24px),
                            contentDescription = "Report",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    },
                    label = { Text("Report", color = Color.Black) }
                )

                /* 4. Settings -> DashboardActivity (드론 정보) */
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, DashboardActivity::class.java))
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.settings_24px),
                            contentDescription = "Settings",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    },
                    label = { Text("Settings", color = Color.Black) }
                )
            }
        }
    ) { padding ->

        /* ---------- MAIN CONTENT ---------- */
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Safety Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Today's Helmet\nNon-Compliance", "3 People", Modifier.weight(1f))
                StatCard("Total Warnings\nIssued", "5 Times", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(36.dp))
            Text("Detected Violations", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))

            ViolationItem("Kim Worker", "2 Times", "14:32", "3rd Floor Platform", R.drawable.avatar1)
            ViolationItem("Alex Builder", "1 Time", "15:15", "West Wing", R.drawable.avatar2)
            ViolationItem("Jordan Smith", "2 Times", "16:00", "Loading Dock", R.drawable.avatar3)

            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { /* Export logic */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("Export Data", color = Color.White)
            }
        }
    }
}

/* ---------- UI COMPONENTS ---------- */

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ViolationItem(
    name: String,
    warnings: String,
    time: String,
    zone: String,
    imageResId: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Name: $name", fontWeight = FontWeight.SemiBold)
                Text(
                    "Warnings: $warnings\nLast Detected: $time\nZone: $zone",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Image(
                painter = painterResource(id = imageResId),
                contentDescription = "Avatar of $name",
                modifier = Modifier
                    .height(48.dp)
                    .width(72.dp)
            )
        }
    }
}

/* ---------- PREVIEW ---------- */

@Preview(showBackground = true)
@Composable
fun WorkerPreview() {
    Test01Theme { WorkerScreen() }
}
