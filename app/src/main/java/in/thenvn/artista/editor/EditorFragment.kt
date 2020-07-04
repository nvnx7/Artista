package `in`.thenvn.artista.editor

import `in`.thenvn.artista.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class EditorFragment : Fragment() {

    companion object {
        private const val TAG = "EditorFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        arguments?.let {
            val args = EditorFragmentArgs.fromBundle(it)
            val uriString = args.mediaUri
            Log.d(TAG, "Uri received: $uriString")
        }
        return inflater.inflate(R.layout.fragment_editor, container, false)
    }
}