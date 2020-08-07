package com.naveeen.artista.media

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.naveeen.artista.R
import com.naveeen.artista.custom.GridSpaceItemDecoration
import com.naveeen.artista.databinding.FragmentMediaSelectionBinding
import com.naveeen.artista.utils.ImageUtils
import com.naveeen.artista.utils.PermissionUtils

class MediaSelectionFragment : Fragment() {

    companion object {
        private const val TAG = "MediaSelectionFragment"
        private const val PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    // Uri of captured pic from camera
    private lateinit var capturedPicUri: Uri

    private lateinit var mediaViewModel: MediaViewModel

    // If view model is already busy
    private var isBusy: Boolean = false

    private val photosResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                mediaViewModel.updateSelectedMediaItem(MediaItem(uri, 0, 0))
            }
        }

    private val cameraResultLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                // Proceed only if image was captured
                mediaViewModel.updateSelectedMediaItem(MediaItem(capturedPicUri, 0, 0))
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
            if (!isBusy) mediaViewModel.updateSelectedMediaItem(mediaItem)
        })

        val layoutManager =
            GridLayoutManager(activity, resources.getInteger(R.integer.span_count_media_grid))
        binding.mediaGrid.layoutManager = layoutManager
        binding.mediaGrid.setHasFixedSize(true)
        val spacing = resources.getDimensionPixelSize(R.dimen.item_space_media)

        binding.mediaGrid.addItemDecoration(
            GridSpaceItemDecoration(
                spacing,
                3
            )
        )
        binding.mediaGrid.adapter = adapter

        binding.lifecycleOwner = this

        // Attach observers
        mediaViewModel.mediaItemsListLiveData.observe(viewLifecycleOwner, Observer { mediaUriList ->
            adapter.submitList(mediaUriList)
        })
        mediaViewModel.permissionGrantedLiveData.observe(viewLifecycleOwner, Observer { isGranted ->
            if (isGranted) mediaViewModel.fetchAllMedia()
        })
        mediaViewModel.mediaItemUriLiveData.observe(viewLifecycleOwner, Observer { uri ->
            uri?.let { navigateToEditor(uri) }
        })
        mediaViewModel.isBusy.observe(viewLifecycleOwner, Observer { busy -> isBusy = busy })

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

    override fun onStop() {
        super.onStop()

        // After nav controller has navigated to editor set the selected media as used so that
        // upon coming back from editor observer's lambda function doesn't trigger
        mediaViewModel.setMediaUsed()
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

    private fun navigateToEditor(uri: Uri) {
        val action = MediaSelectionFragmentDirections
            .actionMediaSelectionFragmentToEditorFragment(uri.toString())

        findNavController().navigate(action)
    }

    private fun openPhotosActivity() {
        if (isBusy) return
        photosResultLauncher.launch("image/*")
    }

    private fun launchCamera() {
        if (isBusy) return
        capturedPicUri = ImageUtils.createTemporaryUri(requireContext())!!
        cameraResultLauncher.launch(capturedPicUri)
    }

}