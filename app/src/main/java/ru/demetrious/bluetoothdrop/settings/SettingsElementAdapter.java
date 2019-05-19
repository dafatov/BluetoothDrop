package ru.demetrious.bluetoothdrop.settings;

import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

import ru.demetrious.bluetoothdrop.R;
import ru.demetrious.bluetoothdrop.activities.MainActivity;
import ru.demetrious.bluetoothdrop.activities.PathActivity;

public class SettingsElementAdapter extends ArrayAdapter<SettingsElement> {
    private LayoutInflater inflater;
    private int layout;
    private ArrayList<SettingsElement> settingsElements;
    private MainActivity mainActivity;

    public SettingsElementAdapter(MainActivity mainActivity, int resource, ArrayList<SettingsElement> settingsElements) {
        super(mainActivity, resource, settingsElements);
        this.mainActivity = mainActivity;
        this.settingsElements = settingsElements;
        this.layout = resource;
        this.inflater = LayoutInflater.from(mainActivity);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(this.layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        final SettingsElement settingsElement = settingsElements.get(position);
        final Object[] vars = settingsElement.getVars();

        viewHolder.name.setText(settingsElement.getName());
        viewHolder.description.setText(settingsElement.getDescription());

        switch (settingsElement.getType()) {
            case Directory:
                Button button = new Button(convertView.getContext());
                setSettingType(button, convertView);
                button.setText(mainActivity.getString(R.string.button_change_path));

                button.setOnClickListener(v -> {
                    Intent intent = new Intent(mainActivity, PathActivity.class);
                    intent.putExtra(PathActivity.EXTRA_PARENT_SETTING, settingsElement.getID());
                    intent.putExtra(PathActivity.EXTRA_CURRENT_DIR, settingsElement.getDescription());
                    intent.putExtra(PathActivity.EXTRA_HOME, Settings.DEFAULT_HOME_PATH);
                    mainActivity.startActivityForResult(intent, MainActivity.ACTIVITY_PATH);
                });
                break;
            case Spinner:
                Spinner spinner = new Spinner(convertView.getContext());
                ArrayList<String> arrayList = new ArrayList<>();
                for (Object var : vars) {
                    arrayList.add(String.valueOf(var));
                }
                ArrayAdapter arrayAdapter = new ArrayAdapter<>(getContext(), R.layout.support_simple_spinner_dropdown_item, arrayList);
                spinner.setAdapter(arrayAdapter);

                setSettingType(spinner, convertView);
                spinner.setSelection((Integer) Settings.getPreference(settingsElement.getID(), 0, Integer.class));
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Settings.getPreferencesEditor().putInt(settingsElement.getID(), position).apply();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
                break;
            case ToggleButton:
                ToggleButton toggleButton = new ToggleButton(convertView.getContext());
                setSettingType(toggleButton, convertView);
                toggleButton.setChecked(((int) Settings.getPreference(settingsElement.getID(), 0, int.class)) == 1);
                toggleButton.setText(String.valueOf(settingsElement.getVars()[(int) Settings.getPreference(settingsElement.getID(), 0, int.class)]));
                toggleButton.setTextOff(String.valueOf(settingsElement.getVars()[0]));
                toggleButton.setTextOn(String.valueOf(settingsElement.getVars()[1]));
                toggleButton.setOnClickListener(v -> Settings.getPreferencesEditor().putInt(settingsElement.getID(), toggleButton.isChecked() ? 1 : 0).apply());
                break;
        }
        return convertView;
    }

    private void setSettingType(@NonNull View typeView, View view) {
        int id;
        ConstraintLayout constraintLayout = (ConstraintLayout) view;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            id = View.generateViewId();
        else id = generateViewId();
        typeView.setId(id);
        constraintLayout.addView(typeView);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        constraintSet.setVisibility(id, ConstraintSet.VISIBLE);
        constraintSet.setMargin(id, ConstraintSet.BOTTOM, 16);
        constraintSet.setMargin(id, ConstraintSet.END, 16);
        constraintSet.setMargin(id, ConstraintSet.RIGHT, 16);
        constraintSet.setMargin(id, ConstraintSet.TOP, 16);
        constraintSet.connect(id, ConstraintSet.BOTTOM, 0, ConstraintSet.BOTTOM);
        constraintSet.connect(id, ConstraintSet.END, 0, ConstraintSet.END);
        constraintSet.connect(id, ConstraintSet.TOP, 0, ConstraintSet.TOP);
        constraintSet.connect(R.id.settings_name, ConstraintSet.END, id, ConstraintSet.START);
        constraintSet.connect(R.id.settings_description, ConstraintSet.END, id, ConstraintSet.START);

        constraintSet.applyTo(constraintLayout);
    }

    private int generateViewId() {
        int id = 0;
        while (mainActivity.findViewById(++id) != null) ;
        return id;
    }

    private class ViewHolder {
        final TextView name, description;

        ViewHolder(View view) {
            name = view.findViewById(R.id.settings_name);
            description = view.findViewById(R.id.settings_description);
        }
    }
}
