package com.ehabnaguib.android.securec.utils

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatEditText

class CustomEditText(context: Context, attrs: AttributeSet) : AppCompatEditText(context, attrs) {

    init {
        // Enable vertical scrolling on the EditText
        isVerticalScrollBarEnabled = true
        movementMethod = android.text.method.ScrollingMovementMethod.getInstance()
        scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
        isScrollbarFadingEnabled = false
    }

    private fun canScrollVertically(): Boolean {
        // Check if the EditText can scroll vertically in any direction
        return (scrollY > 0 || height < layout.height + paddingTop + paddingBottom)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Request that the parent ScrollView does not intercept touch events if there is content to scroll
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (canScrollVertically()) {
                    // Only request parent to not intercept touch events if we can scroll
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (canScrollVertically()) {
                    // Only request parent to not intercept touch events if we can scroll
                    parent.requestDisallowInterceptTouchEvent(true)
                } else {
                    // Otherwise, allow the parent to intercept and handle the scrolling
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Parent request to intercept touch events once again
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        // Handle the touch event
        return super.onTouchEvent(event)
    }
}