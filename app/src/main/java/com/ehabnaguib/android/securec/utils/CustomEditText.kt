package com.ehabnaguib.android.securec.utils

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class CustomEditText(context: Context, attrs: AttributeSet) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {

    val scrollableHeight: Int = this.getHeight()
    var contentHeight: Int = 0

    init {
        // Enable vertical scrolling on the EditText
        isVerticalScrollBarEnabled = true
        movementMethod = ScrollingMovementMethod.getInstance()
        scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
        isScrollbarFadingEnabled = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Request that the parent ScrollView does not intercept touch events
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                // Parent request to not intercept touch events
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP -> {
                // Parent request to intercept touch events once again
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        // Handle the touch event
        return super.onTouchEvent(event)
    }
}