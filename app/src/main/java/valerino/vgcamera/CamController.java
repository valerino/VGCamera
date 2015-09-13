package valerino.vgcamera;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.TextureView;

import java.io.IOException;
import java.util.List;

/**
 * controls the camera, singleton
 * Created by valerino on 13/09/15.
 */
public class CamController implements TextureView.SurfaceTextureListener {
    private Camera _camera;
    private TextureView _view;
    private Context _context;
    private static CamController _instance = null;

    /**
     * constructor (use instance())
     *
     * @param ctx a Context
     */
    protected CamController(Context ctx) {
        _context = ctx;
    }

    /**
     * get the singleton
     *
     * @param ctx a Context
     * @return
     */
    public static CamController instance(Context ctx) {
        if (_instance == null) {
            // build the new instance
            _instance = new CamController(ctx);
        }
        return _instance;
    }

    /**
     * set the TextureView to blit on
     *
     * @param view the TextureView
     */
    public void setView(TextureView view) {
        _view = view;
    }


    /**
     * get the TextureView
     *
     * @return
     */
    public TextureView view() {
        return _view;
    }

    /**
     * get the camera object
     *
     * @return
     */
    public Camera camera() {
        return _camera;
    }

    /**
     * start the preview
     */
    public void startPreview() {
        // release camera if it's already
        if (_camera != null) {
            _camera.release();
        }
        _camera = Camera.open();
        try {
            _camera.setPreviewTexture(_view.getSurfaceTexture());
        } catch (IOException e) {
            // error!
            Log.e(this.getClass().getName(), "setPreviewTexture()", e);
            return;
        }
        _camera.startPreview();
    }

    /**
     * stop previewing
     */
    public void stopPreview() {
        if (_camera == null) {
            return;
        }

        // stop the preview
        _camera.stopPreview();
        try {
            _camera.setPreviewDisplay(null);
            _camera.setPreviewTexture(null);
        } catch (IOException e) {
            // error!
            Log.e(this.getClass().getName(), "setPreviewDisplay/setPreviewTexture()", e);
        }

        // release the camera
        _camera.release();
        _camera = null;
    }

    /**
     * set the camera zoom
     *
     * @param zoomFactor a zoom factor (must be < camera.zoomMax())
     */
    public void setZoom(int zoomFactor) {
        // get parameters and current zoom factor
        Camera.Parameters params = _camera.getParameters();
        if (zoomFactor <= params.getMaxZoom() && zoomFactor >= 0) {
            // set the new zoom factor
            params.setZoom(zoomFactor);
        }

        // set back parameters
        _camera.setParameters(params);
    }

    /**
     * zoom the image IN
     */
    public void zoomIn() {
        // get parameters and current cam_menu factor
        Camera.Parameters params = _camera.getParameters();
        int zoom = params.getZoom();

        // set new zoom factor
        zoom += 10;
        setZoom(zoom);
    }

    /**
     * zoom the image OUT
     */
    public void zoomOut() {
        // get parameters and current cam_menu factor
        Camera.Parameters params = _camera.getParameters();
        int zoom = params.getZoom();

        // set new zoom factor
        zoom -= 10;
        setZoom(zoom);
    }

    /**
     * get location (uses paired device)
     * @return Location or null if it's not able to get a fix
     */
    private Location getLocation() {
        Location result = null;
        LocationManager locationManager;
        Criteria locationCriteria;
        List<String> providers;

        locationManager = (LocationManager)_context.getSystemService(Context.LOCATION_SERVICE);
        locationCriteria = new Criteria();
        locationCriteria.setAccuracy(Criteria.NO_REQUIREMENT);
        providers = locationManager.getProviders(locationCriteria, true);

        // get location with best accuracy
        for (String provider : providers) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (result == null) {
                result = location;
            }
            else if (result.getAccuracy() == 0.0) {
                if (location.getAccuracy() != 0.0) {
                    result = location;
                    break;
                } else {
                    if (result.getAccuracy() > location.getAccuracy()) {
                        result = location;
                    }
                }
            }
        }

        return result;
    }

    /**
     * takes a picture
     */
    public void snapPicture() {
        // take the picture
        if (AppConfiguration.instance(_context).addLocation()) {
            // get the last location first
            Location loc = getLocation();
            if (loc != null) {
                Camera.Parameters params = _camera.getParameters();
                params.setGpsAltitude(loc.getAltitude());
                params.setGpsLongitude(loc.getLongitude());
                params.setGpsLatitude(loc.getLatitude());
                params.setGpsTimestamp(loc.getTime());
                params.setGpsProcessingMethod(loc.getProvider());
            }
            else {
                Log.w(this.getClass().getName(), "Can't get location");
            }
        }
        _camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                // show the taken picture
                Intent intent = new Intent(_context, TakenActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("data", bytes);
                _context.startActivity(intent);
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        startPreview();
        android.hardware.Camera.Parameters params = _camera.getParameters();
        if (AppConfiguration.instance(_context).maxZoomMode()) {
            // set zoom to max
            int max_zoom = params.getMaxZoom();
            setZoom(max_zoom);
        } else {
            // no zoom
            setZoom(0);
        }

        // set the zoom label
        MainActivity hostActivity = (MainActivity) _view.getContext();
        hostActivity.setOverlayLabels();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        stopPreview();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }
}
