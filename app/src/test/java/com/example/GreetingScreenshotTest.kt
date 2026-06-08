package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.PdfBook
import com.example.ui.screens.BookShelfItem
import com.example.ui.theme.MyApplicationTheme
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleBook = PdfBook(
      id = 11,
      title = "دیوان حافظ شیرازی",
      filePath = "/dummy/path.pdf",
      category = "شعر و ادبیات",
      totalPages = 120,
      lastPageRead = 24,
      fileSize = 4096 * 1024L,
      isFavorite = true
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        BookShelfItem(
          book = sampleBook,
          onOpen = {},
          onToggleFavorite = {},
          onDelete = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
