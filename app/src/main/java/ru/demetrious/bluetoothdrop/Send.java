package ru.demetrious.bluetoothdrop;

import java.io.File;

class Send {
    private MainActivity mainActivity;

    private String[] filesPaths;
    private File[] files;
    private long[] filesParts;
    private long allParts;
    boolean stop;

    Thread send;

    Send(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    void stop() {
        stop = true;
    }

    private void prepareSend() {
        mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_STATUS_SET, "Processing of files");
        stop = false;

        filesPaths = mainActivity.explorer.selectedFiles.toArray(new String[0]);
        files = new File[filesPaths.length];
        filesParts = new long[filesPaths.length];
        String sharedDir = getSharedDirectory();
        allParts = 0;

        for (int i = 0; i < filesPaths.length && !stop; i++) {
            File file = new File(filesPaths[i]);
            long parts = file.length();

            filesPaths[i] = filesPaths[i].replaceFirst(sharedDir, "/");
            allParts += parts;
            files[i] = file;
            filesParts[i] = parts;
        }
    }

    void send() {
        send = new Thread(() -> {
            prepareSend();

            mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_PROGRESS_ALL_CHG, (int) Math.ceil((double) allParts / Bluetooth.PACKET_WIDTH), -1).sendToTarget();
            byte[] tmp = String.valueOf(allParts).getBytes();
            byte[] full = new byte[Bluetooth.PACKET_WIDTH];
            full[0] = (byte) (tmp.length / 256);
            full[1] = (byte) (tmp.length % 256);
            System.arraycopy(tmp, 0, full, 2, tmp.length);
            mainActivity.bluetooth.transferDate.write(full);//1

            for (int i = 0; i < files.length && !stop; i++) {
                mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_STATUS_SET, filesPaths[i]).sendToTarget();

                byte[] bytes = Explorer.toByteArray(files[i]);

                mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_PROGRESS_FILE_CHG, (int) Math.ceil((double) filesParts[i] / Bluetooth.PACKET_WIDTH), -1).sendToTarget();

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

                for (int j = 0; j < bytes.length && !stop; j += Bluetooth.PACKET_WIDTH - 2) {
                    full = new byte[full.length];
                    int size = Math.min(bytes.length - j, Bluetooth.PACKET_WIDTH - 2);
                    full[0] = (byte) (size / 256);
                    full[1] = (byte) (size % 256);
                    if (full[1] < 0) full[0]++;
                    System.arraycopy(bytes, j, full, 2, size);
                    mainActivity.bluetooth.transferDate.write(full);//4
                }
            }
            mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_ACTIVITY_FINISH).sendToTarget();
        });
        send.start();
    }

    /*void stop() {
        stop = true;
    }*/

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
