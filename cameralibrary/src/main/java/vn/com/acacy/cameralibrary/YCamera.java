package vn.com.acacy.cameralibrary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import vn.com.acacy.cameralibrary.core.ImageUtils;
import vn.com.acacy.cameralibrary.core.KEYs;
import vn.com.acacy.cameralibrary.core.OnTouchHandler;
import vn.com.acacy.cameralibrary.core.SizeUtils;
import vn.com.acacy.cameralibrary.core.StringUtils;
import vn.com.acacy.cameralibrary.interfaces.ICameraCallback;

@SuppressLint("NewApi,MissingPermission")
public class YCamera extends RelativeLayout implements View.OnClickListener, TextureView.SurfaceTextureListener {
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
    private int ratioType = 43;
    private String cameraId = CAMERA_BACK;
    private AutoFitTextureView textureView;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size sizePreview;
    private boolean isFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private ImageView img_takephoto, img_switch, img_flash, img_mode;
    private boolean autoFocus = true;
    private boolean enableSound = false;
    private int flashMode = 0;
    private CircleImageView img_thumNail;
    private MediaPlayer mediaPlayer;
    private TextView txt_cancel, txt_ok;
    private ICameraCallback callback;
    private TouchImageView img_temp;
    private int requestCode;
    private String path = "";
    private FrameLayout layout_action;
    private Activity activity;
    private Bitmap bitmap = null;
    private RelativeLayout layout_option, layout_take_photo, layout_confirm;
    private boolean isShowAction = false;

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
        inflate(activity, R.layout.camera_layout, this);
        textureView = findViewById(R.id.texture);
        assert textureView != null;
        img_takephoto = (ImageView) findViewById(R.id.img_takephoto);
        img_switch = findViewById(R.id.img_switch);
        img_flash = findViewById(R.id.img_flash);
        img_thumNail = findViewById(R.id.img_thumNail);
        img_mode = findViewById(R.id.img_mode);
        layout_action = findViewById(R.id.layout_action);
        layout_option = findViewById(R.id.layout_option);
        layout_confirm = findViewById(R.id.layout_confirm);
        layout_take_photo = findViewById(R.id.layout_take_photo);
        img_temp = findViewById(R.id.img_temp);
        txt_ok = findViewById(R.id.txt_ok);
        txt_cancel = findViewById(R.id.txt_cancel);
        txt_ok.setOnClickListener(this);
        txt_cancel.setOnClickListener(this);
        img_takephoto.setOnClickListener(this);
        img_thumNail.setOnClickListener(this);
        img_switch.setOnClickListener(this);
        img_flash.setOnClickListener(this);
        img_mode.setOnClickListener(this);
        textureView.setSurfaceTextureListener(YCamera.this);
    }

    public void setCameraCallBack(ICameraCallback iCameraCallback) {
        this.callback = iCameraCallback;
    }

    private void configureLayout() {
        if (checkBackCamera()) {
            img_switch.setVisibility(View.VISIBLE);
        }
        RelativeLayout.LayoutParams paramsR = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        FrameLayout.LayoutParams paramsF = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        int barSize = SizeUtils.getStatusBarHeight(activity);
        if (barSize == 0) {
            paramsR.setMargins(0, 20, 0, 0);
            paramsF.setMargins(0, 20, 0, 0);
        } else {
            paramsR.setMargins(0, barSize + 10, 0, 0);
            paramsF.setMargins(0, barSize + 10, 0, 0);
        }

        layout_confirm.setLayoutParams(paramsF);
        layout_option.setLayoutParams(paramsR);
        layout_action.setVisibility(GONE);
        layout_option.setVisibility(VISIBLE);
        layout_take_photo.setVisibility(VISIBLE);
        textureView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int extant = SizeUtils.getRealHeightScreen(activity) - textureView.getMeasuredHeight();
                if (extant > 0) {
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT
                    );
                    params.gravity = Gravity.CENTER;
                    textureView.setLayoutParams(params);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.img_takephoto) {
            takePicture();
        } else if (id == R.id.img_switch) {
            if (cameraId.equals(CAMERA_FRONT)) {
                try {
                    cameraId = CAMERA_BACK;
                    closeCamera();
                    reopenCamera();
                    Toast.makeText(activity, "Camera back on.", Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    if (callback != null) {
                        callback.onError(ex.getMessage());
                    }
                    return;
                }
            } else if (cameraId.equals(CAMERA_BACK)) {
                try {
                    cameraId = CAMERA_FRONT;
                    closeCamera();
                    reopenCamera();
                    Toast.makeText(activity, "Camera front on.", Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    if (callback != null) {
                        callback.onError(ex.getMessage());
                    }
                    return;
                }
            }
        } else if (id == R.id.img_flash) {
            try {
                if (isFlashSupported()) {
                    switchFlash();
                }
            } catch (CameraAccessException ex) {
                if (callback != null) {
                    callback.onError(ex.getMessage());
                }
                return;
            }
        } else if (id == R.id.txt_cancel) {
            try {
                if (!StringUtils.IsNullOrWhiteSpace(path)) {
                    File file = new File(path);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                if (callback != null) {
                    callback.onCaptureCompleted();
                }
                isShowAction = false;
                layout_action.setVisibility(GONE);
                layout_option.setVisibility(VISIBLE);
                layout_take_photo.setVisibility(VISIBLE);
                createCameraPreview(autoFocus);
            } catch (Exception ex) {
                if (callback != null) {
                    callback.onError(ex.getMessage());
                }
                return;
            }
        } else if (id == R.id.txt_ok) {
            try {
                if (callback != null) {
                    callback.onCaptureCompleted();
                    File file = new File(path);
                    if (file.exists()) {
                        callback.onPicture(path, requestCode);
                    }
                }
                isShowAction = false;
                layout_action.setVisibility(GONE);
                layout_option.setVisibility(VISIBLE);
                layout_take_photo.setVisibility(VISIBLE);
                createCameraPreview(autoFocus);
            } catch (Exception ex) {
                if (callback != null) {
                    callback.onError(ex.getMessage());
                }
                return;
            }
        } else if (id == R.id.img_mode) {
            try {
                if (ratioType == 43) {
                    if (SizeUtils.supportFullScreen(activity, cameraId)) {
                        ratioType = 100;
                    } else {
                        ratioType = 169;
                    }
                } else {
                    ratioType = 43;
                }
                Intent intent = activity.getIntent();
                intent.putExtra("RATIO", ratioType);
                activity.finish();
                activity.startActivity(intent);
            } catch (Exception ex) {
                if (callback != null) {
                    callback.onError(ex.getMessage());
                }
                return;
            }

        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        try {
            openCamera(width, height, ratioType);
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

    private void openCamera(int width, int height, int ratioType) {
        configureLayout();
        configureTransform(width, height);
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            int deviceOrientation = activity.getWindowManager().getDefaultDisplay().getRotation();
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

            Size largest = SizeUtils.getMaxImage(activity, ratioType, cameraId);
            sizePreview = SizeUtils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, largest, ratioType);
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(
                        sizePreview.getWidth(), sizePreview.getHeight(), ratioType);
            } else {
                textureView.setAspectRatio(
                        sizePreview.getHeight(), sizePreview.getWidth(), ratioType);
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview(autoFocus);
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

    protected void createCameraPreview(final boolean autoFocus) {
        try {
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            final SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(sizePreview.getWidth(), sizePreview.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == cameraDevice) {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview(autoFocus);
                    if (!autoFocus) {
                        textureView.setOnTouchListener(new OnTouchHandler(characteristics, captureRequestBuilder, cameraCaptureSessions, mBackgroundHandler));
                    } else {
                        textureView.setOnTouchListener(null);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (callback != null) {
                        callback.onError("Has an error, please try it again !");
                        return;
                    }
                }
            }, null);
        } catch (CameraAccessException ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    protected void updatePreview(boolean autoFocus) {
        try {
            if (autoFocus) {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            } else {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            }
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    protected void startBackgroundThread() {
        try {
            mBackgroundThread = new HandlerThread("Camera Background");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        try {
            if (null == sizePreview || null == activity) {
                return;
            }
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

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrentation) {
        int sensorOrentation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrentation = ORIENTATIONS.get(deviceOrentation);
        return (sensorOrentation + deviceOrentation + 360) % 360;
    }

    public void closeCamera() {
        try {
            if (null != cameraCaptureSessions) {
                cameraCaptureSessions.close();
                cameraCaptureSessions = null;
            }
            if (null != cameraDevice) {
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

    public void startCamera(int requestCode, int ratio) {
        try {
            this.ratioType = ratio;
            img_takephoto.setClickable(true);
            this.requestCode = requestCode;
            startBackgroundThread();
            if (textureView.isAvailable()) {
                openCamera(textureView.getWidth(), textureView.getHeight(), ratioType);
            } else {
                textureView.setSurfaceTextureListener(this);
            }
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    public void stopCamera() {
        try {
            closeCamera();
            stopBackgroundThread();
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }

    }

    private void switchFlash() {
        if (cameraId.equals(CAMERA_BACK)) {
            if (isFlashSupported) {
                try {
                    switch (flashMode) {
                        case KEYs.FLASH_MODE.ON:
                            flashMode = KEYs.FLASH_MODE.AUTO;
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                            Toast.makeText(activity, "Flash auto.", Toast.LENGTH_SHORT).show();
                            break;
                        case KEYs.FLASH_MODE.OFF:
                            flashMode = KEYs.FLASH_MODE.ON;
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                            Toast.makeText(activity, "Flash on.", Toast.LENGTH_SHORT).show();
                            break;
                        case KEYs.FLASH_MODE.AUTO:
                            flashMode = KEYs.FLASH_MODE.OFF;
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                            Toast.makeText(activity, "Flash off.", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                } catch (Exception ex) {
                    if (callback != null) {
                        callback.onError(ex.getMessage());
                    }
                    return;
                }

            }
        }
    }

    private boolean isFlashSupported() throws CameraAccessException {
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        isFlashSupported = available == null ? false : available;
        return isFlashSupported;
    }

    public void reopenCamera() {
        try {
            if (textureView.isAvailable()) {
                openCamera(textureView.getWidth(), textureView.getHeight(), ratioType);
            } else {
                textureView.setSurfaceTextureListener(this);
            }
        } catch (Exception ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
            }
            return;
        }
    }

    private boolean checkBackCamera() {
        String backCameraId = null;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
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

    private void takePicture() {
        try {
            img_takephoto.setClickable(false);
            if (enableSound) {
                mediaPlayer = MediaPlayer.create(getContext(), R.raw.camera_take_picture);
                mediaPlayer.start();
            }
            ImageReader reader = ImageReader.newInstance(sizePreview.getWidth(), sizePreview.getHeight(), ImageFormat.JPEG, 2);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO);
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            switch (mTotalRotation) {
                case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                    captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, INVERSE_ORIENTATIONS.get(rotation));
                    break;
                case SENSOR_ORIENTATION_INVERSE_DEGREES:
                    captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
                    break;
            }
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(final ImageReader reader) {
                    Runnable saveImages = new SaveImage(reader, cameraId);
                    new Thread(saveImages).start();
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Runnable displayImage = new DisplayImage();
                    new Thread(displayImage).start();
                }
                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    img_takephoto.setClickable(true);
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
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException ex) {
                        if (callback != null) {
                            callback.onError(ex.getMessage());
                            return;
                        }
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    stopBackgroundThread();
                }

                @Override
                public void onClosed(CameraCaptureSession session) {
                    super.onClosed(session);
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                    }
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
                return;
            }
        }
    }

    private class DisplayImage implements Runnable {
        @Override
        public void run() {
            try {
                Bitmap desct = ImageUtils.decodeSampledBitmapFromPath(path, 500, 500);
                while (desct == null) {
                    desct = ImageUtils.decodeSampledBitmapFromPath(path, 500, 500);
                }
                if (cameraId.equals(CAMERA_BACK)) {
                    if (desct.getWidth() < desct.getHeight()) {
                        Matrix matrix = new Matrix();
                        bitmap = Bitmap.createBitmap(desct, 0, 0,
                                desct.getWidth(), desct.getHeight(), matrix, true);
                    } else {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        bitmap = Bitmap.createBitmap(desct, 0, 0,
                                desct.getWidth(), desct.getHeight(), matrix, true);
                    }
                } else {
                    if (desct.getWidth() < desct.getHeight()) {
                        Matrix matrix = new Matrix();
                        matrix.setScale(-1, 1);
                        bitmap = Bitmap.createBitmap(desct, 0, 0,
                                desct.getWidth(), desct.getHeight(), matrix, true);
                    } else {
                        Matrix matrix = new Matrix();
                        matrix.setScale(-1, 1);
                        matrix.postRotate(90);
                        bitmap = Bitmap.createBitmap(desct, 0, 0,
                                desct.getWidth(), desct.getHeight(), matrix, true);
                    }
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        img_takephoto.setClickable(true);
                        if (bitmap != null) {
                            layout_option.setVisibility(GONE);
                            layout_take_photo.setVisibility(GONE);
                            layout_action.setVisibility(VISIBLE);
                            isShowAction = true;
                            img_temp.setImageBitmap(bitmap);
                        }
                    }
                });
            } catch (Exception ex) {
                if (callback != null) {
                    callback.onError(ex.getMessage());
                }
            }

        }
    }

    public void enableAutoFocus(boolean autoFocus) {
        this.autoFocus = autoFocus;
    }

    public void enableSound(boolean enableSound) {
        this.enableSound = enableSound;
    }

    public boolean isShowAction() {
        return isShowAction;
    }

    private class SaveImage implements Runnable {
        private ImageReader reader;
        private String cameraId;

        public SaveImage(ImageReader reader, String cameraId) {
            this.reader = reader;
            this.cameraId = cameraId;
        }

        @Override
        public void run() {
            try {
                File root = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    root = new File(activity.getExternalFilesDir(null), "/IMAGE_ACACY");
                    if (!root.exists()) {
                        root.mkdirs();
                    }
                } else {
                    root = new File(Environment.getExternalStorageDirectory(), "/IMAGE_ACACY");
                    if (!root.exists()) {
                        root.mkdirs();
                    }
                }
                if (root.exists()) {
                    final File dsFile = new File(root.getAbsolutePath() + "/IMAGE_TEMP_ACACY" + ".jpg");
                    path = dsFile.getAbsolutePath();
                    if (dsFile.exists()) {
                        dsFile.delete();
                        dsFile.createNewFile();
                        if (dsFile.exists()) {
                            ImageUtils.getInstance().saveImage(dsFile, reader, cameraId);
                        }
                    } else {
                        ImageUtils.getInstance().saveImage(dsFile, reader, cameraId);
                    }
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
