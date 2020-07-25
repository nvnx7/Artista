package `in`.thenvn.artista.editor

import `in`.thenvn.artista.R
import `in`.thenvn.artista.StyleTransferModelExecutor
import `in`.thenvn.artista.utils.ImageUtils
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class EditorViewModel(
    private val _originalMediaUri: Uri,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "EditorViewModel"
    }

    val originalMediaUri: Uri
        get() = _originalMediaUri

    // Final result bitmap of applying style to original image
    private val _styledBitmapLiveData = MutableLiveData<Bitmap>()
    val styledBitmapLiveData: LiveData<Bitmap>
        get() = _styledBitmapLiveData

    private var _blendRatio: Float = 0.5F

    // List of style images
    private val _stylesListLiveData = MutableLiveData<ArrayList<Style>>()
    val stylesListLiveData: LiveData<ArrayList<Style>>
        get() = _stylesListLiveData

    // Integer between 0 and 100 representing progress of style transfer
    // Negative integer will mean progress is indeterminant
    private val _progressLiveData = MutableLiveData<Int>()
    val progressLiveData: LiveData<Int>
        get() = _progressLiveData
    val progressIndeterminant: LiveData<Boolean> =
        Transformations.map(_progressLiveData) { progress -> progress < 0 }

    // Message to the end - user about what is actually being done when busy
    private val _progressMessageLiveData = MutableLiveData<String>()
    val progressMessageLiveData: LiveData<String>
        get() = _progressMessageLiveData

    // Boolean representing whether interpreter is busy processing
    private val _processBusyLiveData = MutableLiveData<Boolean>()
    val processBusyLiveData: LiveData<Boolean>
        get() = _processBusyLiveData

    private lateinit var styleTransferModelExecutor: StyleTransferModelExecutor
    private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    init {
        _progressLiveData.value = -1
        _processBusyLiveData.value = true

        _progressMessageLiveData.value =
            application.resources.getString(R.string.message_loading_model)

        _stylesListLiveData.value = ArrayList()
        application.assets!!.list("styles")!!.forEach {
            _stylesListLiveData.value!!.add(
                Style(
                    Uri.parse("file:///android_asset/styles/$it"),
                    Style.FIXED
                )
            )
        }

        viewModelScope.launch(inferenceThread) {
            styleTransferModelExecutor = StyleTransferModelExecutor(application.applicationContext)
            _processBusyLiveData.postValue(false)
            _progressLiveData.postValue(0)
            Log.i(TAG, "Executor created ")
        }
    }

    fun applyStyle(
        context: Context,
        contentImageUri: Uri,
        style: Style
    ) {
        _processBusyLiveData.value = true
        _progressMessageLiveData.value =
            getApplication<Application>().resources.getString(R.string.message_performing_inference)

        viewModelScope.launch(inferenceThread) {
            val result =
                styleTransferModelExecutor.execute(context, contentImageUri, style, _blendRatio) {
                    _progressLiveData.postValue(it)
                }
            _styledBitmapLiveData.postValue(result)
            _processBusyLiveData.postValue(false)
            _progressLiveData.postValue(0)
        }
    }

    fun updateBlendRatio(ratio: Float) {
        Log.i(TAG, "updateBlendRatio: $ratio")
        _blendRatio = ratio
    }

    fun addStyle(style: Style) {
        val newList: ArrayList<Style> = _stylesListLiveData.value!!
        newList.add(1, style)
        _stylesListLiveData.value = newList
    }

    fun saveStyledBitmap() {
        ImageUtils.savePicture(getApplication(), styledBitmapLiveData.value!!)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}