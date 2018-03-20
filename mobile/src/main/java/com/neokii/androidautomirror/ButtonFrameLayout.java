package com.neokii.androidautomirror;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class ButtonFrameLayout extends FrameLayout
{
    public ButtonFrameLayout(Context context)
    {
        super(context);
        init();
    }

    public ButtonFrameLayout(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs, 0);
        init();
    }

    public void init()
    {
        setAlpha(0.8f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(event.getAction() == MotionEvent.ACTION_DOWN)
        {
            setAlpha(0.4f);
            return true;
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            setAlpha(0.8f);
            return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        init();
    }
}
