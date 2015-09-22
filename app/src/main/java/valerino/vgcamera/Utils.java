package valerino.vgcamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.glass.widget.CardBuilder;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * generic utilities
 * Created by valerino on 13/09/15.
 */
public class Utils {
    /**
     * attempt to close a closeable object handling exception eventually
     * @param c a Closeable (OutputStream, ...)
     */
    public static void closeNoEx(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                // swallow
            }
        }
    }

    /**
     * creates a new file out of a buffer
     * @param buf the data to be written
     * @param path path to the file to be created (will be overwritten)
     * @return a File (pointing at path), or null on error
     */
    public static File bufferToFile (byte[] buf, final String path) {
        FileOutputStream fos = null;
        File f = new File (path);
        try {
            fos = new FileOutputStream(f);
            fos.write(buf);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            f.delete();
            return null;
        }
        finally {
            closeNoEx(fos);
        }
        return f;
    }

    /**
     * read input stream to a byte array
     * @param inputStream the input stream
     * @param maxRead max bytes to be read, 0 to read all input stream
     * @return byte[]
     * @throws IOException
     */
    public static byte[] inputStreamToByteArray (InputStream inputStream, int maxRead) throws IOException {
        byte[] buf = new byte[8192];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int totRead = 0;
        while (true) {
            int read = 0;
            try {
                read = inputStream.read(buf);
            }
            finally {
                bos.close();
            }
            if (read == -1) {
                break;
            }
            if (maxRead == 0) {
                // write all
                bos.write(buf,0,read);
            }
            else {
                if (totRead < maxRead) {
                    // write a full chunk
                    bos.write(buf,0,read);
                }
                else {
                    // last chunk
                    bos.write(buf,0,maxRead - totRead);
                    bos.close();
                    break;
                }
            }
            totRead += read;
        }
        return bos.toByteArray();
    }

    /**
     * read a file to buffer
     * @param f file to be read
     * @return a byte array or null
     */
    public static byte[] fileToBuffer(File f) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            return null;
        }
        byte[] buf = null;
        try {
            buf = inputStreamToByteArray(fis, 0);
        } catch (IOException e) {
            return null;
        }
        finally {
            closeNoEx(fis);
        }
        return buf;
    }

    /**
     * creates a temporary file out of a buffer
     * @param buf the data to be written
     * @return a File (should be deleted after usage), or null on error
     */
    public static File bufferToTempFile(byte[] buf) {
        File tmpFile;
        try {
            tmpFile = File.createTempFile(null,null);
        } catch (IOException e) {
            return null;
        }
        File f = bufferToFile(buf, tmpFile.getAbsolutePath());
        return f;
    }

    /**
     * create a private app file out of a buffer (uses openFileOutput())
     * @param ctx a Context
     * @param buf the data to be written
     * @param filename name of the file
     * @return File or null
     */
    public static File bufferToPrivateFile(Context ctx, byte[] buf, final String filename) {
        // this is the file we will have as output
        File f = new File (ctx.getFilesDir(), filename);

        // create the private file
        FileOutputStream fos = null;
        try {
            fos = ctx.openFileOutput(filename, 0);
        } catch (FileNotFoundException e) {
            return null;
        }

        // and write to it
        try {
            fos.write(buf);
        } catch (IOException e) {
            f.delete();
            return null;
        }
        finally {
            closeNoEx(fos);
        }

        // done
        return f;
    }

    /**
     * create and set a card to the given activity
     * @param activity an Activity
     * @param layout the CardBuilder layout to be used
     * @param text optional text
     * @param img optional image id
     */
    public static void setViewCard(Activity activity, CardBuilder.Layout layout, String text, int img) {
        CardBuilder card = new CardBuilder(activity, CardBuilder.Layout.ALERT).setText(text).addImage(img);
        activity.setContentView(card.getView());
    }

    /**
     * inject a d-pad event
     * @param keyCode the keycode
     * @return true if successful
     */
    public static boolean injectKeyEvent(int keyCode) {
        try {
            java.lang.Runtime.getRuntime().exec("input keyevent " + Integer.toString(keyCode) + "\n");
            return true;
        } catch (IOException e) {

        }
        return false;
    }

    /**
     * get a properly named File in the temporary folder
     *
     * @param ctx a Context
     * @param mode one of the CamController.CAM_MODE
     * @return
     */
    public static File getTempMediaFile(Context ctx, CamController.CAM_MODE mode) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        File f = new File(AppConfiguration.instance(ctx).tmpFolder(), timeStamp +
                (mode == CamController.CAM_MODE.MODE_PHOTO ? ".jpg" : ".mp4"));
        return f;
    }

    /**
     * play a sound
     * @param ctx a Context
     * @param sound sound id
     */
    public static void playSound (Context ctx, int sound) {
        AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
        am.playSoundEffect(sound);
    }

    /**
     * create a thumbnail for the given file
     * @param file input file (.jpg or .mp4)
     * @parm dest destination file for the thumbnail
     * @return File, or null
     */
    public static File generateThumbnail (File file, File dest) {
        Bitmap bmp = fileToThumbnail(file);
        if (bmp == null) {
            return null;
        }

        // save bitmap to file
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos) == false) {
            Log.e(Utils.class.getName(), "compress() failed, file=" + file.getAbsolutePath());
            closeNoEx(bos);
            dest.delete();
            return null;
        }
        closeNoEx(bos);
        File f = bufferToFile(bos.toByteArray(), dest.getAbsolutePath());
        if (f == null) {
            Log.e(Utils.class.getName(), "bufferToFile() returned null, dest=" + dest.getAbsolutePath() + ", file=" + file.getAbsolutePath());
        }
        return f;
    }

    /**
     * generate a thumbnail Bitmap for the given file
     * @param file input file (.jpg or .mp4)
     * @return Bitmap, or null
     */
    public static Bitmap fileToThumbnail (File file) {
        Bitmap bmp;
        if (file.getAbsolutePath().endsWith(".mp4")) {
            // generate a video thumbnail
            bmp = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
            if (bmp == null) {
                Log.e(Utils.class.getName(), "createVideoThumbnail() failed, file=" + file.getAbsolutePath());
                return null;
            }
            bmp = Bitmap.createScaledBitmap(bmp, 640, 360, false);
            if (bmp == null) {
                Log.e(Utils.class.getName(), "createScaledBitmap() failed, file=" + file.getAbsolutePath());
                return null;
            }
        } else {
            // we have an image file
            bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), null);
            if (bmp == null) {
                Log.e(Utils.class.getName(), "decodeFile() failed, file=" + file.getAbsolutePath());
                return null;
            }
            bmp = ThumbnailUtils.extractThumbnail(bmp, 640, 360);
            if (bmp == null) {
                Log.e(Utils.class.getName(), "extractThumbnail() failed, file=" + file.getAbsolutePath());
                return null;
            }
        }
        return bmp;
    }

    /**
     * shows a thumbnail of the given media file in the given ImageView
     * @param file path to the media file
     * @param dest the destination ImageView
     * @return 0 on success
     */
    public static int setMediaThumbnail(File file, ImageView dest) {
        if (file == null) {
            Log.e(Utils.class.getName(), "setMediaThumbnail(), file = null!");
            return -1;
        }

        Bitmap bmp = fileToThumbnail(file);
        if (bmp == null) {
            return -1;
        }
        dest.setImageBitmap(bmp);
        return 0;
    }

    /**
     * thread sleep
     * @param millisec time to sleep
     */
    public static void sleepThread(int millisec) {
        try {
            Thread.sleep(millisec);
        } catch (InterruptedException e) {

        }
    }
}
