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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class Explorer {
    private MainActivity mainActivity;

    //
    private int sortBy = R.id.sort_name, sort = R.id.sort_asc;
    private boolean ignoreCase = true, sortFolders = true;
    //

    ArrayList<String> selectedFiles;
    String currentDirectory;

    Explorer(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        selectedFiles = new ArrayList<>();
        //currentDirectory = Settings.DEFAULT_HOME_PATH;
        //Log.e("String", currentDirectory);
    }

    void explorer() {
        mainActivity.listMain.setAdapter(mainActivity.explorerElementsAdapter);

        mainActivity.imageButtonUp.setVisibility(View.VISIBLE);
        mainActivity.textAmount.setVisibility(View.VISIBLE);
        mainActivity.textPath.setVisibility(View.VISIBLE);
        mainActivity.imageButtonHome.setVisibility(View.VISIBLE);
        mainActivity.listSpinner.setVisibility(View.VISIBLE);
        mainActivity.imageButtonRefresh.setVisibility(View.VISIBLE);

        mainActivity.imageButtonHome.setImageResource(R.drawable.ic_action_home);

        showDirectory(new File(currentDirectory));
    }

    File getGlobalFileDir() {
        if (Environment.isExternalStorageEmulated()) {
            return Environment.getExternalStorageDirectory();
        } else {
            return /*mainActivity.getFilesDir();*/new File("/storage/emmc");
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
                    if (selectedFiles.contains(file.getAbsolutePath())) {
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
                regex = Pattern.compile(data.getStringExtra(SelectActivity.EXTRA_REGEX));

                switch (resultCode) {
                    case SelectActivity.SELECT:
                    case SelectActivity.UNSELECT:
                        for (ExplorerElement explorerElement : mainActivity.explorerElements) {
                            Matcher matcher = regex.matcher(explorerElement.getName());
                            if (!explorerElement.isFolder() && matcher.matches()) {
                                explorerElement.setSelected(resultCode == SelectActivity.SELECT);
                                if (explorerElement.isSelected()) {
                                    if (mainActivity.explorer.selectedFiles.add(mainActivity.explorer.currentDirectory +
                                            "/" + explorerElement.getName())) {
                                        mainActivity.selectedFiles.add(explorerElement.getName());
                                        mainActivity.explorer.addAmount(1);
                                    }
                                } else {
                                    if (mainActivity.explorer.selectedFiles.remove(mainActivity.explorer.currentDirectory +
                                            "/" + explorerElement.getName())) {
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
                                    if (mainActivity.explorer.selectedFiles.add(mainActivity.explorer.currentDirectory +
                                            "/" + explorerElement.getName())) {
                                        mainActivity.selectedFiles.add(explorerElement.getName());
                                        mainActivity.explorer.addAmount(1);
                                    }
                                } else {
                                    if (mainActivity.explorer.selectedFiles.remove(mainActivity.explorer.currentDirectory +
                                            "/" + explorerElement.getName())) {
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
                        Toast.makeText(mainActivity.getApplicationContext(), mainActivity.getString(R.string.error), Toast.LENGTH_LONG).show();
                        System.exit(4354);
                }
                mainActivity.explorerElementsAdapter.notifyDataSetChanged();
            } catch (PatternSyntaxException e) {
                Toast.makeText(mainActivity.getApplicationContext(), mainActivity.getString(R.string.error_regex), Toast.LENGTH_SHORT).show();
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
            if (tmp)
                result.add((sortFolders && explorerElement.isFolder()) ? mid : result.size(), explorerElement);
            if (sortFolders && explorerElement.isFolder()) mid++;
        }
        //mainActivity.explorerElements = result;
        mainActivity.explorerElements.clear();
        mainActivity.explorerElements.addAll(result);
        mainActivity.explorerElementsAdapter.notifyDataSetChanged();
    }

    void addAmount(int addCountSelected) {
        String previous = mainActivity.textAmount.getText().toString();
        String[] parameters = previous.replaceAll("\\)", "").replaceAll("\\(", "/").split("/");//change values of ammout files in current directory

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

    /*static byte[] toByteArray(File file) {
        FileInputStream fileInputStream;
        byte[] byteArray = new byte[((int) file.length())];

        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(byteArray);
            fileInputStream.close();
        } catch (FileNotFoundException f) {
            f.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        }
        return byteArray;
    }*/
/*
    static byte[] toByteArray(File file, int start, int length) {
        FileInputStream fileInputStream;
        byte[] byteArray = new byte[length];

        try {
            if (start == 0) fileInputStream = new FileInputStream(file);
            fileInputStream.read(byteArray, start, length);
        } catch (IOException io) {
            io.printStackTrace();
        }
        return byteArray;
    }

    static boolean saveFile(byte[] bytes, String name) {
        File file = new File(Settings.getPreference(Settings.APP_PREFERENCES_SAVE_PATH, Settings.DEFAULT_SAVE_PATH, String.class) + name);
        FileOutputStream fileOutputStream;

        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) return false;
        }
        try {
            if (!file.createNewFile()) return false;
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes);
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }*/
}
