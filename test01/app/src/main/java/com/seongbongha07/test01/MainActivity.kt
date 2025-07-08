package com.seongbongha07.test01

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seongbongha07.test01.ui.theme.Test01Theme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Test01Theme {
                WelcomeScreen(
                    onDashboardClick = {
                        startActivity(Intent(this, WorkerboardActivity::class.java))
                    },
                    onDroneCameraClick = {
                        startActivity(Intent(this, SafetyScanActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onDashboardClick: () -> Unit,
    onDroneCameraClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        /* ---------- 상단 타이틀 ---------- */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Safedrony", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            /* 우측 설정 아이콘 → 역할 선택 화면 */
            val context = LocalContext.current           // ← 추가
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {
                    /* 1) 저장된 역할 초기화 */
                    context.getSharedPreferences("role", MODE_PRIVATE)
                        .edit().remove("app_role").apply()
                    /* 2) RoleSelectActivity 실행 */
                    context.startActivity(
                        Intent(context, RoleSelectActivity::class.java)
                    )
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        /* ---------- 본문 ---------- */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to Safedrony", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Ensure safety on site with real-time monitoring and alerts.",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onDashboardClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDE6F7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) { Text("Go to Dashboard", color = Color.Black) }

            Button(
                onClick = onDroneCameraClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6EAF0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) { Text("Connect Drone Camera", color = Color.Black) }

            Spacer(modifier = Modifier.height(100.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF14424F), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(                                       // ← Text 대신 Image
                    painter = painterResource(R.drawable.logo), // drawable/logo.png (또는 .xml)
                    contentDescription = "Logo",
                    modifier = Modifier.fillMaxSize(),          // 원 안을 꽉 채우도록
                    contentScale = ContentScale.Crop
                )
            }
        }

        /* ---------- 하단 바 ---------- */
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Powered by Safedrony", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            BottomNavigationBar()                       // 하단 네비게이션
        }
    }
}

/* ---------- BottomNavigation ---------- */
@Composable
fun BottomNavigationBar() {
    val context = LocalContext.current                // ⬅ 현재 Context 가져오기

    NavigationBar(
        containerColor = Color(0xFFF8F8F8),
        tonalElevation = 0.dp
    ) {
        /* Home → MainActivity(현재 화면) */
        NavigationBarItem(
            selected = true,                           // 현재 화면
            onClick = { /* 아무 동작 없음 */ },
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

        /* Scan → SafetyScanActivity */
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

        /* Profile – 미래 기능(현재는 동작 없음) */
        NavigationBarItem(
            selected = false,
            onClick = { /* Profile 클릭 이벤트 */ },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.profile_24px),
                    contentDescription = "Profile",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Black
                )
            },
            label = { Text("Profile", color = Color.Black) }
        )
    }
}

/* ---------- 프리뷰 ---------- */
@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    Test01Theme {
        WelcomeScreen(
            onDashboardClick = {},
            onDroneCameraClick = {}
        )
    }
}
