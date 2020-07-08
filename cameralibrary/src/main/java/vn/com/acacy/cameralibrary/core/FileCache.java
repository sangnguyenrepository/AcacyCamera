package vn.com.acacy.cameralibrary.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class FileCache {
    private final File cacheDir;

    public FileCache(Context context) {
        if (Environment.getExternalStorageState().equals("mounted")) {
            this.cacheDir = new File(Environment.getExternalStorageDirectory(), "Cache");
            if (!this.cacheDir.exists()) {
                this.cacheDir.mkdirs();
            }
        } else {
            this.cacheDir = context.getCacheDir();
        }

        if (!this.cacheDir.exists()) {
            this.cacheDir.mkdirs();
        }

    }

    public File getFile(String url) {
        String filename = String.valueOf(url.hashCode());
        File f = new File(this.cacheDir, filename);
        return f;
    }

    public void clear(String url) {
        String filename = String.valueOf(url.hashCode());
        File f = new File(this.cacheDir, filename);
        if (f.exists()) {
            f.delete();
        }

    }

    public Bitmap getBitmap(String url) {
        try {
            String filename = String.valueOf(url.hashCode());
            File f = new File(this.cacheDir, filename);
            if (!f.exists()) {
                return null;
            }

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), (Rect) null, o);
            boolean REQUIRED_SIZE = true;
            int width_tmp = o.outWidth;
            int height_tmp = o.outHeight;

            int scale;
            for (scale = 1; width_tmp / 2 >= 200 && height_tmp / 2 >= 200; scale *= 2) {
                width_tmp /= 2;
                height_tmp /= 2;
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeFile(f.getPath(), o2);
        } catch (OutOfMemoryError var10) {
            var10.printStackTrace();
        } catch (FileNotFoundException var11) {
            var11.printStackTrace();
        }

        return null;
    }

    public void put(String id, Bitmap bitmap) {
        String filename = String.valueOf(id.hashCode());
        File f = new File(this.cacheDir, filename);

        try {
            FileOutputStream stream = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
        } catch (Exception var6) {
            var6.printStackTrace();
        }

    }

    public void clear() {
        File[] files = this.cacheDir.listFiles();
        if (files != null) {
            File[] var5 = files;
            int var4 = files.length;

            for (int var3 = 0; var3 < var4; ++var3) {
                File f = var5[var3];
                f.delete();
            }

        }
    }
}
