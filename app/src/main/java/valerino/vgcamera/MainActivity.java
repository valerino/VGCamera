package valerino.vgcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;

import java.io.File;

/**
 * valerino glass camera
 * simple app which shows how the google glass camera should have been from beginning :)
 */
public class MainActivity extends Activity {
    private GestureDetector _gestureDetector = null;
    private Menu _menu = null;
    private File _tmpMedia = null;
    private boolean _isShortPress = false;
    private int _timerCount = 0;
    private CountDownTimer _timer = null;

    /**
     * this is the operation mode in which the app is working
     */
    private enum OPERATION_MODE {
        MODE_PREVIEW, // during preview
        MODE_TAKEN  // after preview (media taken)
    }
    private OPERATION_MODE _mode;

    /**
     * to signal status visually
     */
    private enum DONE_STATUS {
        STATUS_OK,  // media saved ok
        STATUS_CANCELED, // canceled (taken media deleted)
        STATUS_STOP_VIDEO, // video stopped
        STATUS_START_VIDEO, // video started
        STATUS_ERROR, // error saving media
        STATUS_GOT_PICTURE, // got a picture
        STATUS_UNSUPPORTED // unsupported in the current mode
    }

    /**
     * update the zoom label
     */
    public void updateZoomLabel() {
        try {
            TextView tv = (TextView) findViewById(R.id.zoomText);
            int zoom = CamController.instance(this).camera().getParameters().getZoom();
            if (zoom == 0) {
                // no zoom
                tv.setText("");
            } else {
                tv.setText(CamController.instance(this).camera().getParameters().getZoom() + "x");
            }
        }
        catch (Exception e) {
            // camera may not be set
        }
    }

