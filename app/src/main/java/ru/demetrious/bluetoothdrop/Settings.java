package ru.demetrious.bluetoothdrop;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import java.text.MessageFormat;
import java.util.Map;

class Settings {
    private static MainActivity mainActivity;
    private static SharedPreferences sharedPreferences;
    private final static String APP_PREFERENCES = "settings";
    private static Map<String, ?> preferences;

    final static String APP_PREFERENCES_SORT_BY = "ru.demetrious.bluetoothdrop.sort_by";
    final static String APP_PREFERENCES_SORT = "ru.demetrious.bluetoothdrop.sort";
    final static String APP_PREFERENCES_FOLDERS = "ru.demetrious.bluetoothdrop.folders";
    final static String APP_PREFERENCES_IGNORE_CASE = "ru.demetrious.bluetoothdrop.ignore_case";
    final static String APP_PREFERENCES_REGEX = "ru.demetrious.bluetoothdrop.regex";
    final static String APP_PREFERENCES_CURRENT_DIRECTORY = "ru.demetrious.bluetoothdrop.current_directory";

    final static String APP_SETTING_SAVE_PATH = "ru.demetrious.bluetoothdrop.setting0";
    final static String APP_SETTING_HOME_PATH = "ru.demetrious.bluetoothdrop.setting1";
    final static String APP_SETTING_DISCOVERABLE_TIME = "ru.demetrious.bluetoothdrop.setting2";

    static String DEFAULT_SAVE_PATH = null;
    static String DEFAULT_HOME_PATH = null;

    //Список, свичер, путь,
    Settings(MainActivity mainActivity) {
        Settings.mainActivity = mainActivity;
        DEFAULT_HOME_PATH = mainActivity.explorer.getGlobalFileDir().getAbsolutePath();
        DEFAULT_SAVE_PATH = DEFAULT_HOME_PATH + "/bluetoothReceived";
        sharedPreferences = mainActivity.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        //sharedPreferences.edit().clear().apply();
        generateSettings();
    }

    private static void loadPreferences() {
        preferences = sharedPreferences.getAll();
    }

    void settings() {
        mainActivity.listMain.setAdapter(mainActivity.settingsElementAdapter);

        mainActivity.imageButtonUp.setVisibility(View.GONE);
        mainActivity.textAmount.setVisibility(View.GONE);
        mainActivity.textPath.setVisibility(View.GONE);
        mainActivity.imageButtonHome.setVisibility(View.GONE);
        mainActivity.listSpinner.setVisibility(View.GONE);
        mainActivity.imageButtonRefresh.setVisibility(View.GONE);
    }

    static Object getSetting(String key, Object defaultValue, Class<?> oClass) {
        Object o = getPreference(key, defaultValue, oClass);
        int index = Integer.parseInt(key.replaceAll("ru.demetrious.bluetoothdrop.setting", ""));
        return mainActivity.settingsElements.get(index).getVars()[(Integer) o];
    }

    static SharedPreferences.Editor getPreferencesEditor() {
        return sharedPreferences.edit();
    }

    static Object getPreference(String key, Object defaultValue, Class<?> oClass) {
        if (defaultValue.getClass() != oClass)
            throw new ClassFormatError(MessageFormat.format("{0} {1}", mainActivity.getString(R.string.error_class), oClass.toString()));
        loadPreferences();
        Object value = preferences.get(key);
        if (value != null && !value.getClass().equals(oClass)) {
            value = null;
            getPreferencesEditor().remove(key);
        }
        if (value == null) value = defaultValue;
        return value;
    }

    private void generateSettings() {
        mainActivity.settingsElements.add(new SettingsElement(SettingsElement.Type.Directory, mainActivity.getString(R.string.setting_default_path), (String) Settings.getPreference(APP_SETTING_SAVE_PATH, DEFAULT_SAVE_PATH, String.class)));//setting0
        mainActivity.settingsElements.add(new SettingsElement(SettingsElement.Type.Directory, mainActivity.getString(R.string.setting_home_path), (String) Settings.getPreference(APP_SETTING_HOME_PATH, DEFAULT_HOME_PATH, String.class)));//setting1
        mainActivity.settingsElements.add(new SettingsElement(SettingsElement.Type.Spinner, mainActivity.getString(R.string.setting_discoverable_time), mainActivity.getString(R.string.timeUnits_seconds), 30, 60, 90, 120, 150, 180, 210, 240, 270, 300));//setting2

        //mainActivity.settingsElementAdapter.notifyDataSetChanged();
    }
}
