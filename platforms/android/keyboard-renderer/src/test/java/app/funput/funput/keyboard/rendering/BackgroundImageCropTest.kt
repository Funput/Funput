package app.funput.funput.keyboard.rendering

import app.funput.funput.theme.KeyboardThemeBackgroundImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundImageCropTest {
    @Test
    fun aWideImageIsCroppedSidewaysToFillTheKeyboard() {
        val rect = crop(imageWidth = 2000, imageHeight = 1000, framing = framing())

        // Target is 2:1 wide but the image is 2:1 as well, so nothing is trimmed.
        assertEquals(0, rect.left)
        assertEquals(2000, rect.right)
        assertEquals(0, rect.top)
        assertEquals(1000, rect.bottom)
    }

    @Test
    fun aTallImageKeepsFullWidthAndTrimsHeight() {
        val rect = crop(imageWidth = 1000, imageHeight = 4000, framing = framing())

        assertEquals(0, rect.left)
        assertEquals(1000, rect.right)
        // Full width at the target's 2:1 ratio means a 500px tall slice, centred in 4000.
        assertEquals(500, rect.bottom - rect.top)
        assertEquals(1750, rect.top)
    }

    @Test
    fun zoomNarrowsTheCropAroundTheFocalPoint() {
        val zoomed = crop(1000, 1000, framing(zoom = 2f))
        val full = crop(1000, 1000, framing())

        assertTrue(zoomed.width < full.width)
        assertEquals(full.centerX, zoomed.centerX)
    }

    @Test
    fun aFocalPointAtTheEdgeStopsAtTheImageBoundary() {
        val rect = crop(1000, 1000, framing(zoom = 2f, focalX = 1f, focalY = 1f))

        // Clamped rather than sliding past the edge and exposing blank space.
        assertEquals(1000, rect.right)
        assertEquals(1000, rect.bottom)
        assertTrue(rect.left >= 0)
        assertTrue(rect.top >= 0)
    }

    @Test
    fun theCropNeverLeavesTheImage() {
        listOf(0f, 0.5f, 1f).forEach { focus ->
            listOf(1f, 2.5f, 4f).forEach { zoom ->
                val rect = crop(800, 1200, framing(zoom = zoom, focalX = focus, focalY = focus))

                assertTrue("left $rect", rect.left >= 0)
                assertTrue("top $rect", rect.top >= 0)
                assertTrue("right $rect", rect.right <= 800)
                assertTrue("bottom $rect", rect.bottom <= 1200)
            }
        }
    }

    private fun crop(
        imageWidth: Int,
        imageHeight: Int,
        framing: KeyboardThemeBackgroundImage,
    ) = BackgroundImageCrop.sourceRect(
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        targetWidth = 1000,
        targetHeight = 500,
        framing = framing,
    )

    private fun framing(
        zoom: Float = 1f,
        focalX: Float = 0.5f,
        focalY: Float = 0.5f,
    ) = KeyboardThemeBackgroundImage(
        source = "/tmp/image",
        opacity = 1f,
        focalX = focalX,
        focalY = focalY,
        zoom = zoom,
    )
}
