package valerino.vgcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;

/**
 * valerino glass camera
 * simple app which shows how the google glass camera should have been from beginning :)
 */
public class MainActivity extends Activity {
    private TextureView _capView;
    private GestureDetector _gestureDetector;

    /**
     * set labels for the overlay
     */
    public void setOverlayLabels() {
        if (AppConfiguration.instance(this).overlayMode() == AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY) {
            TextView tv;
            try {
                // only if we're in overlay mode, set zoom label
                tv = (TextView) findViewById(R.id.zoomText);
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

            // smooth zoom on/off
            tv = (TextView) findViewById(R.id.smoothZoomText);
            if (AppConfiguration.instance(this).smoothZoom()) {
                tv.setText(" SMZ");
            }
            else {
                tv.setText("");
            }

            // set location label
            tv = (TextView) findViewById(R.id.locationText);
            if (AppConfiguration.instance(this).addLocation()) {
                // location enabled
                tv.setText(" LOC");
            }
            else {
                tv.setText("");
            }

            // set autosave label
            tv = (TextView) findViewById(R.id.autoSaveText);
            if (AppConfiguration.instance(this).autoSave()) {
                // autosavre enabled
                tv.setText(" SAV");
            }
            else {
                tv.setText("");
            }
        }
    }

    /**
     * toggle the max zoom mode
     */
    void toggleMaxZoom() {
        boolean currentMaxZoomMode = AppConfiguration.instance(this).maxZoomMode();
        AppConfiguration.instance(this).setMaxZoomMode(!currentMaxZoomMode);
        CamController.instance(this).toggleMaxZoom();
        if (!AppConfiguration.instance(this).smoothZoom()) {
            // when smooth zooming, the listener will handle updating label
            setOverlayLabels();
        }
    }

    /**
     * toggle smooth zoom feature on/off
     */
    void toggleSmoothZoom() {
        boolean currentSmoothZoomMode = AppConfiguration.instance(this).smoothZoom();
        AppConfiguration.instance(this).setSmoothZoom(!currentSmoothZoomMode);
        setOverlayLabels();
    }

    /**
     * toggle autosave feature on/off
     */
    void toggleAutoSave() {
        boolean currentAutosave = AppConfiguration.instance(this).autoSave();
        AppConfiguration.instance(this).setAutoSave(!currentAutosave);
        setOverlayLabels();
    }

    /**
     * toggle geotagging on/off
     */
    void toggleGeotagging() {
        boolean currentGeotagging = AppConfiguration.instance(this).addLocation();
        AppConfiguration.instance(this).setAddLocation(!currentGeotagging);
        setOverlayLabels();
    }

    /**
     * here we react to specific voice commands to control the camera
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            switch (item.getItemId()) {
                case R.id.zoom_in:
                    // zoom the image in
                    CamController.instance(this).zoomIn();
                    if (!AppConfiguration.instance(this).smoothZoom()) {
                        // when smooth zooming, the listener will handle updating label
                        setOverlayLabels();
                    }
                    break;

                case R.id.zoom_out:
                    // zoom the image out
                    CamController.instance(this).zoomOut();
                    if (!AppConfiguration.instance(this).smoothZoom()) {
                        // when smooth zooming, the listener will handle updating label
                        setOverlayLabels();
                    }
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
                    CamController.instance(this).snapPicture();
                    break;

                case R.id.close_app:
                    // close application
                    CamController.instance(this).stopPreview();
                    System.gc();
                    finish();
                    break;

                default:
                    return true;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cam_menu, menu);
        return true;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            getMenuInflater().inflate(R.menu.cam_menu, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // ask for 'ok glass' prompt to accept commands
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // setup the layout, showing overlay if OVERLAY_MODE.SHOW_OVERLAY is set in the preferences
        AppConfiguration.OVERLAY_MODE overlay = AppConfiguration.instance(this).overlayMode();
        setupLayout(overlay == AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY);

        // set touch/gestures detector, will be catched in onGenericMotionEvent() which, in turn,
        // will use the gesture detector's listener logic to react.
        _gestureDetector = createGestureDetector(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * setup the layout (common)
     */
    private void setupLayoutInternal() {
        // screen must be always on
        _capView.setKeepScreenOn(true);

        // setup the listener to show/hide the preview
        CamController.instance(this).setView(_capView);
        _capView.setSurfaceTextureListener(CamController.instance(this));

        // set the overlay labels if we're in overlay mode
        setOverlayLabels();
    }

    /**
     * setup the app layout
     *
     * @param overlay if true, shows the overlay
     */
    private void setupLayout(boolean overlay) {
        // set the main layout
        setContentView(R.layout.preview_layout);

        // this is the texture view we'll blit on
        _capView = (TextureView) findViewById(R.id.cameraTextureView);

        if (overlay) {
            // use the overlay
            AppConfiguration.instance(this).setOverlayMode(AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY);

            // inflate the overlays layout over the preview one
            LayoutInflater inflater = LayoutInflater.from(getBaseContext());
            View overlays = inflater.inflate(R.layout.overlay_layout, null);
            WindowManager.LayoutParams layoutParamsControl = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            addContentView(overlays, layoutParamsControl);
        }
        else {
            // no overlay
            AppConfiguration.instance(this).setOverlayMode(AppConfiguration.OVERLAY_MODE.HIDE_OVERLAY);
        }
        // finish setup
        setupLayoutInternal();
    }

    /**
     * toggle the overlay
     */
    private void toggleOverlay() {
        if (AppConfiguration.instance(this).overlayMode() == AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY) {
            // hide overlay
            setupLayout(false);
        }
        else {
            // show overlay
            setupLayout(true);
        }
    }

    /**
     * cacthes results from the options activity (handles touchpad controls)
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }

        // get the result
        int res = data.getIntExtra("choice", -1);
        switch (res) {
            case OptionsScroller.CHOICE_EXIT:
                // exit app
                System.gc();
                finish();
                break;
            case OptionsScroller.CHOICE_TOGGLE_OVERLAY:
                // toggle overlay
                toggleOverlay();
                break;
            case OptionsScroller.CHOICE_TOGGLE_MAXZOOM: {
                // toggle max zoom
                toggleMaxZoom();
                break;
            }
            case OptionsScroller.CHOICE_TOGGLE_SMOOTHZOOM: {
                // toggle smooth zoom
                toggleSmoothZoom();
                break;
            }
            case OptionsScroller.CHOICE_TOGGLE_AUTOSAVE: {
                // toggle autosave
                toggleAutoSave();
                break;
            }
            case OptionsScroller.CHOICE_TOGGLE_LOCATION: {
                // toggle geotagging
                toggleGeotagging();
                break;
            }

            default:
                break;
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return _gestureDetector.onMotionEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            // take a picture when the camera button is pressed
            CamController.instance(this).snapPicture();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * create the gestures detector to detect for swipes (inc/dec zoom)
     *
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
                    if (!AppConfiguration.instance(ctx).smoothZoom()) {
                        // when smooth zooming, the listener will handle updating label
                        setOverlayLabels();
                    }
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    // zoom out
                    CamController.instance(ctx).zoomOut();
                    if (!AppConfiguration.instance(ctx).smoothZoom()) {
                        // when smooth zooming, the listener will handle updating label
                        setOverlayLabels();
                    }
                    return true;
                } else if (gesture == Gesture.TAP) {
                    // show options cards
                    Intent intent = new Intent(ctx, OptionsScroller.class);
                    startActivityForResult(intent, 1);
                    return true;
                }

                return false;
            }
        });
        return gd;
    }
}
