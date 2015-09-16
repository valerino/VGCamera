package valerino.vgcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TextureView;
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
        STATUS_CANCELED, // media canceled (taken media deleted)
        STATUS_ERROR // error saving media
    };

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

        // apply scaling (they're 50x50)
        // TODO: avoid scaling at runtime, just scale the images with gimp once for all :)
        locImg.setScaleX((float) 0.5);
        locImg.setScaleY((float) 0.5);
        smoothImg.setScaleX((float) 0.5);
        smoothImg.setScaleY((float) 0.5);
        saveImg.setScaleX((float) 0.5);
        saveImg.setScaleY((float) 0.5);

        // enable/disable the whole overlay
        boolean enabled = (AppConfiguration.instance(this).overlayMode() == AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY);
        if (!enabled) {
            // the whole overlay is disabled
            locImg.setVisibility(View.INVISIBLE);
            smoothImg.setVisibility(View.INVISIBLE);
            saveImg.setVisibility(View.INVISIBLE);
            return;
        }

        // enable/disable selectively
        locImg.setVisibility(AppConfiguration.instance(this).addLocation() ? View.VISIBLE : View.INVISIBLE);
        smoothImg.setVisibility(AppConfiguration.instance(this).smoothZoom() ? View.VISIBLE : View.INVISIBLE);
        saveImg.setVisibility(AppConfiguration.instance(this).autoSave() ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * initialize the options menu (for preview mode)
     * @param menu the options menu
     */
    private void initializePreviewMenu(Menu menu) {
        final String on = " ON";
        final String off = " OFF";

        // max zoom
        boolean enabled = AppConfiguration.instance(this).maxZoomMode();
        String s = getResources().getString(R.string.toggle_max_zoom);
        menu.findItem(R.id.zoom_toggle_max).setTitle(s + (enabled ? off : on));

        // smooth zoom
        enabled = AppConfiguration.instance(this).maxZoomMode();
        s = getResources().getString(R.string.toggle_smooth_zoom);
        menu.findItem(R.id.zoom_toggle_smooth).setTitle(s + (enabled ? off : on));

        // geotag
        enabled = AppConfiguration.instance(this).addLocation();
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
        boolean currentMaxZoomMode = AppConfiguration.instance(this).maxZoomMode();
        AppConfiguration.instance(this).setMaxZoomMode(!currentMaxZoomMode);
        CamController.instance(this).toggleMaxZoom();
        setupOverlay();
    }

    /**
     * toggle smooth zoom feature on/off
     */
    void toggleSmoothZoom() {
        boolean currentSmoothZoomMode = AppConfiguration.instance(this).smoothZoom();
        AppConfiguration.instance(this).setSmoothZoom(!currentSmoothZoomMode);
        setupOverlay();
    }

    /**
     * toggle autosave feature on/off
     */
    void toggleAutoSave() {
        boolean currentAutosave = AppConfiguration.instance(this).autoSave();
        AppConfiguration.instance(this).setAutoSave(!currentAutosave);
        setupOverlay();
    }

    /**
     * toggle geotagging on/off
     */
    void toggleGeotagging() {
        boolean currentGeotagging = AppConfiguration.instance(this).addLocation();
        AppConfiguration.instance(this).setAddLocation(!currentGeotagging);
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
            initializePreviewMenu(_menu);

            // restart preview too
            CamController.instance(this).startPreview();
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

            // error!
            AudioManager audio = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
            audio.playSoundEffect(Sounds.ERROR);
            return null;
        }

        // ok
        AudioManager audio = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        audio.playSoundEffect(Sounds.SUCCESS);
        Log.d(this.getClass().getName(), "saved media: " + f.getAbsolutePath());

        // update media library too
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
     * discard taken media and restart preview
     * @param playSound true to play sound
     */
    private void discardMedia(boolean playSound) {
        // delete cached media
        if (playSound) {
            AudioManager audio = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
            audio.playSoundEffect(Sounds.DISMISSED);
        }
        if (_tmpMedia != null) {
            _tmpMedia.delete();
        }
        signalDone(this, DONE_STATUS.STATUS_CANCELED, false);
    }

    /**
     * close application
     */
    void closeApp() {
        if (_tmpMedia != null) {
            _tmpMedia.delete();
        }
        finish();
    }

    /**
     * signal save ok/error through an icon in the center of the screen, and restart preview mode
     * @param ctx a Context
     * @param status one of the DONE_STATUS
     * @param restartPreview true to restart the preview
     */
    private void signalDone(final Context ctx, DONE_STATUS status , final boolean restartPreview) {
        final ImageView img = (ImageView)findViewById(R.id.takenResultImage);
        switch (status) {
            case STATUS_OK:
                img.setImageResource(R.drawable.ic_done_50);
                break;
            case STATUS_CANCELED:
                img.setImageResource(R.drawable.ic_delete_50);
                break;
            case STATUS_ERROR:
                img.setImageResource(R.drawable.ic_warning_50);
                break;
        }

        // make the result label visible for just 2 seconds
        img.setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                img.setVisibility(View.INVISIBLE);

                if (restartPreview) {
                    // and restart preview
                    CamController.instance(ctx).startPreview();
                }
            }
        }, 2000);

    }

    /**
     * take a picture
     * @param ctx a Context
     */
    private void takePicture(final Context ctx) {
        // use an async task, since snapPicture() would block the UI thread
        AsyncTask<Void, Void, File> t = new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... params) {
                // take picture, will stop the preview
                File f = CamController.instance(ctx).snapPicture();
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
                    return;
                }

                // we have a picture
                if (AppConfiguration.instance(ctx).autoSave()) {
                    // directly save and restart preview (taking picture disable the preview)
                    boolean ok = (saveMedia() != null);
                    signalDone(ctx, ok ? DONE_STATUS.STATUS_OK : DONE_STATUS.STATUS_ERROR, true);
                }
                else {
                    // user will take action
                    switchPanelMenu(OPERATION_MODE.MODE_TAKEN);
                }
            }
        };

        // run the task
        t.execute();
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
                signalDone(this, ok ? DONE_STATUS.STATUS_OK : DONE_STATUS.STATUS_ERROR, false);
                break;

            case R.id.discard:
                // discard the media
                discardMedia(true);
                break;

            default:
                // default is discard
                discardMedia(false);
                break;
        }

        // in the end, switch operation mode back to preview
        switchPanelMenu(OPERATION_MODE.MODE_PREVIEW);
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

            case R.id.zoom_toggle_max: {
                // toggle max zoom on/off
                toggleMaxZoom();
                break;
            }

            case R.id.zoom_toggle_smooth: {
                // toggle smooth zoom on/off
                toggleSmoothZoom();
                break;
            }

            case R.id.toggle_overlay:
                // toggle overlay on/off
                toggleOverlay();
                break;

            case R.id.toggle_location: {
                // toggle location on/off
                toggleGeotagging();
                break;
            }

            case R.id.toggle_autosave: {
                // toggle autosave on/off
                toggleAutoSave();
                break;
            }

            case R.id.take_picture:
                // take a picture
                takePicture(this);
                break;

            case R.id.close_app:
                // close application
                closeApp();
                break;

            default:
                break;
        }

        // reinitialize with the new values for later usage
        initializePreviewMenu(_menu);
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
                initializePreviewMenu(menu);
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

        // this is the texture view we'll blit on
        TextureView cameraTextureView = (TextureView) findViewById(R.id.cameraTextureView);

        // add the overlay layer
        AppConfiguration.instance(this).setOverlayMode(AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY);

        // inflate the overlays layout over the preview one
        LayoutInflater inflater = LayoutInflater.from(getBaseContext());
        View overlays = inflater.inflate(R.layout.overlay_layout, null);
        WindowManager.LayoutParams layoutParamsControl = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        addContentView(overlays, layoutParamsControl);

        // screen must be always on
        cameraTextureView.setKeepScreenOn(true);

        // setup the listener to show/hide the preview
        CamController.instance(this).setView(cameraTextureView);
        cameraTextureView.setSurfaceTextureListener(CamController.instance(this));
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
    protected void onPause() {
        Log.d(this.getClass().getName(), "vgcamera is pausing");

        // delete temporary files and do some cleanup
        System.gc();
        if (_tmpMedia != null) {
            _tmpMedia.delete();
        }
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
            // take a picture when the camera button is pressed, avoid propagating to the default camera
            takePicture(this);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return _gestureDetector.onMotionEvent(event);
    }

    /**
     * create the gestures detector to detect for swipes
     * swipe left/right : zoom in/out
     * tap : show cards
     * double tap: take picture
     * triple tap : take video
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
                    // show cards depending on mode
                    Intent it;
                    if (_mode == OPERATION_MODE.MODE_PREVIEW) {
                        // in preview mode, show cam options
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
                return false;
            }
        });
        return gd;
    }
}
