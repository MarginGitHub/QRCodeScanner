/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.margin.qrcode.decode.scanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.margin.qrcode.decode.camera.CameraManager;
import com.margin.qrcode.decode.cons.DecodeCons;

import java.util.Collection;
import java.util.Map;

/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ScannerHandler extends Handler {

    private static final String TAG = ScannerHandler.class.getSimpleName();

    private final QRCodeScannerManager mQRCodeScannerManager;
    private final DecodeThread decodeThread;
    private State state;
    private final CameraManager cameraManager;

    private enum State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    ScannerHandler(QRCodeScannerManager scanner,
                   Collection<BarcodeFormat> decodeFormats,
                   Map<DecodeHintType, ?> baseHints,
                   String characterSet,
                   CameraManager cameraManager) {
        mQRCodeScannerManager = scanner;
        decodeThread = new DecodeThread(scanner, decodeFormats, baseHints, characterSet,
                new ScannerResultPointCallback(scanner.getViewfinderView()));
        decodeThread.start();
        state = State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        this.cameraManager = cameraManager;
        cameraManager.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case DecodeCons.Status.RESTART_PREVIEW:
                restartPreviewAndDecode();
                break;
            case DecodeCons.Status.SUCCESS:
                state = State.SUCCESS;
                Bundle bundle = message.getData();
                Bitmap barcode = null;
                float scaleFactor = 1.0f;
                if (bundle != null) {
                    byte[] compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
                    if (compressedBitmap != null) {
                        barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
                        // Mutable copy:
                        barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
                    }
                    scaleFactor = bundle.getFloat(DecodeThread.BARCODE_SCALED_FACTOR);
                }
                mQRCodeScannerManager.handleDecode((Result) message.obj, barcode, scaleFactor);
                break;
            case DecodeCons.Status.FAIL:
                // We're decoding as fast as possible, so when one decode fails, start another.
                state = State.PREVIEW;
                cameraManager.requestPreviewFrame(decodeThread.getHandler(), DecodeCons.Type.DECODE);
                break;
        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        cameraManager.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), DecodeCons.Type.QUIT);
        quit.sendToTarget();
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(DecodeCons.Status.SUCCESS);
        removeMessages(DecodeCons.Status.FAIL);
    }

    /**
     * 重新开始扫描
     */
     public void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), DecodeCons.Type.DECODE);
            mQRCodeScannerManager.drawViewfinder();
        }
    }

}
