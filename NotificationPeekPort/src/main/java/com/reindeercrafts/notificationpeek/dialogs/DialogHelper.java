package com.reindeercrafts.notificationpeek.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reindeercrafts.notificationpeek.R;

/**
 * Created by zhelu on 5/12/14.
 */
public class DialogHelper {

    public static final String ABOUT_DIALOG_TAG = "NotificationPeekPort.AboutDialog";

    public static void showAboutDialog(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ((DialogFragment) Fragment
                .instantiate(activity, AboutDialogFragment.class.getName()))
                .show(ft, ABOUT_DIALOG_TAG);
    }

    public static void showHelpDialog(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ((DialogFragment) Fragment
                .instantiate(activity, HelpDialogFragment.class.getName()))
                .show(ft, ABOUT_DIALOG_TAG);
    }


    public static class Builder {

        private static final String VERSION_UNAVAILABLE = "N/A";
        protected final Context mContext;

        private Drawable mIcon;

        public Builder(Context context) {
            mContext = context;
            mIcon = context.getResources().getDrawable(R.drawable.ic_launcher);
        }

        private CharSequence getVersionTitle() {
            PackageManager pm = mContext.getPackageManager();
            String packageName = mContext.getPackageName();
            String versionName;
            try {
                PackageInfo info = pm.getPackageInfo(packageName, 0);
                versionName = info.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                versionName = VERSION_UNAVAILABLE;
            }

            Resources res = mContext.getResources();
            return Html.fromHtml(
                    res.getString(R.string.about_title, res.getString(R.string.app_name),
                            versionName));
        }

        private CharSequence getAboutMessage() {
            return Html.fromHtml(mContext.getResources().getString(R.string.about_message));
        }

        private CharSequence getHelpTitle() {
            return mContext.getString(R.string.help);
        }

        private CharSequence getHelpMessage() {
            return Html.fromHtml(mContext.getString(R.string.help_msg));
        }

        private View create(CharSequence title, CharSequence message) {
            LayoutInflater inflater = LayoutInflater.from(mContext);

            View root = inflater.inflate(R.layout.about_dialog, null);

            // Title.
            TextView titleView = (TextView) root.findViewById(R.id.dialog_title_text);

            Drawable left = (mContext.getResources().getConfiguration().screenLayout &
                    Configuration.SCREENLAYOUT_SIZE_MASK) !=
                    Configuration.SCREENLAYOUT_SIZE_LARGE ? mIcon : null;
            Drawable top = left == null ? mIcon : null;

            titleView.setText(title);
            titleView.setCompoundDrawablePadding(mContext.getResources().getDimensionPixelSize(R.dimen.item_padding));
            titleView.setCompoundDrawablesWithIntrinsicBounds(left, top, null, null);

            // About message.
            TextView messageView = (TextView) root.findViewById(R.id.dialog_message_text);
            messageView.setText(message);
            messageView.setMovementMethod(LinkMovementMethod.getInstance());


            return root;
        }


        /**
         * Create About Dialog as a default {@link AlertDialog.Builder} with custom view.
         */
        public AlertDialog.Builder createAboutDialog() {
            return new AlertDialog.Builder(mContext)
                    .setView(create(getVersionTitle(), getAboutMessage()));
        }

        /**
         * Create Help Dialog as a default {@link AlertDialog.Builder} with custom view.
         */
        public AlertDialog.Builder createHelpDialog() {
            return new AlertDialog.Builder(mContext)
                    .setView(create(getHelpTitle(), getHelpMessage()));
        }

    }

}
