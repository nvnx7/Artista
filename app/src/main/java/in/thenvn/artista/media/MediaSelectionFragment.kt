package `in`.thenvn.artista.media

import `in`.thenvn.artista.BuildConfig
import `in`.thenvn.artista.GridSpaceItemDecoration
import `in`.thenvn.artista.R
import `in`.thenvn.artista.databinding.FragmentMediaSelectionBinding
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import java.io.File
import java.io.IOException

class MediaSelectionFragment : Fragment() {

    companion object {
        private const val TAG = "MediaSelectionFragment"
    }

    private lateinit var capturedPicUri: Uri

    private val photosResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            navigateToEditor(uri.toString())
        }

    private val cameraResultLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            navigateToEditor(capturedPicUri.toString())
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentMediaSelectionBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_media_selection, container, false
        )

        Log.i(TAG, "onCreateView App id: ${BuildConfig.APPLICATION_ID}")
        requireNotNull(activity as AppCompatActivity).supportActionBar?.show()
        val application = requireNotNull(this.activity).application

        val viewModelFactory =
            MediaViewModelFactory(application)
        val mediaViewModel =
            ViewModelProvider(this, viewModelFactory).get(MediaViewModel::class.java)

        binding.mediaViewModel = mediaViewModel
        val adapter =
            MediaItemsAdapter(MediaItemsAdapter.MediaItemClickListener { mediaItem ->
                Log.d(TAG, "Item clicked with uri ${mediaItem.uri}")
                navigateToEditor(mediaItem.uri.toString())
            })

        adapter.submitList(mediaViewModel.mediaItemListLiveData?.value)

        val layoutManager = GridLayoutManager(activity, 3)
        binding.mediaGrid.layoutManager = layoutManager
        binding.mediaGrid.setHasFixedSize(true)
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_space)
        binding.mediaGrid.addItemDecoration(GridSpaceItemDecoration(spacing, 3))

        binding.mediaGrid.adapter = adapter
        binding.lifecycleOwner = this

        mediaViewModel.mediaItemListLiveData?.observe(viewLifecycleOwner, Observer { mediaUriList ->
            Log.d(TAG, "Fetched media list!")
            binding.mediaGrid.adapter = adapter
            adapter.submitList(mediaUriList)
        })

        binding.photosButton.setOnClickListener { openPhotosActivity() }
        binding.cameraButton.setOnClickListener { launchCamera() }

        return binding.root
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
        capturedPicUri = createTemporaryFile()!!
        cameraResultLauncher.launch(capturedPicUri)
    }

    private fun createTemporaryFile(): Uri? {
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        Log.i(TAG, "createTemporaryFile: ${storageDir.toString()}")

        return try {
            val file = File.createTempFile("tmp", ".jpg", storageDir)
            FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )
        } catch (ex: IOException) {
            Toast.makeText(requireContext(), "Error creating new file!", Toast.LENGTH_SHORT).show()
            null
        }
    }
}