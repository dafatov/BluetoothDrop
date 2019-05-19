package ru.demetrious.bluetoothdrop.bluetooth;

import java.io.File;

import ru.demetrious.bluetoothdrop.FileManager;
import ru.demetrious.bluetoothdrop.R;
import ru.demetrious.bluetoothdrop.activities.LoadActivity;
import ru.demetrious.bluetoothdrop.activities.MainActivity;

public class Send {
    private MainActivity mainActivity;

    private String[] filesPaths;
    private File[] files;
    private long[] filesParts;
    private long allParts;
    private boolean stop;

    private Thread send;

    public Send(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    void stop() {
        stop = true;
    }

    private void prepareSend() {
        mainActivity.getBluetooth().getHandlerLoadActivity().obtainMessage(LoadActivity.HANDLER_STATUS_SET, R.string.send_process);
        stop = false;

        filesPaths = mainActivity.getExplorer().selectedFiles.toArray(new String[0]);
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

    public void send() {
        send = new Thread(() -> {
            prepareSend();

            if (allParts > 0) {
                mainActivity.getBluetooth().getHandlerLoadActivity().obtainMessage(LoadActivity.HANDLER_PROGRESS_ALL_CHG, (int) Math.ceil((double) allParts / Bluetooth.PACKET_WIDTH), -1).sendToTarget();
                byte[] tmp = String.valueOf(allParts).getBytes();
                byte[] full = new byte[Bluetooth.PACKET_WIDTH];
                full[0] = (byte) (tmp.length / 256);
                full[1] = (byte) (tmp.length % 256);
                System.arraycopy(tmp, 0, full, 2, tmp.length);
                mainActivity.getBluetooth().getTransferDate().write(full);//1


                for (int i = 0; i < files.length && !stop; i++) {
                    if (filesParts[i] > 0) {
                        mainActivity.getBluetooth().getHandlerLoadActivity().obtainMessage(LoadActivity.HANDLER_STATUS_SET, filesPaths[i]).sendToTarget();
                        mainActivity.getBluetooth().getHandlerLoadActivity().obtainMessage(LoadActivity.HANDLER_PROGRESS_FILE_CHG, (int) Math.ceil((double) filesParts[i] / Bluetooth.PACKET_WIDTH), -1).sendToTarget();

                        tmp = String.valueOf(filesParts[i]).getBytes();
                        full = new byte[full.length];
                        full[0] = (byte) (tmp.length / 256);
                        full[1] = (byte) (tmp.length % 256);
                        System.arraycopy(tmp, 0, full, 2, tmp.length);
                        mainActivity.getBluetooth().getTransferDate().write(full);//2

                        tmp = filesPaths[i].getBytes();
                        full = new byte[full.length];
                        full[0] = (byte) (tmp.length / 256);
                        full[1] = (byte) (tmp.length % 256);
                        System.arraycopy(tmp, 0, full, 2, tmp.length);
                        mainActivity.getBluetooth().getTransferDate().write(full);//3
                        if (!stop) FileManager.openFileInputStream(files[i]);
                        for (int j = 0; j < filesParts[i] && !stop; j += Bluetooth.PACKET_WIDTH - 2) {
                            full = new byte[full.length];
                            int size = (int) Math.min(filesParts[i] - j, Bluetooth.PACKET_WIDTH - 2);
                            tmp = FileManager.read(size);
                            full[0] = (byte) (size / 256);
                            full[1] = (byte) (size % 256);
                            if (full[1] < 0) full[0]++;
                            System.arraycopy(tmp, 0, full, 2, size);
                            mainActivity.getBluetooth().getTransferDate().write(full);//4
                        }
                        FileManager.closeFileInputStream();
                    }
                }
                mainActivity.getBluetooth().setTransferring(false);
            } else {
                MainActivity.getHandler().obtainMessage(MainActivity.HANDLER_EMPTY_SEND).sendToTarget();
            }
            mainActivity.getBluetooth().getHandlerLoadActivity().obtainMessage(LoadActivity.HANDLER_ACTIVITY_FINISH).sendToTarget();
        });
        getSend().start();
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

    Thread getSend() {
        return send;
    }
}
