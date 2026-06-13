package com.voicerider.core.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AddressParserTest {

    @Test
    fun `extract address with label`() {
        val text = "地址：万达广场3号楼1203室"
        assertEquals("万达广场3号楼1203室", AddressParser.extractAddress(text))
    }

    @Test
    fun `extract address with delivery label`() {
        val text = "送到：银泰城A座5楼508"
        assertEquals("银泰城A座5楼508", AddressParser.extractAddress(text))
    }

    @Test
    fun `return original text if no label`() {
        val text = "万达广场南门"
        assertEquals("万达广场南门", AddressParser.extractAddress(text))
    }

    @Test
    fun `detect valid address`() {
        assertTrue(AddressParser.isAddress("万达广场3号楼"))
        assertTrue(AddressParser.isAddress("中山路128号"))
        assertFalse(AddressParser.isAddress("你好"))
    }
}
