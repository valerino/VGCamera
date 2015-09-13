package valerino.vgcamera;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.TextureView;

import java.io.IOException;

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
     * cam_menu the image IN
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
     * cam_menu the image OUT
     */
    public void zoomOut() {
        // get parameters and current cam_menu factor
        Camera.Parameters params = _camera.getParameters();
        int zoom = params.getZoom();

        // set new zoom factor
        zoom -= 10;
        setZoom(zoom);
    }
/*
    private File getDir() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Test");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        Log.d("Gauss", "Saving to: " + mediaFile.getAbsolutePath());
        return mediaFile;
    }
*/

    /**
     * takes a picture
     */
    public void snapPicture() {
        // take the picture
        _camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                // TODO: save picture

                // show the taken picture
                Intent intent = new Intent(_context, TakenActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                _context.startActivity(intent);
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        startPreview();
        android.hardware.Camera.Parameters params = _camera.getParameters();
        if (AppConfiguration.instance().maxZoomMode()) {
            // set zoom to max
            int max_zoom = params.getMaxZoom();
            setZoom(max_zoom);
        } else {
            // no zoom
            setZoom(0);
        }

        // set the zoom label
        MainActivity hostActivity = (MainActivity) _view.getContext();
        hostActivity.setZoomLabel();
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
