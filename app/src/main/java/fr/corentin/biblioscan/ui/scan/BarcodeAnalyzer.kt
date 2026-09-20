package fr.corentin.biblioscan.ui.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import fr.corentin.biblioscan.util.IsbnUtils

/**
 * CameraX analyzer that decodes EAN-13 barcodes (the format used by book
 * ISBNs) from the live camera feed via ZXing, without any Google Play
 * Services dependency.
 */
class BarcodeAnalyzer(
    private val onIsbnDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.EAN_13),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    @Volatile
    private var lastDetected: String? = null

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val packed = packYPlane(plane.buffer, plane.rowStride, image.width, image.height)
            val (rotated, rotatedWidth, rotatedHeight) =
                rotate(packed, image.width, image.height, image.imageInfo.rotationDegrees)

            val source = PlanarYUVLuminanceSource(
                rotated, rotatedWidth, rotatedHeight, 0, 0, rotatedWidth, rotatedHeight, false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(bitmap)
            val text = result.text
            if (IsbnUtils.isValidBookEan13(text) && text != lastDetected) {
                lastDetected = text
                onIsbnDetected(text)
            }
        } catch (_: NotFoundException) {
            // No barcode in this frame - expected most of the time.
        } catch (_: Exception) {
            // Decoding hiccup on a single frame - ignore and keep scanning.
        } finally {
            reader.reset()
            image.close()
        }
    }

    /** Allow re-detecting the same code again (e.g. after the user dismisses a result). */
    fun resetLastDetected() {
        lastDetected = null
    }

    /** Strips row-stride padding, returning a tightly packed width*height luminance array. */
    private fun packYPlane(buffer: java.nio.ByteBuffer, rowStride: Int, width: Int, height: Int): ByteArray {
        buffer.rewind()
        val raw = ByteArray(buffer.remaining())
        buffer.get(raw)
        if (rowStride == width) return raw
        val packed = ByteArray(width * height)
        for (row in 0 until height) {
            System.arraycopy(raw, row * rowStride, packed, row * width, width)
        }
        return packed
    }

    /** Rotates a tightly packed single-channel image clockwise by [degrees] (0/90/180/270). */
    private fun rotate(data: ByteArray, width: Int, height: Int, degrees: Int): Triple<ByteArray, Int, Int> {
        if (degrees == 0) return Triple(data, width, height)
        val rotated = ByteArray(data.size)
        return when (degrees) {
            90 -> {
                var i = 0
                for (x in 0 until width) {
                    for (y in height - 1 downTo 0) {
                        rotated[i++] = data[y * width + x]
                    }
                }
                Triple(rotated, height, width)
            }
            180 -> {
                for (i in data.indices) rotated[i] = data[data.size - 1 - i]
                Triple(rotated, width, height)
            }
            270 -> {
                var i = 0
                for (x in width - 1 downTo 0) {
                    for (y in 0 until height) {
                        rotated[i++] = data[y * width + x]
                    }
                }
                Triple(rotated, height, width)
            }
            else -> Triple(data, width, height)
        }
    }
}
