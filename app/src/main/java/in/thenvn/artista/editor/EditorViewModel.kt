package `in`.thenvn.artista.editor

import `in`.thenvn.artista.StyleTransferModelExecutor
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.launch

class EditorViewModel(
    private val _originalMediaUri: Uri,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "EditorViewModel"
    }

    private val _originalMediaUriLiveData = MutableLiveData<Uri>()
    val originalMediaUriLiveData: LiveData<Uri>
        get() = _originalMediaUriLiveData

    private val _styledBitmapLiveData = MutableLiveData<Bitmap>()
    val styledBitmap: LiveData<Bitmap>
        get() = _styledBitmapLiveData

    private val _stylesListLiveData = MutableLiveData<ArrayList<Style>>()
    val stylesList: LiveData<ArrayList<Style>>
        get() = _stylesListLiveData

    init {
        _originalMediaUriLiveData.value = _originalMediaUri

        _stylesListLiveData.value = ArrayList()
        application.assets!!.list("styles")!!.forEach {
            _stylesListLiveData.value!!.add(Style(Uri.parse("file:///android_asset/styles/$it")))
        }

        Log.i(TAG, "list sample: ${_stylesListLiveData.value!!.get(0)}")
    }

    fun applyStyle(
        context: Context,
        contentImageUri: Uri,
        styleImageUri: Uri,
        styleTransferModelExecutor: StyleTransferModelExecutor,
        inferenceThread: ExecutorCoroutineDispatcher
    ) {
        viewModelScope.launch(inferenceThread) {
            val result = styleTransferModelExecutor.execute(context, contentImageUri, styleImageUri)
            _styledBitmapLiveData.postValue(result)
        }
    }
}