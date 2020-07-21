package `in`.thenvn.artista.editor

import `in`.thenvn.artista.StyleTransferModelExecutor
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class EditorViewModel(
    _originalMediaUri: Uri,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "EditorViewModel"
    }

    // Original image's URI
    private val _originalMediaUriLiveData = MutableLiveData<Uri>()
    val originalMediaUriLiveData: LiveData<Uri>
        get() = _originalMediaUriLiveData

    // Final result bitmap of applying style to original image
    private val _styledBitmapLiveData = MutableLiveData<Bitmap>()
    val styledBitmap: LiveData<Bitmap>
        get() = _styledBitmapLiveData

    private var _blendRatio: Float = 0.5F

    // List of style images
    private val _stylesListLiveData = MutableLiveData<ArrayList<Style>>()
    val stylesList: LiveData<ArrayList<Style>>
        get() = _stylesListLiveData

    // Integer between 0 and 100 representing progress of style transfer
    private val _progressLiveData = MutableLiveData<Int>()
    val progressLiveData: LiveData<Int>
        get() = _progressLiveData

    // Boolean representing whether interpreter is busy processing
    private val _processBusyLiveData = MutableLiveData<Boolean>()
    val processBusyLiveData: LiveData<Boolean>
        get() = _processBusyLiveData
    val progressVisibility: LiveData<Int> = Transformations.map(_processBusyLiveData) { busy ->
        if (busy) View.VISIBLE
        else View.INVISIBLE
    }

    private lateinit var styleTransferModelExecutor: StyleTransferModelExecutor
    private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    init {
        _processBusyLiveData.value = true
        _originalMediaUriLiveData.value = _originalMediaUri

        _stylesListLiveData.value = ArrayList()
        application.assets!!.list("styles")!!.forEach {
            _stylesListLiveData.value!!.add(
                Style(
                    Uri.parse("file:///android_asset/styles/$it"),
                    Style.FIXED
                )
            )
        }

        Log.i(TAG, "list sample: ${_stylesListLiveData.value!!.get(0)}")

        viewModelScope.launch(inferenceThread) {
            styleTransferModelExecutor = StyleTransferModelExecutor(application)
            _processBusyLiveData.postValue(false)
            Log.i(TAG, "Executor created ")
        }
    }

    fun applyStyle(
        context: Context,
        contentImageUri: Uri,
        style: Style
    ) {
        _processBusyLiveData.value = true
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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}