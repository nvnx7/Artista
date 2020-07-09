package `in`.thenvn.artista.editor

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class EditorViewModel(
    private val _originalMediaUri: Uri,
    application: Application
) : AndroidViewModel(application) {

    private val _originalMediaUriLiveData = MutableLiveData<Uri>()
    val originalMediaUriLiveData: LiveData<Uri>
        get() = _originalMediaUriLiveData

    private val _styledBitmap = MutableLiveData<Bitmap>()
    val styledBitmap: LiveData<Bitmap>
        get() = _styledBitmap

    init {
        _originalMediaUriLiveData.value = _originalMediaUri
    }

//    fun applyStyle()
}