package com.margin.qrcode.decode.scanner;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.margin.qrcode.R;
import com.margin.qrcode.decode.camera.CameraManager;

/**
 * Created by margin on 2017/12/2.
 * 页面展示包括扫描界面
 */

public class ScannerView extends View {
    private CameraManager mCameraManager;
    private Paint mMarkPaint;
    private Paint mCornerPaint;
    private Paint mDrawLinePaint;
    private int mCornerColor = Color.BLUE;
    private float mCornerHeight = 10;
    private float mCornerWidth = 100;
    private int mMarkColor = Color.parseColor("#66000000");
    private float mTopOffset = 0;
    private Rect mAreaRect;
    private long mStartTime = 0;
    private boolean mCancelScannerAnimation = false;

    public ScannerView(Context context) {
        super(context);
        init();
    }

    public ScannerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }


    public ScannerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init() {
        mMarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMarkPaint.setStyle(Paint.Style.FILL);
        mMarkPaint.setColor(mMarkColor);

        mCornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCornerPaint.setColor(mCornerColor);
        mCornerPaint.setStyle(Paint.Style.STROKE);
        mCornerPaint.setStrokeWidth(mCornerHeight);
        mCornerPaint.setStrokeJoin(Paint.Join.ROUND);

        mDrawLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDrawLinePaint.setColor(mCornerColor);
        mDrawLinePaint.setStyle(Paint.Style.STROKE);
        mDrawLinePaint.setStrokeWidth(4f);
    }


    private void init(Context context, AttributeSet attrs) {
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ScannerView);
        mMarkColor = attributes.getColor(R.styleable.ScannerView_markColor, Color.parseColor("#66000000"));
        mCornerColor = attributes.getColor(R.styleable.ScannerView_cornerColor, Color.BLUE);
        mCornerHeight = attributes.getDimension(R.styleable.ScannerView_cornerHeight, 10);
        mCornerWidth = attributes.getDimension(R.styleable.ScannerView_cornerHeight, 100);
        mTopOffset = attributes.getDimension(R.styleable.ScannerView_topOffset, 0);
        attributes.recycle();
        init();
    }

    public void setCameraManager(CameraManager cameraManager) {
        mCameraManager = cameraManager;
        mCameraManager.setTopOffset((int) mTopOffset);
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mCameraManager == null) {
            return;
        }
        if (mAreaRect == null) {
            mAreaRect = getAreaRect();
        }
        drawArea(canvas);
        drawCorners(canvas);
        if (!mCancelScannerAnimation) {
            drawScannerLine(canvas);
        }
    }


    private void drawArea(Canvas canvas) {
        canvas.drawRect(0, 0, getMeasuredWidth(), mAreaRect.top, mMarkPaint);
        canvas.drawRect(0, mAreaRect.top, mAreaRect.left, mAreaRect.bottom, mMarkPaint);
        canvas.drawRect(mAreaRect.right, mAreaRect.top, getMeasuredWidth(), mAreaRect.bottom, mMarkPaint);
        canvas.drawRect(0, mAreaRect.bottom, getMeasuredWidth(), getMeasuredHeight(), mMarkPaint);
    }

    private void drawCorners(Canvas canvas) {
        Path corners = new Path();
        corners.moveTo(mAreaRect.left, mAreaRect.top + mCornerWidth);
        corners.rLineTo(0, -mCornerWidth);
        corners.rLineTo(mCornerWidth, 0);
        corners.moveTo(mAreaRect.right - mCornerWidth, mAreaRect.top);
        corners.rLineTo(mCornerWidth, 0);
        corners.rLineTo(0, mCornerWidth);
        corners.moveTo(mAreaRect.right, mAreaRect.bottom - mCornerWidth);
        corners.rLineTo(0, mCornerWidth);
        corners.rLineTo(-mCornerWidth, 0);
        corners.moveTo(mAreaRect.left + mCornerWidth, mAreaRect.bottom);
        corners.rLineTo(-mCornerWidth, 0);
        corners.rLineTo(0, -mCornerWidth);
        canvas.drawPath(corners, mCornerPaint);
    }

    private void drawScannerLine(Canvas canvas) {
        if (mCancelScannerAnimation) {
            return;
        }
        if (mStartTime == 0) {
            mStartTime = System.currentTimeMillis();
        }
        int offset = (int) (mAreaRect.height() * ((System.currentTimeMillis() - mStartTime) % 3000L) / 3000);
        canvas.drawLine(mAreaRect.left, mAreaRect.top + offset, mAreaRect.right,
                mAreaRect.top + offset, mDrawLinePaint);
        invalidate();
    }

    private Rect getAreaRect() {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int maxSize = Math.min(width, height);
        Rect rect = new Rect();
        rect.left = maxSize / 5;
        rect.right = maxSize - (maxSize / 5);
        rect.top = (height - (3 * maxSize / 5)) >> 1;
        rect.bottom = (height + (3 * maxSize / 5)) >> 1;
        return rect;
    }

    public void cancelScannerAnimation() {
        mCancelScannerAnimation = true;
    }

    public void startScannerAnimation() {
        mCancelScannerAnimation = false;
        mStartTime = 0;
        invalidate();
    }
}
