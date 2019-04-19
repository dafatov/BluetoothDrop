package ru.demetrious.bluetoothdrop;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

class Settings {
    private MainActivity mainActivity;
    private static SharedPreferences sharedPreferences;
    private final static String APP_PREFERENCES = "settings";

    final static String APP_PREFERENCES_SORT_BY = "sort_by";
    final static String APP_PREFERENCES_SORT = "sort";
    final static String APP_PREFERENCES_FOLDERS = "folders";
    final static String APP_PREFERENCES_IGNORE_CASE = "ignore_case";

    Settings(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        sharedPreferences = mainActivity.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    static SharedPreferences getPreferences() {return sharedPreferences;}
}
