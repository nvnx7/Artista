package `in`.thenvn.artista.editor

import `in`.thenvn.artista.databinding.ListItemStyleBinding
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class StylesAdapter(private val clickListener: StyleClickListener) :
    ListAdapter<Style, StylesAdapter.ViewHolder>(StyleDiffCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    class ViewHolder private constructor(val binding: ListItemStyleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(style: Style, clickListener: StyleClickListener) {
            binding.style = style
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemStyleBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
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

    class StyleClickListener(val clickListener: (style: Style) -> Unit) {
        fun onClick(style: Style) = clickListener(style)
    }
}