# Voice Rider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a voice-controlled Android app that automates Meituan rider app operations via AccessibilityService.

**Architecture:** 5-module Gradle project (core → voice/accessibility/navigation → app). Kotlin + XML Views, Xunfei speech SDK (primary) + Android SpeechRecognizer (fallback), AMap navigation SDK.

**Tech Stack:** Kotlin 1.9+, XML View, Gradle Kotlin DSL, JUnit 5 + MockK, min SDK 29 (Android 10)

---

## File Structure Map

```
VoiceRider/
├── build.gradle.kts                          # Root build config
├── settings.gradle.kts                       # Module includes
├── gradle.properties                         # Gradle & Android props
├── gradle/libs.versions.toml                 # Version catalog
│
├── :core/
│   └── src/main/java/com/voicerider/core/
│       ├── model/   Order.kt, VoiceCommand.kt, AirReminder.kt, RouteInfo.kt
│       ├── util/    PrefsManager.kt, Logger.kt, AddressParser.kt
│       └── config/  AppConfig.kt
│   └── src/test/java/com/voicerider/core/
│       ├── model/   OrderTest.kt
│       └── util/    AddressParserTest.kt
│
├── :voice/
│   └── src/main/java/com/voicerider/voice/
│       ├── engine/  WakeUpEngine.kt, CommandRecognizer.kt, TtsSpeaker.kt
│       ├── fallback/ SystemRecognizer.kt
│       └── config/  VoiceConfig.kt
│   └── src/main/res/values/ voice_strings.xml
│
├── :accessibility/
│   └── src/main/java/com/voicerider/accessibility/
│       ├── service/ RiderAccessibilityService.kt
│       ├── automator/ MeituanAutomator.kt, OrderActionHandler.kt
│       └── strategy/ ElementLocator.kt
│
├── :navigation/
│   └── src/main/java/com/voicerider/navigation/
│       ├── navigator/ AmapNavigator.kt
│       └── planner/ RoutePlanner.kt
│
└── :app/
    └── src/main/java/com/voicerider/app/
        ├── ui/       MainActivity.kt, OrderDetailActivity.kt, SettingsActivity.kt
        ├── ui/home/  HomeFragment.kt, OrderListAdapter.kt, AIReminderAdapter.kt
        ├── ui/widget/ VoiceInputBar.kt, FloatingWindow.kt
        ├── service/  VoiceRoutingService.kt, FloatingWindowService.kt
        └── viewmodel/ HomeViewModel.kt, OrderDetailViewModel.kt
    └── src/main/res/
        ├── layout/   activity_main.xml, activity_order_detail.xml, ...
        ├── values/   colors.xml, themes.xml, styles.xml, strings.xml
        ├── drawable/ bg_order_card.xml, bg_voice_bar.xml, ...
        └── anim/     slide_in_right.xml, slide_out_left.xml, ...
```

---

## Phase 1: Project Scaffolding

### Task 1.1: Create Gradle Version Catalog

**Files:**
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`
- Create: `.gitignore` (already exists — verify)

- [ ] **Step 1: Write version catalog**

```toml
[versions]
agp = "8.2.2"
kotlin = "1.9.22"
core-ktx = "1.12.0"
appcompat = "1.6.1"
material = "1.11.0"
lifecycle = "2.7.0"
navigation = "2.7.6"
coroutines = "1.7.3"
junit = "5.10.1"
mockk = "1.13.9"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
lifecycle-livedata = { group = "androidx.lifecycle", name = "lifecycle-livedata-ktx", version.ref = "lifecycle" }
navigation-fragment = { group = "androidx.navigation", name = "navigation-fragment-ktx", version.ref = "navigation" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

- [ ] **Step 2: Write gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 3: Commit**

```bash
git add gradle/libs.versions.toml gradle.properties
git commit -m "feat: add Gradle version catalog and properties"
```

### Task 1.2: Create Root & Module Build Files

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `:core/build.gradle.kts`
- Create: `:voice/build.gradle.kts`
- Create: `:accessibility/build.gradle.kts`
- Create: `:navigation/build.gradle.kts`
- Create: `:app/build.gradle.kts`

- [ ] **Step 1: Write settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "VoiceRider"
include(":core", ":voice", ":accessibility", ":navigation", ":app")
```

- [ ] **Step 2: Write root build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
```

- [ ] **Step 3: Write :core/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.voicerider.core"
    compileSdk = 34
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}
```

- [ ] **Step 4: Write :voice/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.voicerider.voice"
    compileSdk = 34
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
}
```

- [ ] **Step 5: Write :accessibility/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.voicerider.accessibility"
    compileSdk = 34
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
}
```

- [ ] **Step 6: Write :navigation/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.voicerider.navigation"
    compileSdk = 34
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
}
```

