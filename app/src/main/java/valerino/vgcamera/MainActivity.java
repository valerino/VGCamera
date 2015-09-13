package valerino.vgcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TextureView;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;

/**
 * valerino glass camera
 * simple app which shows how the google glass camera should have been from beginning :)
 */
public class MainActivity extends Activity {
    private TextureView _capView;
    private TextureView _capViewNoOVerlay;
    private GestureDetector _gestureDetector;

    /**
     * set the zoom label if a zoom factor > 0 has been set
     */
    public void setZoomLabel() {
        if (AppConfiguration.instance().overlayMode() == AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY) {
            // only if we're in overlay mode
            TextView tv = (TextView) findViewById(R.id.zoom);
            int zoom = CamController.instance(this).camera().getParameters().getZoom();
            if (zoom == 0) {
                // no zoom
                tv.setText("");
            } else {
                tv.setText(CamController.instance(this).camera().getParameters().getZoom() + "x");
            }
        }
    }

    /**
     * here we react to specific voice/touch commands to control the camera
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            switch (item.getItemId()) {
                case R.id.zoom_in:
                    // zoom the image in
                    CamController.instance(this).zoomIn();
                    setZoomLabel();
                    break;

                case R.id.zoom_out:
                    // zoom the image out
                    CamController.instance(this).zoomOut();
                    setZoomLabel();
                    break;

/*                case R.id.zoom_toggle:
                    // set zoom back to 0 or max, depending on current value
                    android.hardware.Camera.Parameters params = _camController.camera().getParameters();
                    int current_zoom = params.getZoom();
                    int max_zoom = params.getMaxZoom();
                    if (current_zoom == max_zoom) {
                        _camController.setZoom(0);
                    }
                    else {
                        _camController.setZoom(max_zoom);
                    }
                    setZoomLabel();
                    break;

*/
                case R.id.take_picture:
                    // take a picture
                    CamController.instance(this).snapPicture();
                    break;

                case R.id.close_app:
                    // close application
                    CamController.instance(this).stopPreview();
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

        // Handle the TAP event.
        /*mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Plays disallowed sound to indicate that TAP actions are not supported.
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.DISALLOWED);
            }
        });*/

        // by default, we use our own layout with cam_menu and mode indicator, using the EMBED_INSIDE
        // feature of the CardBuilder
        setupLayout(true);

        // set mode/zoom text
        TextView tv = (TextView) findViewById(R.id.zoom);
        tv.setText("");

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
    private void setupLayoutInternal(TextureView view) {
        // screen must be always on
        view.setKeepScreenOn(true);

        // setup the listener to show/hide the preview
        CamController.instance(this).setView(view);
        view.setSurfaceTextureListener(CamController.instance(this));
    }

    /**
     * setup the app layout
     *
     * @param overlay if true, shows the overlay
     */
    private void setupLayout(boolean overlay) {
        if (overlay) {
            // use the overlay
            AppConfiguration.instance().setOverlayMode(AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY);
            CardBuilder cb = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE);
            cb.setEmbeddedLayout(R.layout.preview_layout);
            setContentView(cb.getView());
            _capView = (TextureView) findViewById(R.id.preview);
            setupLayoutInternal(_capView);
        } else {
            // no overlay, just use a new TextureView
            AppConfiguration.instance().setOverlayMode(AppConfiguration.OVERLAY_MODE.HIDE_OVERLAY);
            if (_capViewNoOVerlay == null) {
                // reuse the previous
                _capViewNoOVerlay = new TextureView(this);
            }
            setContentView(_capViewNoOVerlay);
            setupLayoutInternal(_capViewNoOVerlay);
        }
    }

    /**
     * cacthes results from the options activity
     *
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
                finish();
                break;
            case OptionsScroller.CHOICE_TOGGLE_OVERLAY:
                // toggle overlay on/off
                if (AppConfiguration.instance().overlayMode() == AppConfiguration.OVERLAY_MODE.HIDE_OVERLAY) {
                    AppConfiguration.instance().setOverlayMode(AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY);
                    setupLayout(true);
                } else {
                    AppConfiguration.instance().setOverlayMode(AppConfiguration.OVERLAY_MODE.HIDE_OVERLAY);
                    setupLayout(false);
                }
                break;
            case OptionsScroller.CHOICE_MAX_ZOOM: {
                // toggle max zoom on
                AppConfiguration.instance().setMaxZoomMode(true);
                break;
            }

            case OptionsScroller.CHOICE_RESET_ZOOM: {
                // reset zoom
                AppConfiguration.instance().setMaxZoomMode(false);
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
                    int currentZoom = CamController.instance(ctx).camera().getParameters().getZoom();
                    currentZoom += 2;
                    CamController.instance(ctx).setZoom(currentZoom);
                    setZoomLabel();
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    // zoom out
                    int currentZoom = CamController.instance(ctx).camera().getParameters().getZoom();
                    currentZoom -= 2;
                    CamController.instance(ctx).setZoom(currentZoom);
                    setZoomLabel();
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

        gd.setScrollListener(new GestureDetector.ScrollListener() {
            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                // do something on scrolling, but I'm not.
                Log.d(this.getClass().getName(), "scrolling detected");
                return false;
            }
        });
        return gd;
    }
}
