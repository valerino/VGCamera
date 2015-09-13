package valerino.vgcamera;

/**
 * holds app configuration (singleton)
 * Created by valerino on 13/09/15.
 */
public class AppConfiguration {
    private static AppConfiguration _instance = null;

    /**
     * overlay mode on/off enum
     */
    public  enum OVERLAY_MODE {
        SHOW_OVERLAY,
        HIDE_OVERLAY
    }
    private OVERLAY_MODE _overlayMode;

    private boolean _maxZoomMode;

    /**
     * constructor (use instance())
     */
    protected AppConfiguration() {

    }

    /**
     * get the singleton instance
     * @return
     */
    public static AppConfiguration instance () {
        if (_instance == null) {
            _instance = new AppConfiguration();
        }
        return _instance;
    }

    /**
     * get the max-zoom mode status
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
    }
}