- [ ] **Step 7: Write :app/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.voicerider.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.voicerider.app"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { viewBinding = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":voice"))
    implementation(project(":accessibility"))
    implementation(project(":navigation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.navigation.fragment)
    implementation(libs.coroutines.android)
}
```

- [ ] **Step 8: Verify build compiles**

```bash
./gradlew :core:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add settings.gradle.kts build.gradle.kts */build.gradle.kts
git commit -m "feat: scaffold Gradle multi-module project"
```

---

## Phase 2: :core — Data Models & Utilities

### Task 2.1: Define Enums

**Files:**
- Create: `:core/src/main/java/com/voicerider/core/model/OrderStatus.kt`
- Create: `:core/src/main/java/com/voicerider/core/model/CommandType.kt`
- Create: `:core/src/main/java/com/voicerider/core/model/ReminderLevel.kt`
- Create: `:core/src/main/java/com/voicerider/core/model/ReminderType.kt`

- [ ] **Step 1: Write OrderStatus.kt**

```kotlin
package com.voicerider.core.model

enum class OrderStatus(val label: String) {
    WAITING("抢单大厅"),
    ACCEPTED("已接单"),
    PICKED_UP("已取餐"),
    DELIVERING("配送中"),
    COMPLETED("已送达");
}
```

- [ ] **Step 2: Write CommandType.kt**

```kotlin
package com.voicerider.core.model

enum class CommandType(
    val patterns: List<String>,
    val requiredStatus: OrderStatus?
) {
    ACCEPT_ORDER(listOf("接单", "抢", "接"), OrderStatus.WAITING),
    REJECT_ORDER(listOf("不接", "拒单", "拒"), OrderStatus.WAITING),
    PICKUP_DONE(listOf("已取餐", "取货完成", "取餐了"), OrderStatus.ACCEPTED),
    DELIVERY_DONE(listOf("已送达", "送到了", "到了"), OrderStatus.DELIVERING),
    NAV_TO_PICKUP(listOf("导航到取餐点", "去取餐", "取餐导航"), null),
    NAV_TO_CUSTOMER(listOf("导航到顾客", "去送货", "送餐导航"), null),
    CALL_CUSTOMER(listOf("打电话给顾客", "打电话", "联系顾客"), null),
    SEND_MESSAGE(listOf("发消息", "发短信", "给顾客发消息"), null),
    QUERY_ORDER(listOf("查看订单", "订单详情", "还剩几单", "什么单"), null);

    fun matches(text: String): Boolean =
        patterns.any { text.contains(it) }

    companion object {
        fun fromText(text: String): CommandType? =
            entries.firstOrNull { it.matches(text) }
    }
}
```

- [ ] **Step 3: Write ReminderLevel.kt**

```kotlin
package com.voicerider.core.model

enum class ReminderLevel(val priority: Int) {
    URGENT(0),    // 🔴 催单、即将超时
    IMPORTANT(1), // 🟡 状态变更、取餐提醒
    INFO(2),      // 🔵 路线建议、系统通知
    SUMMARY(3);   // 🟢 收入统计
}
```

- [ ] **Step 4: Write ReminderType.kt**

```kotlin
package com.voicerider.core.model

enum class ReminderType(val label: String) {
    CUSTOMER_URGE("顾客催单"),
    TIMEOUT_WARNING("即将超时"),
    STATUS_CHANGE("状态变更"),
    PICKUP_REMINDER("取餐提醒"),
    ROUTE_SUGGESTION("路线建议"),
    ADDRESS_CHANGE("地址变更"),
    INCOME_SUMMARY("收入统计");
}
```

- [ ] **Step 5: Commit**

```bash
git add :core/src/main/java/com/voicerider/core/model/
git commit -m "feat(core): add OrderStatus, CommandType, ReminderLevel, ReminderType enums"
```

### Task 2.2: Define Data Models

**Files:**
- Create: `:core/src/main/java/com/voicerider/core/model/Order.kt`
- Create: `:core/src/main/java/com/voicerider/core/model/VoiceCommand.kt`
- Create: `:core/src/main/java/com/voicerider/core/model/AirReminder.kt`
- Create: `:core/src/main/java/com/voicerider/core/model/RouteInfo.kt`

- [ ] **Step 1: Write Order.kt**

```kotlin
package com.voicerider.core.model

data class Order(
    val id: String,
    val status: OrderStatus,
    val merchantName: String,
    val merchantAddress: String,
    val customerName: String,
    val customerAddress: String,
    val customerPhone: String,
    val amount: Double,
    val distanceKm: Float,
    val acceptedTime: Long = System.currentTimeMillis(),
    val estimatedDelivery: Long = System.currentTimeMillis() + 1800_000
)
```

- [ ] **Step 2: Write VoiceCommand.kt**

```kotlin
package com.voicerider.core.model

data class VoiceCommand(
    val type: CommandType,
    val rawText: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: Write AirReminder.kt**

```kotlin
package com.voicerider.core.model

data class AirReminder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ReminderType,
    val level: ReminderLevel,
    val title: String,
    val message: String,
    val orderId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
```

- [ ] **Step 4: Write RouteInfo.kt**

```kotlin
package com.voicerider.core.model

data class RouteInfo(
    val fromName: String,
    val toName: String,
    val fromLat: Double,
    val fromLng: Double,
    val toLat: Double,
    val toLng: Double,
    val distanceMeters: Float,
    val durationSeconds: Int
)
```

- [ ] **Step 5: Commit**

```bash
git add :core/src/main/java/com/voicerider/core/model/Order.kt \
        :core/src/main/java/com/voicerider/core/model/VoiceCommand.kt \
        :core/src/main/java/com/voicerider/core/model/AirReminder.kt \
        :core/src/main/java/com/voicerider/core/model/RouteInfo.kt
git commit -m "feat(core): add Order, VoiceCommand, AirReminder, RouteInfo models"
```

### Task 2.3: Write Unit Tests for Models

**Files:**
- Create: `:core/src/test/java/com/voicerider/core/model/CommandTypeTest.kt`

- [ ] **Step 1: Write CommandTypeTest.kt**

```kotlin
package com.voicerider.core.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommandTypeTest {

    @Test
    fun `match accept order from text`() {
        assertEquals(CommandType.ACCEPT_ORDER, CommandType.fromText("接单"))
        assertEquals(CommandType.ACCEPT_ORDER, CommandType.fromText("抢"))
    }

    @Test
    fun `match pickup done from text`() {
        assertEquals(CommandType.PICKUP_DONE, CommandType.fromText("已取餐"))
        assertEquals(CommandType.PICKUP_DONE, CommandType.fromText("取货完成"))
    }

    @Test
    fun `match delivery done from text`() {
        assertEquals(CommandType.DELIVERY_DONE, CommandType.fromText("已送达"))
        assertEquals(CommandType.DELIVERY_DONE, CommandType.fromText("到了"))
    }

    @Test
    fun `return null for unrecognized text`() {
        assertNull(CommandType.fromText("随便说点什么"))
    }

    @Test
    fun `accept order requires waiting status`() {
        assertEquals(OrderStatus.WAITING, CommandType.ACCEPT_ORDER.requiredStatus)
    }

    @Test
    fun `delivery done requires delivering status`() {
        assertEquals(OrderStatus.DELIVERING, CommandType.DELIVERY_DONE.requiredStatus)
    }

    @Test
    fun `nav commands have no required status`() {
        assertNull(CommandType.NAV_TO_PICKUP.requiredStatus)
        assertNull(CommandType.NAV_TO_CUSTOMER.requiredStatus)
    }
}
```

- [ ] **Step 2: Run test**

```bash
./gradlew :core:testDebugUnitTest --tests "com.voicerider.core.model.CommandTypeTest"
```

Expected: PASS (7 tests)

- [ ] **Step 3: Commit**

```bash
git add :core/src/test/
git commit -m "test(core): add CommandType matching tests"
```

### Task 2.4: Write Utility Classes

**Files:**
- Create: `:core/src/main/java/com/voicerider/core/util/PrefsManager.kt`
- Create: `:core/src/main/java/com/voicerider/core/util/Logger.kt`
- Create: `:core/src/main/java/com/voicerider/core/util/AddressParser.kt`
- Create: `:core/src/main/java/com/voicerider/core/config/AppConfig.kt`

- [ ] **Step 1: Write PrefsManager.kt**

```kotlin
package com.voicerider.core.util

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("voice_rider_prefs", Context.MODE_PRIVATE)

    var wakeWord: String
        get() = prefs.getString("wake_word", "美团精灵") ?: "美团精灵"
        set(value) = prefs.edit().putString("wake_word", value).apply()

    var isVoiceEnabled: Boolean
        get() = prefs.getBoolean("voice_enabled", true)
        set(value) = prefs.edit().putBoolean("voice_enabled", value).apply()

    var isAccessibilityEnabled: Boolean
        get() = prefs.getBoolean("accessibility_enabled", false)
        set(value) = prefs.edit().putBoolean("accessibility_enabled", value).apply()

    var navMode: String
        get() = prefs.getString("nav_mode", "BIKE") ?: "BIKE"
        set(value) = prefs.edit().putString("nav_mode", value).apply()

    var elementMappingVersion: String
        get() = prefs.getString("element_mapping_version", "1.0.0") ?: "1.0.0"
        set(value) = prefs.edit().putString("element_mapping_version", value).apply()

    fun getString(key: String, default: String = ""): String =
        prefs.getString(key, default) ?: default

    fun putString(key: String, value: String) =
        prefs.edit().putString(key, value).apply()
}
```

- [ ] **Step 2: Write Logger.kt**

```kotlin
package com.voicerider.core.util

import android.util.Log

object Logger {
    private const val TAG = "VoiceRider"
    private var isDebug = true

    fun d(message: String) { if (isDebug) Log.d(TAG, message) }
    fun i(message: String) { Log.i(TAG, message) }
    fun w(message: String) { Log.w(TAG, message) }
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
```

- [ ] **Step 3: Write AddressParser.kt**

```kotlin
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
```

- [ ] **Step 4: Write AppConfig.kt**

```kotlin
package com.voicerider.core.config

object AppConfig {
    const val APP_NAME = "VoiceRider"
    const val VERSION = "1.0.0"
    const val MEITUAN_PACKAGE = "com.sankuai.meituan.takeoutnew"
    const val MEITUAN_RIDER_PACKAGE = "com.sankuai.meituan.delivery"
    const val COMMAND_TIMEOUT_MS = 5_000L
    const val MAX_AUTOMATION_RETRIES = 3
    const val AMAP_WEB_KEY = ""  // Fill after registration
}
```

- [ ] **Step 5: Write AddressParserTest.kt**

```kotlin
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
```

- [ ] **Step 6: Run tests**

```bash
./gradlew :core:testDebugUnitTest
```

Expected: PASS (11 tests total)

- [ ] **Step 7: Commit**

```bash
git add :core/src/main/java/com/voicerider/core/util/ \
        :core/src/main/java/com/voicerider/core/config/ \
        :core/src/test/java/com/voicerider/core/util/
git commit -m "feat(core): add PrefsManager, Logger, AddressParser, AppConfig"
```

---

## Phase 3: :voice — Speech Engine

### Task 3.1: Create Voice Skeleton & Config

**Files:**
- Create: `:voice/src/main/java/com/voicerider/voice/config/VoiceConfig.kt`
- Create: `:voice/src/main/java/com/voicerider/voice/engine/WakeUpEngine.kt`
- Create: `:voice/src/main/java/com/voicerider/voice/engine/CommandRecognizer.kt`
- Create: `:voice/src/main/java/com/voicerider/voice/engine/TtsSpeaker.kt`
- Create: `:voice/src/main/java/com/voicerider/voice/fallback/SystemRecognizer.kt`

- [ ] **Step 1: Write VoiceConfig.kt**

```kotlin
package com.voicerider.voice.config

object VoiceConfig {
    const val WAKE_WORD = "美团精灵"
    const val LISTEN_TIMEOUT_MS = 5_000L
    const val CONFIDENCE_THRESHOLD = 0.5f

    // Xunfei SDK — fill after registration
    var XUNFEI_APP_ID = ""
    var XUNFEI_API_KEY = ""
    var XUNFEI_API_SECRET = ""

    // TTS
    const val TTS_SPEED = 50
    const val TTS_PITCH = 50
    const val TTS_VOLUME = 100
}
```

- [ ] **Step 2: Write WakeUpEngine.kt (skeleton)**

The actual Xunfei SDK integration requires `.aar`/`.jar` files placed in `:voice/libs/`. This skeleton defines the interface. When SDK files are available, replace method bodies with actual SDK calls.

```kotlin
package com.voicerider.voice.engine

import com.voicerider.core.util.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class WakeUpEngine {
    private val _wakeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val wakeEvents: SharedFlow<Unit> = _wakeEvents

    private var isInitialized = false

    fun initialize(wakeWord: String = "美团精灵"): Boolean {
        Logger.i("WakeUpEngine: initializing with wake word '$wakeWord'")
        // TODO: Replace with Xunfei SpeechWakeup SDK init
        // SpeechWakeup.createInstance(context, object : SpeechWakeupListener {
        //     override fun onWakeUp(word: String) { _wakeEvents.tryEmit(Unit) }
        // })
        isInitialized = true
        Logger.i("WakeUpEngine: initialized (stub — awaiting SDK)")
        return true
    }

    fun startListening() {
        if (!isInitialized) return
        Logger.d("WakeUpEngine: started listening")
        // TODO: Xunfei SpeechWakeup.startListening()
    }

    fun stopListening() {
        Logger.d("WakeUpEngine: stopped listening")
        // TODO: Xunfei SpeechWakeup.stopListening()
    }

    fun destroy() {
        Logger.d("WakeUpEngine: destroyed")
        // TODO: Xunfei SpeechWakeup.destroy()
    }
}
```

- [ ] **Step 3: Write SystemRecognizer.kt (fallback)**

```kotlin
package com.voicerider.voice.fallback

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.voicerider.core.util.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SystemRecognizer(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    suspend fun recognize(): String? = suspendCancellableCoroutine { cont ->
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Logger.w("SystemRecognizer: not available")
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onResults(results: android.os.Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    cont.resume(matches?.firstOrNull())
                }
                override fun onError(error: Int) {
                    Logger.w("SystemRecognizer: error code $error")
                    cont.resume(null)
                }
                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            startListening(intent)
        }

        cont.invokeOnCancellation { recognizer?.destroy() }
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}
```

- [ ] **Step 4: Write CommandRecognizer.kt**

```kotlin
package com.voicerider.voice.engine

import com.voicerider.core.model.CommandType
import com.voicerider.core.model.VoiceCommand
import com.voicerider.core.util.Logger
import com.voicerider.voice.config.VoiceConfig
import com.voicerider.voice.fallback.SystemRecognizer

class CommandRecognizer(private val systemRecognizer: SystemRecognizer) {
    private var useXunfeiOnline = true

    suspend fun recognize(): VoiceCommand? {
        val rawText = if (useXunfeiOnline) {
            recognizeViaXunfei() ?: fallbackToSystem()
        } else {
            fallbackToSystem()
        } ?: return null

        val commandType = CommandType.fromText(rawText)
        if (commandType == null) {
            Logger.w("CommandRecognizer: no match for '$rawText'")
            return null
        }

        Logger.i("CommandRecognizer: matched ${commandType.name} from '$rawText'")
        return VoiceCommand(
            type = commandType,
            rawText = rawText,
            confidence = 0.8f
        )
    }

    private suspend fun recognizeViaXunfei(): String? {
        // TODO: Replace with Xunfei ASR SDK online recognition
        // Returns recognized text or null
        Logger.d("CommandRecognizer: Xunfei online ASR (stub)")
        return null
    }

    private suspend fun fallbackToSystem(): String? {
        Logger.d("CommandRecognizer: falling back to system recognizer")
        useXunfeiOnline = false
        return systemRecognizer.recognize()
    }

    fun setOnlineMode(online: Boolean) {
        useXunfeiOnline = online
    }

    fun destroy() {
        systemRecognizer.destroy()
    }
}
```

- [ ] **Step 5: Write TtsSpeaker.kt**

```kotlin
package com.voicerider.voice.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import com.voicerider.core.util.Logger
import com.voicerider.voice.config.VoiceConfig
import java.util.Locale

class TtsSpeaker(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun initialize(): Boolean {
        tts = TextToSpeech(context) { status ->
            isInitialized = (status == TextToSpeech.SUCCESS)
            if (isInitialized) {
                tts?.language = Locale.CHINESE
                tts?.setSpeechRate(VoiceConfig.TTS_SPEED / 50f)
                tts?.setPitch(VoiceConfig.TTS_PITCH / 50f)
            }
        }
        return true
    }

    fun speak(text: String) {
        if (!isInitialized) {
            Logger.w("TtsSpeaker: not initialized")
            return
        }
        Logger.i("TtsSpeaker: speaking '$text'")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_rider_tts")
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add :voice/
git commit -m "feat(voice): add WakeUpEngine, CommandRecognizer, TtsSpeaker, SystemRecognizer with Xunfei stubs"
```

---

## Phase 4: :accessibility — Meituan Automation

### Task 4.1: Write ElementLocator

**Files:**
- Create: `:accessibility/src/main/java/com/voicerider/accessibility/strategy/ElementLocator.kt`
- Create: `:accessibility/src/main/java/com/voicerider/accessibility/automator/MeituanAutomator.kt`
- Create: `:accessibility/src/main/java/com/voicerider/accessibility/automator/OrderActionHandler.kt`
- Create: `:accessibility/src/main/java/com/voicerider/accessibility/service/RiderAccessibilityService.kt`

- [ ] **Step 1: Write ElementLocator.kt**

```kotlin
package com.voicerider.accessibility.strategy

import android.view.accessibility.AccessibilityNodeInfo
import com.voicerider.core.util.Logger

enum class LocateStrategy { RESOURCE_ID, TEXT, CONTENT_DESC }

data class ElementTarget(
    val resourceId: String? = null,
    val text: String? = null,
    val contentDesc: String? = null
)

object ElementLocator {

    fun findAndClick(
        root: AccessibilityNodeInfo,
        target: ElementTarget
    ): Boolean {
        var node = findNode(root, target)
        if (node != null) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Logger.i("ElementLocator: clicked ${target.text ?: target.resourceId} → $clicked")
            return clicked
        }
        Logger.w("ElementLocator: not found → ${target.text ?: target.resourceId}")
        return false
    }

    fun extractText(
        root: AccessibilityNodeInfo,
        target: ElementTarget
    ): String? {
        val node = findNode(root, target)
        return node?.text?.toString()?.trim()
    }

    fun findNode(root: AccessibilityNodeInfo, target: ElementTarget): AccessibilityNodeInfo? {
        // Layer 1: Resource ID
        target.resourceId?.let { id ->
            root.findAccessibilityNodeInfosByViewId(id)?.firstOrNull()?.let {
                Logger.d("ElementLocator: found by resource-id '$id'")
                return it
            }
        }

        // Layer 2: Text match
        target.text?.let { text ->
            val found = findByText(root, text)
            if (found != null) {
                Logger.d("ElementLocator: found by text '$text'")
                return found
            }
        }

        // Layer 3: Content description
        target.contentDesc?.let { desc ->
            val found = findByContentDesc(root, desc)
            if (found != null) {
                Logger.d("ElementLocator: found by content-desc '$desc'")
                return found
            }
        }

        return null
    }

    private fun findByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text) == true && node.isClickable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findByText(child, text)
            if (found != null) return found
        }
        return null
    }

    private fun findByContentDesc(node: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        if (node.contentDescription?.toString()?.contains(desc) == true && node.isClickable)
            return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findByContentDesc(child, desc)
            if (found != null) return found
        }
        return null
    }
}
```

- [ ] **Step 2: Write MeituanAutomator.kt**

```kotlin
package com.voicerider.accessibility.automator

import android.view.accessibility.AccessibilityNodeInfo
import com.voicerider.accessibility.strategy.ElementLocator
import com.voicerider.accessibility.strategy.ElementTarget
import com.voicerider.core.config.AppConfig
import com.voicerider.core.util.Logger

class MeituanAutomator {
    private var retryCount = 0

    fun clickAcceptButton(root: AccessibilityNodeInfo): Boolean {
        Logger.i("MeituanAutomator: clicking accept button")
        return clickWithRetry(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/btn_accept",
            text = "抢单"
        ))
    }

    fun clickRejectButton(root: AccessibilityNodeInfo): Boolean {
        return clickWithRetry(root, ElementTarget(
            text = "拒单"
        ))
    }

    fun clickPickupDone(root: AccessibilityNodeInfo): Boolean {
        return clickWithRetry(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/btn_pickup_done",
            text = "确认取餐"
        ))
    }

    fun clickDeliveryDone(root: AccessibilityNodeInfo): Boolean {
        return clickWithRetry(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/btn_delivery_done",
            text = "确认送达"
        ))
    }

    fun extractOrderInfo(root: AccessibilityNodeInfo): Map<String, String> {
        val info = mutableMapOf<String, String>()
        info["merchant"] = ElementLocator.extractText(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/tv_merchant_name"
        )) ?: ""
        info["customer_address"] = ElementLocator.extractText(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/tv_customer_address"
        )) ?: ""
        info["amount"] = ElementLocator.extractText(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/tv_amount"
        )) ?: ""
        Logger.d("MeituanAutomator: extracted order info — $info")
        return info
    }

    fun extractAddress(root: AccessibilityNodeInfo): String? {
        return ElementLocator.extractText(root, ElementTarget(
            resourceId = "${AppConfig.MEITUAN_RIDER_PACKAGE}:id/tv_address"
        )) ?: ElementLocator.extractText(root, ElementTarget(
            text = "地址"
        ))
    }

    private fun clickWithRetry(root: AccessibilityNodeInfo, target: ElementTarget): Boolean {
        retryCount = 0
        while (retryCount < AppConfig.MAX_AUTOMATION_RETRIES) {
            if (ElementLocator.findAndClick(root, target)) return true
            retryCount++
            Logger.w("MeituanAutomator: retry $retryCount/${AppConfig.MAX_AUTOMATION_RETRIES}")
        }
        return false
    }
}
```

- [ ] **Step 3: Write OrderActionHandler.kt**

```kotlin
package com.voicerider.accessibility.automator

import android.view.accessibility.AccessibilityNodeInfo
import com.voicerider.core.model.CommandType
import com.voicerider.core.model.Order
import com.voicerider.core.model.OrderStatus
import com.voicerider.core.model.VoiceCommand
import com.voicerider.core.util.Logger

class OrderActionHandler(
    private val automator: MeituanAutomator
) {
    private var currentOrder: Order? = null

    fun handleCommand(
        command: VoiceCommand,
        root: AccessibilityNodeInfo
    ): ActionResult {
        val order = currentOrder

        // Validate state
        command.type.requiredStatus?.let { required ->
            if (order?.status != required) {
                Logger.w("OrderActionHandler: state mismatch — need $required, have ${order?.status}")
                return ActionResult(
                    success = false,
                    message = "当前不是${required.label}状态，无法执行${command.rawText}",
                    requiresManual = false
                )
            }
        }

        val success = when (command.type) {
            CommandType.ACCEPT_ORDER -> automator.clickAcceptButton(root)
            CommandType.REJECT_ORDER -> automator.clickRejectButton(root)
            CommandType.PICKUP_DONE -> automator.clickPickupDone(root)
            CommandType.DELIVERY_DONE -> automator.clickDeliveryDone(root)
            else -> false // NAV/CALL/SMS handled by :app
        }

        if (!success) {
            return ActionResult(
                success = false,
                message = "${command.rawText}操作失败，请手动点击",
                requiresManual = true
            )
        }

        updateOrderState(command.type)
        return ActionResult(
            success = true,
            message = "${command.rawText}成功",
            requiresManual = false
        )
    }

    fun updateCurrentOrder(order: Order) {
        currentOrder = order
        Logger.i("OrderActionHandler: current order = ${order.id} (${order.status.label})")
    }

    private fun updateOrderState(commandType: CommandType) {
        currentOrder = currentOrder?.copy(
            status = when (commandType) {
                CommandType.ACCEPT_ORDER -> OrderStatus.ACCEPTED
                CommandType.PICKUP_DONE -> OrderStatus.PICKED_UP
                CommandType.DELIVERY_DONE -> OrderStatus.COMPLETED
                else -> currentOrder?.status ?: OrderStatus.WAITING
            }
        )
    }
}

data class ActionResult(
    val success: Boolean,
    val message: String,
    val requiresManual: Boolean
)
```

- [ ] **Step 4: Write RiderAccessibilityService.kt**

```kotlin
package com.voicerider.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.voicerider.accessibility.automator.MeituanAutomator
import com.voicerider.accessibility.automator.OrderActionHandler
import com.voicerider.core.config.AppConfig
import com.voicerider.core.util.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class RiderAccessibilityService : AccessibilityService() {

    private val automator = MeituanAutomator()
    private val handler = OrderActionHandler(automator)

    private val _screenRoot = MutableSharedFlow<android.view.accessibility.AccessibilityNodeInfo>(
        extraBufferCapacity = 1
    )
    val screenRoot: SharedFlow<android.view.accessibility.AccessibilityNodeInfo> = _screenRoot

    val orderActionHandler: OrderActionHandler get() = handler

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName != AppConfig.MEITUAN_RIDER_PACKAGE) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                rootInActiveWindow?.let { _screenRoot.tryEmit(it) }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Logger.i("RiderAccessibilityService: connected")
    }

    override fun onInterrupt() {
        Logger.w("RiderAccessibilityService: interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.i("RiderAccessibilityService: destroyed")
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add :accessibility/
git commit -m "feat(accessibility): add ElementLocator, MeituanAutomator, OrderActionHandler, RiderAccessibilityService"
```

---

## Phase 5: :navigation — AMap Navigation

### Task 5.1: Write Navigation Components

**Files:**
- Create: `:navigation/src/main/java/com/voicerider/navigation/planner/RoutePlanner.kt`
- Create: `:navigation/src/main/java/com/voicerider/navigation/navigator/AmapNavigator.kt`

- [ ] **Step 1: Write RoutePlanner.kt**

```kotlin
package com.voicerider.navigation.planner

import com.voicerider.core.model.RouteInfo
import com.voicerider.core.util.Logger

class RoutePlanner {
    private var amapSdkAvailable = false

    fun initialize(webApiKey: String): Boolean {
        Logger.i("RoutePlanner: initializing with AMap key")
        // TODO: Replace with AMap Services SDK init
        // AMapLocationClient.setApiKey(webApiKey)
        amapSdkAvailable = true
        return true
    }

    suspend fun planRoute(
        fromName: String,
        toName: String,
        fromLat: Double = 0.0,
        fromLng: Double = 0.0,
        toLat: Double = 0.0,
        toLng: Double = 0.0,
        mode: String = "BIKE"
    ): RouteInfo? {
        if (!amapSdkAvailable) {
            Logger.w("RoutePlanner: AMap SDK not available")
            return null
        }

        Logger.i("RoutePlanner: planning $mode route from '$fromName' to '$toName'")

        // TODO: Replace with AMap route search
        // val query = RouteSearch.Query(fromLatLng, toLatLng, RouteSearch.DrivingDefault)
        // val result = routeSearch.calculateRoute(query)

        return RouteInfo(
            fromName = fromName,
            toName = toName,
            fromLat = fromLat,
            fromLng = fromLng,
            toLat = toLat,
            toLng = toLng,
            distanceMeters = 2300f,
            durationSeconds = 480
        )
    }
}
```

- [ ] **Step 2: Write AmapNavigator.kt**

```kotlin
package com.voicerider.navigation.navigator

import com.voicerider.core.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class NavState { IDLE, PLANNING, NAVIGATING, ARRIVED, ERROR }

class AmapNavigator {
    private val _navState = MutableStateFlow(NavState.IDLE)
    val navState: StateFlow<NavState> = _navState

    var currentDistance: Float = 0f
    var currentDuration: Int = 0

    fun startNavigation() {
        Logger.i("AmapNavigator: starting navigation")
        _navState.value = NavState.NAVIGATING
        // TODO: Replace with AMap Navi SDK
        // AMapNavi.startNavi(NaviType.GPS)
    }

    fun stopNavigation() {
        Logger.i("AmapNavigator: stopping navigation")
        _navState.value = NavState.IDLE
        // TODO: AMapNavi.stopNavi()
    }

    fun getRemainingInfo(): String {
        return "剩余 ${currentDistance.toInt()} 米，约 ${currentDuration / 60} 分钟"
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add :navigation/
git commit -m "feat(navigation): add RoutePlanner and AmapNavigator with AMap stubs"
```

---

## Phase 6: :app — UI & Service Orchestration

### Task 6.1: Create Android Resources

**Files:**
- Create: `:app/src/main/res/values/colors.xml`
- Create: `:app/src/main/res/values/themes.xml`
- Create: `:app/src/main/res/values/styles.xml`
- Create: `:app/src/main/res/values/strings.xml`
- Create: `:app/src/main/res/drawable/bg_order_card.xml`
- Create: `:app/src/main/res/drawable/bg_voice_bar.xml`
- Create: `:app/src/main/res/drawable/bg_floating_window.xml`
- Create: `:app/src/main/res/anim/slide_in_right.xml`
- Create: `:app/src/main/res/anim/pulse_scale.xml`
- Create: `:app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write colors.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Background -->
    <color name="bg_warm">#F8F6F2</color>
    <color name="bg_white">#FFFFFF</color>
    <color name="bg_dark">#1A1A2E</color>

    <!-- Brand -->
    <color name="brand_primary">#667EEA</color>
    <color name="brand_secondary">#764BA2</color>

    <!-- Semantic -->
    <color name="semantic_success">#38A169</color>
    <color name="semantic_warning">#D69E2E</color>
    <color name="semantic_danger">#E53E3E</color>
    <color name="semantic_info">#3182CE</color>

    <!-- Neutral -->
    <color name="text_primary">#1A1A1A</color>
    <color name="text_secondary">#888888</color>
    <color name="text_hint">#CCCCCC</color>
    <color name="divider">#F0F0F0</color>

    <!-- Tags -->
    <color name="tag_waiting_bg">#FEF9E7</color>
    <color name="tag_waiting_text">#B45309</color>
    <color name="tag_delivering_bg">#EFF6FF</color>
    <color name="tag_delivering_text">#1E40AF</color>

    <!-- Voice bar -->
    <color name="voice_bar_bg">#2C2C2C</color>
</resources>
```

- [ ] **Step 2: Write themes.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.VoiceRider" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/brand_primary</item>
        <item name="colorPrimaryDark">@color/brand_secondary</item>
        <item name="colorAccent">@color/brand_primary</item>
        <item name="android:windowBackground">@color/bg_warm</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>

    <style name="Theme.VoiceRider.Dark" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/brand_primary</item>
        <item name="android:windowBackground">@color/bg_dark</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@color/bg_dark</item>
    </style>
</resources>
```

- [ ] **Step 3: Write strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Voice Rider</string>
    <string name="voice_hint">说"美团精灵"或输入指令…</string>
    <string name="label_orders">进行中</string>
    <string name="label_ai_reminders">智能提醒</string>
    <string name="label_voice_ready">语音就绪</string>
    <string name="label_accessibility_on">无障碍已开</string>
    <string name="label_today_income">今日 ¥%1$s</string>
    <string name="accessibility_service_desc">Voice Rider 无障碍服务，用于自动化操作美团骑手APP，实现语音控制功能。</string>
</resources>
```

- [ ] **Step 4: Write drawable resources**

bg_order_card.xml:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_white"/>
    <corners android:radius="12dp"/>
</shape>
```

bg_voice_bar.xml:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/voice_bar_bg"/>
    <corners android:radius="22dp"/>
</shape>
```

bg_floating_window.xml:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/brand_primary"/>
</shape>
```

- [ ] **Step 5: Write animation resources**

slide_in_right.xml:
```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <translate
        android:fromXDelta="30%p"
        android:toXDelta="0"
        android:duration="200"
        android:interpolator="@android:interpolator/decelerate_cubic"/>
    <alpha
        android:fromAlpha="0.0"
        android:toAlpha="1.0"
        android:duration="200"/>
</set>
```

pulse_scale.xml:
```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <scale
        android:fromXScale="1.0"
        android:toXScale="1.12"
        android:fromYScale="1.0"
        android:toYScale="1.12"
        android:pivotX="50%"
        android:pivotY="50%"
        android:duration="600"
        android:repeatMode="reverse"
        android:repeatCount="infinite"/>
</set>
```

- [ ] **Step 6: Write AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
    <uses-permission android:name="android.permission.CALL_PHONE"/>
    <uses-permission android:name="android.permission.VIBRATE"/>
    <uses-permission android:name="android.permission.BLUETOOTH"/>

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.VoiceRider">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

        <activity
            android:name=".ui.OrderDetailActivity"
            android:exported="false"/>

        <activity
            android:name=".ui.SettingsActivity"
            android:exported="false"/>

        <service
            android:name=".service.VoiceRoutingService"
            android:foregroundServiceType="microphone"
            android:exported="false"/>

        <service
            android:name=".service.FloatingWindowService"
            android:foregroundServiceType="specialUse"
            android:exported="false"/>

        <service
            android:name="com.voicerider.accessibility.service.RiderAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService"/>
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config"/>
        </service>

    </application>
</manifest>
```

- [ ] **Step 7: Create accessibility service config**

Create `:app/src/main/res/xml/accessibility_service_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_desc"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="100"/>
```

- [ ] **Step 8: Commit**

```bash
git add :app/src/main/res/
git commit -m "feat(app): add resources — colors, themes, drawables, animations, manifest"
```

### Task 6.2: Write Homepage Layout XML

**Files:**
- Create: `:app/src/main/res/layout/activity_main.xml`
- Create: `:app/src/main/res/layout/item_order_card.xml`
- Create: `:app/src/main/res/layout/item_ai_reminder.xml`
- Create: `:app/src/main/res/layout/fragment_home.xml`
- Create: `:app/src/main/res/layout/view_voice_input_bar.xml`

- [ ] **Step 1: Write fragment_home.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/bg_warm"
    android:paddingHorizontal="12dp">

    <!-- Header -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingTop="16dp"
        android:paddingBottom="8dp">
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/app_name"
                android:textSize="20sp"
                android:textColor="@color/text_primary"
                android:fontFamily="sans-serif-light"
                android:letterSpacing="0.04"/>
            <TextView
                android:id="@+id/tv_status_line"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="10sp"
                android:textColor="@color/text_secondary"
                android:layout_marginTop="2dp"/>
        </LinearLayout>
        <TextView
            android:id="@+id/tv_today_income"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="14sp"
            android:textColor="@color/text_primary"/>
    </LinearLayout>

    <!-- Content: Left orders + Right AI reminders -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:orientation="horizontal"
        android:baselineAligned="false">

        <!-- Left: Order List (55%) -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1.1"
            android:orientation="vertical"
            android:paddingEnd="4dp">
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/label_orders"
                android:textSize="10sp"
                android:textColor="@color/text_hint"
                android:letterSpacing="0.08"
                android:layout_marginBottom="6dp"/>
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rv_orders"
                android:layout_width="match_parent"
                android:layout_height="0dp"
                android:layout_weight="1"/>
        </LinearLayout>

        <!-- Right: AI Reminders (45%) -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="0.9"
            android:orientation="vertical"
            android:paddingStart="4dp">
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginBottom="6dp">
                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/label_ai_reminders"
                    android:textSize="10sp"
                    android:textColor="@color/text_hint"
                    android:letterSpacing="0.08"/>
                <TextView
                    android:id="@+id/tv_reminder_count"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textSize="10sp"
                    android:textColor="@color/text_hint"/>
            </LinearLayout>

            <!-- AI Reminders Card -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="0dp"
                android:layout_weight="1"
                app:cardCornerRadius="12dp"
                app:cardElevation="1dp"
                app:cardBackgroundColor="@color/bg_white">
                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/rv_reminders"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:padding="12dp"/>
            </androidx.cardview.widget.CardView>

            <!-- Stats Row -->
            <LinearLayout
                android:id="@+id/ll_stats"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center"
                android:paddingVertical="12dp"/>
        </LinearLayout>
    </LinearLayout>

    <!-- Voice Input Bar -->
    <include
        android:id="@+id/voice_input_bar"
        layout="@layout/view_voice_input_bar"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:layout_marginVertical="10dp"/>

</LinearLayout>
```

- [ ] **Step 2: Write view_voice_input_bar.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:background="@drawable/bg_voice_bar"
    android:gravity="center_vertical"
    android:paddingHorizontal="4dp">

    <FrameLayout
        android:layout_width="34dp"
        android:layout_height="34dp"
        android:layout_marginStart="2dp">
        <View
            android:layout_width="34dp"
            android:layout_height="34dp"
            android:background="@drawable/bg_mic_button"/>
        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:text="🎤"
            android:textSize="14sp"
            android:gravity="center"/>
    </FrameLayout>

    <EditText
        android:id="@+id/et_command"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:background="@null"
        android:hint="@string/voice_hint"
        android:textColorHint="#666666"
        android:textColor="@color/text_primary"
        android:textSize="11sp"
        android:paddingHorizontal="8dp"
        android:inputType="text"
        android:maxLines="1"/>

    <TextView
        android:id="@+id/tv_send"
        android:layout_width="30dp"
        android:layout_height="30dp"
        android:text="📤"
        android:textSize="12sp"
        android:gravity="center"
        android:alpha="0.3"/>
</LinearLayout>
```

bg_mic_button.xml:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/bg_white"/>
</shape>
```

- [ ] **Step 3: Write item_order_card.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardElevation="1dp"
    app:cardBackgroundColor="@color/bg_white"
    android:layout_marginBottom="6dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="14dp">

        <!-- Title row -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">
            <TextView
                android:id="@+id/tv_merchant_name"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textSize="14sp"
                android:textColor="@color/text_primary"
                android:fontFamily="sans-serif-medium"/>
            <TextView
                android:id="@+id/tv_status_tag"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="10sp"
                android:paddingHorizontal="8dp"
                android:paddingVertical="2dp"
                android:background="@drawable/bg_status_tag"/>
        </LinearLayout>

        <!-- Divider -->
        <View
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="@color/divider"
            android:layout_marginVertical="10dp"/>

        <!-- Info row: amount + distance -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="金额"
                    android:textSize="9sp"
                    android:textColor="@color/text_hint"/>
                <TextView
                    android:id="@+id/tv_amount"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textSize="17sp"
                    android:textColor="@color/text_primary"/>
            </LinearLayout>
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="距离"
                    android:textSize="9sp"
                    android:textColor="@color/text_hint"/>
                <TextView
                    android:id="@+id/tv_distance"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textSize="17sp"
                    android:textColor="@color/text_primary"/>
            </LinearLayout>
        </LinearLayout>

        <!-- Address -->
        <TextView
            android:id="@+id/tv_address"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="11sp"
            android:textColor="@color/text_secondary"
            android:layout_marginTop="10dp"
            android:paddingTop="8dp"
            android:drawablePadding="4dp"/>

    </LinearLayout>
</androidx.cardview.widget.CardView>
```

bg_status_tag.xml:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/tag_waiting_bg"/>
    <corners android:radius="8dp"/>
</shape>
```

- [ ] **Step 4: Write item_ai_reminder.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:paddingVertical="7dp"
    android:gravity="top">

    <View
        android:id="@+id/v_level_dot"
        android:layout_width="6dp"
        android:layout_height="6dp"
        android:layout_marginTop="4dp"
        android:layout_marginEnd="8dp"
        android:background="@drawable/bg_dot_danger"/>

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">
        <TextView
            android:id="@+id/tv_reminder_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="11sp"
            android:textColor="@color/text_primary"
            android:lineSpacingMultiplier="1.2"/>
        <TextView
            android:id="@+id/tv_reminder_time"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="9sp"
            android:textColor="@color/text_hint"
            android:layout_marginTop="2dp"/>
    </LinearLayout>
</LinearLayout>
```

bg_dot_danger.xml (etc — generate per level):
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/semantic_danger"/>
</shape>
```

- [ ] **Step 5: Write activity_main.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <fragment
        android:id="@+id/fragment_home"
        android:name="com.voicerider.app.ui.home.HomeFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

- [ ] **Step 6: Commit**

```bash
git add :app/src/main/res/layout/ :app/src/main/res/drawable/
git commit -m "feat(app): add homepage layout XML — fragment, cards, voice bar, reminders"
```

### Task 6.3: Write Homepage Kotlin Code

**Files:**
- Create: `:app/src/main/java/com/voicerider/app/ui/MainActivity.kt`
- Create: `:app/src/main/java/com/voicerider/app/ui/home/HomeFragment.kt`
- Create: `:app/src/main/java/com/voicerider/app/ui/home/OrderListAdapter.kt`
- Create: `:app/src/main/java/com/voicerider/app/ui/home/AIReminderAdapter.kt`
- Create: `:app/src/main/java/com/voicerider/app/viewmodel/HomeViewModel.kt`

- [ ] **Step 1: Write MainActivity.kt**

```kotlin
package com.voicerider.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.voicerider.app.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
```

- [ ] **Step 2: Write HomeViewModel.kt**

```kotlin
package com.voicerider.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.voicerider.core.model.AirReminder
import com.voicerider.core.model.Order
import com.voicerider.core.model.ReminderLevel
import com.voicerider.core.model.ReminderType

class HomeViewModel : ViewModel() {
    private val _orders = MutableLiveData<List<Order>>(emptyList())
    val orders: LiveData<List<Order>> = _orders

    private val _reminders = MutableLiveData<List<AirReminder>>(emptyList())
    val reminders: LiveData<List<AirReminder>> = _reminders

    private val _todayCompleted = MutableLiveData(0)
    val todayCompleted: LiveData<Int> = _todayCompleted

    private val _todayInProgress = MutableLiveData(0)
    val todayInProgress: LiveData<Int> = _todayInProgress

    private val _todayIncome = MutableLiveData(0.0)
    val todayIncome: LiveData<Double> = _todayIncome

    private val _isVoiceReady = MutableLiveData(true)
    val isVoiceReady: LiveData<Boolean> = _isVoiceReady

    init {
        loadMockData()
    }

    fun onOrderClicked(order: Order) {
        // Navigate to OrderDetailActivity
    }

    fun onVoiceInput(text: String) {
        // Dispatch to VoiceRoutingService
    }

    private fun loadMockData() {
        _todayCompleted.value = 8
        _todayInProgress.value = 2
        _todayIncome.value = 186.0

        _orders.value = listOf(
            Order(
                id = "20240612038",
                status = com.voicerider.core.model.OrderStatus.ACCEPTED,
                merchantName = "肯德基",
                merchantAddress = "万达店",
                customerName = "张先生",
                customerAddress = "万达广场3号楼1203",
                customerPhone = "138****5678",
                amount = 32.5,
                distanceKm = 2.3f
            ),
            Order(
                id = "20240612037",
                status = com.voicerider.core.model.OrderStatus.DELIVERING,
                merchantName = "麦当劳",
                merchantAddress = "银泰店",
                customerName = "李女士",
                customerAddress = "银泰城A座508",
                customerPhone = "139****1234",
                amount = 28.0,
                distanceKm = 1.8f
            )
        )

        _reminders.value = listOf(
            AirReminder(
                type = ReminderType.CUSTOMER_URGE,
                level = ReminderLevel.URGENT,
                title = "#038 顾客催单",
                message = "顾客发消息：\"到哪了？\"",
                orderId = "20240612038"
            ),
            AirReminder(
                type = ReminderType.PICKUP_REMINDER,
                level = ReminderLevel.IMPORTANT,
                title = "#037 取餐提醒",
                message = "预计出餐时间已到，请尽快取餐",
                orderId = "20240612037"
            ),
            AirReminder(
                type = ReminderType.ROUTE_SUGGESTION,
                level = ReminderLevel.INFO,
                title = "#038 路线建议",
                message = "南门进电梯更快到3号楼",
                orderId = "20240612038"
            )
        )
    }
}
```

- [ ] **Step 3: Write OrderListAdapter.kt**

```kotlin
package com.voicerider.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.voicerider.app.R
import com.voicerider.core.model.Order
import com.voicerider.core.model.OrderStatus

class OrderListAdapter(
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderViewHolder>() {

    private var orders: List<Order> = emptyList()

    fun submitList(list: List<Order>) {
        orders = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_card, parent, false)
        return OrderViewHolder(view, onOrderClick)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount() = orders.size
}

class OrderViewHolder(
    private val view: android.view.View,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.ViewHolder(view) {

    private val tvMerchant = view.findViewById<android.widget.TextView>(R.id.tv_merchant_name)
    private val tvStatus = view.findViewById<android.widget.TextView>(R.id.tv_status_tag)
    private val tvAmount = view.findViewById<android.widget.TextView>(R.id.tv_amount)
    private val tvDistance = view.findViewById<android.widget.TextView>(R.id.tv_distance)
    private val tvAddress = view.findViewById<android.widget.TextView>(R.id.tv_address)

    fun bind(order: Order) {
        tvMerchant.text = "${order.merchantName} ${order.merchantAddress}"

        tvStatus.apply {
            text = order.status.label
            when (order.status) {
                OrderStatus.ACCEPTED -> {
                    setTextColor(view.context.getColor(R.color.tag_waiting_text))
                    setBackgroundResource(R.drawable.bg_status_tag_waiting)
                }
                OrderStatus.DELIVERING -> {
                    setTextColor(view.context.getColor(R.color.tag_delivering_text))
                    setBackgroundResource(R.drawable.bg_status_tag_delivering)
                }
                else -> {
                    setTextColor(view.context.getColor(R.color.text_secondary))
                    setBackgroundResource(R.drawable.bg_status_tag)
                }
            }
        }

        tvAmount.text = "¥${order.amount}"
        tvDistance.text = "${order.distanceKm}km"
        tvAddress.text = "📍 ${order.customerAddress}"

        view.setOnClickListener { onOrderClick(order) }
    }
}
```

- [ ] **Step 4: Write AIReminderAdapter.kt**

```kotlin
package com.voicerider.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.voicerider.app.R
import com.voicerider.core.model.AirReminder
import com.voicerider.core.model.ReminderLevel

class AIReminderAdapter :
    RecyclerView.Adapter<AIReminderViewHolder>() {

    private var reminders: List<AirReminder> = emptyList()

    fun submitList(list: List<AirReminder>) {
        reminders = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AIReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_reminder, parent, false)
        return AIReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: AIReminderViewHolder, position: Int) {
        holder.bind(reminders[position])
        // Animate entry
        holder.itemView.startAnimation(
            AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_in_right)
        )
    }

    override fun getItemCount() = reminders.size
}

class AIReminderViewHolder(
    private val view: android.view.View
) : RecyclerView.ViewHolder(view) {

    private val dot = view.findViewById<android.view.View>(R.id.v_level_dot)
    private val title = view.findViewById<android.widget.TextView>(R.id.tv_reminder_title)
    private val time = view.findViewById<android.widget.TextView>(R.id.tv_reminder_time)

    fun bind(reminder: AirReminder) {
        dot.setBackgroundResource(when (reminder.level) {
            ReminderLevel.URGENT -> R.drawable.bg_dot_danger
            ReminderLevel.IMPORTANT -> R.drawable.bg_dot_warning
            ReminderLevel.INFO -> R.drawable.bg_dot_info
            ReminderLevel.SUMMARY -> R.drawable.bg_dot_success
        })

        title.text = "${reminder.title}\n${reminder.message}"
        time.text = "${reminder.timestamp}前" // Simplified for brevity
    }
}
```

- [ ] **Step 5: Write HomeFragment.kt**

```kotlin
package com.voicerider.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voicerider.app.R

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var orderAdapter: OrderListAdapter
    private lateinit var reminderAdapter: AIReminderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupOrders(view)
        setupReminders(view)
        setupStats(view)
        setupVoiceBar(view)
        observeData()
    }

    private fun setupOrders(view: View) {
        orderAdapter = OrderListAdapter { order -> viewModel.onOrderClicked(order) }
        view.findViewById<RecyclerView>(R.id.rv_orders).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = orderAdapter
        }
    }

    private fun setupReminders(view: View) {
        reminderAdapter = AIReminderAdapter()
        view.findViewById<RecyclerView>(R.id.rv_reminders).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reminderAdapter
        }
    }

    private fun setupStats(view: View) {
        val statsContainer = view.findViewById<LinearLayout>(R.id.ll_stats)
        // Stats are set via observeData()
    }

    private fun setupVoiceBar(view: View) {
        view.findViewById<android.widget.EditText>(R.id.et_command).setOnEditorActionListener { _, _, _ ->
            val text = view.findViewById<android.widget.EditText>(R.id.et_command).text.toString()
            if (text.isNotBlank()) {
                viewModel.onVoiceInput(text)
                view.findViewById<android.widget.EditText>(R.id.et_command).text?.clear()
                true
            } else false
        }
    }

    private fun observeData() {
        viewModel.orders.observe(viewLifecycleOwner) { orderAdapter.submitList(it) }
        viewModel.reminders.observe(viewLifecycleOwner) { reminders ->
            reminderAdapter.submitList(reminders)
            view?.findViewById<TextView>(R.id.tv_reminder_count)?.text = "${reminders.size}条"
        }
        viewModel.todayIncome.observe(viewLifecycleOwner) { income ->
            view?.findViewById<TextView>(R.id.tv_today_income)?.text =
                getString(R.string.label_today_income, income.toInt().toString())
        }
        viewModel.isVoiceReady.observe(viewLifecycleOwner) { ready ->
            val statusText = if (ready) "● 语音 · 无障碍 已开启" else "⚠ 服务未完全就绪"
            view?.findViewById<TextView>(R.id.tv_status_line)?.text = statusText
        }
    }
}
```

- [ ] **Step 6: Verify build compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add :app/src/main/java/
git commit -m "feat(app): add MainActivity, HomeFragment, adapters, HomeViewModel"
```

