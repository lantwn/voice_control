package com.voicerider.core.util

object AddressParser {

    // 匹配中英文冒号 + 全角空格 + 普通空格
    private val SEP = """[：:\s　]+"""

    fun extractAddress(text: String): String {
        val patterns = listOf(
            Regex("""地址$SEP(.+?)(?=[\s　。，,！!]|$)"""),
            Regex("""送到$SEP(.+?)(?=[\s　。，,！!]|$)"""),
            Regex("""收货地址$SEP(.+?)(?=[\s　。，,！!]|$)""")
        )
        for (pattern in patterns) {
            pattern.find(text)?.let { return it.groupValues[1].trim() }
        }
        return text.trim()
    }

    /** 地址关键词加权匹配：至少命中 2 个地址特征词（或长度足够长 + 命中 1 个） */
    private val ADDRESS_KEYWORDS = listOf("路", "街", "号", "楼", "广场", "小区", "大厦", "栋", "单元", "室", "层")

    fun isAddress(text: String): Boolean {
        val hitCount = ADDRESS_KEYWORDS.count { text.contains(it) }
        return hitCount >= 2 || (hitCount >= 1 && text.length >= 8)
    }
}
