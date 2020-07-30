package `in`.thenvn.artista

import `in`.thenvn.artista.utils.ImageUtils
import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.ceil

/**
 * Helper class to build a large final bitmap by putting pieces.
 *
 * @param width Width of final bitmap
 * @param height Height of final bitmap
 * @param pieceWidth Width of individual pieces
 * @param pieceHeight Height of individual pieces
 * @param overlapOffset Amount by which pieces overlap at respective positions
 * @see ContentImageDecoder
 */
class BitmapBuilder(
    private val width: Int,
    private val height: Int,
    private val pieceWidth: Int,
    private val pieceHeight: Int,
    private val overlapOffset: Int
) {

    /**
     * Final bitmap
     */
    private val _bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val bitmap: Bitmap
        get() = _bitmap

    /**
     * Canvas of final bitmap used to draw pieces
     */
    private val canvas: Canvas

    /**
     * No. of pieces in each column and each row. Imagine each piece as a square of a grid which
     * corresponds to final bitmap
     */
    private val nCols: Int
    private val nRows: Int

    /**
     * Total number of bitmap pieces
     */
    val numberOfPieces: Int
        get() = nCols * nRows

    /**
     * Current position of slot in grid to draw bitmap in
     */
    private var col: Int = 0
    private var row: Int = 0

    /**
     * Amounts by which the bitmap piece placing window (size [pieceWidth] * [pieceHeight])
     * will move horizontally and vertically to place subsequent pieces in grid/bitmap.
     */
    private val strideX: Int = pieceWidth - overlapOffset
    private val strideY: Int = pieceHeight - overlapOffset

    init {
        nCols = ceil((width - overlapOffset).toFloat() / (strideX)).toInt()
        nRows = ceil((height - overlapOffset).toFloat() / (strideY)).toInt()
        canvas = Canvas(_bitmap)
    }

    /**
     * Convert the given image array to bitmap & put it at current slot in grid
     * @param imageArray Array to convert to bitmap & put
     */
    fun convertAndPut(imageArray: Array<Array<Array<FloatArray>>>) {
        if (col >= nCols) {
            throw IllegalArgumentException("No space left to put additional piece!")
        }

        if (imageArray[0].size != pieceWidth || imageArray[0][0].size != pieceHeight) {
            throw IllegalArgumentException("Mis-matched piece size provided!")
        }

        // Put the piece by moving by a strides of strideX and strideY respectively.
        // For the piece at extreme edges (right & bottom) put so that it fits with edges maintaining
        // original size of constructed bitmap irrespective of corresponding strides.
        val left: Int = when {
            (col == 0) -> 0
            (col == nCols - 1) -> (width - 1) - pieceWidth
            else -> col * strideX
        }

        val top: Int = when {
            (row == 0) -> 0
            (row == nRows - 1) -> (height - 1) - pieceHeight
            else -> row * strideY
        }

        // If a fragment overlapped with it's top or left neighbour fade the overlapping portion
        // of this fragment like a gradient (alpha 0 to 1) at it's left or top edge respectively.
        // This is done to obscure the seams formed by patching fragments
        val fadeLeft = col != 0
        val fadeTop = row != 0

        canvas.drawBitmap(
            ImageUtils.convertArrayToBitmap(
                imageArray, pieceWidth, pieceHeight,
                overlapOffset, fadeTop, fadeLeft
            ),
            left.toFloat(),
            top.toFloat(),
            null
        )

        if (col < nCols - 1) col++
        else {
            col = 0
            row++
        }
    }
}