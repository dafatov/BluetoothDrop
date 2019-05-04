package ru.demetrious.bluetoothdrop;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

class Settings {
    private MainActivity mainActivity;
    private static SharedPreferences sharedPreferences;
    private final static String APP_PREFERENCES = "settings";

    final static String APP_PREFERENCES_SORT_BY = "sort_by";
    final static String APP_PREFERENCES_SORT = "sort";
    final static String APP_PREFERENCES_FOLDERS = "folders";
    final static String APP_PREFERENCES_IGNORE_CASE = "ignore_case";
    final static String APP_PREFERENCES_REGEX = "regex";
    final static String APP_PREFERENCES_CURRENT_DIRECTORY = "current_directory";

    //
    static String DEFAULT_SAVE_PATH = null;
    //

    Settings(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        DEFAULT_SAVE_PATH = mainActivity.explorer.getGlobalFileDir().getAbsolutePath() + "/bluetoothReceived";
        sharedPreferences = mainActivity.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    void settings() {
        mainActivity.imageButtonUp.setVisibility(View.GONE);
        mainActivity.textAmount.setVisibility(View.GONE);
        mainActivity.textPath.setVisibility(View.GONE);
        mainActivity.imageButtonHome.setVisibility(View.GONE);
        mainActivity.listSpinner.setVisibility(View.GONE);
        mainActivity.imageButtonRefresh.setVisibility(View.GONE);
    }

    static SharedPreferences getPreferences() {
        return sharedPreferences;
    }
}
