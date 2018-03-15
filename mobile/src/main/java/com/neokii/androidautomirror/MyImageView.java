package com.neokii.androidautomirror;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyImageView extends AppCompatImageView
{
    public MyImageView(Context context)
    {
        super(context);
        setColorFilter(0x55191F27, PorterDuff.Mode.SRC_OVER);
    }

    public MyImageView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs, 0);
        setColorFilter(0x55191F27, PorterDuff.Mode.SRC_OVER);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(event.getAction() == MotionEvent.ACTION_DOWN)
        {
            setColorFilter(0xaa191F27, PorterDuff.Mode.SRC_OVER);
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            setColorFilter(0x55191F27, PorterDuff.Mode.SRC_OVER);
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if(heightMode == MeasureSpec.EXACTLY)
        {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
