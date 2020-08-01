package `in`.thenvn.artista.media

import `in`.thenvn.artista.GridSpaceItemDecoration
import `in`.thenvn.artista.R
import `in`.thenvn.artista.databinding.FragmentMediaSelectionBinding
import `in`.thenvn.artista.utils.ImageUtils
import `in`.thenvn.artista.utils.PermissionUtils
import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class MediaSelectionFragment : Fragment() {

    companion object {
        private const val TAG = "MediaSelectionFragment"
        private const val PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
        private const val MIN_DIMENS = 256
        private const val MAX_DIMENS = 1280
    }

    private lateinit var capturedPicUri: Uri
    private lateinit var mediaViewModel: MediaViewModel

    // Dimensions size of chosen media
    private var size: Size = Size(0, 0)

    private val photosResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                size = ImageUtils.getImageSizeFromUri(requireContext(), uri)
                navigateToEditor(uri.toString())
            }
        }

    private val cameraResultLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                // Proceed only if image was captured
                size = ImageUtils.getImageSizeFromUri(requireContext(), capturedPicUri)
                navigateToEditor(capturedPicUri.toString())
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) mediaViewModel.updatePermissionGrantedStatus(true)
            else askPermission()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "onCreateView: ")
        // Inflate the layout for this fragment
        val binding: FragmentMediaSelectionBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_media_selection, container, false
        )

        requireNotNull(activity as AppCompatActivity).supportActionBar?.show()

        mediaViewModel = ViewModelProvider(this).get(MediaViewModel::class.java)

        binding.mediaViewModel = mediaViewModel

        // After setting up view model check permissions
        checkPermissions()

        // Set up adapter and list of media
        val adapter = MediaItemsAdapter(MediaItemsAdapter.MediaItemClickListener { mediaItem ->
            // If media dimension is less than 256, do not proceed & show error message instead
            size = if (mediaItem.width > 0 && mediaItem.height > 0) Size(
                mediaItem.width,
                mediaItem.height
            )
            else ImageUtils.getImageSizeFromUri(requireContext(), mediaItem.uri)

            when {
                (size.width < MIN_DIMENS || size.height < MIN_DIMENS) -> {
                    Toast.makeText(
                        requireContext(),
                        resources.getString(R.string.error_small_dimension),
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> navigateToEditor(mediaItem.uri.toString())
            }

        })
        val layoutManager = GridLayoutManager(activity, 3)
        binding.mediaGrid.layoutManager = layoutManager
        binding.mediaGrid.setHasFixedSize(true)
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_space)
        binding.mediaGrid.addItemDecoration(GridSpaceItemDecoration(spacing, 3))
        binding.mediaGrid.adapter = adapter

        binding.lifecycleOwner = this

        // Attach observers
        mediaViewModel.mediaItemsListLiveData.observe(viewLifecycleOwner, Observer { mediaUriList ->
            adapter.submitList(mediaUriList)
        })
        mediaViewModel.permissionGrantedLiveData.observe(viewLifecycleOwner, Observer { isGranted ->
            if (isGranted) mediaViewModel.fetchAllMedia()
        })

        // Set click listeners
        binding.photosButton.setOnClickListener { openPhotosActivity() }
        binding.cameraButton.setOnClickListener { launchCamera() }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // If user possibly returns from settings after giving permissions update the status in
        // view model so that data is fetched
        if (PermissionUtils.isPermissionGranted(requireContext(), PERMISSION_STORAGE)) {
            if (mediaViewModel.permissionGrantedLiveData.value == false) {
                mediaViewModel.updatePermissionGrantedStatus(true)
            }
        }
    }

    private fun checkPermissions() {

        when {
            // Already granted continue with loading data
            PermissionUtils.isPermissionGranted(
                requireContext(),
                PERMISSION_STORAGE
            ) -> mediaViewModel.updatePermissionGrantedStatus(true)

            // Ask permission showing rationale without settings option
            shouldShowRequestPermissionRationale(PERMISSION_STORAGE) -> askPermission()

            // Try asking for permission
            else -> requestPermissionLauncher.launch(PERMISSION_STORAGE)

        }
    }

    private fun askPermission() {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Don't ask again was opted previously, so ask with settings option in rationale dialog
            askPermissionWithSettings()
        } else {
            // Permission is denied still
            PermissionUtils.showRationale(
                requireContext(),
                resources.getString(R.string.permission_rationale)
            ) {
                requestPermissionLauncher.launch(PERMISSION_STORAGE)
            }
        }
    }

    private fun askPermissionWithSettings() {
        PermissionUtils.showRationaleWithSettings(
            requireActivity(),
            resources.getString(R.string.permission_rationale_setting)
        )
    }

    private fun navigateToEditor(uriString: String) {
        var uri: String? = uriString
        MainScope().launch(Dispatchers.IO) {
            // If original size too large, first save a temporary scaled down version at uri
            if (max(size.width, size.height) > MAX_DIMENS) {
                uri = loadAndSaveScaledBitmap(uriString)
            }

            withContext(Dispatchers.Main) {
                if (uri == null) {
                    Toast.makeText(
                        requireContext(), resources.getString(R.string.error_unknown),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val action = MediaSelectionFragmentDirections
                        .actionMediaSelectionFragmentToEditorFragment(uri.toString())

                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun openPhotosActivity() {
        photosResultLauncher.launch("image/*")
    }

    private fun launchCamera() {
        capturedPicUri = ImageUtils.createTemporaryUri(requireContext())!!
        cameraResultLauncher.launch(capturedPicUri)
    }

    private fun loadAndSaveScaledBitmap(uriString: String): String? {
        val r: Float = MAX_DIMENS / max(size.width, size.height).toFloat()

        val bitmap = ImageUtils.loadScaledBitmap(
            requireContext(), Uri.parse(uriString),
            size.width, size.height, r
        )
        val uri = ImageUtils.createTemporaryUri(requireContext())

        if (uri != null) {
            val out = requireContext().contentResolver.openOutputStream(uri)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out!!.close()
        }

        bitmap?.recycle()

        return uri.toString()
    }

}