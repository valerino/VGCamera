package valerino.vgcamera;

import android.view.View;
import android.view.ViewGroup;

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * adapter used for scrollers to show different options as cards
 * Created by valerino on 15/09/15.
 */
public class ScrollerAdapter extends CardScrollAdapter {

    /**
     * array of cards. they called the object CardBuilder, but its really a Card (which has been deprecated....)
     */
    private List<CardBuilder> _cards = new ArrayList<>();

    /**
     * the cards array
     */
    public List<CardBuilder> cards() {
        return _cards;
    }

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
