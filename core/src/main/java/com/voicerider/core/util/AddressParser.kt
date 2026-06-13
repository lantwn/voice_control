package com.voicerider.core.util

object AddressParser {

    fun extractAddress(text: String): String {
        val patterns = listOf(
            Regex("""地址[：:]\s*(.+?)(?:\s|$)"""),
            Regex("""送到[：:]\s*(.+?)(?:\s|$)"""),
            Regex("""收货地址[：:]\s*(.+?)(?:\s|$)""")
        )
        for (pattern in patterns) {
            pattern.find(text)?.let { return it.groupValues[1].trim() }
        }
        return text.trim()
    }

    fun isAddress(text: String): Boolean =
        text.length > 5 && (text.contains("路") || text.contains("街") ||
            text.contains("号") || text.contains("楼") || text.contains("广场") ||
            text.contains("小区") || text.contains("大厦"))
}
