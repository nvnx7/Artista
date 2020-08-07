package com.naveeen.artista.custom

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * Custom image view that makes the drawable height to be same as the view regardless of
 * width of drawable, while also maintaining aspect ratio of drawable.
 */
class FullHeightImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (drawable != null) {
            val h: Int = MeasureSpec.getSize(heightMeasureSpec)
            val w: Int = h * drawable.intrinsicWidth / drawable.intrinsicHeight
            setMeasuredDimension(w, h)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}