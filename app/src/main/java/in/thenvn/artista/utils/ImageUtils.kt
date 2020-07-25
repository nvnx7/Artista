package `in`.thenvn.artista.utils

import `in`.thenvn.artista.R
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.ColorUtils
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.round


abstract class ImageUtils {

    companion object {
        /**
         * Helper function used to convert an EXIF orientation enum into a transformation matrix
         * that can be applied to a bitmap.
         *
         * @param orientation - One of the constants from [ExifInterface]
         */
        private fun decodeExifOrientation(orientation: Int): Matrix {
            val matrix = Matrix()

            when (orientation) {
                ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED -> Unit
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1F, 1F)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1F, -1F)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postScale(-1F, 1F)
                    matrix.postRotate(270F)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postScale(-1F, 1F)
                    matrix.postRotate(90F)
                }

                else -> throw IllegalArgumentException("Invalid orientation $orientation")
            }

            return matrix
        }

        /**
         * Decode a bitmap from a file and apply the transformations described in its EXIF data
         *
         * @param file - The image file to be read using [BitmapFactory.decodeFile]
         */
        fun decodeBitmap(file: File): Bitmap {
            val exif = ExifInterface(file.absolutePath)
            val transformation =
                decodeExifOrientation(
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90
                    )
                )

            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            return Bitmap.createBitmap(
                BitmapFactory.decodeFile(file.absolutePath), 0, 0,
                bitmap.width, bitmap.height,
                transformation,
                true
            )
        }

        fun decodeBitmap(context: Context, imageUri: Uri): Bitmap {
            val stream = context.contentResolver.openInputStream(imageUri)
            val exif = ExifInterface(stream!!)
            //TODO Wrong orientation is received
            val transformation =
                decodeExifOrientation(
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90
                    )
                )

            val bitmap = Glide.with(context).asBitmap().load(imageUri).submit().get()

//            return Bitmap.createBitmap(
//                Glide.with(context).asBitmap().load(imageUri).submit().get(), 0, 0,
//                bitmap.width, bitmap.height,
//                transformation,
//                true
//            )
            return bitmap
        }

        fun scaleBitmapAndKeepRatio(
            targetBitmap: Bitmap,
            reqWidthInPixels: Int,
            reqHeightInPixels: Int
        ): Bitmap {
            if (targetBitmap.height == reqHeightInPixels
                && targetBitmap.width == reqWidthInPixels
            ) return targetBitmap

            val matrix = Matrix()
            matrix.setRectToRect(
                RectF(0F, 0F, targetBitmap.width.toFloat(), targetBitmap.height.toFloat()),
                RectF(0F, 0F, reqWidthInPixels.toFloat(), reqHeightInPixels.toFloat()),
                Matrix.ScaleToFit.FILL
            )

            val bitmap = Bitmap.createBitmap(
                targetBitmap, 0, 0,
                targetBitmap.width, targetBitmap.height,
                matrix, true
            )

            return bitmap
        }

        fun bitmapToByteBuffer(
            bitmapIn: Bitmap,
            width: Int,
            height: Int,
            mean: Float = 0.0F,
            std: Float = 255.0F
        ): ByteBuffer {
            val bitmap =
                scaleBitmapAndKeepRatio(
                    bitmapIn,
                    width,
                    height
                )
            val inputImage = ByteBuffer.allocateDirect(1 * width * height * 3 * 4)
            inputImage.order(ByteOrder.nativeOrder())
            inputImage.rewind()

            val intValues = IntArray(width * height)
            bitmap.getPixels(intValues, 0, width, 0, 0, width, height)
            var pixel = 0

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val value = intValues[pixel++]

                    // Normalize channel values to [-1.0, 1.0]. This requirement varies by
                    // model. For example, some models might require values to be normalized
                    // to the range [0.0, 1.0] instead.
                    inputImage.putFloat(((value shr 16 and 0xFF) - mean) / std)
                    inputImage.putFloat(((value shr 8 and 0xFF) - mean) / std)
                    inputImage.putFloat(((value and 0xFF) - mean) / std)
                }
            }

            inputImage.rewind()
            return inputImage
        }

