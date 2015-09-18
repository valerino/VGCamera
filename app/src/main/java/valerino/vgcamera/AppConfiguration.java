package valerino.vgcamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

/**
 * holds app configuration (singleton)
 * Created by valerino on 13/09/15.
 */
public class AppConfiguration {
    private static AppConfiguration _instance = null;

    /**
     * overlay mode on/off enum
     */
    public enum OVERLAY_MODE {
        SHOW_OVERLAY,
        HIDE_OVERLAY
    }
    private OVERLAY_MODE _overlayMode;

    private boolean _maxZoomMode;

    private boolean _smoothZoom;

    private boolean _autoSave;

    private boolean _geoTagging;

    private QUALITY _quality;

    private File _storageFolder;

    SharedPreferences _sharedPrefs;
    SharedPreferences.Editor _editor;
    Context _context;

    private final static String PREFS_GEOTAGGING = "geotagging";
    private final static String PREFS_AUTOSAVE = "autosave";
    private final static String PREFS_OVERLAY = "overlay";
    private final static String PREFS_SMOOTH_ZOOM = "smooth_zoom";
    private final static String PREFS_MAX_ZOOM = "max_zoom_mode";
    private final static String PREFS_QUALITY = "quality";

    public enum QUALITY {
        HIGH,
        LOW
    }

    /**
     * constructor (use instance())
     * Context ctx a Context
     */
    protected AppConfiguration(Context ctx) {
        _context = ctx;
        _sharedPrefs = _context.getSharedPreferences("VGCamera", 0);
        _editor = _sharedPrefs.edit();

        // initialize values
        _geoTagging = _sharedPrefs.getBoolean(PREFS_GEOTAGGING, false);
        _autoSave = _sharedPrefs.getBoolean(PREFS_AUTOSAVE, false);
        _overlayMode = OVERLAY_MODE.valueOf(_sharedPrefs.getString(PREFS_OVERLAY, OVERLAY_MODE.SHOW_OVERLAY.toString()));
        _smoothZoom = _sharedPrefs.getBoolean(PREFS_SMOOTH_ZOOM, false);
        _maxZoomMode = _sharedPrefs.getBoolean(PREFS_MAX_ZOOM, false);
        _quality = QUALITY.valueOf(_sharedPrefs.getString(PREFS_QUALITY, QUALITY.HIGH.toString()));

        // this is the storage folder (hardcoded too)
        _storageFolder = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        //_storageFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    }

    /**
     * get the singleton instance
     * @return
     */
    public static AppConfiguration instance(Context ctx) {
        if (_instance == null) {
            _instance = new AppConfiguration(ctx);
        }
        return _instance;
    }

    /**
     * get the max-zoom mode status. Note that maxzoom mode do not get saved, it's only runtime
     * @return
     */
    public boolean maxZoomMode() {
        return _maxZoomMode;
    }

    /**
     * enable max-zoom mode
     * @param enabled true to enable
     */
    public void setMaxZoomMode(boolean enabled) {
        _maxZoomMode = enabled;
        _editor.putBoolean(PREFS_MAX_ZOOM, enabled);
        _editor.commit();
    }

    /**
     * get the smooth zoom status
     * @return
     */
    public boolean smoothZoom() {
        return _smoothZoom;
    }

    /**
     * enable zooming smoothly
     * @param enabled true to enable
     */
    public void setSmoothZoom(boolean enabled) {
        _smoothZoom = enabled;
        _editor.putBoolean(PREFS_SMOOTH_ZOOM, enabled);
        _editor.commit();
    }

    /**
     * the storage folder for the taken media
     * @return
     */
    public File storageFolder() {
        return _storageFolder;
    }

    /**
     * get the overlay mode
     * @return
     */
    public OVERLAY_MODE overlayMode() {
        return _overlayMode;
    }

    /**
     * set the overlay mode on/off
     * @param mode SHOW_OVERLAY or HIDE_OVERLAY
     */
    public void setOverlayMode(OVERLAY_MODE mode) {
        _overlayMode = mode;

        // update prefs
        _editor.putString(PREFS_OVERLAY, mode.toString());
        _editor.commit();
    }

    /**
     * get the output quality
     * @return
     */
    public QUALITY quality() {
        return _quality;
    }

    /**
     * set the output quality (for pictures and video)
     * @param quality HIGH or LOW
     */
    public void setQuality(QUALITY quality) {
        _quality = quality;

        // update prefs
        _editor.putString(PREFS_QUALITY, quality.toString());
        _editor.commit();
    }

    /**
     * sets whether pictures/videos must have location added
     * @param enable true to enable
     */
    public void setGeotagging(boolean enable) {
        _geoTagging = enable;

        // update prefs
        _editor.putBoolean(PREFS_GEOTAGGING, enable);
        _editor.commit();
    }

    /**
     * returns whether pictures/videos must have location added
     * @return
     */
    boolean geoTagging() {
        return _geoTagging;
    }

    /**
     * sets whether pictures/videos must be autosaved
     * @param enable true to enable
     */
    public void setAutoSave(boolean enable) {
        _autoSave = enable;

        // update prefs
        _editor.putBoolean(PREFS_AUTOSAVE, enable);
        _editor.commit();
    }

    /**
     * returns whether pictures/videos must be autosaved
     * @return
     */
    boolean autoSave() {
        return _autoSave;
    }
}
