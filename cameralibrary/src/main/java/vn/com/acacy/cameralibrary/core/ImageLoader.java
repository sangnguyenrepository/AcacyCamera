package vn.com.acacy.cameralibrary.core;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.com.acacy.cameralibrary.R;

public class ImageLoader {
    FileCache fileCache;
    private Map<ImageView, String> imageViews = Collections.synchronizedMap(new WeakHashMap());
    ExecutorService executorService;
    final int stub_id;
    int no_image;

    public ImageLoader(Context context, int drawableLoading, int drawableNoImage) {
        this.fileCache = new FileCache(context);
        this.executorService = Executors.newFixedThreadPool(5);
        this.stub_id = drawableLoading;
        this.no_image = drawableNoImage;
    }

    public void DisplayImage(String url, ImageView imageView) {
        this.imageViews.put(imageView, url);
        Bitmap bitmap = this.fileCache.getBitmap(url);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            this.queuePhoto(url, imageView);
            imageView.setImageResource(this.stub_id);
        }

    }

    public ImageLoader(Context context) {
        this.fileCache = new FileCache(context);
        this.executorService = Executors.newFixedThreadPool(5);
        this.stub_id = R.drawable.logo;
    }

    public void DisplayImageRotate(String url, ImageView imageView) {
        this.imageViews.put(imageView, url);
        Bitmap bitmap = this.fileCache.getBitmap(url);
        int widthImage = bitmap.getWidth();
        int heightImage = bitmap.getHeight();
        if (widthImage < heightImage) {
            Matrix matrix = new Matrix();
            bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    widthImage, heightImage, matrix, true);
        } else {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    widthImage, heightImage, matrix, true);
        }
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            this.queuePhoto(url, imageView);
            imageView.setImageResource(this.stub_id);
        }

    }

    public void DisplayImageFromFileRotate(String url, ImageView imageView) {
        this.imageViews.put(imageView, url);
        Bitmap bitmap = BitmapFactory.decodeFile(url);
        int widthImage = bitmap.getWidth();
        int heightImage = bitmap.getHeight();
        if (widthImage < heightImage) {
            Matrix matrix = new Matrix();
            bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    widthImage, heightImage, matrix, true);
        } else {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    widthImage, heightImage, matrix, true);
        }
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            this.queuePhoto(url, imageView);
            imageView.setImageResource(this.stub_id);
        }

    }

    public void clear(String url) {
        this.fileCache.clear(url);
    }

    private void queuePhoto(String url, ImageView imageView) {
        ImageLoader.PhotoToLoad p = new ImageLoader.PhotoToLoad(url, imageView);
        this.executorService.submit(new ImageLoader.PhotosLoader(p));
    }

    public Bitmap getBitmapFull(String url) {
        File f = this.fileCache.getFile(url);
        Bitmap b = this.decodeFile(f);
        if (b != null) {
            return b;
        } else {
            try {
                Bitmap bitmap = null;
                URL imageUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                InputStream is = conn.getInputStream();
                OutputStream os = new FileOutputStream(f);
                CopyStream(is, os);
                os.close();
                bitmap = BitmapFactory.decodeStream(new FileInputStream(f));
                return bitmap;
            } catch (Exception var9) {
                var9.printStackTrace();
                return null;
            }
        }
    }

    public File getFile(String url) {
        File f = this.fileCache.getFile(url);
        if (f != null && f.exists()) {
            return f;
        } else {
            try {
                URL imageUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                InputStream is = conn.getInputStream();
                OutputStream os = new FileOutputStream(f);
                CopyStream(is, os);
                os.close();
                return f;
            } catch (Exception var7) {
                var7.printStackTrace();
                return null;
            }
        }
    }

    public Bitmap getBitmap(String url) {
        File f = this.fileCache.getFile(url);
        Bitmap b = this.decodeFile(f);
        if (b != null) {
            return b;
        } else if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(new FileInputStream(url), (Rect) null, o);
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
                Bitmap bitmap = BitmapFactory.decodeFile(url, o2);
                OutputStream os = new FileOutputStream(f);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.close();
                bitmap.recycle();
                bitmap = this.decodeFile(f);
                return bitmap;
            } catch (Exception var13) {
                var13.printStackTrace();
                return null;
            }
        } else {
            try {
                Bitmap bitmap = null;
                URL imageUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                InputStream is = conn.getInputStream();
                OutputStream os = new FileOutputStream(f);
                CopyStream(is, os);
                os.close();
                bitmap = this.decodeFile(f);
                return bitmap;
            } catch (Exception var12) {
                var12.printStackTrace();
                return null;
            }
        }
    }

    private Bitmap decodeFile(File f) {
        try {
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
        } catch (FileNotFoundException var8) {
            return null;
        }
    }

    boolean imageViewReused(ImageLoader.PhotoToLoad photoToLoad) {
        String tag = (String) this.imageViews.get(photoToLoad.imageView);
        return tag == null || !tag.equals(photoToLoad.url);
    }

    public void clearCache() {
        this.fileCache.clear();
    }

    class BitmapDisplayer implements Runnable {
        Bitmap bitmap;
        ImageLoader.PhotoToLoad photoToLoad;

        public BitmapDisplayer(Bitmap b, ImageLoader.PhotoToLoad p) {
            this.bitmap = b;
            this.photoToLoad = p;
        }

        public void run() {
            if (!ImageLoader.this.imageViewReused(this.photoToLoad)) {
                if (this.bitmap != null) {
                    this.photoToLoad.imageView.setImageBitmap(this.bitmap);
                } else {
                    this.photoToLoad.imageView.setImageResource(ImageLoader.this.no_image);
                }

            }
        }
    }

    private class PhotoToLoad {
        public String url;
        public ImageView imageView;

        public PhotoToLoad(String u, ImageView i) {
            this.url = u;
            this.imageView = i;
        }
    }

    class PhotosLoader implements Runnable {
        ImageLoader.PhotoToLoad photoToLoad;

        PhotosLoader(ImageLoader.PhotoToLoad photoToLoad) {
            this.photoToLoad = photoToLoad;
        }

        public void run() {
            if (!ImageLoader.this.imageViewReused(this.photoToLoad)) {
                Bitmap bmp = ImageLoader.this.getBitmap(this.photoToLoad.url);
                ImageLoader.this.fileCache.put(this.photoToLoad.url, bmp);
                if (!ImageLoader.this.imageViewReused(this.photoToLoad)) {
                    ImageLoader.BitmapDisplayer bd = ImageLoader.this.new BitmapDisplayer(bmp, this.photoToLoad);
                    Activity a = (Activity) this.photoToLoad.imageView.getContext();
                    a.runOnUiThread(bd);
                }
            }
        }
    }

    public static void CopyStream(InputStream is, OutputStream os) {
        final int buffer_size = 1024;
        try {
            byte[] bytes = new byte[buffer_size];
            for (; ; ) {
                int count = is.read(bytes, 0, buffer_size);
                if (count == -1)
                    break;
                os.write(bytes, 0, count);
            }
        } catch (Exception ex) {
        }
    }


}
