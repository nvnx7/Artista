package `in`.thenvn.artista.media

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MediaViewModel(application: Application) : AndroidViewModel(application), CoroutineScope {

    private val viewModelJob = Job()

    override val coroutineContext: CoroutineContext
        get() = viewModelJob + Dispatchers.Main

    // List fetched media from the user's gallery
    private val _mediaItemsListLiveData = MutableLiveData<MutableList<MediaItem>>()
    val mediaItemsListLiveData: LiveData<MutableList<MediaItem>>
        get() = _mediaItemsListLiveData

    // Live data to track permission status
    private val _permissionGrantedLiveData = MutableLiveData<Boolean>(false)
    val permissionGrantedLiveData: LiveData<Boolean>
        get() = _permissionGrantedLiveData

    // Live data to set visibility of empty view depending upon whether any media was found
    val visibilityEmptyView: LiveData<Int> =
        Transformations.map(_mediaItemsListLiveData) { mediaList ->
            when {
                mediaList == null || mediaList.isEmpty() -> View.VISIBLE
                else -> View.GONE
            }
        }

    /**
     * Load all image media (uri, width & height) from device through content resolver
     * @return List of MediaItem objects
     */
    private fun loadMediaFromStorage(): MutableList<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()

        val contentResolver = getApplication<Application>().applicationContext.contentResolver
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val width = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.WIDTH))
                val height = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT))
                val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID))
                val imageUri: Uri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                mediaList.add(MediaItem(imageUri, width, height))
            }
        }
        return mediaList
    }

    /**
     * Update the permission status
     * @param isGranted Boolean which is true if permission granted
     */
    fun updatePermissionGrantedStatus(isGranted: Boolean) {
        _permissionGrantedLiveData.value = isGranted
    }

    /**
     * Start fetching all media in a coroutine
     */
    fun fetchAllMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaList = loadMediaFromStorage()
            _mediaItemsListLiveData.postValue(mediaList)
        }
    }

}