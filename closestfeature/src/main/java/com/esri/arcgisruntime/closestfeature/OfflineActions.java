package com.esri.arcgisruntime.closestfeature;

/**
 * Created by mani8177 on 7/23/16.
 */

import android.content.Context;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuItem;

public class OfflineActions implements Callback {

    private static final int MENU_DISCARD = 1;

    Context mContext;

    public OfflineActions(final MainActivity activity) {
        mContext = activity;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case MENU_DISCARD:
                ((MainActivity) mContext).clear();
                break;

            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuItem item;
        item = menu.add(Menu.NONE, MENU_DISCARD, 1, "save");
        item.setIcon(R.drawable.ic_action_content_discard);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        ((MainActivity) mContext).save();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

}