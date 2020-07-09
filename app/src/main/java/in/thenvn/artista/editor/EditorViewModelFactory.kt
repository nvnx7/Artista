package `in`.thenvn.artista.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EditorViewModelFactory(
    private val originalImageUri: Uri,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            return EditorViewModel(originalImageUri, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class.")
    }
}