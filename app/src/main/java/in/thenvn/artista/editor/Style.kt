package `in`.thenvn.artista.editor

import android.net.Uri

data class Style(
    val uri: Uri,
    val type: Int,
    var selected: Boolean = false
) {
    companion object {
        const val FIXED = 0
        const val CUSTOM = 1
    }

}