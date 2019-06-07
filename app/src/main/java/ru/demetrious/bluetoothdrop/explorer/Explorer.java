package ru.demetrious.bluetoothdrop.explorer;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ru.demetrious.bluetoothdrop.R;
import ru.demetrious.bluetoothdrop.activities.MainActivity;
import ru.demetrious.bluetoothdrop.activities.SelectActivity;

public class Explorer {
    private String currentDirectory;
    private ArrayList<String> selectedFiles;
    private MainActivity mainActivity;
    private int sortBy = R.id.sort_name, sort = R.id.sort_asc;
    private boolean ignoreCase = true, sortFolders = true;

    public Explorer(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        setSelectedFiles(new ArrayList<>());
    }

    public void explorer() {
        mainActivity.getListMain().setAdapter(mainActivity.getExplorerElementsAdapter());

        mainActivity.getImageButtonUp().setVisibility(View.VISIBLE);
        mainActivity.getTextAmount().setVisibility(View.VISIBLE);
        mainActivity.getTextPath().setVisibility(View.VISIBLE);
        mainActivity.getImageButtonHome().setVisibility(View.VISIBLE);
        mainActivity.getListSpinner().setVisibility(View.VISIBLE);
        mainActivity.getImageButtonRefresh().setVisibility(View.VISIBLE);

        mainActivity.getImageButtonHome().setImageResource(R.drawable.ic_action_home);

        showDirectory(new File(getCurrentDirectory()));
    }

    public File getGlobalFileDir() {
        if (Environment.isExternalStorageEmulated()) {
            return Environment.getExternalStorageDirectory();
        } else {
            return Environment.getRootDirectory();
        }
    }

    public void showDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            int countSelected = 0;
            int countFiles = 0;

