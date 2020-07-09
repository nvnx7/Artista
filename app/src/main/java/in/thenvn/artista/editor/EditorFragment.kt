package `in`.thenvn.artista.editor

import `in`.thenvn.artista.R
import `in`.thenvn.artista.databinding.FragmentEditorBinding
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class EditorFragment : Fragment() {

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
            Log.d(TAG, "Uri received: $uriString")
            val application = requireNotNull(activity).application

            val viewModelFactory = EditorViewModelFactory(Uri.parse(uriString), application)
            val viewModel =
                ViewModelProvider(this, viewModelFactory).get(EditorViewModel::class.java)
            binding.editorViewModel = viewModel
        }




        return binding.root
    }
}