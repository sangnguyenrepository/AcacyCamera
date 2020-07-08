package vn.com.acacy.cameralibrary.interfaces;

import java.util.HashMap;

public interface ICameraCallback {
    void onPicture(String path, HashMap<String,Object> keys);

    void onError(String message);

    void onCameraDisconnected();

    void onCaptureCompleted();
}
