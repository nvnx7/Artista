package `in`.thenvn.artista

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ListSpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = space
        outRect.right = space
    }
}