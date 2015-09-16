package valerino.vgcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;

/**
 * shows different options as cards (activated by single-tapping on the preview view) to configure the cam
 * Created by valerino on 13/09/15.
 */
public class OptionsScroller extends Activity {
    private CardScrollView _view = null;

    /**
     * these are the ids of the cards
     */
    public static final int CHOICE_TOGGLE_OVERLAY = 0;
    public static final int CHOICE_TOGGLE_LOCATION = 1;
    public static final int CHOICE_TOGGLE_AUTOSAVE = 2;
    public static final int CHOICE_TOGGLE_MAXZOOM = 3;
    public static final int CHOICE_TOGGLE_SMOOTHZOOM = 4;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // create the adapter
        ScrollerAdapter adapter = new ScrollerAdapter();

        // create each card
        final String on = "ON";
        final String off = "OFF";
        String s = "Overlay " + (AppConfiguration.instance(this).overlayMode() == AppConfiguration.OVERLAY_MODE.SHOW_OVERLAY ? off : on);
        adapter.cards().add(new CardBuilder(this, CardBuilder.Layout.MENU).setText(s).setFootnote("Toggle overlay on/off"));
        s = "Geotagging " + (AppConfiguration.instance(this).addLocation() ? off : on);
        adapter.cards().add(new CardBuilder(this, CardBuilder.Layout.MENU).setText(s).setFootnote("Toggle geotagging on/off"));
        s = "Autosave " + (AppConfiguration.instance(this).autoSave() ? off : on);
        adapter.cards().add(new CardBuilder(this, CardBuilder.Layout.MENU).setText(s).setFootnote("Toggle autosave on/off"));
        s = "Max-Zoom  " + (AppConfiguration.instance(this).maxZoomMode() ? off : on);
        adapter.cards().add(new CardBuilder(this, CardBuilder.Layout.MENU).setText(s).setFootnote("Toggle max zoom on/off"));
        s = "Smooth-Zoom  " + (AppConfiguration.instance(this).smoothZoom() ? off : on);
        adapter.cards().add(new CardBuilder(this, CardBuilder.Layout.MENU).setText(s).setFootnote("Toggle smooth zoom on/off"));

        // setup the view
        _view = new CardScrollView(this);
        _view.setAdapter(adapter);
        _view.activate();
        setContentView(_view);
        setOnClickListener();
    }

    private void setOnClickListener() {
        _view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // play sound
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.TAP);

                // react to click
                Intent resIntent = getIntent();
                switch (i) {
                    case CHOICE_TOGGLE_OVERLAY:
                        // toggle overlay
                        resIntent.putExtra("choice", R.id.toggle_overlay);
                        setResult(RESULT_OK, resIntent);
                        break;
                    case CHOICE_TOGGLE_MAXZOOM:
                        // toggle maxzoom
                        resIntent.putExtra("choice", R.id.zoom_toggle_max);
                        setResult(RESULT_OK, resIntent);
                        break;
                    case CHOICE_TOGGLE_SMOOTHZOOM:
                        // toggle smooth zoom
                        resIntent.putExtra("choice", R.id.zoom_toggle_smooth);
                        setResult(RESULT_OK, resIntent);
                        break;
                    case CHOICE_TOGGLE_AUTOSAVE:
                        // toggle autosave
                        resIntent.putExtra("choice", R.id.toggle_autosave);
                        setResult(RESULT_OK, resIntent);
                        break;
                    case CHOICE_TOGGLE_LOCATION:
                        // toggle geotagging
                        resIntent.putExtra("choice", R.id.toggle_location);
                        setResult(RESULT_OK, resIntent);
                        break;
                    default:
                        setResult(RESULT_CANCELED, resIntent);
                        break;
                }
                finish();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            // camera is inhibited here
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}