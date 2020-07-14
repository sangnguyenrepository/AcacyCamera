package vn.com.acacy.cameralibrary;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.Serializable;

public class AcaCamera {
    private int requestCode;
    public static final String RATIO = "ration";
    public static final String REQUEST_CODE = "request_code";
    public static final String OPTION = "option";
    private static AcaCamera INSTANCE;
    private Activity activity;
    private Intent _intent;
    private Option option;

    public static synchronized AcaCamera activity(Activity activity) {
        if (INSTANCE == null) {
            INSTANCE = new AcaCamera(activity);
        }
        return (INSTANCE);
    }

    public AcaCamera(Activity activity) {
        this.activity = activity;
        this.option = new Option();
        this._intent = new Intent(activity, CameraActivity.class);
    }

    public AcaCamera destination(@Nullable Uri destUri, int requestCode) {
        this.requestCode = requestCode;
        _intent.putExtra(RATIO, 43);
        _intent.putExtra(REQUEST_CODE, requestCode);
        _intent.setData(destUri);
        return INSTANCE;
    }

    public AcaCamera enableAutoFocus(boolean isFocus) {
        option.isFocus = isFocus;
        return INSTANCE;
    }

    public AcaCamera enableSound(boolean isSound) {
        option.isSound = isSound;
        return INSTANCE;
    }

    public AcaCamera setWidthTarget(int widthTarget) {
        option.widthTarget = widthTarget;
        return INSTANCE;
    }

    public void start() {
        _intent.putExtra(OPTION, (Serializable) option);
        activity.startActivityForResult(_intent, requestCode);
    }


}
