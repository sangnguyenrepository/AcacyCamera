package vn.com.acacy.cameralibrary;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
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
import java.util.HashMap;
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
    private int ratio = 43;
    private String cameraId = CAMERA_BACK;
    private AutoFitTextureView textureView;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession captureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size sizePreview;
    private boolean isFlashSupported;
    private Handler handler;
    private HandlerThread thread;
    private ImageView img_capture, img_switch, img_flash, img_mode;
    private boolean isFocus = true;
    private boolean isSound = false;
    private int flashMode = 0;
    private CircleImageView img_thumb;
    private MediaPlayer media;
    private TextView txtCancel, txtOk;
    private ICameraCallback callback;
    private TouchImageView img_temp;
    private HashMap<String, Object> data;
    private String path = "";
    private FrameLayout layout_action;
    private Activity activity;
    private Bitmap bitmap = null;
    private RelativeLayout layout_option, layout_take_photo, layout_confirm;
    private boolean isShowAction = false;
    private CameraManager manager;
    private CameraCharacteristics characteristics;
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String DATA = "DATA";
    public Thread handleImageThread;

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
            img_capture = (ImageView) findViewById(R.id.img_takephoto);
            img_switch = findViewById(R.id.img_switch);
            img_flash = findViewById(R.id.img_flash);
            img_thumb = findViewById(R.id.img_thumNail);
            img_mode = findViewById(R.id.img_mode);
            layout_action = findViewById(R.id.layout_action);
            layout_option = findViewById(R.id.layout_option);
            layout_confirm = findViewById(R.id.layout_confirm);
            layout_take_photo = findViewById(R.id.layout_take_photo);
            img_temp = findViewById(R.id.img_temp);
            txtOk = findViewById(R.id.txt_ok);
            txtCancel = findViewById(R.id.txt_cancel);
            txtOk.setOnClickListener(this);
            txtCancel.setOnClickListener(this);
            img_capture.setOnClickListener(this);
            img_thumb.setOnClickListener(this);
            img_switch.setOnClickListener(this);
            img_flash.setOnClickListener(this);
            img_mode.setOnClickListener(this);
            textureView.setSurfaceTextureListener(YCamera.this);
            manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            characteristics = manager.getCameraCharacteristics(cameraId);
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

    private void configureLayout() {
        if (isBackCameraAvailable()) {
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
            capture();
        } else if (id == R.id.img_switch) {
            if (cameraId.equals(CAMERA_FRONT)) {
                cameraId = CAMERA_BACK;
                Toast.makeText(activity, "Camera back on.", Toast.LENGTH_SHORT).show();
            } else if (cameraId.equals(CAMERA_BACK)) {
                cameraId = CAMERA_FRONT;
                Toast.makeText(activity, "Camera front on.", Toast.LENGTH_SHORT).show();
            }
            try {
                close();
                open(textureView.getWidth(), textureView.getHeight(), ratio);
            } catch (Exception ex) {
                if (callback != null) {
                    callback.onError(ex.getMessage());
                }
                return;
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
                preview();
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
                        callback.onPicture(path, data);
                    }
                }
                isShowAction = false;
                layout_action.setVisibility(GONE);
                layout_option.setVisibility(VISIBLE);
                layout_take_photo.setVisibility(VISIBLE);
                preview();
            } catch (Exception ex) {
                if (callback != null) {
                    callback.onError(ex.getMessage());
                }
                return;
            }
        } else if (id == R.id.img_mode) {
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
                close();
                Intent intent = activity.getIntent();
                intent.putExtra("RATIO", ratio);
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
            open(width, height, ratio);
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
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) { }

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

    public void startCamera(String path, int ratio, HashMap<String, Object> data) {
        try {
            this.path = path;
            this.ratio = ratio;
            img_capture.setClickable(true);
            this.data = data;
            startThread();
            open(textureView.getWidth(), textureView.getHeight(), this.ratio);
        } catch (Exception ex) {
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

    private void open(int width, int height, int ratio) {
        if (textureView.isAvailable()) {
            configureLayout();
            configureTransform(width, height);
            try {
                characteristics = manager.getCameraCharacteristics(cameraId);
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

                Size largest = SizeUtils.getMaxImage(activity, ratio, cameraId);
                sizePreview = SizeUtils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, largest, ratio);
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

    private void preview() {
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
                            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
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

    private void close() {
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

    public void stop() {
        stopThread();
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

    private void capture() {
        try {
            img_capture.setClickable(false);
            if (isSound) {
                media = MediaPlayer.create(getContext(), R.raw.camera_take_picture);
                media.start();
            }
            Size maxImage = SizeUtils.getMaxImage(activity, ratio, cameraId);
            if (maxImage != null) {
                if (maxImage.getWidth() >= 2048) {
                    if (ratio == 43) {
                        maxImage = new Size(2048, 1536);
                    } else {
                        maxImage = new Size(2048, 1152);
                    }
                }

            }
            ImageReader reader = ImageReader.newInstance(maxImage.getWidth(), maxImage.getHeight(), ImageFormat.JPEG, 2);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_TRIGGER_START);
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
                    HandelImage handelImage = new HandelImage(reader, cameraId);
                    handleImageThread = new Thread(handelImage);
                    handleImageThread.start();
                }
            };
            reader.setOnImageAvailableListener(readerListener, handler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    img_capture.setClickable(true);
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
            }, handler);

        } catch (CameraAccessException ex) {
            if (callback != null) {
                callback.onError(ex.getMessage());
                return;
            }
        }
    }

    public void isAutoFocus(boolean isFocus) {
        this.isFocus = isFocus;
    }

    public void isSound(boolean isSound) {
        this.isSound = isSound;
    }

    private boolean isFlashSupported() throws CameraAccessException {
        CameraManager manager = (CameraManager) activity.getSystemService(activity.CAMERA_SERVICE);
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


    private boolean isBackCameraAvailable() {
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

    private class HandelImage implements Runnable {
        private ImageReader reader;
        private String cameraId;

        public HandelImage(ImageReader reader, String cameraId) {
            this.reader = reader;
            this.cameraId = cameraId;
        }

        @Override
        public void run() {
            try {
                final File dsFile = new File(path);
                if (dsFile.exists()) {
                    dsFile.delete();
                    dsFile.createNewFile();
                }
                if (ImageUtils.saveImage(dsFile, reader, cameraId)) {
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
                                img_capture.setClickable(true);
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