### Task 6.4: Write Services

**Files:**
- Create: `:app/src/main/java/com/voicerider/app/service/VoiceRoutingService.kt`
- Create: `:app/src/main/java/com/voicerider/app/service/FloatingWindowService.kt`

- [ ] **Step 1: Write VoiceRoutingService.kt**

```kotlin
package com.voicerider.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.voicerider.app.R
import com.voicerider.core.model.CommandType
import com.voicerider.core.model.VoiceCommand
import com.voicerider.core.util.Logger
import com.voicerider.voice.engine.CommandRecognizer
import com.voicerider.voice.engine.TtsSpeaker
import com.voicerider.voice.engine.WakeUpEngine
import com.voicerider.voice.fallback.SystemRecognizer

class VoiceRoutingService : Service() {

    private lateinit var wakeUpEngine: WakeUpEngine
    private lateinit var commandRecognizer: CommandRecognizer
    private lateinit var ttsSpeaker: TtsSpeaker

    private var onCommand: ((VoiceCommand) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        Logger.i("VoiceRoutingService: created")

        wakeUpEngine = WakeUpEngine()
        ttsSpeaker = TtsSpeaker(this)
        commandRecognizer = CommandRecognizer(SystemRecognizer(this))

        wakeUpEngine.initialize()
        ttsSpeaker.initialize()

        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        wakeUpEngine.startListening()
        return START_STICKY
    }

    fun setOnCommandListener(listener: (VoiceCommand) -> Unit) {
        onCommand = listener
    }

    fun handleTextCommand(text: String) {
        val commandType = CommandType.fromText(text)
        if (commandType != null) {
            val cmd = VoiceCommand(type = commandType, rawText = text, confidence = 1.0f)
            Logger.i("VoiceRoutingService: text command matched ${commandType.name}")
            onCommand?.invoke(cmd)
        } else {
            ttsSpeaker.speak("未识别的指令")
        }
    }

    fun speakFeedback(message: String) {
        ttsSpeaker.speak(message)
    }

    private fun startForeground() {
        val channelId = "voice_rider_voice"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "语音服务", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Voice Rider")
            .setContentText("语音服务运行中")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(1001, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        wakeUpEngine.destroy()
        commandRecognizer.destroy()
        ttsSpeaker.destroy()
        super.onDestroy()
    }
}
```

