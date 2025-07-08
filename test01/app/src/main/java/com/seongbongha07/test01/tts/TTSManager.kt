package com.seongbongha07.test01.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    @Volatile var isReady = false        // 초기화 여부 공개

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(Locale.KOREAN)
            isReady = res != TextToSpeech.LANG_MISSING_DATA &&
                    res != TextToSpeech.LANG_NOT_SUPPORTED
            Log.d("TTSManager", if (isReady) "TTS 초기화 성공" else "한국어 미지원")
        } else {
            isReady = false
            Log.e("TTSManager", "TTS 초기화 실패: $status")
        }
    }

    fun speakOut(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
        }
    }

    fun shutdown() {
        tts?.stop(); tts?.shutdown(); isReady = false
    }
}
