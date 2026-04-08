package com.example.secviz.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.cardview.widget.CardView;

public class ResizableCardView extends CardView {

    private final int minHeightPx;
    private final int dragZone;

    private boolean isDragging = false;
    private float lastY = 0f;

    public ResizableCardView(Context context) {
        this(context, null);
    }

    public ResizableCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ResizableCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = context.getResources().getDisplayMetrics().density;
        minHeightPx = (int) (120 * density);
        dragZone    = (int) (24 * density);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isInDragZone(ev) || isDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isInDragZone(ev)) {
                    isDragging = true;
                    lastY = ev.getRawY();
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float delta = ev.getRawY() - lastY;
                    lastY = ev.getRawY();
                    int newHeight = Math.max(
                            getLayoutParams().height + (int) delta,
                            minHeightPx
                    );
                    getLayoutParams().height = newHeight;
                    requestLayout();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    private boolean isInDragZone(MotionEvent ev) {
        return ev.getY() >= getHeight() - dragZone;
    }
}