//        fun blendBitmaps(
//            bitmap1: Bitmap, bitmap2: Bitmap,
//            mean: Float = 0.0F, std: Float = 255.0F,
//            multiplier: Float = 0.5F
//        ): ByteBuffer {
//            val width = bitmap1.width
//            val height = bitmap1.height
//            if (width != bitmap2.width || height != bitmap2.height) {
//                throw IllegalArgumentException("Bitmaps dimensions mismatch!")
//            }
//
//            val inputImage = ByteBuffer.allocateDirect(1 * width * height * 3 * 4)
//            inputImage.order(ByteOrder.nativeOrder())
//            inputImage.rewind()
//
//            val intValues1 = IntArray(width * height)
//            val intValues2 = IntArray(width * height)
//            bitmap1.getPixels(intValues1, 0, width, 0, 0, width, height)
//            bitmap2.getPixels(intValues2, 0, width, 0, 0, width, height)
//
//            var pixel = 0
//            for (y in 0 until height) {
//                for (x in 0 until width) {
//                    val value = intValues1[pixel]
//
//                    inputImage.putFloat(((value shr 16 and 0xFF) - mean) / std)
//                    inputImage.putFloat(((value shr 8 and 0xFF) - mean) / std)
//                    inputImage.putFloat(((value and 0xFF) - mean) / std)
//
//                    pixel++
//                }
//            }
//
//            inputImage.rewind()
//            return inputImage
//        }

        fun loadBitmapFromAssets(context: Context, path: String): Bitmap {
            val inputStream = context.assets.open(path)
            return BitmapFactory.decodeStream(inputStream)
        }

        fun convertArrayToBitmap(
            imageArray: Array<Array<Array<FloatArray>>>,
            imageWidth: Int,
            imageHeight: Int
        ): Bitmap {
            val conf = Bitmap.Config.ARGB_8888
            val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, conf)

            for (x in imageArray[0].indices) {
                for (y in imageArray[0][0].indices) {
                    val color = Color.rgb(
                        ((imageArray[0][x][y][0] * 255).toInt()),
                        ((imageArray[0][x][y][1] * 255).toInt()),
                        (imageArray[0][x][y][2] * 255).toInt()
                    )

                    bitmap.setPixel(y, x, color)
                }
            }
            return bitmap
        }

        fun createEmptyBitmap(width: Int, height: Int, color: Int = 0): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            if (color != 0) bitmap.eraseColor(color)
            return bitmap
        }

        fun loadScaledBitmap(context: Context, uri: Uri, maxSize: Int): Bitmap {
            return Glide
                .with(context)
                .asBitmap()
                .load(uri)
                .apply(RequestOptions().override(maxSize).fitCenter())
                .submit()
                .get()
        }

        /**
         * Helper function to pad a bitmap at right and bottom edges so that to create a final
         * bitmap of width [finalWidth] and height [finalHeight]
         *
         * @param bitmapIn Bitmap to be processed upon
         * @param finalWidth Required final width of padded bitmap
         * @param finalHeight Required final height of padded bitmap
         *
         * @return A new padded bitmap
         */
        fun padBitmap(bitmapIn: Bitmap, finalWidth: Int, finalHeight: Int): Bitmap {
            if (bitmapIn.width > finalWidth || bitmapIn.height > finalHeight) {
                throw IllegalArgumentException("Provided bitmap dimensions should be less than final dimensions")
            }
            val paddedBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(paddedBitmap)
            canvas.drawARGB(255, 255, 255, 255) // white background
            canvas.drawBitmap(bitmapIn, 0F, 0F, null) // draw bitmap at top left
            return paddedBitmap
        }

        /**
         * Helper method to fade top edge of bitmap [bitmapIn] linearly like gradient (alpha 0 to 1) up to [nRows]
         * number of rows from top edge of bitmap.
         *
         * @param bitmapIn Bitmap to fade top edge of
         * @param nRows No of rows up to which linear fading to be applied
         */
        fun fadeTopEdge(bitmapIn: Bitmap, nRows: Int) {
            val pixels = IntArray(bitmapIn.width * bitmapIn.height)
            bitmapIn.getPixels(pixels, 0, bitmapIn.width, 0, 0, bitmapIn.width, bitmapIn.height)
            val dAlpha: Float = round((1F / nRows) * 100) / 100

            var alphaM = 0F
            for (i in 0..(bitmapIn.width * nRows)) {
                val alpha: Int = (255 * alphaM).toInt()
                pixels[i] = ColorUtils.setAlphaComponent(pixels[i], alpha)

                if (i % bitmapIn.width == 0 && i >= bitmapIn.width) {
                    alphaM += dAlpha
                }
            }

            bitmapIn.setPixels(pixels, 0, bitmapIn.width, 0, 0, bitmapIn.width, bitmapIn.height)
        }

        /**
         * Helper method to fade left edge of bitmap [bitmapIn] linearly like gradient (alpha 0 to 1) up to [nCols]
         * number of columns from left edge of bitmap.
         *
         * @param bitmapIn Bitmap to fade top edge of
         * @param nCols No of columns up to which linear fading to be applied
         */
        fun fadeLeftEdge(bitmapIn: Bitmap, nCols: Int) {
            val pixels = IntArray(bitmapIn.width * bitmapIn.height)
            bitmapIn.getPixels(pixels, 0, bitmapIn.width, 0, 0, bitmapIn.width, bitmapIn.height)
            val dAlpha: Float = round((1F / nCols) * 100) / 100

            for (i in pixels.indices step bitmapIn.width) {
                var alphaM = 0F
                for (j in 0..nCols) {
                    val alpha = (255 * alphaM).toInt()
                    pixels[i + j] = ColorUtils.setAlphaComponent(pixels[i + j], alpha)
                    alphaM += dAlpha
                }
            }

            bitmapIn.setPixels(pixels, 0, bitmapIn.width, 0, 0, bitmapIn.width, bitmapIn.height)
        }

        /**
         * Helper method to write the bitmap to the standard public Pictures directory
         * of the device.
         */
        fun savePicture(context: Context, bitmap: Bitmap) {
            val dirName = context.resources.getString(R.string.app_name)
            val fileName = "${dirName}_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/$dirName"
                    )
                }

                val uri: Uri? = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let {
                    val out = context.contentResolver.openOutputStream(uri)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    out?.close()
                }
            } else {
                val dirPath =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        .toString() +
                            "${File.separator}${dirName}"
                val file = File(dirPath)
                if (!file.exists()) file.mkdir()
                val imageFile = File(dirPath, fileName)
                val out = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.close()
            }
        }
    }
}