package `in`.thenvn.artista.media

import `in`.thenvn.artista.GridSpaceItemDecoration
import `in`.thenvn.artista.R
import `in`.thenvn.artista.databinding.FragmentMediaSelectionBinding
import `in`.thenvn.artista.utils.ImageUtils
import `in`.thenvn.artista.utils.PermissionUtils
import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
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

class MediaSelectionFragment : Fragment() {

    companion object {
        private const val TAG = "MediaSelectionFragment"
        val permissionString = Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    private lateinit var capturedPicUri: Uri
    private lateinit var mediaViewModel: MediaViewModel

    private val photosResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { navigateToEditor(uri.toString()) }
        }

    private val cameraResultLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) navigateToEditor(capturedPicUri.toString())
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
        val application = requireNotNull(this.activity).application

        val viewModelFactory =
            MediaViewModelFactory(application)
        mediaViewModel =
            ViewModelProvider(this, viewModelFactory).get(MediaViewModel::class.java)

        binding.mediaViewModel = mediaViewModel

        // After setting up view model check permissions
        checkPermissions()

        // Set up adapter and list of media
        val adapter =
            MediaItemsAdapter(MediaItemsAdapter.MediaItemClickListener { mediaItem ->
                navigateToEditor(mediaItem.uri.toString())
            })
        val layoutManager = GridLayoutManager(activity, 3)
        binding.mediaGrid.layoutManager = layoutManager
        binding.mediaGrid.setHasFixedSize(true)
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_space)
        binding.mediaGrid.addItemDecoration(GridSpaceItemDecoration(spacing, 3))
        binding.mediaGrid.adapter = adapter

        binding.lifecycleOwner = this

        // Attach observers
        mediaViewModel.mediaItemListLiveData?.observe(viewLifecycleOwner, Observer { mediaUriList ->
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
        Log.i(TAG, "onResume: ")
        // If user possibly returns from settings after giving permissions update the status in
        // view model so that data is fetched
        if (PermissionUtils.isPermissionGranted(requireContext(), permissionString)) {
            Log.i(TAG, "onResume: Permission granted")
            if (mediaViewModel.permissionGrantedLiveData.value == false) {
                Log.i(TAG, "onResume: Updating status in view model")
                mediaViewModel.updatePermissionGrantedStatus(true)
            }
        }
    }

    private fun checkPermissions() {

        when {
            // Already granted continue with loading data
            PermissionUtils.isPermissionGranted(
                requireContext(),
                permissionString
            ) -> mediaViewModel.updatePermissionGrantedStatus(true)

            // Ask permission showing rationale without settings option
            shouldShowRequestPermissionRationale(permissionString) -> askPermission()


            // Try asking for permission
            else -> requestPermissionLauncher.launch(permissionString)

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
                requestPermissionLauncher.launch(permissionString)
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
        val action = MediaSelectionFragmentDirections
            .actionMediaSelectionFragmentToEditorFragment(uriString)

        findNavController().navigate(action)
    }

    private fun openPhotosActivity() {
        photosResultLauncher.launch("image/*")
    }

    private fun launchCamera() {
        capturedPicUri = ImageUtils.createTemporaryFile(requireContext())!!
        cameraResultLauncher.launch(capturedPicUri)
    }
}