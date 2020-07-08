package vn.com.acacy.acacycamera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import vn.com.acacy.cameralibrary.YCamera;
import vn.com.acacy.cameralibrary.interfaces.ICameraCallback;

public class CameraActivity extends AppCompatActivity implements ICameraCallback {
    private YCamera camera;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private int requestCode, ratio;
    private static CameraResultListener listener;
    private AlertDialog alert = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        camera = findViewById(R.id.aca_camera);
        camera.enableAutoFocus(true);
        camera.enableSound(false);
        camera.setCameraCallBack(this);
        try {
            requestCode = getIntent().getIntExtra("REQUEST_CODE", 0);
            ratio = getIntent().getIntExtra("RATIO", 43);
            if (requestCode == 0) {
                Toast.makeText(this, "Request code not found", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public static void setCameraListener(CameraResultListener _listener) {
        listener = _listener;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        if (camera != null) {
            camera.startCamera(requestCode, ratio);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            camera.stopCamera();
        }
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
            }
        } else {
            if (camera != null) {
                camera.startCamera(this.requestCode, ratio);
            }
        }
    }

    @Override
    public void onPicture(String path, int requestCode) {
        listener.onCameraResult(path, requestCode);
    }

    @Override
    public void onError(final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Alert("Thông báo", message);
                return;
            }
        });

    }

    @Override
    public void onCameraDisconnected() {
    }

    @Override
    public void onCaptureCompleted() {
    }

    public interface CameraResultListener {
        void onCameraResult(String path, int requestCode);
    }

    public void Alert(String Title, String Message) {
        this.alert = (new AlertDialog.Builder(this)).create();
        this.alert.setCancelable(false);
        this.alert.setTitle(Title);
        this.alert.setMessage(Message);
        this.alert.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                camera.startCamera(requestCode, ratio);
            }
        });
        this.alert.show();
    }

    @Override
    public void onBackPressed() {
        if (!camera.isShowAction()) {
            super.onBackPressed();
        } else {
            return;
        }
    }
}
