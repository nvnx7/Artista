package `in`.thenvn.artista.media

import `in`.thenvn.artista.databinding.ListItemMediaBinding
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class MediaItemsAdapter(private val clickListener: MediaItemClickListener) :
    ListAdapter<MediaItem, MediaItemsAdapter.ViewHolder>(
        MediaItemDiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(
            parent
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    class ViewHolder private constructor(val binding: ListItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MediaItem, clickListener: MediaItemClickListener) {
            binding.mediaItem = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemMediaBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(
                    binding
                )
            }
        }
    }

    class MediaItemDiffCallback:DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.uri == newItem.uri;
        }

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem;
        }

    }

    class MediaItemClickListener(val clickListener: (uri: Uri) -> Unit) {
        fun onClick(item: MediaItem) = clickListener(item.uri)
    }

}