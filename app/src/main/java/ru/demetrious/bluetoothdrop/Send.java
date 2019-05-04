package ru.demetrious.bluetoothdrop;

import android.util.Log;

import java.io.File;

class Send {
    private MainActivity mainActivity;

    private String[] filesPaths;
    private File[] files;
    private long[] filesParts;
    private long allParts;

    Send(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    //TODO переделать передачу: передаваться должен полный объем файла а не кол-во частей + -> проверить
    private void prepareSend() {
        LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_STATUS_SET, "Processing of files");

        filesPaths = mainActivity.explorer.selectedFiles.toArray(new String[0]);
        files = new File[filesPaths.length];
        filesParts = new long[filesPaths.length];
        String sharedDir = getSharedDirectory();
        allParts = 0;

        Log.e("PrepareSend", sharedDir);

        for (int i = 0; i < filesPaths.length; i++) {
            File file = new File(filesPaths[i]);
            long parts = file.length();

            filesPaths[i] = filesPaths[i].replaceFirst(sharedDir, "/");
            allParts += parts;
            files[i] = file;
            filesParts[i] = parts;
        }
        Log.e("PrepareSend", String.valueOf(allParts));
    }

    void send() {
        prepareSend();
        new Thread(() -> {
            LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_PROGRESS_ALL_CHG, (int) Math.ceil((double) allParts / Bluetooth.PACKET_WIDTH), -1).sendToTarget();
            byte[] tmp = String.valueOf(allParts).getBytes();
            byte[] full = new byte[Bluetooth.PACKET_WIDTH];
            full[0] = (byte) (tmp.length / 256);
            full[1] = (byte) (tmp.length % 256);
            System.arraycopy(tmp, 0, full, 2, tmp.length);
            mainActivity.bluetooth.transferDate.write(full);//1

            for (int i = 0; i < files.length; i++) {
                LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_STATUS_SET, filesPaths[i]).sendToTarget();

                byte[] bytes = Explorer.toByteArray(files[i]);

                LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_PROGRESS_FILE_CHG, (int) Math.ceil((double) filesParts[i] / Bluetooth.PACKET_WIDTH), -1).sendToTarget();

                tmp = String.valueOf(filesParts[i]).getBytes();
                full = new byte[full.length];
                full[0] = (byte) (tmp.length / 256);
                full[1] = (byte) (tmp.length % 256);
                System.arraycopy(tmp, 0, full, 2, tmp.length);
                mainActivity.bluetooth.transferDate.write(full);//2

                tmp = filesPaths[i].getBytes();
                full = new byte[full.length];
                full[0] = (byte) (tmp.length / 256);
                full[1] = (byte) (tmp.length % 256);
                System.arraycopy(tmp, 0, full, 2, tmp.length);
                mainActivity.bluetooth.transferDate.write(full);//3

                for (int j = 0; j < bytes.length; j += Bluetooth.PACKET_WIDTH - 2) {
                    full = new byte[full.length];
                    int size = Math.min(bytes.length - j, Bluetooth.PACKET_WIDTH - 2);
                    full[0] = (byte) (size / 256);
                    full[1] = (byte) (size % 256);
                    if (full[1] < 0) full[0]++;
                    Log.e("SizeSend", size + "." + full[0] + "." + full[1]);
                    System.arraycopy(bytes, j, full, 2, size);
                    mainActivity.bluetooth.transferDate.write(full);//4
                }
            }
            LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_ACTIVITY_FINISH).sendToTarget();
        }).start();
    }

    private String getSharedDirectory() {
        String backup = "";
        String[] folders = filesPaths[0].split("/");
        StringBuilder tmp = new StringBuilder();
        boolean stop = true;
        for (int i = 0; stop && i < folders.length; i++) {
            backup = tmp.toString();
            tmp.append(folders[i]).append("/");
            for (String filesPath : filesPaths) {
                if (!filesPath.contains(tmp.toString())) {
                    stop = false;
                    break;
                }
            }
        }
        return backup;
    }
}
