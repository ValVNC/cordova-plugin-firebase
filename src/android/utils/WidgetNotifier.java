package org.apache.cordova.firebase.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;


public class WidgetNotifier {

    public static void notifyMessagesListUpdated(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            final int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, "biz.vnc.mailwidget.WidgetProvider"));

            refreshListsForMailWidgets(context, manager, appWidgetIds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void refreshListsForMailWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        if (appWidgetIds.length == 0) return;

        manager.notifyAppWidgetViewDataChanged(appWidgetIds, context.getResources().getIdentifier("messages_list", "id", context.getPackageName()));
    }
    
    public static void updateCalendarWidgets(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            
            final int[] appMonthWidgetIds = manager.getAppWidgetIds(new ComponentName(context, "biz.vnc.calendarwidget.WidgetProviderMonth"));
            refreshListsForCalendarWidgets(context, manager, appMonthWidgetIds);

            final int[] appMonthListWidgetIds = manager.getAppWidgetIds(new ComponentName(context, "biz.vnc.calendarwidget.WidgetProviderMonthList"));
            refreshListsForCalendarWidgets(context, manager, appMonthListWidgetIds);

            final int[] appWeekWidgetIds = manager.getAppWidgetIds(new ComponentName(context, "biz.vnc.calendarwidget.WidgetProviderWeek"));
            refreshListsForCalendarWidgets(context, manager, appWeekWidgetIds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void refreshListsForCalendarWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        if (appWidgetIds.length == 0) return;

        manager.notifyAppWidgetViewDataChanged(appWidgetIds, context.getResources().getIdentifier("appointments_list", "id", context.getPackageName()));
    }
}