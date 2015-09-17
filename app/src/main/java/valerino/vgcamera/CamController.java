package valerino.vgcamera;

import android.content.Context;
import android.hardware.Camera;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * controls the camera, singleton
 * Created by valerino on 13/09/15.
 */
public class CamController implements Camera.OnZoomChangeListener, SurfaceHolder.Callback {
    private Camera _camera = null;
    private SurfaceView _surfaceView = null;
    private Context _context = null;
    private static CamController _instance = null;
    private int _savedZoom = 0;
    private MediaRecorder _mediaRecorder = null;
    private File _tmpVideo = null;

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
     * set the SurfaceView to blit on
     *
     * @param view the SurfaceView
     */
    public void setSurfaceView(SurfaceView view) {
        _surfaceView = view;
    }

    /**
     * get the SurfaceView
     *
     * @return
     */
    public SurfaceView surfaceView() {
        return _surfaceView;
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
            _camera.setPreviewDisplay(_surfaceView.getHolder());
        } catch (IOException e) {
            // error!
            Log.e(this.getClass().getName(), "setPreviewDisplay(), _surfaceView=" + _surfaceView, e);
            return;
        }

        // update camera parameters
        Camera.Parameters params = _camera.getParameters();
        params.setPreviewFpsRange(30000, 30000);
        params.setPreviewSize(640, 360);
        params.setZoom(_savedZoom);
        _camera.setZoomChangeListener(this);
        _camera.setParameters(params);

        // start preview
        _camera.startPreview();

        // restore any previous zoom set
        if (AppConfiguration.instance(_context).maxZoomMode()) {
            // zoom to max
            setMaxZoom();
        }
        else {
            // restore
            setZoom(_savedZoom);
        }

        // update zoom label if needed
        MainActivity hostActivity = (MainActivity)_context;
        hostActivity.updateZoomLabel();
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
     * reset zoom to 0
     */
    public void resetZoom() {
        // maxzoom mode disabled
        AppConfiguration.instance(_context).setMaxZoomMode(false);

        // reset zoom to 0
        setZoom(0);
    }

    /**
     * set the camera zoom
     *
     * @param zoomFactor a zoom factor (must be <= camera.zoomMax())
     */
    public void setZoom(int zoomFactor) {
        if (_camera == null) {
            Log.w(this.getClass().getName(), "camera not yet initialized");
            // will be set on surfaceCreated()
            _savedZoom = zoomFactor;
            return;
        }

        // get parameters and current zoom factor
        Camera.Parameters params = _camera.getParameters();
        if (zoomFactor <= params.getMaxZoom() && zoomFactor >= 0) {
            // set the new zoom factor
            if (AppConfiguration.instance(_context).smoothZoom()) {
                // zoom smoothly
                try {
                    _camera.startSmoothZoom(zoomFactor);
                }
                catch (Throwable ex) {
                    Log.e(this.getClass().getName(), "can't smooth-zoom");
                }
            }
            else {
                // zoom normally
                params.setZoom(zoomFactor);

                // set back parameters
                _camera.setParameters(params);
            }
        }
        if (!AppConfiguration.instance(_context).smoothZoom()) {
            // manually update zoom label (either, it would be done in OnZoomChangeListener())
            updateZoomLabelHost(_camera);
        }
    }

    /**
     * set zoom to max
     */
    public void setMaxZoom() {
        if (_camera == null) {
            Log.w(this.getClass().getName(), "camera not yet initialized");
            return;
        }

        Camera.Parameters params = _camera.getParameters();
        setZoom(params.getMaxZoom());
    }

    /**
     * toggle max zoom on/off
     */
    public void toggleMaxZoom() {
        if (AppConfiguration.instance(_context).maxZoomMode()) {
            // zoom to max
            setMaxZoom();
        } else {
            // no zoom
            setZoom(0);
        }
    }

