package `in`.thenvn.artista

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

@BindingAdapter("srcUri")
fun ImageView.setSrcUri(item: MediaItem) {
    Glide.with(context)
        .load(item.uri)
        .override(400)
        .centerCrop()
        .into(this)
}