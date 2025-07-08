package com.seongbongha07.test01

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class RoleSelectActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("role", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 이미 선택된 경우 건너뛰기
        prefs.getString("app_role", null)?.let {
            goMain(it); finish(); return
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Button(
                        onClick = { setRole("ADMIN") },
                        modifier = Modifier.width(240.dp)
                    ) { Text("관리자 모드 시작") }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { setRole("CLIENT") },
                        modifier = Modifier.width(240.dp)
                    ) { Text("클라이언트 모드 시작") }
                }
            }
        }
    }

    private fun setRole(role: String) {
        prefs.edit().putString("app_role", role).apply()
        goMain(role)
        finish()
    }

    private fun goMain(role: String) {
        Log.d("RoleSelect", "선택된 역할: $role")
        val dest = if (role == "ADMIN")
            AdminMainActivity::class.java      // ← 여기만 변경
        else
            SafetyScanActivity::class.java
        startActivity(Intent(this, dest))
    }
}