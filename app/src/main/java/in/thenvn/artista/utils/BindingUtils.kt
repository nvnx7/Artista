package `in`.thenvn.artista.utils

import `in`.thenvn.artista.editor.Style
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

@BindingAdapter("srcStyle")
fun ImageView.setSrcStyle(style: Style) {
    Glide.with(context)
        .load(style.uri)
        .override(300)
        .centerInside()
        .into(this)
}