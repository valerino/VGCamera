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
 * shows different options as cards (activated by single-tapping on the taken view) to handle taken image/video
 * Created by valerino on 13/09/15.
 */
public class TakenScroller extends Activity {
    private CardScrollView _view = null;

    /**
     * these are the ids of the cards
     */
    public static final int CHOICE_SAVE = 0;
    public static final int CHOICE_DISCARD = 1;
    public static final int CHOICE_SHARE = 2;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // create the adapter
        ScrollerAdapter adapter = new ScrollerAdapter();

        // create each card
        adapter.cards().add(new CardBuilder(this, CardBuilder.Layout.MENU).setText("Save").setFootnote("Save taken photo/video"));
        adapter.cards().add(new CardBuilder(this, CardBuilder.Layout.MENU).setText("Discard").setFootnote("Discard taken photo/video"));
        adapter.cards().add(new CardBuilder(this, CardBuilder.Layout.MENU).setText("Share").setFootnote("Share taken photo/video"));

        // setup the view
        _view  = new CardScrollView(this);
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
                    case CHOICE_SAVE:
                        // save media
                        resIntent.putExtra("choice", R.id.save);
                        setResult(RESULT_OK, resIntent);
                        break;
                    case CHOICE_DISCARD:
                        // save media
                        resIntent.putExtra("choice", R.id.discard);
                        setResult(RESULT_OK, resIntent);
                        break;
                    case CHOICE_SHARE:
                        // share media
                        resIntent.putExtra("choice", R.id.share);
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
