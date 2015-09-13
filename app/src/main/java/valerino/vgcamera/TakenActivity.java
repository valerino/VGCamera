package valerino.vgcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.google.android.glass.view.WindowUtils;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.TimelineItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * shows the taken image/video
 * Created by valerino on 13/09/15.
 */
public class TakenActivity extends Activity {
    private ImageView _imgView;
    private byte[] _tempData;
    private Bitmap _tempBmp;
    private File _storageFolder;
    private enum MEDIA_TYPE {
        MEDIA_TYPE_IMAGE,
        MEDIA_TYPE_VIDEO
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.taken_menu, menu);
        return true;
    }

    /**
     * cleanup and close this activity
     */
    private void doCleanupAndFinish() {
        _tempData = null;
        _tempBmp = null;
        System.gc();
        finish();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            switch (item.getItemId()) {
                case R.id.share_with:
                    // TODO: share the image/video
                    doCleanupAndFinish();
                    break;

                case R.id.save_this:
                    // save the image
                    // TODO: video
                    File f = saveImage(_tempData);
                    if (f != null) {
                        // let mediaserver do its updates
                        Log.d(this.getClass().getName(), "saved picture: " + f.getAbsolutePath());
                        Intent it = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        it.setData(Uri.fromFile(f));
                        this.getApplicationContext().sendBroadcast(it);
                    }

                    // and exit
                    doCleanupAndFinish();
                    break;

                case R.id.back:
                    // get back to the parent activity
                    doCleanupAndFinish();
                    break;

                default:
                    return true;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            getMenuInflater().inflate(R.menu.taken_menu, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // this is the storage folder
        _storageFolder = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");

        // ask for 'ok glass' prompt to accept commands
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // get the taken image
        Intent it = this.getIntent();
        byte[] data = it.getByteArrayExtra("data");

        // set the taken image as view
        _imgView = new ImageView(this);
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        _imgView.setImageBitmap(bmp);

        // we also save the data temporary, if the user decides to save
        _tempBmp = bmp;
        _tempData = data;

        // finally show image
        setContentView(_imgView);
    }

    /**
     * create a new timeline card for image thumbnail
     * @param f the image file
     */
    private void addTimelineItem (File f) {
        TimelineItem timelineItem = new TimelineItem();
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        timelineItem.setText(timeStamp);
        FileInputStream fis;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            return;
        }
        InputStreamContent mediaContent = new InputStreamContent("image/*", fis);
        Mirror service = new Mirror(new NetHttpTransport(), new JacksonFactory(), null);
        try {
            service.timeline().insert(timelineItem, mediaContent).execute();
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "timeline().insert()", e);
        }
        finally {
            Utils.closeNoEx(fis);
        }
    }
    /**
     * save picture to the storage folder
     * @param data the picture data
     * @return
     */
    private File saveImage(byte[] data) {
        // create a timed name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        File image = new File (_storageFolder, timeStamp + ".jpg");

        // this is the full image
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(image);
            fos.write(data);
        } catch (FileNotFoundException e) {
            Log.e(this.getClass().getName(), "can't create file: " + image.getAbsolutePath(), e);
            return null;
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "can't write to file: " + image.getAbsolutePath(), e);
            image.delete();
            return null;
        }
        finally {
            Utils.closeNoEx(fos);
        }

        // create the thumbnail and card too
        Bitmap scaledBitmap = android.media.ThumbnailUtils.extractThumbnail(_tempBmp, 640, 360);
        File scaledImage;
        try {
            scaledImage = File.createTempFile(timeStamp + "_SCALED_", ".jpg", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
        } catch (IOException e) {
            Log.e(this.getClass().getName(), "can't generate thumbnail for file: " + image.getAbsolutePath(), e);
            image.delete();
            return null;
        }

        try {
            fos = new FileOutputStream(scaledImage);
        } catch (FileNotFoundException e) {
            Log.e(this.getClass().getName(), "can't create file: " + image.getAbsolutePath(), e);
            image.delete();
            return null;
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
        Utils.closeNoEx(fos);
        addTimelineItem(scaledImage);

        return image;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