            mainActivity.getExplorerElements().clear();
            setCurrentDirectory(dir.getAbsolutePath());
            mainActivity.getTextPath().setText(getCurrentDirectory());
            for (File file : dir.listFiles()) {
                boolean selected = false;

                if (!file.isDirectory()) {
                    if (getSelectedFiles().contains(file.getAbsolutePath())) {
                        countSelected++;
                        selected = true;
                    }
                    countFiles++;
                }
                mainActivity.getExplorerElements().add(new ExplorerElement(file.getName(), new Date(file.lastModified()),
                        file.length(), file.isDirectory(), selected));
            }
            setAmount(countSelected, countFiles, dir.listFiles().length);
            sort();
            mainActivity.getExplorerElementsAdapter().notifyDataSetChanged();
        }
    }

    public void select(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_CANCELED) {
            Pattern regex;
            try {
                regex = Pattern.compile(data.getStringExtra(SelectActivity.EXTRA_REGEX));

                switch (resultCode) {
                    case SelectActivity.SELECT:
                    case SelectActivity.UNSELECT:
                        for (ExplorerElement explorerElement : mainActivity.getExplorerElements()) {
                            Matcher matcher = regex.matcher(explorerElement.getName());
                            if (!explorerElement.isFolder() && matcher.matches()) {
                                explorerElement.setSelected(resultCode == SelectActivity.SELECT);
                                if (explorerElement.isSelected()) {
                                    if (getSelectedFiles().add(getCurrentDirectory() +
                                            "/" + explorerElement.getName())) {
                                        mainActivity.getSelectedFiles().add(explorerElement.getName());
                                        addAmount(1);
                                    }
                                } else {
                                    if (getSelectedFiles().remove(getCurrentDirectory() +
                                            "/" + explorerElement.getName())) {
                                        mainActivity.getSelectedFiles().remove(explorerElement.getName());
                                        addAmount(-1);
                                    }
                                }

                            }
                        }
                        break;
                    case SelectActivity.INVERT:
                        for (ExplorerElement explorerElement : mainActivity.getExplorerElements()) {
                            if (!explorerElement.isFolder()) {
                                explorerElement.setSelected(!explorerElement.isSelected());

                                if (explorerElement.isSelected()) {
                                    if (getSelectedFiles().add(getCurrentDirectory() +
                                            "/" + explorerElement.getName())) {
                                        mainActivity.getSelectedFiles().add(explorerElement.getName());
                                        addAmount(1);
                                    }
                                } else {
                                    if (getSelectedFiles().remove(getCurrentDirectory() +
                                            "/" + explorerElement.getName())) {
                                        mainActivity.getSelectedFiles().remove(explorerElement.getName());
                                        addAmount(-1);
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        Toast.makeText(mainActivity.getApplicationContext(), mainActivity.getString(R.string.error), Toast.LENGTH_LONG).show();
                        System.exit(4354);
                }
                mainActivity.getExplorerElementsAdapter().notifyDataSetChanged();
                mainActivity.getSelectedFilesAdapter().notifyDataSetChanged();
            } catch (PatternSyntaxException e) {
                Toast.makeText(mainActivity.getApplicationContext(), mainActivity.getString(R.string.error_regex), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void setSort(@IdRes int sortBy, @IdRes int sort, boolean ignoreCase, boolean sortFolders) {
        this.sortBy = sortBy;
        this.sort = sort;
        this.ignoreCase = ignoreCase;
        this.sortFolders = sortFolders;
        sort();
    }

    private void sort() {
        ArrayList<ExplorerElement> result = new ArrayList<>();
        int mid = 0;

        for (ExplorerElement explorerElement : mainActivity.getExplorerElements()) {
            boolean tmp = true;
            for (int i = (!sortFolders || explorerElement.isFolder()) ? 0 : mid; i < ((sortFolders && explorerElement.isFolder()) ? mid : result.size()); i++) {
                if (sortCondition(explorerElement, result.get(i))) {
                    result.add(i, explorerElement);
                    tmp = false;
                    break;
                }
            }
            if (tmp)
                result.add((sortFolders && explorerElement.isFolder()) ? mid : result.size(), explorerElement);
            if (sortFolders && explorerElement.isFolder()) mid++;
        }
        mainActivity.getExplorerElements().clear();
        mainActivity.getExplorerElements().addAll(result);
        mainActivity.getExplorerElementsAdapter().notifyDataSetChanged();
    }

    void addAmount(int addCountSelected) {
        String previous = mainActivity.getTextAmount().getText().toString();
        String[] parameters = previous.replaceAll("\\)", "").replaceAll("\\(", "/").split("/");//change values of ammout files in current directory

        mainActivity.getTextAmount().setText(MessageFormat.format("{0}/{1}({2})", Integer.parseInt(parameters[0]) + addCountSelected, parameters[1], parameters[2]));
    }

    private void setAmount(int countSelected, int countFiles, int countAll) {
        if (countSelected >= 0 && countFiles >= 0 && countAll >= 0 && countSelected <= countFiles && countFiles <= countAll) {
            mainActivity.getTextAmount().setText(MessageFormat.format("{0}/{1}({2})", countSelected, countFiles, countAll));
        } else {
            Toast.makeText(mainActivity.getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
            System.exit(-1122);
        }
    }

    private boolean sortCondition(ExplorerElement explorerElementCompared, ExplorerElement explorerElement) {
        float compareTo = 0;

        switch (sortBy) {
            case R.id.sort_name:
                if (ignoreCase)
                    compareTo = explorerElement.getName().compareToIgnoreCase(explorerElementCompared.getName());
                else
                    compareTo = explorerElement.getName().compareTo(explorerElementCompared.getName());
                break;
            case R.id.sort_date:
                compareTo = explorerElement.getDate().compareTo(explorerElementCompared.getDate());
                break;
            case R.id.sort_size:
                compareTo = explorerElement.getSize() - explorerElementCompared.getSize();
                break;
            default:
                Toast.makeText(mainActivity.getApplicationContext(), mainActivity.getString(R.string.error), Toast.LENGTH_LONG).show();
                System.exit(5454);
        }
        switch (sort) {
            case R.id.sort_asc:
                return compareTo > 0;
            case R.id.sort_desc:
                return compareTo <= 0;
            default:
                Toast.makeText(mainActivity.getApplicationContext(), mainActivity.getString(R.string.error), Toast.LENGTH_LONG).show();
                System.exit(5493);
        }
        return false;
    }

    public ArrayList<String> getSelectedFiles() {
        return selectedFiles;
    }

    private void setSelectedFiles(ArrayList<String> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }
}
