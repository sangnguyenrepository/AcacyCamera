package vn.com.acacy.cameralibrary;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class AutoFitTextureView extends TextureView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    public int mratioType = 0;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public void setAspectRatio(int width, int height, int ratioType) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        mratioType = ratioType;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (mratioType == 43) {
                if (width < height * mRatioWidth / mRatioHeight) {
                    setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
                } else {
                    setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
                }
            } else if (mratioType == 169) {
                Double ratioWidth = Double.valueOf(width);
                Double ratioHeight = Double.valueOf(height);
                if (Double.compare((ratioHeight / ratioWidth), (Double.valueOf(16) / Double.valueOf(9))) == 0) {
                    if (width < height * mRatioWidth / mRatioHeight) {
                        setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
                    } else {
                        setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
                    }
                } else {
                    if (width < height * mRatioWidth / mRatioHeight) {
                        setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
                    } else {
                        setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
                    }
                }
            } else {
                if (width < height * mRatioWidth / mRatioHeight) {
                    setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
                } else {
                    setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
                }
            }
        }

    }

}
