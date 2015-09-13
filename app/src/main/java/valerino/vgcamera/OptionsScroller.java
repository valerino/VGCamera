package valerino.vgcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

/**
 * shows different options as cards (activated by single-tapping on the preview view)
 * Created by valerino on 13/09/15.
 */
public class OptionsScroller extends Activity {
    private List<CardBuilder> _cards = new ArrayList<CardBuilder>();
    private CardScrollView _view;

    /**
     * these are the ids of the cards
     */
    public static final int CHOICE_TOGGLE_OVERLAY = 0;
    public static final int CHOICE_MAX_ZOOM = 1;
    public static final int CHOICE_RESET_ZOOM = 2;
    public static final int CHOICE_EXIT = 3;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // create each card
        _cards.add(new CardBuilder(this, CardBuilder.Layout.MENU).setText("Toggle Overlay").setFootnote("Toggle overlay on/off"));
        _cards.add(new CardBuilder(this, CardBuilder.Layout.MENU).setText("Zoom full").setFootnote("Apply maximum zoom"));
        _cards.add(new CardBuilder(this, CardBuilder.Layout.MENU).setText("Reset zoom").setFootnote("Reset zoom to 1x"));
        _cards.add(new CardBuilder(this, CardBuilder.Layout.MENU).setText("Exit").setFootnote("Bye bye :("));

        // setup the view
        _view  = new CardScrollView(this);
        _view.setAdapter(new optionsScrollAdapter());
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
                    case CHOICE_EXIT:
                        // exit app
                        resIntent.putExtra("choice", CHOICE_EXIT);
                        setResult(RESULT_OK, resIntent);
                        break;
                    case CHOICE_TOGGLE_OVERLAY:
                        // toggle overlay on/off
                        resIntent.putExtra("choice", CHOICE_TOGGLE_OVERLAY);
                        setResult(RESULT_OK, resIntent);
                        break;
                    case CHOICE_MAX_ZOOM:
                        // set max zoom
                        resIntent.putExtra("choice", CHOICE_MAX_ZOOM);
                        setResult(RESULT_OK, resIntent);
                        break;
                    case CHOICE_RESET_ZOOM:
                        // reset zoom
                        resIntent.putExtra("choice", CHOICE_RESET_ZOOM);
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
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * manages the scroller view
     */
    private class optionsScrollAdapter extends CardScrollAdapter {

        @Override
        public int getCount() {
            return _cards.size();
        }

        @Override
        public Object getItem(int i) {
            return _cards.get(i);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            return _cards.get(i).getView(view, viewGroup);
        }

        @Override
        public int getItemViewType(int i){
            return _cards.get(i).getItemViewType();
        }

        @Override
        public int getViewTypeCount() {
            return CardBuilder.getViewTypeCount();
        }

        @Override
        public int getPosition(Object o) {
            return _cards.indexOf(o);
        }
    }
}
