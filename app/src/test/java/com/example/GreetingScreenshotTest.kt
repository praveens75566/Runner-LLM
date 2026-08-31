package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.components.PerformanceHud
import com.example.core.inference.GenerationTokenUpdate
import com.example.ui.theme.LLMRunnerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun performance_hud_screenshot() {
    val sampleUpdate = GenerationTokenUpdate(
        token = " neural",
        accumulatedText = "Testing on-device neural generation stream.",
        isFinished = false,
        isPrefillDone = true,
        totalTokens = 38,
        prefillTokensPerSec = 310.0,
        currentTokensPerSec = 28.5,
        timeToFirstTokenMs = 95L,
        currentThermalState = "NORMAL",
        activeThreads = 4,
        kvCacheMemoryMb = 142.5,
        backendUsed = "Adreno OpenCL",
        kvCacheTokens = 38
    )

    composeTestRule.setContent {
      LLMRunnerTheme {
        PerformanceHud(tokenUpdate = sampleUpdate, isGenerating = true)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/performance_hud.png")
  }
}
