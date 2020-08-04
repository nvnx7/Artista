package `in`.thenvn.artista.custom

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * Custom image view that makes the drawable width to be same as the view regardless of
 * height of drawable, while also maintaining aspect ratio of drawable.
 */
class FullWidthImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (drawable != null) {
            val w: Int = MeasureSpec.getSize(widthMeasureSpec)
            val h: Int = w * drawable.intrinsicHeight / drawable.intrinsicWidth
            setMeasuredDimension(w, h)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}