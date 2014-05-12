package com.reindeercrafts.notificationpeek.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import com.reindeercrafts.notificationpeek.R;

/**
 * Created by zhelu on 5/12/14.
 */
public class HelpDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new DialogHelper.Builder(getActivity())
                .createHelpDialog()
                .setPositiveButton(R.string.close, null)
                .create();
    }
}
