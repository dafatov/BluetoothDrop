package ru.demetrious.bluetoothdrop.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import java.text.MessageFormat;
import java.util.Map;

import ru.demetrious.bluetoothdrop.R;
import ru.demetrious.bluetoothdrop.activities.MainActivity;

public class Settings {
    public final static String APP_PREFERENCES_SORT_BY = "ru.demetrious.bluetoothdrop.sort_by";
    public final static String APP_PREFERENCES_SORT = "ru.demetrious.bluetoothdrop.sort";
    public final static String APP_PREFERENCES_FOLDERS = "ru.demetrious.bluetoothdrop.folders";
    public final static String APP_PREFERENCES_IGNORE_CASE = "ru.demetrious.bluetoothdrop.ignore_case";
    public final static String APP_PREFERENCES_REGEX = "ru.demetrious.bluetoothdrop.regex";
    public final static String APP_PREFERENCES_CURRENT_DIRECTORY = "ru.demetrious.bluetoothdrop.current_directory";

    public final static String APP_SETTING_HOME_PATH = "ru.demetrious.bluetoothdrop.setting0";
    public final static String APP_SETTING_DISCOVERABLE_TIME = "ru.demetrious.bluetoothdrop.setting1";
    public final static String APP_SETTING_SAVE_PATH = "ru.demetrious.bluetoothdrop.setting2";

    private final static String APP_PREFERENCES = "settings";

    public static String DEFAULT_SAVE_PATH = null;
    public static String DEFAULT_HOME_PATH = null;

    private static MainActivity mainActivity;

    private static SharedPreferences sharedPreferences;
    private static Map<String, ?> preferences;

    //Список, свичер, путь,
    public Settings(MainActivity mainActivity) {
        Settings.mainActivity = mainActivity;
        DEFAULT_HOME_PATH = mainActivity.getExplorer().getGlobalFileDir().getAbsolutePath();
        DEFAULT_SAVE_PATH = DEFAULT_HOME_PATH + "/bluetoothReceived";
        sharedPreferences = mainActivity.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        generateSettings();
    }

    private static void loadPreferences() {
        preferences = sharedPreferences.getAll();
    }

    public static Object getSetting(String key, Object defaultValue, Class<?> oClass) {
        Object o = getPreference(key, defaultValue, oClass);
        int index = Integer.parseInt(key.replaceAll("ru.demetrious.bluetoothdrop.setting", ""));
        return mainActivity.getSettingsElements().get(index).getVars()[(Integer) o];
    }

    public static SharedPreferences.Editor getPreferencesEditor() {
        return sharedPreferences.edit();
    }

    public static Object getPreference(String key, Object defaultValue, Class<?> oClass) {
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

    public void settings() {
        mainActivity.getListMain().setAdapter(mainActivity.getSettingsElementAdapter());

        mainActivity.getImageButtonUp().setVisibility(View.GONE);
        mainActivity.getTextAmount().setVisibility(View.GONE);
        mainActivity.getTextPath().setVisibility(View.GONE);
        mainActivity.getImageButtonHome().setVisibility(View.GONE);
        mainActivity.getListSpinner().setVisibility(View.GONE);
        mainActivity.getImageButtonRefresh().setVisibility(View.GONE);
    }

    private void generateSettings() {
        mainActivity.getSettingsElements().add(new SettingsElement<>(SettingsElement.Type.Directory, mainActivity.getString(R.string.setting_home_path), (String) Settings.getPreference(APP_SETTING_HOME_PATH, DEFAULT_HOME_PATH, String.class)));//setting0
        mainActivity.getSettingsElements().add(new SettingsElement<>(SettingsElement.Type.Spinner, mainActivity.getString(R.string.setting_discoverable_time), mainActivity.getString(R.string.timeUnits_seconds), 30, 60, 90, 120, 150, 180, 210, 240, 270, 300));//setting1
        mainActivity.getSettingsElements().add(new SettingsElement<>(SettingsElement.Type.Directory, mainActivity.getString(R.string.setting_default_path), (String) Settings.getPreference(APP_SETTING_SAVE_PATH, DEFAULT_SAVE_PATH, String.class)));//setting2
    }
}
