package vn.com.acacy.acacycamera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.HashMap;

import vn.com.acacy.cameralibrary.YCamera;
import vn.com.acacy.cameralibrary.interfaces.ICameraCallback;

public class CameraActivity extends AppCompatActivity implements ICameraCallback {
    private YCamera camera;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private HashMap<String, Object> data;
    private int ratio;
    private static CameraResultListener listener;
    private AlertDialog alert = null;
    private String path = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        camera = findViewById(R.id.aca_camera);
        camera.isAutoFocus(true);
        camera.isSound(false);
        camera.setWidthTarget(0);
        camera.setResize(1024);
        camera.setCameraCallBack(this);
        path = this.getExternalFilesDir(null) + "/IMAGE_TEMP_ACACY" + ".jpg";
        try {
            data = (HashMap<String, Object>) getIntent().getSerializableExtra("DATA");
            ratio = getIntent().getIntExtra("RATIO", 43);
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
            camera.startCamera(path, ratio, data);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
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
                camera.startCamera(path, ratio, this.data);
            }
        }
    }

    @Override
    public void onPicture(String path, HashMap<String, Object> data) {
        listener.onCameraResult(path, data);
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
        void onCameraResult(String path, HashMap<String, Object> data);
    }

    public void Alert(String Title, String Message) {
        this.alert = (new AlertDialog.Builder(this)).create();
        this.alert.setCancelable(false);
        this.alert.setTitle(Title);
        this.alert.setMessage(Message);
        this.alert.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                camera.startCamera(path, ratio, data);
            }
        });
        this.alert.show();
    }

    @Override
    public void onBackPressed() {
        if (!camera.getAction()) {
            super.onBackPressed();
        } else {
            return;
        }
    }
}
