package vn.com.acacy.cameralibrary;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vn.com.acacy.cameralibrary.core.DateTime;
import vn.com.acacy.cameralibrary.core.ImageUtils;
import vn.com.acacy.cameralibrary.core.KEYs;
import vn.com.acacy.cameralibrary.core.OnTouchHandler;
import vn.com.acacy.cameralibrary.core.SizeUtils;
import vn.com.acacy.cameralibrary.core.StringUtils;
import vn.com.acacy.cameralibrary.interfaces.ICameraCallback;

import static android.content.Context.MODE_PRIVATE;

@SuppressLint("NewApi,MissingPermission")
public class YCamera extends RelativeLayout implements TextureView.SurfaceTextureListener {
    private int mTotalRotation;
    public static final String CAMERA_FRONT = "1";
    public static final String CAMERA_BACK = "0";
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 180;
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 0;

    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
    }

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
    }

    private static final int MAX_PREVIEW_WIDTH = 3264;
    private static final int MAX_PREVIEW_HEIGHT = 2448;
    private int ratio = 100;
    private String cameraId = CAMERA_BACK;
    private AutoFitTextureView textureView;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession captureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size sizePreview;
    private boolean isFlashSupported;
    private Handler handler;
    private HandlerThread thread;
    private boolean isFocus = true;
    private boolean isSound = false;
    private int flashMode = 0;
    private MediaPlayer media;
    private ICameraCallback callback;
    private Activity activity;
    private boolean isShowAction = false;
    private CameraManager manager;
    private CameraCharacteristics characteristics;
    private Thread handleImageThread;
    private int widthTarget = 1024;
    private Uri uri;
    private File dsFile = null;
    private int requestCode;
    private Size maxImage;
    private int deviceOrientation;


    public YCamera(Context context) {
        super(context);
        this.activity = (Activity) context;
        init();
    }

    public YCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.activity = (Activity) context;
        init();
    }

    public YCamera(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.activity = (Activity) context;
        init();
    }

    private void init() {
        try {
            inflate(activity, R.layout.camera_layout, this);
            textureView = findViewById(R.id.texture);
            assert textureView != null;
            textureView.setSurfaceTextureListener(YCamera.this);
            manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            characteristics = manager.getCameraCharacteristics(cameraId);
            deviceOrientation = 0;
        } catch (CameraAccessException ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    public void setCameraCallBack(ICameraCallback iCameraCallback) {
        this.callback = iCameraCallback;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public void reopen() {
        close();
        open(textureView.getWidth(), textureView.getHeight(), ratio);
    }

    public void finishCapture() {
        try {
            if (dsFile.exists()) {
                Intent intent = new Intent();
                intent.setData(Uri.fromFile(new File(dsFile.getPath())));
                activity.setResult(requestCode, intent);
                activity.finish();
            } else {
                if (callback != null) {
                    callback.onError("Save file error");
                }
                return;
            }
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }

    }

    public void changeMode() {
        try {
            if (ratio == 43) {
                if (SizeUtils.supportFullScreen(activity, cameraId)) {
                    ratio = 100;
                } else {
                    ratio = 169;
                }
            } else {
                ratio = 43;
            }
            reopen();
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        try {
            if (callback != null) {
                callback.onTextureAvailable(width, height);
            }
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            preview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
            if (callback != null) {
                callback.onCameraDisconnected();
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            String message = "";
            cameraDevice.close();
            cameraDevice = null;
            switch (error) {
                case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                    message = "Camera device has encountered a fatal error.";
                    break;
                case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                    message = "Camera device could not be opened due to a device policy.";
                    break;
                case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                    message = "Camera device is in use already.";
                    break;
                case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                    message = "Camera service has encountered a fatal error.";
                    break;
                case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                    message = "Camera device could not be opened because there are too many other open camera devices.";
                    break;
            }
            if (!StringUtils.IsNullOrWhiteSpace(message)) {
                if (callback != null) {
                    callback.onError(message);
                }
                return;
            }
        }
    };

    protected void stopThread() {
        if (thread == null) {
            return;
        }
        thread.quitSafely();
        try {
            thread.join();
            thread = null;
            handler = null;
        } catch (InterruptedException ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    protected void startThread() {
        try {
            thread = new HandlerThread("Camera Background");
            thread.start();
            handler = new Handler(thread.getLooper());
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        try {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, sizePreview.getHeight(), sizePreview.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max(
                        (float) viewHeight / sizePreview.getHeight(),
                        (float) viewWidth / sizePreview.getWidth());
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            }
            textureView.setTransform(matrix);
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    public void open(int width, int height, int ratio) {
        if (textureView.isAvailable()) {
            try {
                SharedPreferences.Editor editor = activity.getSharedPreferences(KEYs.KEY, MODE_PRIVATE).edit();
                editor.putLong(KEYs.TIME_START, DateTime.getTime());
                editor.apply();

                characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map != null;
                mTotalRotation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swapRotation = false;
                switch (deviceOrientation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mTotalRotation == 90 || mTotalRotation == 270) {
                            swapRotation = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mTotalRotation == 0 || mTotalRotation == 180) {
                            swapRotation = true;
                        }
                        break;
                }
                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getRealSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;
                if (swapRotation) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                maxImage = SizeUtils.getMaxImage(activity, ratio, cameraId, widthTarget);
                sizePreview = SizeUtils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, maxImage, ratio);
                configureTransform(width, height);
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(
                            sizePreview.getWidth(), sizePreview.getHeight(), ratio);
                } else {
                    textureView.setAspectRatio(
                            sizePreview.getHeight(), sizePreview.getWidth(), ratio);
                }
                manager.openCamera(cameraId, stateCallback, handler);
            } catch (CameraAccessException ex) {
                if (callback != null) {
                    callback.onError(ex.getMessage());
                }
                return;
            }
        } else {
            textureView.setSurfaceTextureListener(this);
        }

    }


    public void preview() {
        try {
            characteristics = manager.getCameraCharacteristics(cameraId);
            final SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(sizePreview.getWidth(), sizePreview.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        captureSessions = cameraCaptureSession;
                        if (isFocus && isAutoFocusSupported()) {
                            textureView.setOnTouchListener(null);
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                        } else {
                            textureView.setOnTouchListener(new OnTouchHandler(characteristics, captureRequestBuilder, cameraCaptureSession, handler));
                        }
                        captureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, handler);
                    } catch (CameraAccessException ex) {
                        if (callback != null) {
                            callback.onError(ex.getMessage());
                        }
                        return;
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (callback != null) {
                        callback.onError("Has an error, please try it again !");
                        return;
                    }
                }
            }, handler);
        } catch (CameraAccessException ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    public void close() {
        try {
            if (captureSessions != null) {
                captureSessions.close();
                captureSessions = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    public void switchFlash() {
        if (cameraId.equals(CAMERA_BACK)) {
            if (isFlashSupported) {
                try {
                    switch (flashMode) {
                        case KEYs.FLASH_MODE.ON:
                            flashMode = KEYs.FLASH_MODE.AUTO;
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(activity, "Flash auto.", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case KEYs.FLASH_MODE.OFF:
                            flashMode = KEYs.FLASH_MODE.ON;
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(activity, "Flash on.", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case KEYs.FLASH_MODE.AUTO:
                            flashMode = KEYs.FLASH_MODE.OFF;
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(activity, "Flash off.", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                    }
                    captureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, handler);
                } catch (Exception ex) {
                    if (callback != null) {
                        callback.onError(ex.getMessage());
                    }
                    return;
                }

            }
        }
    }

    public void enableAutoFocus(boolean isFocus) {
        this.isFocus = isFocus;
    }

    public void enableSound(boolean isSound) {
        this.isSound = isSound;
    }

    public void setWidthTarget(int widthTarget) {
        this.widthTarget = widthTarget;
    }

    public void setRatio(int ratio) {
        this.ratio = ratio;
    }

    private int getOrientation(int rotation) {
        return (ORIENTATIONS.get(rotation) + mTotalRotation + 270) % 360;
    }

    private int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    public void setOrientations(int orientations) {
        this.deviceOrientation = orientations;
    }

    public void clearTemp() {
        if (dsFile != null && dsFile.exists()) {
            dsFile.delete();
        }
    }


    public void capture() {
        try {
            if (callback != null) {
                callback.onCaptureStart();
            }
            if (this.isSound) {
                media = MediaPlayer.create(getContext(), R.raw.camera_take_picture);
                media.start();
            }
            maxImage = SizeUtils.getMaxImage(activity, ratio, cameraId, widthTarget);
            ImageReader reader = ImageReader.newInstance(maxImage.getWidth(), maxImage.getHeight(), ImageFormat.JPEG, 2);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_TRIGGER_START);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(deviceOrientation));
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(final ImageReader reader) {
                    HandelImage handelImage = new HandelImage(reader);
                    handleImageThread = new Thread(handelImage);
                    handleImageThread.start();
                }
            };
            reader.setOnImageAvailableListener(readerListener, handler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    if (callback != null) {
                        callback.onCaptureCompleted();
                    }
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    if (callback != null) {
                        callback.onCaptureFailed();
                    }
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onReady(@NonNull CameraCaptureSession session) {
                    super.onReady(session);
                }

                @Override
                public void onConfigured(final CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, handler);
                    } catch (CameraAccessException ex) {
                        if (callback != null) {
                            callback.onError(ex.getMessage());
                            return;
                        }
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    stopThread();
                }

                @Override
                public void onClosed(CameraCaptureSession session) {
                    super.onClosed(session);
                    if (media != null) {
                        media.stop();
                    }
                }
            }, null);

        } catch (CameraAccessException ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
                return;
            }
        }
    }

    public void stop() {
        stopThread();
    }

    public void start(int ratio, Uri uri, int requestCode) {
        try {
            this.ratio = ratio;
            this.uri = uri;
            this.requestCode = requestCode;
            if (uri != null) {
                dsFile = new File(this.uri.getPath());
            }
            startThread();
            open(textureView.getWidth(), textureView.getHeight(), this.ratio);
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    public boolean isFlashSupported() throws CameraAccessException {
        manager = (CameraManager) activity.getSystemService(activity.CAMERA_SERVICE);
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        isFlashSupported = available == null ? false : available;
        return isFlashSupported;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean isHardwareLevelSupported(int requiredLevel) {
        boolean result = false;
        if (cameraDevice == null)
            return result;
        try {
            manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            characteristics = manager.getCameraCharacteristics(cameraId);

            int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                result = requiredLevel == deviceLevel;
            } else {
                result = requiredLevel <= deviceLevel;
            }

        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
        }
        return result;
    }

    private boolean isAutoFocusSupported() {
        boolean isSupport = isHardwareLevelSupported(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) || getMinimumFocusDistance() > 0;
        return isSupport;
    }

    private float getMinimumFocusDistance() {
        if (cameraId == null)
            return 0;
        Float minimumLens = null;
        try {
            manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics c = manager.getCameraCharacteristics(cameraId);
            minimumLens = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
        }
        if (minimumLens != null)
            return minimumLens;
        return 0;
    }


    public boolean isBackCameraAvailable() {
        String backCameraId = null;
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == CameraMetadata.LENS_FACING_BACK) {
                    backCameraId = cameraId;
                    break;
                }
            }
        } catch (CameraAccessException ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
                return false;
            }
        }
        if (backCameraId != null) {
            return true;
        } else {
            return false;
        }
    }

    public boolean getAction() {
        return isShowAction;
    }

    public void setAction(boolean action) {
        this.isShowAction = action;
    }

    private class HandelImage implements Runnable {
        private ImageReader reader;

        public HandelImage(ImageReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            try {
                if (dsFile == null || !dsFile.exists()) {
                    if (uri != null) {
                        dsFile = new File(uri.getPath());
                    } else {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onError("Không thể lưu hình, hãy tắt và mở lại camera");
                                    return;
                                }
                            }
                        });
                    }

                }
                boolean isFinish = ImageUtils.saveImage(dsFile, reader);
                if (isFinish) {
                    callback.onSaveImageComplete(dsFile.getPath());
                }
            } catch (final Exception ex) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onError(ex.getMessage());
                            return;
                        }
                    }
                });

            }
        }
    }

}
