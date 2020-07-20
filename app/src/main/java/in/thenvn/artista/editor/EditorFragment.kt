package `in`.thenvn.artista.editor

import `in`.thenvn.artista.ListSpaceItemDecoration
import `in`.thenvn.artista.R
import `in`.thenvn.artista.databinding.FragmentEditorBinding
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide

class EditorFragment : Fragment() {

    private lateinit var editorViewModel: EditorViewModel
    private var isBusy: Boolean = false

    companion object {
        private const val TAG = "EditorFragment"
        private const val RC_PICK_IMAGE = 1000
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentEditorBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_editor, container, false
        )

        var uriString: String?

        arguments?.let { bundle ->
            val args = EditorFragmentArgs.fromBundle(bundle)
            uriString = args.mediaUri
            Log.i(TAG, "Uri received: $uriString")
            val application = requireNotNull(activity).application

            val viewModelFactory = EditorViewModelFactory(Uri.parse(uriString), application)
            editorViewModel =
                ViewModelProvider(this, viewModelFactory).get(EditorViewModel::class.java)
            binding.editorViewModel = editorViewModel

            val adapter = StylesAdapter(
                StylesAdapter.StyleClickListener(
                    { style -> applyStyle(style) },
                    { openPhotosActivity() }
                )
            )

            adapter.submitList(editorViewModel.stylesList.value)

            val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.stylesList.layoutManager = layoutManager
            val space = resources.getDimensionPixelSize(R.dimen.item_space)
            binding.stylesList.addItemDecoration(ListSpaceItemDecoration(space))

            binding.stylesList.adapter = adapter
            binding.lifecycleOwner = this

            editorViewModel.processBusyLiveData.observe(
                viewLifecycleOwner,
                Observer { isBusy = it })

            editorViewModel.stylesList.observe(viewLifecycleOwner, Observer { styles ->
                binding.stylesList.adapter = adapter
                adapter.submitList(styles)
            })

            editorViewModel.styledBitmap.observe(viewLifecycleOwner, Observer { bitmap ->
                Glide.with(this)
                    .load(bitmap)
                    .into(binding.preview)
            })
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == RC_PICK_IMAGE) {
            val uri = data!!.data!!
            applyStyle(Style(uri, Style.CUSTOM))
        } else {
            Toast.makeText(context, "Something went wrong!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyStyle(style: Style) {
        if (isBusy) return

        Log.i(TAG, "applyStyle trigger")
        editorViewModel.applyStyle(
            requireContext(),
            editorViewModel.originalMediaUriLiveData.value!!,
            style
        )
    }

    private fun openPhotosActivity() {
        if (isBusy) return

        Log.i(TAG, "chooseCustomStyle: execute")
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, RC_PICK_IMAGE)
    }
}