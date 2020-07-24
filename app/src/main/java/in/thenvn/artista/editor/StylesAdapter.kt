package `in`.thenvn.artista.editor

import `in`.thenvn.artista.databinding.ListItemButtonBinding
import `in`.thenvn.artista.databinding.ListItemStyleBinding
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class StylesAdapter(private val clickListener: StyleClickListener) :
    ListAdapter<Style, RecyclerView.ViewHolder>(StyleDiffCallback()) {

    companion object {
        const val TYPE_BUTTON = 0
        const val TYPE_STYLE = 1
    }

    private lateinit var lastSelection: Style

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_BUTTON) return ButtonViewHolder.from(parent)
        return StyleViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ButtonViewHolder) holder.bind(clickListener)
        else (holder as StyleViewHolder).bind(getItem(position), clickListener)
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return TYPE_BUTTON
        return TYPE_STYLE
    }

    fun showAsSelection(style: Style) {
        if (this::lastSelection.isInitialized) {
            lastSelection.selected = false
            notifyItemChanged(currentList.indexOf(lastSelection))
        }

        style.selected = true
        lastSelection = style
        notifyItemChanged(currentList.indexOf(style))
    }

    fun updateList(styles: ArrayList<Style>) {
        submitList(styles)
        notifyItemInserted(1)
    }

    class StyleViewHolder private constructor(val binding: ListItemStyleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(style: Style, clickListener: StyleClickListener) {
            binding.style = style
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): StyleViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemStyleBinding.inflate(layoutInflater, parent, false)
                return StyleViewHolder(binding)
            }
        }
    }

    class ButtonViewHolder private constructor(val binding: ListItemButtonBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(clickListener: StyleClickListener) {
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ButtonViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemButtonBinding.inflate(layoutInflater, parent, false)
                return ButtonViewHolder(binding)
            }
        }
    }

    class StyleDiffCallback : DiffUtil.ItemCallback<Style>() {
        override fun areItemsTheSame(oldItem: Style, newItem: Style): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: Style, newItem: Style): Boolean {
            return oldItem == newItem
        }

    }

    class StyleClickListener(
        val styleClickListener: (style: Style) -> Unit,
        val buttonClickListener: () -> Unit
    ) {
        fun onStyleClick(style: Style) = styleClickListener(style)


        fun onButtonClick() = buttonClickListener()
    }
}