package valerino.vgcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
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
import java.io.IOException;
import java.util.Date;

/**
 * shows the taken image/video
 * Created by valerino on 13/09/15.
 */
public class TakenActivity extends Activity implements MediaScannerConnection.OnScanCompletedListener {
    private ImageView _imgView;
    private File _tempFile;
    private File _storageFolder;

    @Override
    public void onScanCompleted(String s, Uri uri) {
        Log.d(this.getClass().getName(), "Scan completed on file: " + s + ", uri:" + uri.toString());

        // and exit
        doCleanupAndFinish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.taken_menu, menu);
        return true;
    }

    /**
     * cleanup and close this activity
     */
    private void doCleanupAndFinish() {
        _tempFile.delete();
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
                    // save the image/video, moving it to the camera folder
                    File f = new File (_storageFolder, _tempFile.getName());
                    if (!_tempFile.renameTo(f)) {
                        Log.e(this.getClass().getName(), "can't rename " + _tempFile.getAbsolutePath() + " to " + f.getAbsolutePath());
                    }
                    else {
                        // let mediaserver do its updates
                        Log.d(this.getClass().getName(), "saved picture: " + f.getAbsolutePath());
                        MediaScannerConnection.scanFile(this, new String[]{f.getAbsolutePath()}, null, this);
                    }
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
        //_storageFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        // ask for 'ok glass' prompt to accept commands
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // get the taken image (store its path temporarly too)
        Intent it = this.getIntent();
        _tempFile = new File (it.getStringExtra("tmp_path"));

        Bitmap preview = null;
        if (_tempFile.getAbsolutePath().endsWith(".jpg")) {
            // it's an image, generate preview
            Bitmap bmp = BitmapFactory.decodeFile(_tempFile.getAbsolutePath(), null);
            preview = android.media.ThumbnailUtils.extractThumbnail(bmp, 640, 360);
        }
        else {
            // TODO: mp4 video
        }
        // set the taken image as view (a thumbnail of it)
        _imgView = new ImageView(this);
        _imgView.setImageBitmap(preview);

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

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
