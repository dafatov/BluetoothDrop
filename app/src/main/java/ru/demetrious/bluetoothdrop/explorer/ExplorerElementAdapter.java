package ru.demetrious.bluetoothdrop.explorer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import ru.demetrious.bluetoothdrop.R;
import ru.demetrious.bluetoothdrop.activities.MainActivity;

public class ExplorerElementAdapter extends ArrayAdapter<ExplorerElement> {
    private LayoutInflater inflater;
    private int layout;
    private ArrayList<ExplorerElement> explorerElementsList;
    private MainActivity mainActivity;

    public ExplorerElementAdapter(MainActivity mainActivity, int resource, ArrayList<ExplorerElement> explorerElementsList) {
        super(mainActivity, resource, explorerElementsList);
        this.mainActivity = mainActivity;
        this.explorerElementsList = explorerElementsList;
        this.layout = resource;
        this.inflater = LayoutInflater.from(mainActivity);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(this.layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final ExplorerElement explorerElement = explorerElementsList.get(position);

        viewHolder.name.setText(explorerElement.getName());
        viewHolder.size.setText(sizeToString(explorerElement.getSize(), explorerElement.isFolder()));
        viewHolder.date.setText(new SimpleDateFormat(mainActivity.getString(R.string.pattern_data_format)).format(explorerElement.getDate()));
        viewHolder.select.setChecked(explorerElement.isSelected());
        viewHolder.select.setFocusable(false);

        if (explorerElement.isFolder()) {
            viewHolder.select.setVisibility(View.INVISIBLE);
            viewHolder.preview.setImageResource(R.drawable.ic_format_folder);
        } else {
            viewHolder.select.setVisibility(View.VISIBLE);
            viewHolder.preview.setImageResource(R.drawable.ic_format_file);
        }

        viewHolder.select.setOnClickListener(v -> {
            if (viewHolder.select.isChecked()) {
                if (mainActivity.getExplorer().getSelectedFiles().add(mainActivity.getExplorer().getCurrentDirectory() +
                        "/" + viewHolder.name.getText())) {
                    mainActivity.getSelectedFiles().add(viewHolder.name.getText().toString());
                    mainActivity.getExplorer().addAmount(1);
                    explorerElement.setSelected(true);
                }
            } else {
                if (mainActivity.getExplorer().getSelectedFiles().remove(mainActivity.getExplorer().getCurrentDirectory() +
                        "/" + viewHolder.name.getText())) {
                    mainActivity.getSelectedFiles().remove(viewHolder.name.getText().toString());
                    mainActivity.getExplorer().addAmount(-1);
                    explorerElement.setSelected(false);
                }
            }
            mainActivity.getSelectedFilesAdapter().notifyDataSetChanged();
        });

        return convertView;
    }

    private String sizeToString(float size, boolean isFolder) {
        if (isFolder) return "<dir>";

        int order = 0;

        while (size > 1024) {
            size /= 1024;
            order++;
        }
        if (order >= MainActivity.getSizeUnits().length) {
            order = 0;
            size = 0;
        }
        return MessageFormat.format("{0} {1}", String.valueOf(Math.rint(100 * size) / 100), MainActivity.getSizeUnits()[order]);
    }

    private class ViewHolder {
        final CheckBox select;
        final TextView name, date, size;
        final ImageView preview;

        ViewHolder(View view) {
            select = view.findViewById(R.id.explorer_select);
            name = view.findViewById(R.id.explorer_name);
            date = view.findViewById(R.id.explorer_date);
            size = view.findViewById(R.id.explorer_size);
            preview = view.findViewById(R.id.explorer_icon);
        }
    }
}
