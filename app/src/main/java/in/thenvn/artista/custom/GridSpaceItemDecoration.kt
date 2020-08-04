package `in`.thenvn.artista.custom

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpaceItemDecoration(private val space: Int, private val span: Int) :
    RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val col = position % span

        outRect.left = space - col * space / span
        outRect.right = (col + 1) * space / span

        if (position < span) outRect.top = space
        outRect.bottom = space
    }
}