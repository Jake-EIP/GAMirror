package com.github.slashmax.aamirror;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyImageView extends AppCompatImageView
{
    public MyImageView(Context context) {
        super(context);
    }

    public MyImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(event.getAction() == MotionEvent.ACTION_DOWN)
        {
            setColorFilter(0xaa111111, PorterDuff.Mode.SRC_OVER);
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            setColorFilter(0x00000000, PorterDuff.Mode.SRC_OVER);
        }

        return super.onTouchEvent(event);
    }
}
