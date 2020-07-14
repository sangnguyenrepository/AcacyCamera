package vn.com.acacy.cameralibrary;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import vn.com.acacy.cameralibrary.core.DateTime;
import vn.com.acacy.cameralibrary.core.ImageUtils;
import vn.com.acacy.cameralibrary.core.KEYs;
import vn.com.acacy.cameralibrary.core.SizeUtils;
import vn.com.acacy.cameralibrary.core.Utility;
import vn.com.acacy.cameralibrary.interfaces.ICameraCallback;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends AppCompatActivity implements ICameraCallback, View.OnClickListener {
    private YCamera camera;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private AlertDialog alert = null;
    private int ration;
    private Uri uri;
    private int requestCode;
    private Option option;
    private RelativeLayout layout_option, layout_take_photo, layout_confirm;
    private ImageView img_capture, img_switch, img_flash, img_mode;
    private FrameLayout layout_action;
    private TouchImageView img_temp;
    private TextView txtCancel, txtOk;
    private CircleImageView img_thumb;
    private OrientationListener orientationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        camera = findViewById(R.id.aca_camera);
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
        orientationListener = new OrientationListener(this);
        try {
            Intent intent = getIntent();
            ration = intent.getIntExtra(AcaCamera.RATIO, 100);
            requestCode = intent.getIntExtra(AcaCamera.REQUEST_CODE, 0);
            option = (Option) intent.getSerializableExtra(AcaCamera.OPTION);
            uri = intent.getData();
            camera.setCameraCallBack(this);
            camera.setRatio(ration);
            camera.setWidthTarget(option.widthTarget);
            camera.enableAutoFocus(option.isFocus);
        } catch (Exception ex) {
            Alert("Notify", ex.getMessage());
            return;
        }
    }

    private void configureLayout() {
        if (camera != null && camera.isBackCameraAvailable()) {
            img_switch.setVisibility(VISIBLE);
        }
        RelativeLayout.LayoutParams paramsR = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        FrameLayout.LayoutParams paramsF = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        int barSize = SizeUtils.getStatusBarHeight(this);
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
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    @Override
    public void onError(final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Alert("Notify", message);
                return;
            }
        });
    }

    @Override
    public void onCameraDisconnected() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Alert("Notify", "Camera is disconnected");
                return;
            }
        });
    }

    @Override
    public void onCaptureCompleted() {
        if (camera != null) {
            camera.preview();
        }
    }

    @Override
    public void onCaptureFailed() {
        img_capture.setEnabled(true);
    }

    @Override
    public void onCaptureStart() {
        img_capture.setEnabled(false);
    }

    @Override
    public void onSaveImageComplete(String path) {
        final Bitmap source = ImageUtils.decodeSampledBitmapFromPath(path, 500, 500);
        Matrix matrix = new Matrix();
        int width = source.getWidth();
        int height = source.getHeight();

        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                if (width > height) {
                    matrix.postRotate(90);
                }
            }
            final Bitmap bitmap = Bitmap.createBitmap(source, 0, 0, width, height, matrix, false);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    img_capture.setClickable(true);
                    if (bitmap != null) {
                        layout_option.setVisibility(GONE);
                        layout_take_photo.setVisibility(GONE);
                        layout_action.setVisibility(VISIBLE);
                        img_temp.setImageBitmap(bitmap);
                    }
                }
            });

        } catch (IOException ex) {
            Alert("Notify", ex.getMessage());
            return;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null) {
            camera.close();
            camera.stop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            camera.close();
            camera.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera != null) {
            configureLayout();
            camera.start(ration, uri, requestCode);
        }
    }

    @Override
    public void onTextureAvailable(int width, int height) {
        if (camera != null) {
            configureLayout();
            camera.start(ration, uri, requestCode);
        }
    }

    @Override
    public void onBackPressed() {
        if (!camera.getAction()) {
            super.onBackPressed();
        } else {
            return;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.img_takephoto) {
            long freeRam = Utility.getFreeRam(this);
            long freeStorage = Utility.getFreeSpace();
            if (freeRam < 200) {
                Alert("Thông báo", "Ram trống đang thấp hơn 200 Mb, không thể tiếp tục chụp hình");
                return;
            } else if (freeStorage < 200) {
                Alert("Thông báo", "Dung lượng trống đang thấp hơn 200 Mb, không thể tiếp tục chụp hình");
                return;
            }
            if (camera != null) {
                camera.capture();
            }
        } else if (view.getId() == R.id.img_switch) {
            if (camera != null) {
                if (camera.getCameraId().equals(YCamera.CAMERA_FRONT)) {
                    camera.setCameraId(YCamera.CAMERA_BACK);
                } else {
                    camera.setCameraId(YCamera.CAMERA_FRONT);
                }
                try {
                    camera.reopen();
                } catch (Exception ex) {
                    Alert("Notify", ex.getMessage());
                    return;
                }
            }
        } else if (view.getId() == R.id.img_flash) {
            if (camera != null) {
                try {
                    if (camera.isFlashSupported()) {
                        camera.switchFlash();
                    }
                } catch (CameraAccessException ex) {
                    Alert("Notify", ex.getMessage());
                    return;
                }
            }
        } else if (view.getId() == R.id.txt_cancel) {
            if (camera != null) {
                camera.clearTemp();
                img_capture.setEnabled(true);
                camera.setAction(false);
                layout_action.setVisibility(GONE);
                layout_option.setVisibility(VISIBLE);
                layout_take_photo.setVisibility(VISIBLE);
                camera.preview();
            }
        } else if (view.getId() == R.id.txt_ok) {
            long freeRam = Utility.getFreeRam(this);
            long freeStorage = Utility.getFreeSpace();
            if (freeRam < 200) {
                Alert("Thông báo", "Ram trống đang thấp hơn 200 Mb, không thể tiếp tục chụp hình");
                return;
            } else if (freeStorage < 200) {
                Alert("Thông báo", "Dung lượng trống đang thấp hơn 200 Mb, không thể tiếp tục chụp hình");
                return;
            }
            SharedPreferences share = getSharedPreferences(KEYs.KEY, MODE_PRIVATE);
            long timeStart = share.getLong(KEYs.TIME_START, 0);
            if (timeStart > 0 && (DateTime.getTime() - timeStart) > 10) {
                camera.clearTemp();
                Alert("Thông báo", "Bạn đã để camera quá 10 phút, vui lòng tắt và mở lại để chụp hình");
                return;
            }
            if (camera != null) {
                camera.finishCapture();
            }
        } else if (view.getId() == R.id.img_mode) {
            if (camera != null) {
                camera.changeMode();
            }
        }
    }

    @Override
    protected void onStart() {
        orientationListener.enable();
        super.onStart();
    }

    @Override
    protected void onStop() {
        orientationListener.disable();
        super.onStop();
    }


    public void Alert(String Title, String Message) {
        this.alert = (new AlertDialog.Builder(this)).create();
        this.alert.setCancelable(false);
        this.alert.setTitle(Title);
        this.alert.setMessage(Message);
        this.alert.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        this.alert.show();
    }

    public void Confirm(String Title, String Message, DialogInterface.OnClickListener yesButton) {
        this.alert = (new AlertDialog.Builder(this)).create();
        this.alert.setCancelable(false);
        this.alert.setTitle(Title);
        this.alert.setMessage(Message);
        this.alert.setButton("YES", yesButton);
        this.alert.setButton2("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        this.alert.show();
    }

    private class OrientationListener extends OrientationEventListener {
        public static final int ORIENTATION_PORTRAIT = 0;
        public static final int ORIENTATION_LANDSCAPE = 1;
        public static final int ORIENTATION_PORTRAIT_REVERSE = 2;
        public static final int ORIENTATION_LANDSCAPE_REVERSE = 3;
        private int rotation = 0;
        private int lastOrientation = 0;

        public OrientationListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(final int orientation) {
            if (orientation < 0) {
                return;
            }
            int curOrientation;
            if (orientation <= 45) {
                curOrientation = ORIENTATION_PORTRAIT;
            } else if (orientation <= 135) {
                curOrientation = ORIENTATION_LANDSCAPE_REVERSE;
            } else if (orientation <= 225) {
                curOrientation = ORIENTATION_PORTRAIT_REVERSE;
            } else if (orientation <= 315) {
                curOrientation = ORIENTATION_LANDSCAPE;
            } else {
                curOrientation = ORIENTATION_PORTRAIT;
            }
            if (curOrientation != lastOrientation) {
                lastOrientation = curOrientation;
                if (camera != null) {
                    camera.setOrientations(lastOrientation);
                }
            }

        }
    }
}
