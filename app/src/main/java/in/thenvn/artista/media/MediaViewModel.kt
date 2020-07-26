package `in`.thenvn.artista.media

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MediaViewModel(application: Application) : AndroidViewModel(application), CoroutineScope {

    private val viewModelJob = Job()

    override val coroutineContext: CoroutineContext
        get() = viewModelJob + Dispatchers.Main

    // List fetched media from the user's gallery
    private val _mediaUriListLiveData = MutableLiveData<MutableList<Uri>>()
    val mediaItemListLiveData: LiveData<List<MediaItem>>? =
        Transformations.map(_mediaUriListLiveData) { uriList ->
            uriList.map { uri ->
                MediaItem(uri)
            }
        }

    // Live data to track permission status
    private val _permissionGrantedLiveData = MutableLiveData<Boolean>(false)
    val permissionGrantedLiveData: LiveData<Boolean>
        get() = _permissionGrantedLiveData
    val permissionControlsVisibility: LiveData<Int> =
        Transformations.map(_permissionGrantedLiveData) { isGranted ->
            if (isGranted) View.GONE
            else View.VISIBLE
        }

    private fun loadMediaFromStorage(): MutableList<Uri> {
        val mediaList = mutableListOf<Uri>()

        val contentResolver = getApplication<Application>().applicationContext.contentResolver
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val colIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(colIndex)
                val imageUri: Uri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                mediaList.add(imageUri)
            }
        }
        return mediaList
    }

    fun updatePermissionGrantedStatus(isGranted: Boolean) {
        _permissionGrantedLiveData.value = isGranted
    }

    fun fetchAllMedia() {
        launch(Dispatchers.Main) {
            _mediaUriListLiveData.value = withContext(Dispatchers.IO) {
                loadMediaFromStorage()
            }
        }
    }

}