package valerino.vgcamera;

import android.content.Context;
import android.hardware.Camera;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * controls the camera, singleton
 * Created by valerino on 13/09/15.
 */
public class CamController implements SurfaceHolder.Callback {
    private Camera _camera = null;
    private SurfaceView _surfaceView = null;
    private Context _context = null;
    private static CamController _instance = null;
    private int _savedZoom = 0;
    private MediaRecorder _mediaRecorder = null;
    private File _tmpVideo = null;
    private CAM_MODE _mode = CAM_MODE.MODE_PHOTO;
    private Camera.OnZoomChangeListener _zoomListener = null;

    public enum CAM_MODE {
        MODE_VIDEO,
        MODE_PHOTO
    }

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
     * set the onzoomchange listener to update the UI
     * @param listener an OnZoomChangeListener
     */
    public void setOnZoomChangeListener (Camera.OnZoomChangeListener listener) {
        _zoomListener = listener;
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
     * get the cam mode (video/photo)
     */
    public CAM_MODE mode() {
        return _mode;
    }

    /**
     * start the preview
     */
    synchronized public void startPreview() {
        Log.d(this.getClass().getName(), "start previewing");

        // release camera if it's already
        if (_camera != null) {
            _camera.release();
        }
        try {
            _camera = Camera.open();
            _camera.setPreviewDisplay(_surfaceView.getHolder());
        } catch (Throwable e) {
            // error!
            Log.e(this.getClass().getName(), "Camera.open() / setPreviewDisplay(), _surfaceView=" + _surfaceView, e);
            return;
        }

        // configure camera
        Camera.Parameters params = _camera.getParameters();
        params.setPreviewFpsRange(30000, 30000);
        params.setPreviewSize(640, 360);
        _camera.setZoomChangeListener(_zoomListener);
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
    }

    /**
     * stop previewing
     */
    synchronized public void stopPreview() {
        Log.d(this.getClass().getName(), "stop previewing");
        if (_camera == null) {
            return;
        }

        // stop the preview
        _camera.stopPreview();
        try {
            _camera.setPreviewDisplay(null);
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
     * @param zoomLevel a zoom factor (must be <= camera.zoomMax())
     */
    synchronized public void setZoom(final int zoomLevel) {
        // use an async task
        AsyncTask<Void, Void, Void> t = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (_camera == null) {
                    Log.w(this.getClass().getName(), "camera not yet initialized");
                    // will be set on surfaceCreated()
                    saveCurrentZoom(zoomLevel);
                    return null;
                }
                if (zoomLevel == _camera.getParameters().getZoom()) {
                    // we're already at the desired zoom level
                    return null;
                }

                // get parameters and current zoom level
                Camera.Parameters par = _camera.getParameters();
                if (zoomLevel <= par.getMaxZoom() && zoomLevel >= 0) {
                    // set the new zoom factor
                    if (AppConfiguration.instance(_context).smoothZoom()) {
                        // zoom smoothly
                        try {
                            _camera.startSmoothZoom(zoomLevel);
                        }
                        catch (Throwable ex) {
                            Log.e(this.getClass().getName(), "can't smooth-zoom");
                        }
                    }
                    else {
                        // zoom normally
                        par.setZoom(zoomLevel);
                        _camera.setParameters(par);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void params) {
                if (!AppConfiguration.instance(_context).smoothZoom()) {
                    // manually call the callback if smoothzoom is not enabled
                    Camera.Parameters par = _camera.getParameters();
                    _zoomListener.onZoomChange(par.getZoom(), false, _camera);
                }
            }
        };

        // run the task
        t.execute();
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
    public boolean camStartRecord() {
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

        /// setup source
        _mediaRecorder.setCamera(_camera);
        _camera.stopPreview();
        _camera.unlock();
        _mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        _mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        _mediaRecorder.setProfile(profile);

        // set display
        _mediaRecorder.setPreviewDisplay(_surfaceView.getHolder().getSurface());
        if (AppConfiguration.instance(_context).geoTagging()) {
            // get the last location first
            Location loc = getLocation();
            if (loc != null) {
                _mediaRecorder.setLocation((float) loc.getLatitude(), (float) loc.getLongitude());
            }
        }

        // set temp file
        _tmpVideo = Utils.getTempMediaFile(_context, CAM_MODE.MODE_VIDEO);
        _mediaRecorder.setOutputFile(_tmpVideo.getAbsolutePath());
        try {
            _mediaRecorder.prepare();
        } catch (IOException e) {
            Log.d(this.getClass().getName(), "mediaRecorder.prepare()", e);
            camStopRecord(true);
            return false;
        }

        // start
        _mediaRecorder.start();
        _mode = CAM_MODE.MODE_VIDEO;
        return true;
    }

    /**
     * stops the videorecorder
     * @param deleteFile true to delete the captured file
     * @return path to the temporary file
     */
    public File camStopRecord(boolean deleteFile) {
        if (_mediaRecorder == null) {
            return null;
        }
        // release the recorder's resources and restart the normal preview
        _mediaRecorder.stop();
        _mediaRecorder.reset();
        _mediaRecorder.release();
        _mediaRecorder = null;

        if (_tmpVideo != null) {
            if (deleteFile) {
                // just delete the captured file
                _tmpVideo.delete();
                _tmpVideo = null;
            }
        }

        // and return the captured media
        File f = _tmpVideo;
        _tmpVideo = null;
        _mode = CAM_MODE.MODE_PHOTO;
        return f;
    }

    /**
     * takes a picture
     * @return path to the temporary file created (the taken picture)
     */
    public File camTakePicture() {
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

        if (AppConfiguration.instance(_context).geoTagging()) {
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
                // save a temporary image
                File f = Utils.getTempMediaFile(_context, CAM_MODE.MODE_PHOTO);
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
        Log.d(this.getClass().getName(), "camTakePicture() returned " + tmpImage[0].getAbsolutePath());
        return tmpImage[0];
    }

    /**
     * save the current zoom, should be called by the ZoomChange callback
     * @param zoom
     */
    public void saveCurrentZoom(int zoom) {
        _savedZoom = zoom;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // start the preview as soon as we have the surface
        Log.d(this.getClass().getName(), "surfaceCreated()");
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // stop the preview once the surface is destroyed (on close)
        Log.d(this.getClass().getName(), "surfaceDestroyed()");
        stopPreview();
    }
}
