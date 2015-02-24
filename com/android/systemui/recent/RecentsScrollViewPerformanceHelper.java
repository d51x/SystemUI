package com.android.systemui.recent;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import com.android.internal.R;

public class RecentsScrollViewPerformanceHelper {
    private boolean mAttachedToWindow;
    private Context mContext;
    private int mFadingEdgeLength;
    private boolean mIsVertical;
    private View mScrollView;
    private boolean mSoftwareRendered;

    public static RecentsScrollViewPerformanceHelper create(Context context, AttributeSet attrs, View scrollView, boolean isVertical) {
        return !context.getResources().getBoolean(2131230720) ? new RecentsScrollViewPerformanceHelper(context, attrs, scrollView, isVertical) : null;
    }

    public RecentsScrollViewPerformanceHelper(Context context, AttributeSet attrs, View scrollView, boolean isVertical) {
        this.mSoftwareRendered = false;
        this.mAttachedToWindow = false;
        this.mScrollView = scrollView;
        this.mContext = context;
        this.mFadingEdgeLength = context.obtainStyledAttributes(attrs, R.styleable.View).getDimensionPixelSize(24, ViewConfiguration.get(context).getScaledFadingEdgeLength());
        this.mIsVertical = isVertical;
    }

    public void onAttachedToWindowCallback(RecentsCallback callback, LinearLayout layout, boolean hardwareAccelerated) {
        this.mSoftwareRendered = !hardwareAccelerated;
        if (this.mSoftwareRendered) {
            this.mScrollView.setVerticalFadingEdgeEnabled(false);
            this.mScrollView.setHorizontalFadingEdgeEnabled(false);
        } else {
            this.mScrollView.setVerticalFadingEdgeEnabled(false);
            this.mScrollView.setHorizontalFadingEdgeEnabled(false);
        }
    }

    public void addViewCallback(View newLinearLayoutChild) {
        if (this.mSoftwareRendered) {
            ViewHolder holder = (ViewHolder) newLinearLayoutChild.getTag();
            holder.labelView.setDrawingCacheEnabled(true);
            holder.labelView.buildDrawingCache();
        }
    }

    public void drawCallback(Canvas canvas, int left, int right, int top, int bottom, int scrollX, int scrollY, float topFadingEdgeStrength, float bottomFadingEdgeStrength, float leftFadingEdgeStrength, float rightFadingEdgeStrength) {
        Paint p;
        Matrix matrix;
        Shader fade;
        boolean drawTop;
        boolean drawBottom;
        boolean drawLeft;
        boolean drawRight;
        float topFadeStrength;
        float bottomFadeStrength;
        float leftFadeStrength;
        float rightFadeStrength;
        float fadeHeight;
        int length;
        if (this.mSoftwareRendered) {
            p = new Paint();
            matrix = new Matrix();
            fade = new LinearGradient(0.0f, 0.0f, 0.0f, 1.0f, -872415232, 0, TileMode.CLAMP);
            p.setShader(fade);
            drawTop = false;
            drawBottom = false;
            drawLeft = false;
            drawRight = false;
            topFadeStrength = 0.0f;
            bottomFadeStrength = 0.0f;
            leftFadeStrength = 0.0f;
            rightFadeStrength = 0.0f;
            fadeHeight = (float) this.mFadingEdgeLength;
            length = (int) fadeHeight;
        } else {
            p = new Paint();
            matrix = new Matrix();
            fade = new LinearGradient(0.0f, 0.0f, 0.0f, 1.0f, -872415232, 0, TileMode.CLAMP);
            p.setShader(fade);
            drawTop = false;
            drawBottom = false;
            drawLeft = false;
            drawRight = false;
            topFadeStrength = 0.0f;
            bottomFadeStrength = 0.0f;
            leftFadeStrength = 0.0f;
            rightFadeStrength = 0.0f;
            fadeHeight = (float) this.mFadingEdgeLength;
            length = (int) fadeHeight;
        }
        if (this.mIsVertical && top + length > bottom - length) {
            length = (bottom - top) / 2;
        }
        if (!this.mIsVertical && left + length > right - length) {
            length = (right - left) / 2;
        }
        if (this.mIsVertical) {
            topFadeStrength = Math.max(0.0f, Math.min(1.0f, topFadingEdgeStrength));
            drawTop = topFadeStrength * fadeHeight > 1.0f;
            bottomFadeStrength = Math.max(0.0f, Math.min(1.0f, bottomFadingEdgeStrength));
            drawBottom = bottomFadeStrength * fadeHeight > 1.0f;
        }
        if (!this.mIsVertical) {
            leftFadeStrength = Math.max(0.0f, Math.min(1.0f, leftFadingEdgeStrength));
            drawLeft = leftFadeStrength * fadeHeight > 1.0f;
            rightFadeStrength = Math.max(0.0f, Math.min(1.0f, rightFadingEdgeStrength));
            drawRight = rightFadeStrength * fadeHeight > 1.0f;
        }
        if (drawTop) {
            matrix.setScale(1.0f, fadeHeight * topFadeStrength);
            matrix.postTranslate((float) left, (float) top);
            fade.setLocalMatrix(matrix);
            canvas.drawRect((float) left, (float) top, (float) right, (float) (top + length), p);
        }
        if (drawBottom) {
            matrix.setScale(1.0f, fadeHeight * bottomFadeStrength);
            matrix.postRotate(180.0f);
            matrix.postTranslate((float) left, (float) bottom);
            fade.setLocalMatrix(matrix);
            canvas.drawRect((float) left, (float) (bottom - length), (float) right, (float) bottom, p);
        }
        if (drawLeft) {
            matrix.setScale(1.0f, fadeHeight * leftFadeStrength);
            matrix.postRotate(-90.0f);
            matrix.postTranslate((float) left, (float) top);
            fade.setLocalMatrix(matrix);
            canvas.drawRect((float) left, (float) top, (float) (left + length), (float) bottom, p);
        }
        if (drawRight) {
            matrix.setScale(1.0f, fadeHeight * rightFadeStrength);
            matrix.postRotate(90.0f);
            matrix.postTranslate((float) right, (float) top);
            fade.setLocalMatrix(matrix);
            canvas.drawRect((float) (right - length), (float) top, (float) right, (float) bottom, p);
        }
    }

    public int getVerticalFadingEdgeLengthCallback() {
        return this.mFadingEdgeLength;
    }

    public int getHorizontalFadingEdgeLengthCallback() {
        return this.mFadingEdgeLength;
    }
}