- [ ] **Step 2: Write FloatingWindowService.kt**

```kotlin
package com.voicerider.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import com.voicerider.app.R
import com.voicerider.core.util.Logger

class FloatingWindowService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isListening = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingWindow()
        startForeground()
    }

    private fun createFloatingWindow() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.view_floating_window, null)

        val params = WindowManager.LayoutParams(
            56.dpToPx(),
            56.dpToPx(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        floatingView?.setOnClickListener {
            Logger.d("FloatingWindow: clicked")
            toggleListening()
        }

        floatingView?.setOnLongClickListener {
            Logger.d("FloatingWindow: long clicked")
            // Launch MainActivity
            true
        }

        windowManager?.addView(floatingView, params)
    }

    fun toggleListening() {
        isListening = !isListening
        if (isListening) {
            floatingView?.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.pulse_scale)
            )
        } else {
            floatingView?.clearAnimation()
        }
    }

    private fun startForeground() {
        val channelId = "voice_rider_floating"
        if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            val channel = NotificationChannel(
                channelId, "悬浮窗", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Voice Rider")
            .setContentText("悬浮窗运行中")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
        startForeground(1002, notification)
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        floatingView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add :app/src/main/java/com/voicerider/app/service/
git commit -m "feat(app): add VoiceRoutingService and FloatingWindowService"
```

