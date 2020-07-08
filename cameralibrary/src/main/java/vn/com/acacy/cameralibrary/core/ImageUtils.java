package vn.com.acacy.cameralibrary.core;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("NewApi")
public class ImageUtils {
    private static class SingletonHelper {
        private static final ImageUtils INSTANCE = new ImageUtils();
    }

    public static ImageUtils getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public synchronized void saveImage(final File file, ImageReader reader, String cameraId) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            final byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
//            final byte[] bytes = NV21toJPEG(YUV420toNV21(image), image.getWidth(), image.getHeight(), 100);
            OutputStream output = null;
            try {
                output = new FileOutputStream(file);
                output.write(bytes);
                output.flush();
                output.close();
                saveBitmapFile(file, cameraId);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            if (image != null) {
                image.close();
            }
        }

    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), quality, out);
        return out.toByteArray();
    }

    private static byte[] YUV420toNV21(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
                    break;
            }

            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();

            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    public void saveBitmapFile(File file, String cameraId) throws IOException {
        try {
            Bitmap bitmap = MakeSquare(readBytesFromFile(file.getAbsolutePath()), Integer.parseInt(cameraId));
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static byte[] readBytesFromFile(String filePath) {
        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;
        try {
            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bytesArray;
    }

    public boolean isImageFile(String filePath) {
        if (filePath.endsWith(".jpg") || filePath.endsWith(".png")) {
            return true;
        }
        return false;
    }

    public static Bitmap MakeSquare(byte[] data, int cameraID) {
        int width;
        int height;
        Matrix matrix = new Matrix();
        Camera.CameraInfo info = new Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraID, info);
        Bitmap bitPic = BitmapFactory.decodeByteArray(data, 0, data.length);
        width = bitPic.getWidth();
        height = bitPic.getHeight();
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
            Matrix matrixMirrorY = new Matrix();
            matrixMirrorY.setValues(mirrorY);
            matrix.postConcat(matrixMirrorY);
        }
        matrix.postRotate(90);
        Bitmap bitPicFinal = Bitmap.createBitmap(bitPic, 0, 0, width, height, matrix, true);
        bitPic.recycle();
        return bitPicFinal;
    }

    public List<String> getlistImage(String root) {
        List<String> dest = new ArrayList<>();
        List<File> list = new ArrayList<>();
        File directory = new File(root);
        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                if (isImageFile(files[i].getAbsolutePath())) {
                    list.add(files[i]);
                }
            }
        }
        if (list != null) {
            dest.addAll(sortFile(list));
            return dest;
        } else {
            return null;
        }
    }

    public List<String> sortFile(final List<File> fileList) {
        if (fileList.size() > 1) {
            for (int i = 0; i < fileList.size(); i++) {
                int pos = i;
                for (int j = i; j < fileList.size(); j++) {
                    if (Integer.parseInt(getTimeImage(fileList.get(j).getName())) > Integer.parseInt(getTimeImage(fileList.get(pos).getName())))
                        pos = j;
                }
                File max = fileList.get(pos);
                fileList.set(pos, fileList.get(i));
                fileList.set(i, max);
            }
        }

        List<String> dest = new ArrayList<>();
        for (int i = 0; i < fileList.size(); i++) {
            dest.add(fileList.get(i).getAbsolutePath());
        }
        return dest;
    }

    public String getTimeImage(String fileName) {
        for (int i = 0; i < fileName.length(); i++) {
            if (Character.isDigit(fileName.charAt(i))) {
                fileName = fileName.substring(i, fileName.indexOf("."));
                break;
            }
        }
        return fileName;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromPath(String path,
                                                     int reqWidth, int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }
}
