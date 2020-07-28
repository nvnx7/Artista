package `in`.thenvn.artista

import `in`.thenvn.artista.editor.Style
import `in`.thenvn.artista.utils.ImageUtils
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class StyleTransferModelExecutor(context: Context, private var useGPU: Boolean = true) {
    companion object {
        private const val TAG = "StyleTransferModelExecutor"
        private const val MAX_SIZE = 1440
        private const val STYLE_IMAGE_SIZE = 256
        private const val CONTENT_IMAGE_SIZE = 384
        private const val BOTTLENECK_SIZE = 100
        private const val OVERLAP_SIZE = 50
        private const val STYLE_PREDICT_INT8_MODEL = "style_predict_quantized_256.tflite"
        private const val STYLE_TRANSFER_INT8_MODEL = "style_transfer_quantized_384.tflite"
        private const val STYLE_PREDICT_FLOAT16_MODEL = "style_predict_f16_256.tflite"
        private const val STYLE_TRANSFER_FLOAT16_MODEL = "style_transfer_f16_384.tflite"
    }

    private var gpuDelegate: GpuDelegate? = null
    private val numberOfThreads = 4

    private val interpreterPredict: Interpreter
    private val interpreterTransform: Interpreter

    // Buffers to reuse to store style bytes & content bytes
    private val styleBuffer: ByteBuffer
    private val contentBuffer: ByteBuffer

    init {
        if (useGPU) {
            interpreterPredict = getInterpreter(
                context, STYLE_PREDICT_FLOAT16_MODEL,
                STYLE_PREDICT_INT8_MODEL, useGPU
            )
            interpreterTransform = getInterpreter(
                context, STYLE_TRANSFER_FLOAT16_MODEL,
                STYLE_TRANSFER_INT8_MODEL, useGPU
            )
        } else {
            interpreterPredict = getInterpreter(
                context, STYLE_PREDICT_INT8_MODEL,
                null, useGPU
            )
            interpreterTransform = getInterpreter(
                context, STYLE_TRANSFER_INT8_MODEL,
                null, useGPU
            )
        }

        styleBuffer = ByteBuffer.allocateDirect(STYLE_IMAGE_SIZE * STYLE_IMAGE_SIZE * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        contentBuffer =
            ByteBuffer.allocateDirect(1 * CONTENT_IMAGE_SIZE * CONTENT_IMAGE_SIZE * 3 * 4).apply {
                order(ByteOrder.nativeOrder())
                rewind()
            }
    }

    /**
     * Start the inference using the models
     *
     * @param context Context to use to load resources
     * @param contentImageUri Uri of content image
     * @param style Style object corresponding to style image
     * @param blendRatio Ratio to use for blending styles of content image & style image
     * @param postProgress Lambda function to be called with progress of inference process
     * @return The final styled bitmap result
     */
    fun execute(
        context: Context,
        contentImageUri: Uri,
        style: Style,
        blendRatio: Float,
        postProgress: (progress: Int) -> Unit
    ): Bitmap {
        try {
            // Extract the style bottleneck from style bitmap
            val styleBitmap = preProcessStyle(context, style)
            ImageUtils.bitmapToByteBuffer(styleBitmap, styleBuffer)

            val inputsForPredict = arrayOf(styleBuffer)
            val outputsForPredict = HashMap<Int, Any>()
            val styleBottleneck = Array(1) { Array(1) { Array(1) { FloatArray(BOTTLENECK_SIZE) } } }
            outputsForPredict[0] = styleBottleneck
            interpreterPredict.runForMultipleInputsOutputs(inputsForPredict, outputsForPredict)

            // Extract the style bottleneck from content bitmap
            val contentStyleBitmap = preProcessStyle(context, Style(contentImageUri, Style.CUSTOM))
            ImageUtils.bitmapToByteBuffer(contentStyleBitmap, styleBuffer)

            val contentStyleBottleneck =
                Array(1) { Array(1) { Array(1) { FloatArray(BOTTLENECK_SIZE) } } }
            inputsForPredict[0] = styleBuffer
            outputsForPredict[0] = contentStyleBottleneck
            interpreterPredict.runForMultipleInputsOutputs(inputsForPredict, outputsForPredict)

            val styleBottleneckBlended =
                blendStyles(styleBottleneck, contentStyleBottleneck, blendRatio)

            // Perform style transfer on content image
            val contentBitmap = ImageUtils.loadScaledBitmap(context, contentImageUri, MAX_SIZE)

            val bitmapFragments = BitmapFragments(
                contentBitmap,
                CONTENT_IMAGE_SIZE, CONTENT_IMAGE_SIZE,
                OVERLAP_SIZE
            )
            contentBitmap.recycle()

            for (i in 0 until bitmapFragments.numberOfFragments) {
                ImageUtils.bitmapToByteBuffer(bitmapFragments[i], contentBuffer)

                val inputForStyleTransfer = arrayOf(contentBuffer, styleBottleneckBlended)
                val outputForStyleTransfer = HashMap<Int, Any>()
                val outputImage =
                    Array(1) { Array(CONTENT_IMAGE_SIZE) { Array(CONTENT_IMAGE_SIZE) { FloatArray(3) } } }
                outputForStyleTransfer[0] = outputImage

                interpreterTransform.runForMultipleInputsOutputs(
                    inputForStyleTransfer,
                    outputForStyleTransfer
                )

                val styledFragment =
                    ImageUtils.convertArrayToBitmap(outputImage, CONTENT_IMAGE_SIZE)

                bitmapFragments[i] = styledFragment

                val progress = (i + 1) * 100 / bitmapFragments.numberOfFragments
                postProgress(progress)
            }

            return bitmapFragments.patchFragments().also { bitmapFragments.recycle() }
        } catch (e: Exception) {
            Log.d(TAG, "Error in inference pipeline: ${e.message}")
            return ImageUtils.createEmptyBitmap(CONTENT_IMAGE_SIZE, CONTENT_IMAGE_SIZE)
        }
    }

    /**
     * Blend the styles of the content image and the style image
     *
     * @param styleBottleneck Array representing style of style image
     * @param contentStyleBottleneck Array representing style of style image
     * @param blendRatio Ratio of much style of style image to use
     * @return Array representing blended styles
     */
    private fun blendStyles(
        styleBottleneck: Array<Array<Array<FloatArray>>>,
        contentStyleBottleneck: Array<Array<Array<FloatArray>>>,
        blendRatio: Float
    ): Array<Array<Array<FloatArray>>> {
        val blendedStyle = Array(1) { Array(1) { Array(1) { FloatArray(BOTTLENECK_SIZE) } } }

        for (i in 0 until BOTTLENECK_SIZE) {
            blendedStyle[0][0][0][i] = ((1 - blendRatio) * contentStyleBottleneck[0][0][0][i]) +
                    (blendRatio * styleBottleneck[0][0][0][i])
        }

        return blendedStyle
    }

    /**
     * Load & pre-process the style image file so to make it suitable to be fed into model
     *
     * @param context Context to load style bitmap from
     * @param style Style object corresponding to the style image
     * @return Pre processed bitmap
     */
    private fun preProcessStyle(context: Context, style: Style): Bitmap {
        return Glide.with(context)
            .asBitmap()
            .load(style.uri)
            .override(STYLE_IMAGE_SIZE)
            .centerCrop()
            .submit()
            .get()
    }

    /**
     * Get the interpreter object corresponding to a model
     *
     * @param context context to access model files
     * @param modelName File name of model
     * @param fallbackModelName File name of model to load on CPU, in case loading modelName
     * on GPU fails
     * @param useGPU Whether to use gpu device (if available) for inference with interpreter
     *@return Interpreter object to use later to make inferences
     */
    @Throws(IOException::class)
    private fun getInterpreter(
        context: Context,
        modelName: String,
        fallbackModelName: String?,
        useGPU: Boolean = true
    ): Interpreter {
        val options = Interpreter.Options().setNumThreads(numberOfThreads)
        gpuDelegate = null
        var interpreter: Interpreter

        if (useGPU) {
            gpuDelegate = GpuDelegate()
            options.addDelegate(gpuDelegate)

            // Try to load float16 model on GPU
            try {
                val model = loadModel(context, modelName)
                interpreter = Interpreter(model, options)
            } catch (ex: IllegalArgumentException) {
                // If failed (OpenGL not found/supported), load quantized model on CPU
                gpuDelegate = null
                this.useGPU = false
                val model = loadModel(context, fallbackModelName!!)
                interpreter =
                    Interpreter(model, Interpreter.Options().setNumThreads(numberOfThreads))
            }
        } else {
            // Load quantized model on CPU
            interpreter = Interpreter(loadModel(context, modelName), options)
        }

        return interpreter
    }

    /**
     * Load the tf lite model
     *
     * @param context Context to access models files from
     * @param modelName File name of the model
     *
     * @return Mapped byte buffer of the model
     */
    @Throws(IOException::class)
    private fun loadModel(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val retFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        fileDescriptor.close()
        return retFile
    }

    /**
     * Release the occupied resources
     */
    fun close() {
        interpreterPredict.close()
        interpreterTransform.close()
        if (gpuDelegate != null) gpuDelegate!!.close()
    }

}