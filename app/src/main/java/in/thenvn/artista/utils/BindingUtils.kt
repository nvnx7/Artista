package `in`.thenvn.artista.utils

import `in`.thenvn.artista.media.MediaItem
import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

@BindingAdapter("srcMedia")
fun ImageView.setSrcMedia(item: MediaItem) {
    Glide.with(context)
        .load(item.uri)
        .override(400)
        .centerCrop()
        .into(this)
}

@BindingAdapter("srcUri")
fun ImageView.setSrcUri(uri: Uri) {
    Glide.with(context)
        .load(uri)
        .into(this)
}