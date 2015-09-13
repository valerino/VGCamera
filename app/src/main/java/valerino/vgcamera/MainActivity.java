package valerino.vgcamera;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.widget.TextView;

import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;

/**
 * valerino glass camera
 * simple app which shows how the google glass camera should have been from beginning :)
 */
public class MainActivity extends Activity {
    private TextureView _capView;
    private CamController _camController;
    private enum OVERLAY_MODE {
        SHOW_OVERLAY,
        HIDE_OVERLAY;
    }
    private OVERLAY_MODE _overlayMode;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cam_menu, menu);
        return true;
    }

    /**
     * set the zoom label if a zoom factor > 0 has been set
     */
    void setZoomLabel () {
        if (_overlayMode == OVERLAY_MODE.SHOW_OVERLAY) {
            // only if we're in overlay mode
            TextView tv = (TextView) findViewById(R.id.zoom);
            int zoom = _camController.camera().getParameters().getZoom();
            if (zoom > 0) {
                // only set text when there's a zoom set
                tv.setText(_camController.camera().getParameters().getZoom() + "x");
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
                    _camController.zoomIn();
                    setZoomLabel();
                    break;

                case R.id.zoom_out:
                    // zoom the image out
                    _camController.zoomOut();
                    setZoomLabel();
                    break;

                case R.id.zoom_reset:
                    // set zoom back to 0
                    _camController.setZoom(0);
                    setZoomLabel();
                    break;

                case R.id.zoom_max:
                    // set zoom to max
                    _camController.setZoom(_camController.camera().getParameters().getMaxZoom());
                    setZoomLabel();
                    break;

                case R.id.hide_overlay:
                    // setup a new preview without overlay
                    _camController.stopPreview();
                    _overlayMode = OVERLAY_MODE.HIDE_OVERLAY;
                    setupLayout(false);
                    break;

                case R.id.show_overlay:
                    // setup a new preview with overlay
                    _camController.stopPreview();
                    _overlayMode = OVERLAY_MODE.SHOW_OVERLAY;
                    setupLayout(true);
                    break;

                case R.id.close_app:
                    // close application
                    _camController.stopPreview();
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

        // build the view
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
        TextView tv = (TextView)findViewById(R.id.mode);
        tv.setText("Camera");
        tv = (TextView) findViewById(R.id.zoom);
        tv.setText("");
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
        _camController = new CamController(_capView);
        _capView.setSurfaceTextureListener(_camController);
    }

    /**
     * setup the app layout
     * @param overlay if true, shows the overlay
     */
    private void setupLayout(boolean overlay) {
        if (overlay) {
            // use the overlay
            CardBuilder cb =  new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE);
            cb.setEmbeddedLayout(R.layout.preview_layout);
            setContentView(cb.getView());

            // setup the preview
            _capView = (TextureView)findViewById(R.id.preview);
            setupLayoutInternal();
        }
        else {
            // no overlay, just use a new TextureView
            _capView = new TextureView(this);
            setupLayoutInternal();
            setContentView(_capView);
        }
    }
}
