package com.github.yeriomin.yalpstore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.github.yeriomin.yalpstore.model.App;
import com.github.yeriomin.yalpstore.notification.NotificationManagerFactory;
import com.github.yeriomin.yalpstore.notification.NotificationManagerWrapper;

import java.io.File;

public class GlobalDownloadReceiver extends BroadcastReceiver {

    private Context context;
    private NotificationManagerWrapper notificationManager;

    @Override
    public void onReceive(Context c, Intent i) {
        context = c;
        notificationManager = NotificationManagerFactory.get(c);

        Bundle extras = i.getExtras();
        long downloadId = extras.getLong(DownloadManagerInterface.EXTRA_DOWNLOAD_ID);

        DownloadState state = DownloadState.get(downloadId);
        if (null == state) {
            return;
        }
        App app = state.getApp();

        DownloadManagerInterface dm = DownloadManagerFactory.get(context);
        if (!dm.finished(downloadId)) {
            return;
        }
        state.setFinished(downloadId);
        if (dm.success(downloadId)) {
            state.setSuccessful(downloadId);
        } else {
            String error = dm.getError(downloadId);
            Toast.makeText(context, error, Toast.LENGTH_LONG).show();
            notificationManager.show(new Intent(), app.getDisplayName(), error);
        }

        if (state.isEverythingFinished() && state.isEverythingSuccessful()) {
            verifyAndInstall(app, state.getTriggeredBy());
        }
    }

    private void verifyAndInstall(App app, DownloadState.TriggeredBy triggeredBy) {
        boolean autoInstall = triggeredBy.equals(DownloadState.TriggeredBy.DOWNLOAD_BUTTON) && shouldAutoInstall();
        if (autoInstall
            || (
                needToInstallUpdates()
                && PreferenceActivity.canInstallInBackground(context)
                && (triggeredBy.equals(DownloadState.TriggeredBy.SCHEDULED_UPDATE)
                    || triggeredBy.equals(DownloadState.TriggeredBy.UPDATE_ALL_BUTTON)
                )
            )
        ) {
            InstallerAbstract installer = InstallerFactory.get(context);
            if (autoInstall) {
                installer.setBackground(false);
            }
            installer.verifyAndInstall(app);
        } else {
            notifyDownloadComplete(app);
        }
    }

    private void notifyDownloadComplete(App app) {
        notifyAndToast(
            R.string.notification_download_complete,
            R.string.notification_download_complete_toast,
            app
        );
    }

    private void notifyAndToast(int notificationStringId, int toastStringId, App app) {
        File file = Downloader.getApkPath(app.getPackageName(), app.getVersionCode());
        Intent openApkIntent = InstallerAbstract.getOpenApkIntent(context, file);
        notificationManager.show(
            openApkIntent,
            app.getDisplayName(),
            context.getString(notificationStringId)
        );
        Toast.makeText(context, context.getString(toastStringId, app.getDisplayName()), Toast.LENGTH_LONG).show();
    }

    private boolean needToInstallUpdates() {
        return PreferenceActivity.getBoolean(context, PreferenceActivity.PREFERENCE_BACKGROUND_UPDATE_INSTALL);
    }

    private boolean shouldAutoInstall() {
        return PreferenceActivity.getBoolean(context, PreferenceActivity.PREFERENCE_AUTO_INSTALL);
    }
}
