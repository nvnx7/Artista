package `in`.thenvn.artista.editor

import `in`.thenvn.artista.ListSpaceItemDecoration
import `in`.thenvn.artista.R
import `in`.thenvn.artista.StyleTransferModelExecutor
import `in`.thenvn.artista.databinding.FragmentEditorBinding
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import java.util.concurrent.Executors

class EditorFragment : Fragment() {

    private lateinit var styleTransferModelExecutor: StyleTransferModelExecutor
    private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val mainScope = MainScope()

    companion object {
        private const val TAG = "EditorFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentEditorBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_editor, container, false
        )

        var uriString: String? = null

        arguments?.let {
            val args = EditorFragmentArgs.fromBundle(it)
            uriString = args.mediaUri
            Log.i(TAG, "Uri received: $uriString")
            val application = requireNotNull(activity).application

            val viewModelFactory = EditorViewModelFactory(Uri.parse(uriString), application)
            val editorViewModel =
                ViewModelProvider(this, viewModelFactory).get(EditorViewModel::class.java)
            binding.editorViewModel = editorViewModel

            mainScope.async(inferenceThread) {
                styleTransferModelExecutor = StyleTransferModelExecutor(requireContext())
                Log.i(TAG, "Executor created")
            }

            val adapter = StylesAdapter(StylesAdapter.StyleClickListener { style ->
                Log.i(TAG, "Style clicked: ${style.uri}")
                editorViewModel.applyStyle(
                    requireContext(),
                    editorViewModel.originalMediaUriLiveData.value!!,
                    style.uri,
                    styleTransferModelExecutor,
                    inferenceThread
                )
            })

            Log.i(TAG, "List to be submitted ${editorViewModel.stylesList.value?.get(0)}")
            adapter.submitList(editorViewModel.stylesList.value)

            val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.stylesList.layoutManager = layoutManager
            val space = resources.getDimensionPixelSize(R.dimen.item_space)
            binding.stylesList.addItemDecoration(ListSpaceItemDecoration(space))

            binding.stylesList.adapter = adapter
            binding.lifecycleOwner = this

            editorViewModel.stylesList.observe(viewLifecycleOwner, Observer { styles ->
                binding.stylesList.adapter = adapter
                adapter.submitList(styles)
            })

            editorViewModel.styledBitmap.observe(viewLifecycleOwner, Observer { bitmap ->
                Glide.with(this)
                    .load(bitmap)
                    .into(binding.preview)
            })

//            binding.preview.setOnClickListener {
//                Log.i(TAG, "onCreateView: setting style img")
//                Glide.with(this)
//                    .load("file:///android_asset/styles/style0.jpg")
//                    .override(300)
//                    .centerInside()
//                    .into(binding.preview)
//
//                Log.i(TAG, "Submitted sample ${adapter.currentList.get(0)}")
//            }
        }

        return binding.root
    }
}