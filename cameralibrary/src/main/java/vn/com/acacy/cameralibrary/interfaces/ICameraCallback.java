package vn.com.acacy.cameralibrary.interfaces;

public interface ICameraCallback {
    void onError(String message);

    void onCameraDisconnected();

    void onCaptureCompleted();

    void onCaptureFailed();

    void onCaptureStart();

    void onSaveImageComplete(String path);

    void onTextureAvailable(int width,int height);
}
