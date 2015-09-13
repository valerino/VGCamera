package valerino.vgcamera;

import android.content.Context;
import android.content.SharedPreferences;

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

    private boolean _autoSave;

    private boolean _addLocation;

    SharedPreferences _sharedPrefs;
    SharedPreferences.Editor _editor;
    Context _context;

    private final static String PREFS_ADD_LOCATION = "add_location";
    private final static String PREFS_AUTOSAVE = "autosave";
    private final static String PREFS_OVERLAY = "overlay";

    /**
     * constructor (use instance())
     * Context ctx a Context
     */
    protected AppConfiguration(Context ctx) {
        _context = ctx;
        _sharedPrefs = _context.getSharedPreferences("VGCamera", 0);
        _editor = _sharedPrefs.edit();

        // initialize values
        _addLocation = _sharedPrefs.getBoolean(PREFS_ADD_LOCATION, false);
        _autoSave = _sharedPrefs.getBoolean(PREFS_AUTOSAVE, false);
        _overlayMode = OVERLAY_MODE.valueOf(_sharedPrefs.getString(PREFS_OVERLAY, OVERLAY_MODE.SHOW_OVERLAY.toString()));
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
     * sets whether pictures/videos must have location added
     * @param enable true to enable
     */
    public void setAddLocation(boolean enable) {
        _addLocation = enable;

        // update prefs
        _editor.putBoolean(PREFS_ADD_LOCATION, enable);
        _editor.commit();
    }

    /**
     * returns whether pictures/videos must have location added
     * @return
     */
    boolean addLocation() {
        return _addLocation;
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
