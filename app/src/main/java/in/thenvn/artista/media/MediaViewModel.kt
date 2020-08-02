package `in`.thenvn.artista.media

import `in`.thenvn.artista.R
import `in`.thenvn.artista.utils.ImageUtils
import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class MediaViewModel(application: Application) : AndroidViewModel(application), CoroutineScope {

    private val viewModelJob = Job()

    override val coroutineContext: CoroutineContext
        get() = viewModelJob + Dispatchers.Main

    // Application context
    private val applicationContext: Context = application.applicationContext

    // List fetched media from the user's gallery
    private val _mediaItemsListLiveData = MutableLiveData<MutableList<MediaItem>>()
    val mediaItemsListLiveData: LiveData<MutableList<MediaItem>>
        get() = _mediaItemsListLiveData

    // Live data to track permission status
    private val _permissionGrantedLiveData = MutableLiveData<Boolean>(false)
    val permissionGrantedLiveData: LiveData<Boolean>
        get() = _permissionGrantedLiveData

    // Live data set to a non-null Uri which will be used to extract bitmap in editor
    private val _mediaItemUriLiveData = MutableLiveData<Uri?>(null)
    val mediaItemUriLiveData: LiveData<Uri?>
        get() = _mediaItemUriLiveData

    // Live data to determine if any process is underway
    private val _isBusy = MutableLiveData<Boolean>(false)
    val isBusy: LiveData<Boolean>
        get() = _isBusy
    val visibilityProgressView: LiveData<Int> =
        Transformations.map(_isBusy) { busy ->
            if (busy) View.VISIBLE else View.GONE
        }

    // Live data to set visibility of empty view depending upon whether any media was found
    val visibilityEmptyView = MediatorLiveData<Int>().apply {
        addSource(_isBusy) { busy ->
            if (busy) value = View.GONE
        }

        addSource(_mediaItemsListLiveData) { list ->
            value = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }

    // Min & max dimensions of content image
    private val minDimens: Int
    private val maxDimens: Int

    init {
        minDimens = applicationContext.resources.getInteger(R.integer.min_size_content)
        maxDimens = applicationContext.resources.getInteger(R.integer.max_size_content)
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
        _isBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val mediaList = loadMediaFromStorage()
            _mediaItemsListLiveData.postValue(mediaList)
            _isBusy.postValue(false)
        }
    }

    /**
     * Given media item (chosen by user) filter through sanity checks and save a scaled temporary
     * bitmap if necessary.
     */
    fun updateSelectedMediaItem(mediaItem: MediaItem) {
        val size = if (mediaItem.width > 0 && mediaItem.height > 0) Size(
            mediaItem.width,
            mediaItem.height
        )
        else ImageUtils.getImageSizeFromUri(applicationContext, mediaItem.uri)

        // If original bitmap's dimension is too small show error
        if (min(size.width, size.height) < minDimens) {
            Toast.makeText(
                applicationContext,
                applicationContext.resources.getString(R.string.error_small_dimension, minDimens),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val r: Float = maxDimens / max(size.width, size.height).toFloat()

        // If scaled bitmap's dimension will be too small show error
        if (ceil(min(size.width, size.height) * r).toInt() < minDimens) {
            Toast.makeText(
                applicationContext,
                applicationContext.resources.getString(R.string.error_dimensions_large_difference),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // If size is less than max value proceed normally. Otherwise create a temporary scaled
        // bitmap and proceed with it's uri
        if (max(size.width, size.height) <= maxDimens)
            _mediaItemUriLiveData.value = mediaItem.uri
        else {
            _isBusy.value = true
            viewModelScope.launch(Dispatchers.IO) {
                val uri = loadAndSaveScaledBitmap(mediaItem.uri, size.width, size.height, r)
                _mediaItemUriLiveData.postValue(uri)
                _isBusy.postValue(false)
            }
        }
    }

    /**
     * Sets selected media item's uri to null, so that lambda at observer doesn't go to the
     * editor upon hitting back button
     */
    fun setMediaUsed() {
        _mediaItemUriLiveData.value = null
    }

    /**
     * Load all image media (uri, width & height) from device through content resolver
     * @return List of MediaItem objects
     */
    private fun loadMediaFromStorage(): MutableList<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()

        val contentResolver = applicationContext.contentResolver
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
     * Loads a scaled version of image scaled by factor [r] at uri, [uri]
     * and saves it temporarily
     *
     * @param uri String value of original image's uri
     * @param originalWidth Width of original image
     * @param originalHeight Height of original image
     * @param r Factor by which to scale
     * @return Uri of temporary scaled image file
     */
    private fun loadAndSaveScaledBitmap(
        uri: Uri,
        originalWidth: Int,
        originalHeight: Int,
        r: Float
    ): Uri? {
        val bitmap = ImageUtils.loadScaledBitmap(
            applicationContext, uri,
            originalWidth, originalHeight, r
        )
        val tempUri = ImageUtils.createTemporaryUri(applicationContext, ".png")

        if (tempUri != null) {
            val out = applicationContext.contentResolver.openOutputStream(tempUri)
            bitmap?.compress(Bitmap.CompressFormat.PNG, 90, out)
            out!!.close()
        }

        bitmap?.recycle()

        return tempUri
    }
}