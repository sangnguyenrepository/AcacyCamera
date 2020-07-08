package vn.com.acacy.cameralibrary.interfaces;

public interface ICameraCallback {
    void onPicture(String path,int requestCode);

    void onError(String message);

    void onCameraDisconnected();

    void onCaptureCompleted();
}
