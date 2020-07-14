package vn.com.acacy.acacycamera;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class CameraUtils {
    public static File moveFile(File filemove) {

        InputStream in = null;
        OutputStream out = null;
        try {
            // create output directory if it doesn't exist
            File dir = new File(filemove.getParent(), "NoResize");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            dir = new File(dir, filemove.getName());
            out = new FileOutputStream(dir);
            in = new FileInputStream(filemove.getPath());
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            // write the output file
            out.flush();
            out.close();
            out = null;
            // delete the original file
            filemove.delete();
            return dir;
        } catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
        return filemove;
    }

    @SuppressLint("CommitPrefEdits")
    public static void ClearTemp(String Folder) {
        File f = new File(Folder);
        if (f.isDirectory())
            for (File temp : f.listFiles()) {
                temp.delete();
            }
    }

    public static File saveBitmapToFile(String ImagePath) {
        try {
            File file = new File(ImagePath);
            // BitmapFactory options to downsize the image
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inSampleSize = 6;
            // factor of downsizing the image
            FileInputStream inputStream = new FileInputStream(file);
            // Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();
            // The new size we want to scale to
            final int REQUIRED_SIZE = 75;
            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            inputStream = new FileInputStream(file);
            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2);
            inputStream.close();
            // here i override the original image file
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);
            selectedBitmap.compress(CompressFormat.JPEG, 100, outputStream);

            return file;
        } catch (Exception e) {
            return null;
        }
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

    public static String Resize(final File fileResize, final int With) {
        // TODO Auto-generated method stub

        String FilePath = null;
        Bitmap imageSource = null;
        try {
            while (imageSource == null)
                imageSource = BitmapFactory.decodeFile(fileResize.getPath());
            File file = new File(fileResize.getParent(), "Resize");
            if (!file.exists()) {
                file.mkdirs();
            }
            file = new File(file, "RS_" + fileResize.getName());
            double xFactor = 0;
            double width = Double.valueOf(imageSource.getWidth());
            Log.v("WIDTH", String.valueOf(width));
            double height = Double.valueOf(imageSource.getHeight());
            Log.v("height", String.valueOf(height));
            if (width > height) {
                xFactor = With / width;
            } else {
                xFactor = With / height;
            }
            int Nheight = (int) ((xFactor * height));
            int NWidth = (int) (xFactor * width);
            Bitmap bm = Bitmap.createScaledBitmap(imageSource, NWidth, Nheight, true);
            // Rotation
            ExifInterface exif = new ExifInterface(fileResize.getPath());
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationInDegrees = exifToDegrees(rotation);
            Matrix matrix = new Matrix();
            if (rotation != 0f) {
                matrix.preRotate(rotationInDegrees);
            }
            Bitmap adjustedBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

            file.createNewFile();
            FileOutputStream ostream = null;
            try {
                ostream = new FileOutputStream(file);
                adjustedBitmap.compress(CompressFormat.JPEG, 100, ostream);
                ostream.close();
                FilePath = file.getPath();
                if (FilePath == null)
                    FilePath = moveFile(fileResize).getPath();
                ClearTemp(fileResize.getParent());
            } catch (Exception ex) {
                throw ex;
            } finally {
                ostream.close();
                adjustedBitmap.recycle();
                bm.recycle();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return FilePath;
    }

}
