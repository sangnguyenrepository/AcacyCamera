package vn.com.acacy.acacycamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WelcomeActivity extends Activity {
    private static final int REQUEST_CODE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        if (!isTaskRoot() && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER) && getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {
            finish();
            return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);
        TextView lblVesion = (TextView) findViewById(R.id.txtVersion);

        PackageManager manager = getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(getPackageName(), 0);
            String version = info.versionName;
            lblVesion.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        final List<String> reqPermissions = Arrays.asList(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE );
        final ArrayList<String> permissionsNeeded = getPermissionNeeded(new ArrayList<String>(reqPermissions));
        if (!permissionsNeeded.isEmpty()) {
            requestForPermission(permissionsNeeded.toArray(new String[permissionsNeeded.size()]));
        } else {
            sendResult(true);
        }
    }

    private final Handler handler = new Handler();

    private void startMain() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }, 2000);
    }

    private ArrayList<String> getPermissionNeeded(@NonNull final ArrayList<String> reqPermissions) {
        final ArrayList<String> permissionNeeded = new ArrayList<String>(reqPermissions.size());

        for (String reqPermission : reqPermissions) {
            if (ContextCompat.checkSelfPermission(WelcomeActivity.this,
                    reqPermission) != PackageManager.PERMISSION_GRANTED) {
                permissionNeeded.add(reqPermission);
            }
        }

        return permissionNeeded;
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                final int numOfRequest = grantResults.length;
                boolean isGranted = true;
                for (int i = 0; i < numOfRequest; i++) {
                    if (PackageManager.PERMISSION_GRANTED != grantResults[i]) {
                        isGranted = false;
                        break;
                    }
                }
                sendResult(isGranted);
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void requestForPermission(final String[] permissions) {
        ActivityCompat.requestPermissions(WelcomeActivity.this, permissions, REQUEST_CODE);
    }

    private void sendResult(final boolean isPermissionGranted) {
        if (isPermissionGranted) {
            startMain();
        } else {
            return;
        }
    }


}