    /**
     * setup the overlay
     */
    private void setupOverlay() {
        // get the overlay images (location, smoothzoom, autosave)
        ImageView locImg = (ImageView) findViewById(R.id.locationImage);
        ImageView smoothImg = (ImageView) findViewById(R.id.smoothZoomImage);
        ImageView saveImg = (ImageView) findViewById(R.id.autoSaveImage);
        ImageView modeImg = (ImageView) findViewById(R.id.modeImageView);
        ImageView qualityImg = (ImageView)findViewById(R.id.qualityImage);


        // apply scaling (they're 50x50)
        // TODO: avoid scaling at runtime, just scale the images with gimp once for all :)
        locImg.setScaleX((float) 0.5);
        locImg.setScaleY((float) 0.5);
        smoothImg.setScaleX((float) 0.5);
        smoothImg.setScaleY((float) 0.5);
        saveImg.setScaleX((float) 0.5);
        saveImg.setScaleY((float) 0.5);
        modeImg.setScaleX((float) 0.5);
        modeImg.setScaleY((float) 0.5);
        qualityImg.setScaleX((float) 0.5);
        qualityImg.setScaleY((float) 0.5);

        // enable/disable the whole overlay
        boolean enabled = (AppConfiguration.instance(this).overlayMode() == AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY);
        if (!enabled) {
            // the whole overlay is disabled
            locImg.setVisibility(View.INVISIBLE);
            smoothImg.setVisibility(View.INVISIBLE);
            saveImg.setVisibility(View.INVISIBLE);
            qualityImg.setVisibility(View.INVISIBLE);
            return;
        }

        // // set visible options based on configuration
        locImg.setVisibility(AppConfiguration.instance(this).geoTagging() ? View.VISIBLE : View.INVISIBLE);
        smoothImg.setVisibility(AppConfiguration.instance(this).smoothZoom() ? View.VISIBLE : View.INVISIBLE);
        saveImg.setVisibility(AppConfiguration.instance(this).autoSave() ? View.VISIBLE : View.INVISIBLE);
        qualityImg.setVisibility(AppConfiguration.instance(this).quality() == AppConfiguration.QUALITY.HIGH ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * initialize the options menu (for preview mode)
     * @param menu the options menu
     */
    private void initializeOptionsMenu(Menu menu) {
        final String on = " ON";
        final String off = " OFF";

        // max zoom
        boolean enabled = AppConfiguration.instance(this).maxZoomMode();
        String s = getResources().getString(R.string.toggle_max_zoom);
        menu.findItem(R.id.zoom_toggle_max).setTitle(s + (enabled ? off : on));

        // smooth zoom
        enabled = AppConfiguration.instance(this).smoothZoom();
        s = getResources().getString(R.string.toggle_smooth_zoom);
        menu.findItem(R.id.zoom_toggle_smooth).setTitle(s + (enabled ? off : on));

        // geotag
        enabled = AppConfiguration.instance(this).geoTagging();
        s = getResources().getString(R.string.toggle_location);
        menu.findItem(R.id.toggle_location).setTitle(s + (enabled ? off : on));

        // autosave
        enabled = AppConfiguration.instance(this).autoSave();
        s = getResources().getString(R.string.toggle_autosave);
        menu.findItem(R.id.toggle_autosave).setTitle(s + (enabled ? off : on));

        // overlay
        enabled = (AppConfiguration.instance(this).overlayMode() == AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY);
        s = getResources().getString(R.string.toggle_overlay);
        menu.findItem(R.id.toggle_overlay).setTitle(s + (enabled ? off : on));

        // quality
        boolean qualityHigh = (AppConfiguration.instance(this).quality() == AppConfiguration.QUALITY.HIGH);
        s = getResources().getString(R.string.toggle_quality);
        menu.findItem(R.id.toggle_quality).setTitle(s + (qualityHigh ? " LOW" : " HIGH"));
    }

    /**
     * toggle the overlay
     */
    private void toggleOverlay() {
        boolean enabled = (AppConfiguration.instance(this).overlayMode() == AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY);
        if (enabled) {
            // disable
            AppConfiguration.instance(this).setOverlayMode(AppConfiguration.OVERLAY_MODE.HIDE_OVERLAY);
        }
        else {
            // enable
            AppConfiguration.instance(this).setOverlayMode(AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY);
        }

        // toggle images/labels
        setupOverlay();
    }

    /**
     * toggle the max zoom mode
     */
    void toggleMaxZoom() {
        // toggle configuration param
        AppConfiguration config = AppConfiguration.instance(this);
        boolean enabled = config.maxZoomMode();
        config.setMaxZoomMode(!enabled);

        // toggle camera param
        CamController cam = CamController.instance(this);
        if (enabled) {
            // reset
            cam.resetZoom();
        } else {
            // zoom to max
            cam.setMaxZoom();
        }
    }

    /**
     * toggle smooth zoom feature on/off
     */
    void toggleSmoothZoom() {
        // toggle configuration param
        AppConfiguration config = AppConfiguration.instance(this);
        boolean enabled = config.smoothZoom();
        config.setSmoothZoom(!enabled);

        // update overlay
        setupOverlay();
    }

    /**
     * toggle autosave feature on/off
     */
    void toggleAutoSave() {
        // toggle configuration param
        AppConfiguration config = AppConfiguration.instance(this);
        boolean enabled = config.autoSave();
        config.setAutoSave(!enabled);

        // update overlay
        setupOverlay();
    }

    /**
     * toggle quality hi/lo
     */
    void toggleQuality() {
        // toggle configuration param
        AppConfiguration config = AppConfiguration.instance(this);
        AppConfiguration.QUALITY currentQuality = config.quality();
        if (currentQuality == AppConfiguration.QUALITY.HIGH) {
            // low quality
            config.setQuality(AppConfiguration.QUALITY.LOW);
        }
        else {
            // high quality
            config.setQuality(AppConfiguration.QUALITY.HIGH);
        }

        // update overlay
        setupOverlay();
    }

    /**
     * toggle geotagging on/off
     */
    void toggleGeotagging() {
        // toggle configuration param
        AppConfiguration config = AppConfiguration.instance(this);
        boolean enabled = config.geoTagging();
        config.setGeotagging(!enabled);

        // update overlay
        setupOverlay();
    }

    /**
     * switch the options menu to show camera (preview mode) options or taken (after taking picture/video) options
     * @param mode the operation mode
     */
    void switchPanelMenu(OPERATION_MODE mode) {
        _mode = mode;
        _menu.clear();
        if (mode == OPERATION_MODE.MODE_PREVIEW) {
            // use the preview menu
            getMenuInflater().inflate(R.menu.cam_menu, _menu);

            // initialize with runtime values
            initializeOptionsMenu(_menu);

            // restart preview too
            Camera cam = CamController.instance(this).camera();
            if (cam != null) {
                cam.startPreview();
            }
        }
        else {
            // we got a media, we're in the taken menu
            getMenuInflater().inflate(R.menu.taken_menu, _menu);
        }
    }

    /**
     * move media to the given storage folder
     * @param src the source media
     * @return File in the media storage folder, or null
     */
    private File moveMediaToStorage (File src) {
        File f = new File (AppConfiguration.instance(this).storageFolder(), src.getName());
        if (!src.renameTo(f)) {
            Log.e(this.getClass().getName(), "can't rename " + src.getAbsolutePath() + " to " + f.getAbsolutePath());
            return null;
        }

        // ok, update media library too
        Log.d(this.getClass().getName(), "saved media: " + f.getAbsolutePath());
        MediaScannerConnection.scanFile(this, new String[]{f.getAbsolutePath()}, null, null);
        return src;
    }

    /**
     * save taken media and restart preview
     * @return File the saved media, or null
     */
    private File saveMedia() {
        // save the captured image/video
        File f = moveMediaToStorage(_tmpMedia);
        return f;
    }

    /**
     * delete cached media
     */
    private void cleanup() {
        if (_tmpMedia != null) {
            _tmpMedia.delete();
        }
    }

    /**
     * discard the taken media and show canceled icon
     */
    private void discardMedia() {
        cleanup();
        signalStatus(this, DONE_STATUS.STATUS_CANCELED);
    }

    /**
     * close application
     */
    void closeApp() {
        finish();
    }

    /**
     * signal status through audio and an icon in the center of the screen
     * @param ctx a Context
     * @param status one of the DONE_STATUS
     */
    private void signalStatus(final Context ctx, DONE_STATUS status) {
        final ImageView img = (ImageView)findViewById(R.id.takenResultImage);
        switch (status) {
            case STATUS_OK:
                img.setImageResource(R.drawable.ic_done_50);
                Utils.playSound(ctx,Sounds.SUCCESS);
                break;
            case STATUS_CANCELED:
                img.setImageResource(R.drawable.ic_delete_50);
                Utils.playSound(ctx, Sounds.DISMISSED);
                break;
            case STATUS_ERROR:
                img.setImageResource(R.drawable.ic_warning_50);
                Utils.playSound(ctx, Sounds.ERROR);
                break;
            case STATUS_STOP_VIDEO:
                img.setImageResource(R.drawable.ic_video_off_50);
                Utils.playSound(ctx, Sounds.SUCCESS);
                break;
            case STATUS_START_VIDEO:
                img.setImageResource(R.drawable.ic_video_50);
                Utils.playSound(ctx, Sounds.SUCCESS);
                break;
            case STATUS_UNSUPPORTED:
                img.setImageResource(R.drawable.ic_no_50);
                Utils.playSound(ctx, Sounds.DISALLOWED);
                break;
            case STATUS_GOT_PICTURE:
                img.setImageResource(R.drawable.ic_camera_50);
                Utils.playSound(ctx, Sounds.SUCCESS);
                break;
            default:
                return;
        }

        // make the result label visible for just 1 second
        img.setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                img.setVisibility(View.INVISIBLE);

            }
        }, 1000);
    }

    /**
     * start recorder timer
     */
    private void startRecordingTimer() {
        // reset counter
        _timerCount = 0;

        // start the timer
        _timer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                _timerCount++;

                // get hours/mins/secs
                int hours = _timerCount / 3600;
                int minutes = (_timerCount % 3600) / 60;
                int seconds = _timerCount % 60;

                // set the time
                String s = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                final TextView tv = (TextView) findViewById(R.id.videoTimeText);
                tv.setText(s);
            }

            @Override
            public void onFinish() {

            }
        };
        _timer.start();
    }

    /**
     * stop recorder timer
     */
    private void stopRecordingTimer() {
        _timer.cancel();
        _timer = null;
    }

    /**
     * show the recording timer
     * @param show true to show, false to hide
     */
    private void showRecordingTimer(boolean show) {
        final TextView tv = (TextView) findViewById(R.id.videoTimeText);
        if (show) {
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.GONE);
        }
    }

    /**
     * start recording a video
     */
    private void startRecording() {
        CamController cam = CamController.instance(this);
        if (cam.mode() == CamController.CAM_MODE.MODE_VIDEO) {
            // no effect
            signalStatus(this, DONE_STATUS.STATUS_UNSUPPORTED);
            return;
        }

        // start recording
        if (cam.camStartRecord()) {
            // signal start
            signalStatus(this, DONE_STATUS.STATUS_START_VIDEO);

            // change mode icon to video
            ImageView modeImg = (ImageView) findViewById(R.id.modeImageView);
            modeImg.setImageResource(R.drawable.ic_video_50);
            setupOverlay();

            // start recording timer
            showRecordingTimer(true);
            startRecordingTimer();
        }
    }

    /**
     * stop recording a video
     * @param ctx a Context
     */
    private void stopRecording(final Context ctx) {
        final CamController cam = CamController.instance(this);
        if (cam.mode() != CamController.CAM_MODE.MODE_VIDEO) {
            // no effect
            signalStatus(this, DONE_STATUS.STATUS_UNSUPPORTED);
            return;
        }

        // signal stop
        signalStatus(ctx, DONE_STATUS.STATUS_STOP_VIDEO);

        // stop timer
        stopRecordingTimer();

        // use an async task, since camTakePicture() would block the UI thread
        AsyncTask<Void, Void, File> t = new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... params) {
                // get the currently recorded media
                File f  = cam.camStopRecord(false);
                if (f == null) {
                    return null;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                }
                // store it as a global, for commodity ....
                _tmpMedia = f;
                return f;
            }

            @Override
            protected void onPostExecute(File f) {
                if (f == null) {
                    // some error here
                    signalStatus(ctx, DONE_STATUS.STATUS_ERROR);
                    return;
                }

                // we have a video
                if (AppConfiguration.instance(ctx).autoSave()) {
                    // directly save
                    boolean ok = (saveMedia() != null);
                    signalStatus(ctx, ok ? DONE_STATUS.STATUS_OK : DONE_STATUS.STATUS_ERROR);
                    showRecordingTimer(false);
                }
                else {
                    // user will take action
                    showTakenThumbnail(true);
                    switchPanelMenu(OPERATION_MODE.MODE_TAKEN);
                }

                // change mode icon to camera
                ImageView modeImg = (ImageView) findViewById(R.id.modeImageView);
                modeImg.setImageResource(R.drawable.ic_camera_50);
                setupOverlay();
            }
        };

        // run the task
        t.execute();
    }

    /**
     * show thumbnail of taken image/video
     * @param show true to show, false to hide
     */
    private void showTakenThumbnail(boolean show) {
        ImageView iv = (ImageView)findViewById(R.id.takenImageView);
        if (show) {
            // show thumbnail
            Utils.setMediaThumbnail(this, _tmpMedia, iv);
            iv.setVisibility(View.VISIBLE);
        }
        else {
            // hide the thumbnail view so preview can get in front
            iv.setVisibility(View.GONE);
        }
    }

    /**
     * take a picture
     * @param ctx a Context
     */
    private void takePicture(final Context ctx) {
        // use an async task, since camTakePicture() would block the UI thread
        AsyncTask<Void, Void, File> t = new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... params) {
                // take picture, will stop the preview
                File f = CamController.instance(ctx).camTakePicture();
                if (f == null) {
                    return null;
                }

                // store it as a global, for commodity ....
                _tmpMedia = f;
                return f;
            }

            @Override
            protected void onPostExecute(File f) {
                if (f == null) {
                    // some error here
                    signalStatus(ctx, DONE_STATUS.STATUS_ERROR);
                    return;
                }

                // we have a picture
                if (AppConfiguration.instance(ctx).autoSave()) {
                    // directly save and restart preview (taking picture disable the preview)
                    boolean ok = (saveMedia() != null);
                    signalStatus(ctx, ok ? DONE_STATUS.STATUS_OK : DONE_STATUS.STATUS_ERROR);
                }
                else {
                    // show a thumbnail, and user will take action
                    showTakenThumbnail(true);

                    // user will take action
                    signalStatus(ctx, DONE_STATUS.STATUS_GOT_PICTURE);
                    switchPanelMenu(OPERATION_MODE.MODE_TAKEN);
                }
            }
        };

        // run the task
        t.execute();
    }

    /**
     * get back to preview mode from taken mode
     */
    private void backToPreviewMode() {
        // restart preview if we're not in autosave mode
        if (!AppConfiguration.instance(this).autoSave()) {
            showTakenThumbnail(false);
        }

        // in the end, switch operation mode back to preview
        switchPanelMenu(OPERATION_MODE.MODE_PREVIEW);
    }

    /**
     * handles the taken menu
     * @param id the selected menu item
     */
    void handleTakenMenu(int id) {
        switch (id) {
            case R.id.share:
                // TODO: share the image/video
                break;

            case R.id.save:
                // save the captured image/video (preview will be restarted automatically)
                boolean ok = (saveMedia() != null);
                signalStatus(this, ok ? DONE_STATUS.STATUS_OK : DONE_STATUS.STATUS_ERROR);
                break;

            case R.id.discard:
                // discard the media
                discardMedia();
                break;

            default:
                // default is discard
                discardMedia();
                break;
        }

        // hide the recording timer (if we've stopped videorecording, stopRecording() has set mode back to photo)
        if (CamController.instance(this).mode() == CamController.CAM_MODE.MODE_PHOTO) {
            showRecordingTimer(false);
        }

        // get back to preview
        backToPreviewMode();
    }

    /**
     * handles the options menu
     * @param id the selected menu item
     */
    void handleOptionsMenu(int id) {
        switch (id) {
            case R.id.back:
                break;

            case R.id.zoom_in:
                // zoom the image in
                CamController.instance(this).zoomIn();
                break;

            case R.id.zoom_out:
                // zoom the image out
                CamController.instance(this).zoomOut();
                break;

            case R.id.zoom_toggle_max:
                // toggle max zoom on/off
                toggleMaxZoom();
                break;

            case R.id.zoom_toggle_smooth:
                // toggle smooth zoom on/off
                toggleSmoothZoom();
                break;

            case R.id.zoom_reset:
                // reset zoom to 0
                CamController.instance(this).resetZoom();
                break;

            case R.id.toggle_quality:
                // toggle quality lo/hi
                toggleQuality();
                break;

            case R.id.toggle_overlay:
                // toggle overlay on/off
                toggleOverlay();
                break;

            case R.id.toggle_location:
                // toggle location on/off
                toggleGeotagging();
                break;

            case R.id.toggle_autosave:
                // toggle autosave on/off
                toggleAutoSave();
                break;

            case R.id.take_picture:
                // take a picture
                takePicture(this);
                break;

            case R.id.stop_video:
                // stop recording a video
                stopRecording(this);
                break;

            case R.id.record_video:
                // start recording a video
                startRecording();
                break;

            case R.id.close_app:
                // close application
                closeApp();
                break;

            default:
                break;
        }

        // reinitialize with the new values for later usage
        initializeOptionsMenu(_menu);
    }

    /**
     * here we react to specific voice commands to control the camera
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            if (_mode == OPERATION_MODE.MODE_PREVIEW) {
                // handle the options menu
                handleOptionsMenu(item.getItemId());
            }
            else {
                // handle the taken menu
                handleTakenMenu(item.getItemId());
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            if (_mode == OPERATION_MODE.MODE_PREVIEW) {
                // the preview menu
                getMenuInflater().inflate(R.menu.cam_menu, menu);
                initializeOptionsMenu(menu);
            }
            else {
                // the post-preview menu
                getMenuInflater().inflate(R.menu.taken_menu, menu);
            }

            // save for later usage
            _menu = menu;
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    /**
     * setup the app layout
     */
    private void setupLayout() {
        // set the main layout
        setContentView(R.layout.preview_layout);

        // add the overlay layer
        AppConfiguration.instance(this).setOverlayMode(AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY);

        // inflate the overlays layout over the preview one
        LayoutInflater inflater = LayoutInflater.from(getBaseContext());
        View overlays = inflater.inflate(R.layout.overlay_layout, null);
        WindowManager.LayoutParams layoutParamsControl = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        addContentView(overlays, layoutParamsControl);

        // set the surface
        SurfaceView sv = (SurfaceView) findViewById(R.id.cameraSurfaceView);
        CamController.instance(this).setSurfaceView(sv);

        // screen must be always on
        sv.setKeepScreenOn(true);

        // setup the listener to show/hide the preview
        sv.getHolder().addCallback(CamController.instance(this));
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // ask for 'ok glass' prompt to accept commands
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        _mode = OPERATION_MODE.MODE_PREVIEW;

        // setup the layout
        setupLayout();

        // set the overlay labels if we're in overlay mode
        setupOverlay();

        // set touch/gestures detector, will be catched in onGenericMotionEvent() which, in turn,
        // will use the gesture detector's listener logic to react.
        _gestureDetector = createGestureDetector(this);
    }

    @Override
    protected void onResume() {
        Log.d(this.getClass().getName(), "vgcamera is resuming");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.d(this.getClass().getName(), "vgcamera is destroying");

        // stop camera if it's recording, deleting temporary files
        CamController.instance(this).camStopRecord(true);

        // cleanup
        cleanup();

        // do some garbage collection too
        System.gc();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.d(this.getClass().getName(), "vgcamera is stopping");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(this.getClass().getName(), "vgcamera is pausing");
        super.onPause();
    }

    /**
     * cacthes results from the scrollers (cards)
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            // get the result
            int res = data.getIntExtra("choice", -1);
            if (_mode == OPERATION_MODE.MODE_TAKEN) {
                // from post-taken scroller
                handleTakenMenu(res);
            } else {
                // from configure cam scroller
                handleOptionsMenu(res);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                event.startTracking();
                if (event.getRepeatCount() == 0) {
                    // set a flag to indicate short press, and offload to onKeyUp()
                    _isShortPress = true;
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (_isShortPress) {
                // take picture
                takePicture(this);
                _isShortPress = false;
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            // start or stop recording on longpress
            _isShortPress = false;
            if (CamController.instance(this).mode() == CamController.CAM_MODE.MODE_PHOTO) {
                startRecording();
            }
            else {
                stopRecording(this);
            }
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return _gestureDetector.onMotionEvent(event);
    }

    /**
     * create the gestures detector to detect for swipes and touchpad taps
     * @param ctx a Context
     * @return
     */
    private GestureDetector createGestureDetector(final Context ctx) {
        GestureDetector gd = new GestureDetector(ctx);
        gd.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.SWIPE_RIGHT) {
                    // zoom in
                    CamController.instance(ctx).zoomIn();
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    // zoom out
                    CamController.instance(ctx).zoomOut();
                    return true;
                } else if (gesture == Gesture.TAP) {
                    if (CamController.instance(ctx).mode() == CamController.CAM_MODE.MODE_VIDEO) {
                        // videorecorder preview can't be interrupted by cards, use voice or long tap
                        signalStatus(ctx, DONE_STATUS.STATUS_UNSUPPORTED);
                        return true;
                    }
                    // show cards depending on mode
                    Intent it;
                    if (_mode == OPERATION_MODE.MODE_PREVIEW) {
                        // in preview & photo mode, show cam options
                        it = new Intent(ctx, OptionsScroller.class);
                    }
                    else {
                        // in taken mode, show save/discard/share
                        it = new Intent(ctx, TakenScroller.class);
                    }
                    startActivityForResult(it, 1);
                    return true;
                }
                else if (gesture == Gesture.TWO_TAP) {
                    // take picture
                    takePicture(ctx);
                    return true;
                }
                else if (gesture == Gesture.THREE_TAP) {
                    if (CamController.instance(ctx).mode() == CamController.CAM_MODE.MODE_PHOTO) {
                        // record a video
                        startRecording();
                    }
                    else {
                        // stop recording
                        stopRecording(ctx);
                    }
                    return true;
                }
                else if (gesture == Gesture.SWIPE_DOWN) {
                    // force close
                    Utils.playSound(ctx, Sounds.DISMISSED);
                    closeApp();
                    return true;
                }
                else if (gesture == Gesture.LONG_PRESS) {
                    if (AppConfiguration.instance(ctx).autoSave() ||
                            CamController.instance(ctx).mode() == CamController.CAM_MODE.MODE_PHOTO) {
                        // only when recording videos, to not confuse the user with too much taps
                        signalStatus(ctx, DONE_STATUS.STATUS_UNSUPPORTED);
                        return true;
                    }

                    // save media
                    signalStatus(ctx, saveMedia() != null ? DONE_STATUS.STATUS_OK : DONE_STATUS.STATUS_ERROR);

                    // get back to preview
                    backToPreviewMode();
                    return true;
                }
                else if (gesture == Gesture.TWO_LONG_PRESS) {
                    if (AppConfiguration.instance(ctx).autoSave() ||
                            CamController.instance(ctx).mode() == CamController.CAM_MODE.MODE_PHOTO) {
                        // only when recording videos, to not confuse the user with too much taps
                        signalStatus(ctx,DONE_STATUS.STATUS_UNSUPPORTED);
                        return true;
                    }
                    // discard taken media
                    discardMedia();
                    signalStatus(ctx, DONE_STATUS.STATUS_CANCELED);

                    // get back to preview
                    backToPreviewMode();
                    return true;
                }
                else if (gesture == Gesture.THREE_LONG_PRESS) {
                    if (AppConfiguration.instance(ctx).autoSave() ||
                            CamController.instance(ctx).mode() == CamController.CAM_MODE.MODE_PHOTO) {
                        // only when recording videos, to not confuse the user with too much taps
                        signalStatus(ctx,DONE_STATUS.STATUS_UNSUPPORTED);
                        return true;
                    }

                    // TODO: share
                    return true;
                }
                return false;
            }
        });
        return gd;
    }
}
