package org.apache.cordova.firebase.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPrefsUtils {

    private static final String DATABASE_FILE_NAME = "local_db_filename";
    private static final int DEFAULT_INT_VALUE = -1;
    private static final long DEFAULT_LONG_VALUE = -1;

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;


    private static void initPreferences(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        context.getSharedPreferences(DATABASE_FILE_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    private static void initPreferences(Context context, String name) {
        context.getSharedPreferences(name, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }


    public static void putBoolean(Context context, String key, boolean value) {
        initPreferences(context);
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static boolean getBoolean(Context context, String key) {
        initPreferences(context);
        return preferences.getBoolean(key, false);
    }

    public static void putString(Context context, String key, String value) {
        initPreferences(context);
        editor.putString(key, value);
        editor.commit();
    }

    public static String getString(Context context, String key) {
        initPreferences(context);
        return preferences.getString(key, "");
    }


    public static void putInt(Context context, String key, int value) {
        initPreferences(context);
        editor.putInt(key, value);
        editor.commit();
    }

    public static void putInt(Context context, String prefsName, String key, int value) {
        initPreferences(context, prefsName);
        editor.putInt(key, value);
        editor.commit();
    }

    public static int getInt(Context context, String key) {
        initPreferences(context);
        return preferences.getInt(key, DEFAULT_INT_VALUE);
    }

    public static int getInt(Context context, String prefsName, String key) {
        initPreferences(context, prefsName);
        return preferences.getInt(key, DEFAULT_INT_VALUE);
    }


    public static void putLong(Context context, String key, long value) {
        initPreferences(context);
        editor.putLong(key, value);
        editor.commit();
    }

    public static long getLong(Context context, String key) {
        initPreferences(context);
        return preferences.getLong(key, DEFAULT_LONG_VALUE);
    }

    public static void putDouble(Context context, String key, double value) {
        initPreferences(context);
        editor.putString(key, String.valueOf(value));
        editor.commit();
    }

    public static double getDouble(Context context, String key) {
        initPreferences(context);
        String value = preferences.getString(key, "");
        if (value.equals("")) {
            return 0.0;
        }
        return Double.parseDouble(value);
    }
}
