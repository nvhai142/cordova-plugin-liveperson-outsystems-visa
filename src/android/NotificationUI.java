package com.liveperson.plugin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.text.TextUtils;

import com.liveperson.infra.model.PushMessage;
import com.liveperson.messaging.sdk.api.LivePerson;

import java.util.List;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

/**
 * ***** Sample app class - Not related to Messaging SDK *****
 * <p>
 * Used as an example of how to create push notification in terms of UI.
 * As best practise each host app needs to handle the push notifications UI implementation.
 */
public class NotificationUI {

    private static final String TAG = NotificationUI.class.getSimpleName();
    public static final int NOTIFICATION_ID = 143434567;
    public static final String PUSH_NOTIFICATION = "push_notification";


    public static void showNotification(Context ctx, PushMessage pushMessage) {

        Notification.Builder builder;
        NotificationChannel notificationChannel;

        // If we're running on Android O we create a notification channel
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder = new Notification.Builder(ctx);
        } else {
            notificationChannel = new NotificationChannel(PUSH_NOTIFICATION, "Push Notification", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setImportance(IMPORTANCE_HIGH);
            getNotificationManager(ctx).createNotificationChannel(notificationChannel);
            builder = new Notification.Builder(ctx, PUSH_NOTIFICATION);
        }

        String message = (TextUtils.isEmpty(pushMessage.getFrom()) ? "" : (pushMessage.getFrom() +": ") ) + pushMessage.getMessage();
        builder.setContentIntent(getPendingIntent(ctx)).
                setContentTitle(message).
                setAutoCancel(true).
                setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS).
                setSmallIcon(ctx.getApplicationContext().getResources().getIdentifier("ic_launcher","mipmap", ctx.getApplicationContext().getPackageName() )).
                setNumber(pushMessage.getCurrentUnreadMessagesCounter());
                /*.setStyle(new Notification.InboxStyle()
                        .addLine(pushMessage.getFrom())
                        .addLine(pushMessage.getBrandId())
                        .addLine(pushMessage.getConversationId())
                        .addLine(pushMessage.getBackendService())
                        .addLine(pushMessage.getCollapseKey())
                        .addLine("Unread messages : " + LivePerson.
                                getNumUnreadMessages(pushMessage.getBrandId()))

                );*/

        if (Build.VERSION.SDK_INT >= 21) {
            builder = builder.
                    setCategory(Notification.CATEGORY_MESSAGE).
                    setPriority(Notification.PRIORITY_HIGH);
        }

        getNotificationManager(ctx).notify(NOTIFICATION_ID, builder.build());
    }

    public static void hideNotification(Context ctx) {
        getNotificationManager(ctx).cancel(NOTIFICATION_ID);

    }

    private static NotificationManager getNotificationManager(Context ctx) {
        return (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    }


    private static PendingIntent getPendingIntent(Context ctx) {
        // there was chatmessagnging here changed by nishant
        Class mainActivity = null;
        Context context = ctx;
        String  packageName = context.getPackageName();
        Intent  launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String  className = launchIntent.getComponent().getClassName();

        try {
            //loading the Main Activity to not import it in the plugin
            mainActivity = Class.forName(className);
        } catch (Exception e) {
           // e.printStackTrace();
        }

        // there was chatmessagnging here changed by nishant
        Intent showIntent = new Intent(ctx, mainActivity);
        showIntent.putExtra(PUSH_NOTIFICATION, true);

        return PendingIntent.getActivity(ctx, 0, showIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    /************************ Example of app Icon Badge - For Samsung *******************************/

    public static void setBadge(Context context, int count) {
        String launcherClassName = getLauncherClassName(context);
        if (launcherClassName == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
        intent.putExtra("badge_count", count);
        intent.putExtra("badge_count_package_name", context.getPackageName());
        intent.putExtra("badge_count_class_name", launcherClassName);
        context.sendBroadcast(intent);
    }

    public static String getLauncherClassName(Context context) {

        PackageManager pm = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            String pkgName = resolveInfo.activityInfo.applicationInfo.packageName;
            if (pkgName.equalsIgnoreCase(context.getPackageName())) {
                return resolveInfo.activityInfo.name;
            }
        }
        return null;
    }


    /**
     * Listen to changes in unread messages counter and updating app icon badge
     */
    public static class BadgeBroadcastReceiver extends BroadcastReceiver {

        public BadgeBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int unreadCounter = intent.getIntExtra(LivePerson.ACTION_LP_UPDATE_NUM_UNREAD_MESSAGES_EXTRA, 0);
            NotificationUI.setBadge(context, unreadCounter);
        }
    }
}
