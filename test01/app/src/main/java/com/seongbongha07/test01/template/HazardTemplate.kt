package com.seongbongha07.test01.template

/**
 *  파일명 접두어(prefix) ↔ 제목·설명 매핑 테이블
 *  예) stairs_1699419001.jpg  →  STAIRS(넘어짐 위험)
 */
enum class HazardTemplate(
    val prefix: String,
    val title:  String,
    val desc:   String
) {
    STAIRS   ("stairs",   "넘어짐 위험",  "계단·난간 없는 단차"),
    EDGE     ("edge",     "추락 위험",   "난간 없는 개방 구역"),
    SLIPPERY ("slip",     "미끄러짐 위험","젖은/기름 묻은 바닥"),
    FALLING  ("fall",     "낙하물 위험", "머리 위 작업"),
    SAFE     ("safe",     "안전 구역",   "지정된 안전 구역");

    companion object {
        fun fromFileName(name: String): HazardTemplate? =
            values().firstOrNull { name.startsWith(it.prefix) }
    }
}
