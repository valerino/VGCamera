package valerino.vgcamera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.TextureView;

import java.io.IOException;

/**
 * controls the camera
 * Created by valerino on 13/09/15.
 */
public class CamController implements TextureView.SurfaceTextureListener {
    private Camera _camera;
    private TextureView _view;

    /**
     * constructor
     * @param view the view we're blitting the preview on
     */
    public CamController(TextureView view) {
        _view = view;
    }

    /**
     * get the camera object
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
            _camera.setPreviewTexture (_view.getSurfaceTexture());
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
        zoom+=10;
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
        zoom-=10;
        setZoom (zoom);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        startPreview();
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
