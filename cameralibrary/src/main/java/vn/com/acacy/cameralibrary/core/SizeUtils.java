package vn.com.acacy.cameralibrary.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.Display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressLint("NewApi")
public class SizeUtils {
    public static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                         int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio, int ratioType) {
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            Double value = Double.valueOf(option.getWidth() * h / w);
            if (ratioType == 100 || ratioType == 169) {
                value = (double) Math.round(value * 10) / 10;
                Double result = (double) (option.getHeight()) - value;
                if ((result >= 0 && result <= 2)) {
                    value = Double.valueOf(option.getHeight());
                }
            }
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == value) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }


    public static int getRealWidthScreen(Activity activity) {
        Point size = new Point();
        Display display = activity.getWindowManager().getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= 17)
            display.getRealSize(size);
        else
            display.getSize(size);
        int realWidth = size.x;
        return realWidth;
    }

    public static int getRealHeightScreen(Activity activity) {
        Point size = new Point();
        Display display = activity.getWindowManager().getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= 17)
            display.getRealSize(size);
        else
            display.getSize(size);
        int realHeight = size.y;
        return realHeight;
    }

    public static Size getMaxImageDefault(Activity activity, String cameraId) {
        Size maxSize = null;
        List<Size> sizespDevice = new ArrayList<>();
        Size[] listSize = null;
        Size[] sizesColect = null;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            listSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Size size = Collections.max(
                Arrays.asList(listSize),
                new CompareSizesByArea());
        return size;

    }

    public static boolean supportFullScreen(Activity activity, String cameraId) throws CameraAccessException {
        List<Size> sizeDevice = new ArrayList<>();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        assert map != null;
        Size[] listSize = map.getOutputSizes(SurfaceTexture.class);
        Double ratioWidth = Double.valueOf(getRealHeightScreen(activity));
        Double ratioHeight = Double.valueOf(getRealWidthScreen(activity));
        Double ratioScreen = getNumber(ratioWidth / ratioHeight);

        for (Size size : listSize) {
            Double sizeRatio = getNumber(Double.valueOf(size.getWidth()) / Double.valueOf(size.getHeight()));
            if (Double.compare(sizeRatio, ratioScreen) == 0) {
                sizeDevice.add(size);
            }
        }
        if (sizeDevice.isEmpty()) {
            return false;
        }
        return true;
    }

    public static Size getMaxImage(Activity activity, int rationType, String cameraId, int widthTarget) {
        Size maxSize = null;
        List<Size> sizeDevice = new ArrayList<>();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Size[] listSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            Double ratioWidth = 0.0;
            Double ratioHeight = 0.0;
            switch (rationType) {
                case 43:
                    ratioWidth = Double.valueOf(4);
                    ratioHeight = Double.valueOf(3);
                    break;
                case 169:
                    ratioWidth = Double.valueOf(16);
                    ratioHeight = Double.valueOf(9);
                case 100:
                    ratioWidth = Double.valueOf(getRealHeightScreen(activity));
                    ratioHeight = Double.valueOf(getRealWidthScreen(activity));
                    break;
            }

            Double ratioScreen = getNumber(ratioWidth / ratioHeight);
            for (Size size : listSize) {
                Double sizeRatio = getNumber(Double.valueOf(size.getWidth()) / Double.valueOf(size.getHeight()));
                if (Double.compare(sizeRatio, ratioScreen) == 0) {
                    sizeDevice.add(size);
                }
            }
            if (rationType == 100 && sizeDevice.isEmpty()) {
                ratioScreen = 1.77;
                for (Size size : listSize) {
                    Double sizeRatio = getNumber(Double.valueOf(size.getWidth()) / Double.valueOf(size.getHeight()));
                    if (Double.compare(sizeRatio, ratioScreen) == 0) {
                        sizeDevice.add(size);
                    }
                }
            }
            if (sizeDevice.isEmpty()) {
                sizeDevice.add(getMaxImageDefault(activity, cameraId));
            }
            List<Size> sizesCollect = new ArrayList<>();
            for (Size size : sizeDevice) {
                if (size.getWidth() >= widthTarget) {
                    maxSize = size;
                    break;
                } else {
                    sizesCollect.add(size);
                }
            }
            if (maxSize == null && sizesCollect.size() > 0) {
                maxSize = Collections.max(
                        sizesCollect, new CompareSizesByArea());
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return maxSize;
    }

    public static double checkRatioScreen(Activity activity) {
        Double width = Double.valueOf(getRealWidthScreen(activity));
        Double height = Double.valueOf(getRealHeightScreen(activity));
        return (height / width);
    }

    public static float dpFromPx(final Context context, final float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public static float pxFromDp(final Context context, final float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public static boolean hasNavBar(Context context) {
        Resources resources = context.getResources();
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        return id > 0 && resources.getBoolean(id);
    }

    public static int getHeightNavBar(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static int getStatusBarHeight(Activity activity) {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static Double getNumber(Double number) {
        String text = String.valueOf(number);
        if (text.length() <= 3) {
            text = text.substring(0, 3);
        } else {
            text = text.substring(0, 4);
        }
        number = Double.valueOf(text);
        return number;
    }

    public static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    public static int getNavigationBarHeight(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int usableHeight = metrics.heightPixels;
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            int realHeight = metrics.heightPixels;
            if (realHeight > usableHeight)
                return realHeight - usableHeight;
            else
                return 0;
        }
        return 0;
    }

    public static int getHeightOfScreen(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels + +getNavigationBarHeight(activity);
    }
}