---

## Phase 7: Integration & Deployment Prep

### Task 7.1: Final Build Verification

- [ ] **Step 1: Full project build**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL, APK at `:app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 2: Run all unit tests**

```bash
./gradlew testDebugUnitTest
```

Expected: All test pass

- [ ] **Step 3: Commit and push**

```bash
git add -A
git commit -m "feat: Voice Rider — complete initial implementation

5-module Android project:
- :core — data models, enums, utilities (11 unit tests)
- :voice — Xunfei wake word engine + command recognizer + TTS + system fallback
- :accessibility — AccessibilityService + Meituan UI automator (3-tier element locator)
- :navigation — AMap route planner + navigator
- :app — Homepage UI (warm minimal design) + VoiceRoutingService + FloatingWindow

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"

git push origin main
```

---

## Post-Implementation Checklist

After all tasks complete, verify the following manually:

- [ ] Open app → see homepage with warm background and empty order list
- [ ] Grant Accessibility permission → service starts
- [ ] Open Meituan rider app → floating window appears
- [ ] Voice input via text bar → command matched and dispatched
- [ ] Check logcat for `VoiceRider` tag → verify all modules working

## SDK Integration (Requires Manual Steps)

These items require real-world registration and cannot be automated:
1. **Xunfei SDK**: Register at https://www.xfyun.cn/ → get AppID → replace stubs in `:voice/engine/`
2. **AMap SDK**: Register at https://lbs.amap.com/ → get API Key → fill `AppConfig.AMAP_WEB_KEY`
3. **Meituan Resource IDs**: Use UI Automator Viewer on actual Meituan rider APK → update `MeituanAutomator.kt` element targets
