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
    fun `extract address with full-width colon`() {
        val text = "地址：　万达广场3号楼1203室"  // 全角冒号 + 全角空格
        assertEquals("万达广场3号楼1203室", AddressParser.extractAddress(text))
    }

    @Test
    fun `extract address with Chinese punctuation suffix`() {
        val text = "送到：银泰城A座5楼508。请尽快"  // 句号截止
        assertEquals("银泰城A座5楼508", AddressParser.extractAddress(text))
    }

    @Test
    fun `return original text if no label`() {
        val text = "万达广场南门"
        assertEquals("万达广场南门", AddressParser.extractAddress(text))
    }

    @Test
    fun `detect valid address with 2+ keywords`() {
        assertTrue(AddressParser.isAddress("万达广场3号楼"))
        assertTrue(AddressParser.isAddress("中山路128号"))
        assertTrue(AddressParser.isAddress("银泰城A座5楼508室"))
    }

    @Test
    fun `detect valid address with 1 keyword and long text`() {
        assertTrue(AddressParser.isAddress("北京市朝阳区大望路"))
    }

    @Test
    fun `reject short text with only one keyword`() {
        assertFalse(AddressParser.isAddress("大望路"))
    }

    @Test
    fun `reject non-address text`() {
        assertFalse(AddressParser.isAddress("你好"))
        assertFalse(AddressParser.isAddress("今天天气不错"))
    }
}
