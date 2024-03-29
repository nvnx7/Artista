package com.naveeen.artista.utils

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.naveeen.artista.editor.Style
import com.naveeen.artista.media.MediaItem

@BindingAdapter("srcMedia")
fun ImageView.setSrcMedia(item: MediaItem) {
    Glide.with(context)
        .load(item.uri)
        .override(200)
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
        .override(200)
        .centerCrop()
        .into(this)
}

@BindingAdapter("srcBitmap")
fun ImageView.setSrcBitmap(bitmap: Bitmap?) {
    bitmap?.let { setImageBitmap(bitmap) }
}

@BindingAdapter("invisible")
fun View.setInvisible(isInvisible: Boolean) {
    visibility = if (isInvisible) View.INVISIBLE else View.VISIBLE
}