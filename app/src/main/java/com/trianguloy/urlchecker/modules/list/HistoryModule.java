package com.trianguloy.urlchecker.modules.list;

import android.app.AlertDialog;
import android.view.View;
import android.widget.ImageButton;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ConfigActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.DescriptionConfig;
import com.trianguloy.urlchecker.url.UrlData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This module keeps a list of previous urls, and allows to jump between them
 */
public class HistoryModule extends AModuleData {
    @Override
    public String getId() {
        return "history";
    }

    @Override
    public int getName() {
        return R.string.mHist_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new HistoryDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ConfigActivity cntx) {
        return new DescriptionConfig(R.string.mHist_desc);
    }
}

class HistoryDialog extends AModuleDialog implements View.OnClickListener {

    // views
    private ImageButton first;
    private ImageButton back;
    private ImageButton list;
    private ImageButton forward;
    private ImageButton last;

    private final List<String> history = new ArrayList<>(); // list of urls
    private int index = -1; // current url in history (-1 if none)

    // ------------------- internal -------------------

    public HistoryDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_history;
    }

    // ------------------- init -------------------

    @Override
    public void onInitialize(View views) {
        // get views
        first = views.findViewById(R.id.first);
        back = views.findViewById(R.id.back);
        list = views.findViewById(R.id.list);
        forward = views.findViewById(R.id.forward);
        last = views.findViewById(R.id.last);

        // set listeners
        first.setOnClickListener(this);
        back.setOnClickListener(this);
        list.setOnClickListener(this);
        forward.setOnClickListener(this);
        last.setOnClickListener(this);

        // update
        updateUI();
    }

    /**
     * updated the UI with the internal data (buttons visibility)
     */
    private void updateUI() {
        setEnabled(first, index > 0); // at least something to go back
        setEnabled(back, index > 0); // at least something to go back
        list.setEnabled(!history.isEmpty()); // at least something
        setEnabled(forward, index < history.size() - 1); // at least something to go forward
        setEnabled(last, index < history.size() - 1); // at least something to go forward
    }

    @Override
    public void onNewUrl(UrlData urlData) {
        // clear newer entries
        if (index + 1 < history.size())
            history.subList(index + 1, history.size()).clear();

        if (urlData.minorUpdate) {
            // minor update, replace previous entry
            history.set(index, urlData.url);
        } else {
            // add new entry
            history.add(urlData.url);
            index++;
        }

        // update
        updateUI();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.list) {
            // cleanup
            removeDuplicates(false);
            // prepare elements (inverted because they look nicer top=newer)
            List<String> items = new ArrayList<>(history);
            Collections.reverse(items);

            // show list
            new AlertDialog.Builder(getActivity())
                    .setSingleChoiceItems(
                            items.toArray(new CharSequence[0]),
                            index == -1 ? -1 : items.size() - 1 - index,
                            (dialog, which) -> {
                                setIndex(history.size() - 1 - which);
                                dialog.dismiss();
                            })
                    .show();
            updateUI();
            return;
        }

        // partial cleanup
        removeDuplicates(true);
        switch (v.getId()) {
            case R.id.first:
                setIndex(0);
                break;
            case R.id.back:
                setIndex(index - 1);
                break;
            case R.id.forward:
                setIndex(index + 1);
                break;
            case R.id.last:
                setIndex(history.size() - 1);
                break;
        }
    }

    /**
     * Removes duplicated entries, also empty ones
     */
    private void removeDuplicates(boolean continuous) {
        for (int i = 0; i < history.size(); ) {
            String entry = history.get(i);
            int replaceIndex = i;
            if (continuous) {
                // check if next element is the same
                if (i + 1 < history.size() && history.get(i + 1).equals(entry)) {
                    replaceIndex = i + 1;
                }
            } else {
                // check if there is another later same element
                replaceIndex = history.lastIndexOf(entry);
            }
            if (entry.isEmpty() && index != i) {
                // remove empty, unless it is current index
                replaceIndex = index;
            }

            if (replaceIndex != i) {
                // remove this and replace
                if (index == i) index = replaceIndex;
                history.remove(i);
                if (index > i) index--;
            } else {
                // keep and continue
                i++;
            }
        }
    }

    /**
     * Sets a new index. If invalid, it is unchanged.
     * Updates UI too
     *
     * @param newIndex new index to set
     */
    private void setIndex(int newIndex) {
        if (newIndex >= 0 && newIndex < history.size()) {
            index = newIndex;
            setUrl(
                    new UrlData(history.get(newIndex))
                            .disableUpdates()
                            .dontTriggerOwn()
            );
        }
        updateUI();
    }

    /**
     * For some reason some drawable buttons are displayed the same when enabled and disabled.
     * This method also sets an alpha as a workaround
     *
     * @param view    view to enable/disable
     * @param enabled new state
     */
    private void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.35f);
    }
}