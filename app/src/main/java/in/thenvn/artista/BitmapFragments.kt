package `in`.thenvn.artista

import `in`.thenvn.artista.utils.ImageUtils
import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.ceil

/**
 * Helper class to split a bigger bitmap into smaller fragments of size [fragmentWidth] * [fragmentHeight].
 * Apart from dimensions splitting positions are also dictated by overlapping size [overlapSize], amount
 * by which the fragments are allowed to overlap (share rows or cols of pixels at their edges left or
 * top respectively)
 *
 * @param bitmap Original bitmap to form fragments from
 * @param fragmentWidth Width of fragment to split/crop
 * @param fragmentHeight Height of fragment to split/crop
 * @param overlapSize Pixel size by which bitmap fragments overlap at left and top edges. Fragments
 *     at right and bottom edge may use other (calculated) value for overlap to fit in.
 */
class BitmapFragments(
    bitmap: Bitmap,
    private val fragmentWidth: Int,
    private val fragmentHeight: Int,
    private val overlapSize: Int
) {
    /**
     * Amounts by which the bitmap fragment cropping window (size [fragmentWidth] * [fragmentHeight])
     * will move horizontally and vertically to crop subsequent fragments from [bitmap]}.
     */
    private val strideX: Int
        get() = fragmentWidth - overlapSize
    private val strideY: Int
        get() = fragmentHeight - overlapSize

    /**
     * Fixed size Array to hold bitmap fragments
     */
    private val fragments: Array<Bitmap>

    /**
     * No. of fragments generated while moving crop window horizontally and vertically
     */
    private val nCols: Int = ceil((bitmap.width - overlapSize).toFloat() / (strideX)).toInt()
    private val nRows: Int = ceil((bitmap.height - overlapSize).toFloat() / (strideY)).toInt()

    /**
     * Total no. of cropped fragments
     */
    val numberOfFragments: Int
        get() = nCols * nRows

    /**
     * Original dimensions of passed input bitmap if now padded else includes
     * padded dimensions
     */
    private var bitmapWidth: Int = bitmap.width
    private var bitmapHeight: Int = bitmap.height

    /**
     * Indices at which padding starts in case bitmap was padded.
     * Negative int indicate it was not padded for that dimension
     */
    private var padLeft: Int = -1
    private var padTop: Int = -1

    init {

        if (overlapSize > fragmentWidth || overlapSize > fragmentHeight) {
            throw IllegalArgumentException("Overlap size greater than fragment dimension!")
        }

        fragments = Array(nCols * nRows) {
            Bitmap.createBitmap(
                fragmentWidth,
                fragmentHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        // Handle smaller images with padding to minimum required dimension
        var tempbitmap = bitmap
        if (bitmap.width < fragmentWidth) {
            tempbitmap = ImageUtils.padIfRequired(bitmap, fragmentWidth, fragmentHeight)
            padLeft = bitmap.width
            bitmapWidth = tempbitmap.width

            if (bitmap.height < fragmentHeight) {
                padTop = bitmap.height
                bitmapHeight = tempbitmap.height
            }
        }

        // Split bitmap to smaller fragments by iterating through grid (nRows * nCols) of bitmap cells
        // of size (fragmentWidth * fragmentHeight)
        var idx = 0
        for (y in 0 until nRows) {
            for (x in 0 until nCols) {

                // Crop fragments by moving by a strides of strideX and strideY respectively.
                // For the fragment at extreme edges (right & top) crop to maintain uniform size of
                // (fragmentWidth * fragmentHeight) irrespective of corresponding strides.

                val w: Int = when {
                    (x == 0) -> 0
                    (x == nCols - 1) -> (bitmapWidth - 1) - fragmentWidth
                    else -> x * strideX
                }

                val h: Int = when {
                    (y == 0) -> 0
                    (y == nRows - 1) -> (bitmapHeight - 1) - fragmentHeight
                    else -> y * strideY
                }

                val fragment = Bitmap.createBitmap(
                    tempbitmap,
                    w, h,
                    fragmentWidth, fragmentHeight
                )

                fragments[idx] = fragment

                idx++
            }
        }

        tempbitmap.recycle()
    }

    /**
     * Reconstruct the original bitmap from the fragments.
     * @return Bitmap of original dimensions
     */
    fun patchFragments(): Bitmap {

        val bitmap = Bitmap.createBitmap(
            bitmapWidth,
            bitmapHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        // Keep appending the fragments at the positions from where they were split
        var idx = 0
        for (y in 0 until nRows) {
            for (x in 0 until nCols) {

                val left: Int = when {
                    (x == 0) -> 0
                    (x == nCols - 1) -> (bitmapWidth - 1) - fragmentWidth
                    else -> x * strideX
                }

                val top: Int = when {
                    (y == 0) -> 0
                    (y == nRows - 1) -> (bitmapHeight - 1) - fragmentHeight
                    else -> y * strideY
                }

                // If a fragment overlapped with it's top or left neighbour fade the overlapping portion
                // of this fragment like a gradient (alpha 0 to 1) at it's left or top edge respectively.
                // This is done to obscure the seams formed by patching fragments
                if (x != 0) ImageUtils.fadeLeftEdge(fragments[idx], overlapSize)
                if (y != 0) ImageUtils.fadeTopEdge(fragments[idx], overlapSize)

                canvas.drawBitmap(
                    fragments[idx],
                    left.toFloat(),
                    top.toFloat(),
                    null
                )

                idx++
            }
        }

        val w: Int = if (padLeft <= 0) bitmap.width else padLeft - 1
        val h: Int = if (padTop <= 0) bitmap.height else padTop - 1

        val bmp = Bitmap.createBitmap(bitmap, 0, 0, w, h)

        return bmp
    }

    /**
     * Recycle all the stored bitmap fragments
     */
    fun recycle() {
        fragments.forEach { bitmap ->
            bitmap.recycle()
        }
    }

    operator fun get(key: Int): Bitmap {
        if (key >= fragments.size) {
            throw IndexOutOfBoundsException("Index $key out of bounds for size ${fragments.size}")
        }

        return fragments[key]
    }

    operator fun set(key: Int, value: Bitmap) {
        if (key >= fragments.size) {
            throw IndexOutOfBoundsException("Index $key out of bounds for size ${fragments.size}")
        }

        if (fragments[key].width != value.width || fragments[key].height != value.height) {
            throw IllegalArgumentException("Bitmap at index $key has different dimensions than parameter bitmap!")
        }

        fragments[key].recycle()
        fragments[key] = value
    }
}