package `in`.thenvn.artista

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import kotlin.math.ceil

/**
 * Helper class to decode large bitmap piece by piece. Each piece is of size [pieceWidth] x [pieceHeight]
 * Each piece share given overlapOffset number of columns with piece on its left (if present) & also
 * overlapOffset number of rows with piece on its top (if present). Overlap is done to obscure the
 * seams formed when later the edited pieces are patched together.
 *
 * @param context Context to get content resolver from to load resources
 * @param uri Uri of the original image
 * @param pieceWidth Width of individual piece
 * @param pieceHeight Height of individual piece
 * @param overlapOffset No. of rows or columns a piece must share with its neighbour piece
 */
class ContentImageDecoder(
    context: Context,
    uri: Uri,
    private val pieceWidth: Int,
    private val pieceHeight: Int = pieceWidth,
    overlapOffset: Int
) {
    /**
     * Amounts by which the bitmap piece cropping window (size [pieceWidth] * [pieceHeight])
     * will move horizontally and vertically to crop subsequent pieces from image.
     */
    private val strideX: Int = pieceWidth - overlapOffset
    private val strideY: Int = pieceHeight - overlapOffset

    /**
     * No. of pieces generated while moving crop window horizontally and vertically.
     * Imagine each piece as a square of a grid
     */
    private val nCols: Int
    private val nRows: Int

    /**
     * Decoder to decode bitmap piece by piece
     */
    private val decoder: BitmapRegionDecoder

    /**
     * Options used to decode pieces
     */
    private val options: BitmapFactory.Options

    /**
     * Original image's width & height
     */
    val originalWidth: Int
    val originalHeight: Int

    private val temp: Bitmap = Bitmap.createBitmap(pieceWidth, pieceHeight, Bitmap.Config.ARGB_8888)

    init {
        if (overlapOffset > pieceWidth || overlapOffset > pieceHeight)
            throw IllegalArgumentException("Overlap offset must be less than chunk dimensions!")

        // Initialize decoder with input stream
        val inputStream = context.contentResolver.openInputStream(uri)
        decoder = BitmapRegionDecoder.newInstance(inputStream, true)
        inputStream?.close()

        originalWidth = decoder.width
        originalHeight = decoder.height

        nCols = ceil((originalWidth - overlapOffset).toFloat() / (strideX)).toInt()
        nRows = ceil((originalHeight - overlapOffset).toFloat() / (strideY)).toInt()

        options = BitmapFactory.Options().apply {
            inBitmap = temp
        }
    }

    /**
     * @return Iterator for getting pieces in order
     */
    fun iterator(): Iterator<Bitmap> {
        return BitmapPieceIterator()
    }

    /**
     * Recycle associated decoder
     */
    fun recycle() {
        decoder.recycle()
    }

    /**
     * Iterator for decoding piece every time next piece is requested
     */
    inner class BitmapPieceIterator : Iterator<Bitmap> {
        // Indices to track current row & column in which piece is present
        private var col: Int = 0
        private var row: Int = 0

        // If all rows are used return false
        override fun hasNext(): Boolean {
            if (row >= nRows) return false
            return true
        }

        // Get piece by moving by a strides of strideX and strideY respectively.
        // For the piece at extreme edges (right & bottom) crop to maintain uniform size of
        // (pieceWidth * pieceHeight) irrespective of corresponding strides.
        override fun next(): Bitmap {
            val left: Int = when {
                (col == 0) -> 0
                (col == nCols - 1) -> (originalWidth - 1) - pieceWidth
                else -> col * strideX
            }

            val top: Int = when {
                (row == 0) -> 0
                (row == nRows - 1) -> (originalHeight - 1) - pieceHeight
                else -> row * strideY
            }

            if (col < nCols - 1) col++
            else {
                col = 0
                row++
            }

            return decoder.decodeRegion(
                Rect(left, top, left + pieceWidth, top + pieceHeight),
                options
            )
        }

    }

}