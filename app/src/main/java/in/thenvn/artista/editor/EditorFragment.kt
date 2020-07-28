package `in`.thenvn.artista.editor

import `in`.thenvn.artista.ListSpaceItemDecoration
import `in`.thenvn.artista.R
import `in`.thenvn.artista.databinding.FragmentEditorBinding
import `in`.thenvn.artista.utils.ImageUtils
import `in`.thenvn.artista.utils.setSrcUri
import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

class EditorFragment : Fragment() {

    private lateinit var editorViewModel: EditorViewModel
    private lateinit var adapter: StylesAdapter

    private lateinit var fadeInAnimator: ObjectAnimator
    private lateinit var fadeOutAnimator: ObjectAnimator

    private var isBusy: Boolean = false

    companion object {
        private const val TAG = "EditorFragment"
        private const val MIN_DIMENS = 256
    }

    private val photosResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                // Proceed only if minimum dimension is at least 256 pixels else show error toast
                if (ImageUtils.validateMinimumDimension(requireContext(), uri, MIN_DIMENS)) {
                    val style = Style(uri, Style.CUSTOM)
                    editorViewModel.addStyle(style)
                    applyStyle(style)
                } else Toast.makeText(
                    context,
                    resources.getString(R.string.error_small_dimension), Toast.LENGTH_LONG
                ).show()
            } else Toast.makeText(
                context,
                resources.getString(R.string.error_unknown),
                Toast.LENGTH_SHORT
            ).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentEditorBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_editor, container, false
        )

        requireNotNull(activity as AppCompatActivity).supportActionBar?.hide()

        var uriString: String?

        arguments?.let { bundle ->
            val args = EditorFragmentArgs.fromBundle(bundle)
            uriString = args.mediaUri

            val application = requireNotNull(activity).application

            // Load view model
            val viewModelFactory = EditorViewModelFactory(Uri.parse(uriString), application)
            editorViewModel =
                ViewModelProvider(this, viewModelFactory).get(EditorViewModel::class.java)
            binding.editorViewModel = editorViewModel

            // Set up the list with adapter
            adapter = StylesAdapter(
                StylesAdapter.StyleClickListener(
                    { style -> applyStyle(style) },
                    { openPhotosActivity() })
            )
            val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.stylesList.layoutManager = layoutManager
            val space = resources.getDimensionPixelSize(R.dimen.item_space)
            binding.stylesList.addItemDecoration(ListSpaceItemDecoration(space))
            binding.stylesList.adapter = adapter

            binding.lifecycleOwner = this

            // Load animators for showing/hiding progress views
            loadAnimators(binding.progressHolder)

            // Attach observers
            editorViewModel.processBusyLiveData.observe(viewLifecycleOwner, Observer {
                isBusy = it
                if (isBusy) fadeInAnimator.start()
                else fadeOutAnimator.start()
            })
            editorViewModel.stylesListLiveData.observe(viewLifecycleOwner, Observer { styles ->
                adapter.updateList(styles)
            })

            // Set initial preview as the original image itself
            binding.preview.setSrcUri(Uri.parse(uriString))

            // Set listeners
            binding.controls.blendingSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) = editorViewModel.updateBlendRatio(progress.toFloat() / seekBar!!.max)

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            })
        }

        return binding.root
    }

    private fun applyStyle(style: Style) {
        if (isBusy) return

        adapter.showAsSelection(style)
        Log.i(TAG, "applyStyle trigger")
        editorViewModel.applyStyle(
            requireContext(),
            editorViewModel.originalMediaUri,
            style
        )
    }

    private fun openPhotosActivity() {
        if (isBusy) return
        photosResultLauncher.launch("image/*")
    }

    private fun loadAnimators(targetView: View) {
        fadeInAnimator =
            (AnimatorInflater.loadAnimator(
                requireContext(),
                R.animator.animator_fade_in
            ) as ObjectAnimator)
                .apply {
                    target = targetView
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            targetView.visibility = View.VISIBLE
                        }
                    })
                }

        fadeOutAnimator =
            (AnimatorInflater.loadAnimator(
                requireContext(),
                R.animator.animator_fade_out
            ) as ObjectAnimator)
                .apply {
                    target = targetView
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            targetView.visibility = View.INVISIBLE
                        }
                    })
                }
    }
}