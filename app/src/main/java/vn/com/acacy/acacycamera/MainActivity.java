package vn.com.acacy.acacycamera;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

import vn.com.acacy.cameralibrary.YCamera;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, CameraActivity.class);
        HashMap<String, Object> data = new HashMap<>();
        data.put(YCamera.REQUEST_CODE, 500);
        intent.putExtra(YCamera.DATA, data);
        startActivity(intent);
        CameraActivity.setCameraListener(listener);
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

    CameraActivity.CameraResultListener listener = new CameraActivity.CameraResultListener() {
        @Override
        public void onCameraResult(String path, HashMap<String, Object> data) {
            Toast.makeText(MainActivity.this, path, Toast.LENGTH_SHORT).show();
        }
    };

}
