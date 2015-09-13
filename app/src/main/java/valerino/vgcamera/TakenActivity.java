package valerino.vgcamera;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.google.android.glass.view.WindowUtils;

/**
 * shows the taken image/video
 * Created by valerino on 13/09/15.
 */
public class TakenActivity extends Activity {
    private ImageView _imgView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.taken_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            switch (item.getItemId()) {
                case R.id.share_with:
                    // share the image/video
                    break;

                case R.id.delete_this:
                    // delete the taken image/video
                    break;

                case R.id.back:
                    // get back to the parent activity
                    finish();
                    break;

                default:
                    return true;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            getMenuInflater().inflate(R.menu.taken_menu, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // ask for 'ok glass' prompt to accept commands
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // set the taken image as view
        _imgView = new ImageView(this);
        setContentView(_imgView);
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
