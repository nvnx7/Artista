package `in`.thenvn.artista

import `in`.thenvn.artista.utils.ImageUtils
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class StyleTransferModelExecutor(context: Context, private var useGPU: Boolean = true) {

    companion object {
        private const val TAG = "StyleTransferModelExecutor"
        private const val STYLE_IMAGE_SIZE = 256
        private const val CONTENT_IMAGE_SIZE = 384
        private const val BOTTLENECK_SIZE = 100
        private const val STYLE_PREDICT_INT8_MODEL = "style_predict_quantized_256.tflite"
        private const val STYLE_TRANSFER_INT8_MODEL = "style_transfer_quantized_384.tflite"
        private const val STYLE_PREDICT_FLOAT16_MODEL = "style_predict_f16_256.tflite"
        private const val STYLE_TRANSFER_FLOAT16_MODEL = "style_transfer_f16_384.tflite"
    }

    private var gpuDelegate: GpuDelegate? = null
    private var numberOfThreads = 4

    private val interpreterPredict: Interpreter
    private val interpreterTransform: Interpreter

    init {
        if (useGPU) {
            interpreterPredict = getInterpreter(context, STYLE_PREDICT_FLOAT16_MODEL, true)
            interpreterTransform = getInterpreter(context, STYLE_TRANSFER_FLOAT16_MODEL, true)
        } else {
            interpreterPredict = getInterpreter(context, STYLE_PREDICT_INT8_MODEL, false)
            interpreterTransform = getInterpreter(context, STYLE_TRANSFER_INT8_MODEL, false)
        }
    }

    fun execute(
        context: Context,
        contentImageUri: Uri,
        styleImageUri: Uri
    ): Bitmap {
        try {
//            val absPath = Uri.parse(contentImagePath).path
//            val file = File(Uri.parse(contentImagePath).path!!)
//            val file = File(absPath!!)
            val stream = context.contentResolver.openInputStream(contentImageUri)

            val contentBitmap = ImageUtils.decodeBitmap(context, contentImageUri)
            val contentArray = ImageUtils.bitmapToByteBuffer(
                contentBitmap,
                CONTENT_IMAGE_SIZE, CONTENT_IMAGE_SIZE
            )
            val styleBitmap =
                ImageUtils.loadBitmapFromResource(context, "thumbnails/${styleImageUri}")
            val styleArray = ImageUtils.bitmapToByteBuffer(
                styleBitmap,
                STYLE_IMAGE_SIZE, STYLE_IMAGE_SIZE
            )

            val inputsForPredict = arrayOf(styleArray)
            val outputsForPredict = HashMap<Int, Any>()
            val styleBottleneck = Array(1) { Array(1) { Array(1) { FloatArray(BOTTLENECK_SIZE) } } }
            outputsForPredict[0] = styleBottleneck

            interpreterPredict.runForMultipleInputsOutputs(inputsForPredict, outputsForPredict)

            val inputForStyleTransfer = arrayOf(contentArray, styleBottleneck)
            val outputForStyleTransfer = HashMap<Int, Any>()
            val outputImage =
                Array(1) { Array(CONTENT_IMAGE_SIZE) { Array(CONTENT_IMAGE_SIZE) { FloatArray(3) } } }

            interpreterTransform.runForMultipleInputsOutputs(
                inputForStyleTransfer,
                outputForStyleTransfer
            )

            val styledBitmap =
                ImageUtils.convertArrayToBitmap(outputImage, CONTENT_IMAGE_SIZE, CONTENT_IMAGE_SIZE)

            return styledBitmap
        } catch (e: Exception) {
            Log.d(TAG, "Error in inference pipeline: ${e.message}")
            return ImageUtils.createEmptyBitmap(CONTENT_IMAGE_SIZE, CONTENT_IMAGE_SIZE)
        }
    }

    private fun getInterpreter(
        context: Context,
        modelName: String,
        useGPU: Boolean = true
    ): Interpreter {
        val tfliteOptions = Interpreter.Options()
        tfliteOptions.setNumThreads(numberOfThreads)

        gpuDelegate = null
        if (useGPU) {
            gpuDelegate = GpuDelegate()
            tfliteOptions.addDelegate(gpuDelegate)
        }

        return Interpreter(loadModel(context, modelName), tfliteOptions)
    }

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

}