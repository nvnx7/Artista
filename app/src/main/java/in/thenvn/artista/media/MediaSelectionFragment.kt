package `in`.thenvn.artista.media

import `in`.thenvn.artista.GridSpaceItemDecoration
import `in`.thenvn.artista.R
import `in`.thenvn.artista.databinding.FragmentMediaSelectionBinding
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager

class MediaSelectionFragment : Fragment() {

    companion object {
        private const val TAG = "MediaSelectionFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentMediaSelectionBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_media_selection, container, false
        )

        val application = requireNotNull(this.activity).application

        val viewModelFactory =
            MediaViewModelFactory(application)
        val mediaViewModel =
            ViewModelProvider(this, viewModelFactory).get(MediaViewModel::class.java)

        binding.mediaViewModel = mediaViewModel
        val adapter =
            MediaItemsAdapter(MediaItemsAdapter.MediaItemClickListener { mediaItem ->
                Log.d(TAG, "Item clicked with uri ${mediaItem.uri}")
                mediaViewModel.onMediaItemClicked(mediaItem)
                val action = MediaSelectionFragmentDirections
                    .actionMediaSelectionFragmentToEditorFragment(mediaItem.uri.toString())

                findNavController().navigate(action)
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

        return binding.root
    }

}