    /**
     * zoom the image IN
     */
    public void zoomIn() {
        if (_camera == null) {
            Log.w(this.getClass().getName(), "camera not yet initialized");
            return;
        }

        // maxzoom mode disabled
        AppConfiguration.instance(_context).setMaxZoomMode(false);

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
        if (_camera == null) {
            Log.w(this.getClass().getName(), "camera not yet initialized");
            return;
        }

        // maxzoom mode disabled
        AppConfiguration.instance(_context).setMaxZoomMode(false);

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
     * starts the videorecorder
     * @return boolean true if start is successful
     */
    public boolean startRecorder() {
        if (_camera == null) {
            Log.w(this.getClass().getName(), "camera not yet initialized");
            return false;
        }
        // initialize a mediarecorder
        _mediaRecorder = new MediaRecorder();

        // set quality
        CamcorderProfile profile;
        if (AppConfiguration.instance(_context).quality() == AppConfiguration.QUALITY.HIGH) {
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        }
        else {
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        }
        _mediaRecorder.setCamera(_camera);
        _camera.stopPreview();
        _camera.unlock();
        _mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        _mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        _mediaRecorder.setProfile(profile);

        // set display
        _mediaRecorder.setPreviewDisplay(_surfaceView.getHolder().getSurface());
        if (AppConfiguration.instance(_context).addLocation()) {
            // get the last location first
            Location loc = getLocation();
            if (loc != null) {
                _mediaRecorder.setLocation((float) loc.getLatitude(), (float) loc.getLongitude());
            }
        }

        // generate filename
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        _tmpVideo = new File(Environment.getExternalStorageDirectory(), timeStamp + ".mp4");
        _mediaRecorder.setOutputFile(_tmpVideo.getAbsolutePath());
        try {
            _mediaRecorder.prepare();
        } catch (IOException e) {
            Log.d(this.getClass().getName(), "mediaRecorder.prepare()", e);
            stopRecorder(true);
            return false;
        }
        _mediaRecorder.start();
        return true;
    }

    /**
     * stops the videorecorder
     * @param deleteFile true to delete the captured file
     * @return path to the temporary file
     */
    public File stopRecorder(boolean deleteFile) {
        if (_mediaRecorder == null) {
            return null;
        }
        // release the recorder's resources and restart the normal preview
        _mediaRecorder.stop();
        _mediaRecorder.release();
        _mediaRecorder = null;
        startPreview();

        if (_tmpVideo != null) {
            if (deleteFile) {
                // just delete the captured file
                _tmpVideo.delete();
                _tmpVideo = null;
                return null;
            }
        }

        // and return the captured media
        File f = _tmpVideo;
        _tmpVideo = null;
        return f;
    }

    /**
     * takes a picture
     * @return path to the temporary file created (the taken picture)
     */
    public File snapPicture() {
        if (_camera == null) {
            Log.w(this.getClass().getName(), "camera not yet initialized");
            return null;
        }

        // prepare the camera
        Camera.Parameters params = _camera.getParameters();
        if (AppConfiguration.instance(_context).quality() == AppConfiguration.QUALITY.HIGH) {
            // high quality
            params.setPictureSize(2592, 1944);
        }
        else {
            // low quality
            params.setPictureSize(1296, 972);
        }

        if (AppConfiguration.instance(_context).addLocation()) {
            // get the last location first
            Location loc = getLocation();
            if (loc != null) {
                // update camera parameters
                params.setGpsAltitude(loc.getAltitude());
                params.setGpsLongitude(loc.getLongitude());
                params.setGpsLatitude(loc.getLatitude());
                params.setGpsTimestamp(loc.getTime());
                params.setGpsProcessingMethod(loc.getProvider());
            }
            else {
                Log.w(this.getClass().getName(), "can't get location");
            }
        }
        _camera.setParameters(params);

        // take the picture
        final Object obj = new Object();
        final File[] tmpImage = {null};
        _camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                // stop the preview
                _camera.stopPreview();

                // save a temporary image
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
                File f = new File(Environment.getExternalStorageDirectory(), timeStamp + ".jpg");
                f = Utils.bufferToFile(bytes, f.getAbsolutePath());
                tmpImage[0] = f;
                if (f == null) {
                    Log.e(this.getClass().getName(), "can't create temporary file for picture");
                }
                // notify
                synchronized (obj) {
                    obj.notify();
                }
            }
        });

        // wait for the callback
        synchronized (obj) {
            try {
                obj.wait();
            } catch (InterruptedException e) {

            }
        }

        // and return path to the captured image
        return tmpImage[0];
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // start the preview as soon as we have the surface
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // stop the preview once the surface is destroyed (on close)
        stopPreview();
    }

    /**
     * update the host zoom label (in MainActivity)
     */
    private void updateZoomLabelHost(Camera camera) {
        try {
            // update the zoom label
            MainActivity hostActivity = (MainActivity) _surfaceView.getContext();
            hostActivity.updateZoomLabel();

            // always save the current zoom factor
            _savedZoom = camera.getParameters().getZoom();
        }
        catch (Throwable ex) {
            // camera may have been released
        }

    }
    @Override
    public void onZoomChange(int i, boolean b, Camera camera) {
        // update the zoom label
        updateZoomLabelHost(camera);
    }
}
