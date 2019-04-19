package ru.demetrious.bluetoothdrop;

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
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class Explorer {
    private MainActivity mainActivity;

    //
    int sortBy = R.id.sort_name, sort = R.id.sort_asc;
    boolean ignoreCase = true, sortFolders = true;
    //

    HashSet<File> selectedFiles;
    String currentDirectory;

    Explorer(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        selectedFiles = new HashSet<>();
        currentDirectory = getGlobalFileDir().getAbsolutePath();
    }

    File getGlobalFileDir() {
        if (Environment.isExternalStorageEmulated()) {
            return Environment.getExternalStorageDirectory();
        } else {
            return mainActivity.getFilesDir();
        }
    }

    void showDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            int countSelected = 0;
            int countFiles = 0;

            mainActivity.explorerElements.clear();
            currentDirectory = dir.getAbsolutePath();
            mainActivity.textPath.setText(currentDirectory);
            for (File file : dir.listFiles()) {
                boolean selected = false;

                if (!file.isDirectory()) {
                    if (selectedFiles.contains(file)) {
                        countSelected++;
                        selected = true;
                    }
                    countFiles++;
                }
                mainActivity.explorerElements.add(new ExplorerElement(file.getName(), new Date(file.lastModified()),
                        file.length(), file.isDirectory(), selected));
            }
            setAmount(countSelected, countFiles, dir.listFiles().length);
            sort();
            mainActivity.explorerElementsAdapter.notifyDataSetChanged();
        }
    }

    void select(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_CANCELED) {
            Pattern regex;
            try {
                regex = Pattern.compile(data.getStringExtra(SelectActivity.REGEX));


                switch (resultCode) {
                    case SelectActivity.SELECT:
                    case SelectActivity.UNSELECT:
                        for (ExplorerElement explorerElement : mainActivity.explorerElements) {
                            Matcher matcher = regex.matcher(explorerElement.getName());
                            if (!explorerElement.isFolder() && matcher.matches()) {
                                explorerElement.setSelected(resultCode == SelectActivity.SELECT);
                                if (explorerElement.isSelected()) {
                                    if (mainActivity.explorer.selectedFiles.add(new File(mainActivity.explorer.currentDirectory +
                                            "/" + explorerElement.getName()))) {
                                        mainActivity.selectedFiles.add(explorerElement.getName());
                                        mainActivity.explorer.addAmount(1);
                                    }
                                } else {
                                    if (mainActivity.explorer.selectedFiles.remove(new File(mainActivity.explorer.currentDirectory +
                                            "/" + explorerElement.getName()))) {
                                        mainActivity.selectedFiles.remove(explorerElement.getName());
                                        mainActivity.explorer.addAmount(-1);
                                    }
                                }
                                mainActivity.explorerElementsAdapter.notifyDataSetChanged();
                                mainActivity.selectedFilesAdapter.notifyDataSetChanged();
                            }
                        }
                        break;
                    case SelectActivity.INVERT:
                        for (ExplorerElement explorerElement : mainActivity.explorerElements) {
                            if (!explorerElement.isFolder()) {
                                explorerElement.setSelected(!explorerElement.isSelected());

                                //
                                if (explorerElement.isSelected()) {
                                    if (mainActivity.explorer.selectedFiles.add(new File(mainActivity.explorer.currentDirectory +
                                            "/" + explorerElement.getName()))) {
                                        mainActivity.selectedFiles.add(explorerElement.getName());
                                        mainActivity.explorer.addAmount(1);
                                    }
                                } else {
                                    if (mainActivity.explorer.selectedFiles.remove(new File(mainActivity.explorer.currentDirectory +
                                            "/" + explorerElement.getName()))) {
                                        mainActivity.selectedFiles.remove(explorerElement.getName());
                                        mainActivity.explorer.addAmount(-1);
                                    }
                                }
                                mainActivity.explorerElementsAdapter.notifyDataSetChanged();
                                mainActivity.selectedFilesAdapter.notifyDataSetChanged();
                                //
                            }
                        }
                        break;
                    default:
                        Toast.makeText(mainActivity.getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                        System.exit(4354);
                }
                mainActivity.explorerElementsAdapter.notifyDataSetChanged();
            } catch (PatternSyntaxException e) {
                Toast.makeText(mainActivity.getApplicationContext(), R.string.error_regex, Toast.LENGTH_SHORT).show();
            }
        }
    }

    void setSort(@IdRes int sortBy, @IdRes int sort, boolean ignoreCase, boolean sortFolders) {
        this.sortBy = sortBy;
        this.sort = sort;
        this.ignoreCase = ignoreCase;
        this.sortFolders = sortFolders;
        sort();
    }

    /*private void sort() {
        for (int i = mainActivity.explorerElements.size() - 1; i > 0; i--) {
            for (int j = 0; j < i; j++) {
                if (sortCondition(j)) {
                    ExplorerElement tmp = mainActivity.explorerElements.get(j);
                    mainActivity.explorerElements.set(j, mainActivity.explorerElements.get(j + 1));
                    mainActivity.explorerElements.set(j + 1, tmp);
                }
            }
        }
        mainActivity.explorerElementsAdapter.notifyDataSetChanged();
    }*/

    private void sort() {
        ArrayList<ExplorerElement> result = new ArrayList<>();
        int mid = 0;

        for (ExplorerElement explorerElement : mainActivity.explorerElements) {
            boolean tmp = true;
            for (int i = (!sortFolders || explorerElement.isFolder()) ? 0 : mid; i < ((sortFolders && explorerElement.isFolder()) ? mid : result.size()); i++) {
                if (sortCondition(explorerElement, result.get(i))) {
                    result.add(i, explorerElement);
                    tmp = false;
                    break;
                }
            }
            if (tmp) result.add((sortFolders && explorerElement.isFolder()) ? mid : result.size(), explorerElement);
            if (sortFolders && explorerElement.isFolder()) mid++;
        }
        //mainActivity.explorerElements = result;
        mainActivity.explorerElements.clear();
        mainActivity.explorerElements.addAll(result);
        mainActivity.explorerElementsAdapter.notifyDataSetChanged();
    }

    void explorer() {
        mainActivity.listMain.setAdapter(mainActivity.explorerElementsAdapter);

        mainActivity.listSpinner.setVisibility(View.VISIBLE);
        mainActivity.imageButtonUp.setVisibility(View.VISIBLE);
        mainActivity.textPath.setVisibility(View.VISIBLE);
        mainActivity.textAmount.setVisibility(View.VISIBLE);

        showDirectory(new File(currentDirectory));
    }

    void addAmount(int addCountSelected) {
        String previous = mainActivity.textAmount.getText().toString();
        String[] parameters = previous.replaceAll("\\)", "").replaceAll("\\(", "/").split("/");

        mainActivity.textAmount.setText(MessageFormat.format("{0}/{1}({2})", Integer.parseInt(parameters[0]) + addCountSelected, parameters[1], parameters[2]));
    }

    private void setAmount(int countSelected, int countFiles, int countAll) {
        if (countSelected >= 0 && countFiles >= 0 && countAll >= 0 && countSelected <= countFiles && countFiles <= countAll) {
            mainActivity.textAmount.setText(MessageFormat.format("{0}/{1}({2})", countSelected, countFiles, countAll));
        } else {
            Toast.makeText(mainActivity.getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
            System.exit(-1122);
        }
    }

    private boolean sortCondition(ExplorerElement explorerElementCompared, ExplorerElement explorerElement) {
        float compareTo = 0;
        //data.getBooleanExtra(SortActivity.SORT_FOLDERS, true);

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
                Toast.makeText(mainActivity.getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                System.exit(5454);
        }
        switch (sort) {
            case R.id.sort_asc:
                return compareTo > 0;
            case R.id.sort_desc:
                return compareTo <= 0;
            default:
                Toast.makeText(mainActivity.getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                System.exit(5493);
        }
        return false;
    }
}
