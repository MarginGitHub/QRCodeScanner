package com.margin.qrcode.decode.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.margin.qrcode.decode.camera.CameraManager;

import java.util.Collection;
import java.util.Map;

/**
 * Created by margin on 2017/12/2.
 * 二维码扫描管理类
 *
 */

public class QRCodeScannerManager implements SurfaceHolder.Callback {
    private static final String TAG = QRCodeScannerManager.class.getSimpleName();
    private final Context mContext;
    private final OnScannerResult mOnScannerResult;

    private boolean mHasSurface;
    private AmbientLightManager mAmbientLightManager;
    private CameraManager mCameraManager;
    private ScannerHandler mHandler;
    private Collection<BarcodeFormat> mDecodeFormats;
    private Map<DecodeHintType, ?> mDecodeHints;
    private String mCharacterSet;

    private SurfaceView mSurfaceView;
    private ScannerView mScannerView;

    public QRCodeScannerManager(Context context, ScannerView scannerView,
                                SurfaceView surfaceView, OnScannerResult onScannerResult) {
        mContext = context;
        mScannerView = scannerView;
        mSurfaceView = surfaceView;
        mHasSurface = false;
        mAmbientLightManager = new AmbientLightManager(context);
        mOnScannerResult = onScannerResult;
    }

    public void onResume() {
        mCameraManager = new CameraManager(mContext);
        mAmbientLightManager.start(mCameraManager);
        mScannerView.setCameraManager(mCameraManager);

        mDecodeFormats = null;
        mCharacterSet = null;
        if (mHasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(mSurfaceView.getHolder());
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            mSurfaceView.getHolder().addCallback(this);
        }
    }

    public void onPause() {
        if (mHandler != null) {
            mHandler.quitSynchronously();
            mHandler = null;
        }
        mAmbientLightManager.stop();
        mCameraManager.closeDriver();
        if (!mHasSurface) {
            mSurfaceView.getHolder().removeCallback(this);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!mHasSurface) {
            mHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (mCameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            mCameraManager.openDriver(surfaceHolder);
            if (mHandler == null) {
                mHandler = new ScannerHandler(this, mDecodeFormats, mDecodeHints, mCharacterSet, mCameraManager);
            }
        } catch (Exception ioe) {
            Log.w(TAG, ioe);
        }
    }

    public ScannerView getViewfinderView() {
        return mScannerView;
    }

    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        mOnScannerResult.onScannerResult(rawResult.getText());
    }


    public void drawViewfinder() {
        mScannerView.invalidate();
    }

    public Context getContext() {
        return mContext;
    }

    public void restartScanner() {
        if (mHandler != null) {
            mHandler.restartPreviewAndDecode();
        }
    }

    /**
     * 扫描结果回调
     */
    public interface OnScannerResult {
        void onScannerResult(String text);
    }
}
