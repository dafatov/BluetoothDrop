package ru.demetrious.bluetoothdrop;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;

import java.io.File;

class Received extends HandlerThread {
    final static int HANDLER_RECEIVED_PART = 0;
    static Handler handler;

    private MainActivity mainActivity;
    private String currentFile = "";
    private byte currentStatus = -2;
    private PartType partType = PartType.receivedAllSize;
    private long allSize;
    private int fileSize;
    private byte[] buffer, tmp;
    private int size;
    private StatFs statFs;
    private boolean blocked = false;

    enum PartType {
        receivedAllSize, receivedFileSize, receivedPart, receivedFileName
    }

    Received(MainActivity mainActivity, String name) {
        super(name);
        this.mainActivity = mainActivity;
        statFs = new StatFs(Settings.DEFAULT_HOME_PATH);
    }

    @Override
    protected void onLooperPrepared() {
        handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == HANDLER_RECEIVED_PART) {
                    buffer = ((byte[]) msg.obj);
                    size = buffer[0] * 256 + buffer[1];
                    //Обработчик прерывания передачи со стороны сервера
                    if (size == 0) {
                        /*if (mainActivity.send.send != null && mainActivity.send.send.isAlive()) {
                            mainActivity.bluetooth.transferDate.cancelClient();

                        }*/
                        mainActivity.bluetooth.transferDate.cancel();
                        handler.removeMessages(HANDLER_RECEIVED_PART);
                        if (blocked = true) {
                            blocked = false;
                        }
                        stopReceive();
                        return;
                    }

                    if (blocked) return;

                    switch (partType) {
                        case receivedAllSize:
                            MainActivity.handler.obtainMessage(MainActivity.HANDLER_RECEIVE_START).sendToTarget();
                            try {
                                while (mainActivity.bluetooth.handlerLoadActivity == null) {
                                    Thread.sleep(50);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            tmp = new byte[size];
                            System.arraycopy(buffer, 2, tmp, 0, size);
                            allSize = Long.parseLong(new String(tmp, 0, size));

                            mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_PROGRESS_ALL_CHG, (int) Math.ceil((double) allSize / Bluetooth.PACKET_WIDTH), -1).sendToTarget();
                            partType = PartType.receivedFileSize;
                            break;
                        case receivedFileSize:
                            tmp = new byte[size];
                            System.arraycopy(buffer, 2, tmp, 0, size);
                            fileSize = Integer.parseInt(new String(tmp, 0, size));
                            //file = new byte[fileSize];
                            mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_PROGRESS_FILE_CHG, (int) Math.ceil((double) fileSize / Bluetooth.PACKET_WIDTH), -1).sendToTarget();
                            partType = PartType.receivedFileName;
                            break;
                        case receivedFileName:
                            tmp = new byte[size];
                            System.arraycopy(buffer, 2, tmp, 0, size);
                            currentFile = Settings.getPreference(Settings.APP_SETTING_SAVE_PATH, Settings.DEFAULT_SAVE_PATH, String.class) + new String(tmp, 0, size);
                            //
                            long available = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2 ? statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong() : statFs.getAvailableBlocks() * statFs.getBlockSize();
                            Log.e("AVAILABLE", available + " байт");
                            if (fileSize >= available) {
                                handler.removeMessages(HANDLER_RECEIVED_PART);
                                blocked = true;
                                mainActivity.bluetooth.transferDate.cancel();
                                MainActivity.handler.obtainMessage(MainActivity.HANDLER_OUT_OF_MEMORY).sendToTarget();
                                stopReceive();
                                return;
                            }
                            //
                            currentStatus = FileManager.openFileOutputStream(currentFile);
                            Log.e("STATUS", String.valueOf(currentStatus));
                            switch (currentStatus) {
                                case FileManager.FILE_CREATED:
                                    break;
                                case FileManager.FILE_DIRECTORY_ERROR:
                                    handler.removeMessages(HANDLER_RECEIVED_PART);
                                    blocked = true;
                                    mainActivity.bluetooth.transferDate.cancel();
                                    MainActivity.handler.obtainMessage(MainActivity.HANDLER_ERROR_CREATE_DIRECTORY).sendToTarget();
                                    stopReceive();
                                    return;
                                case FileManager.FILE_EXCEPTION:
                                case FileManager.FILE_CREATE_ERROR:
                                    handler.removeMessages(HANDLER_RECEIVED_PART);
                                    blocked = true;
                                    mainActivity.bluetooth.transferDate.cancel();
                                    MainActivity.handler.obtainMessage(MainActivity.HANDLER_ERROR_CREATE_FILE).sendToTarget();
                                    stopReceive();
                                    return;
                            }
                            //
                            mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_STATUS_SET, new String(tmp, 0, size)).sendToTarget();
                            partType = PartType.receivedPart;
                            break;
                        case receivedPart:
                            tmp = new byte[size];
                            /*System.arraycopy(buffer, 2, file, receivedFileSize, size);
                            receivedFileSize += size;
                            allSize -= size;
                            mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_PROGRESS_INC).sendToTarget();
                            if (fileSize == receivedFileSize) {
                                receivedFileSize = 0;
                                if (!Explorer.saveFile(file, currentFile))
                                    MainActivity.handler.obtainMessage(MainActivity.HANDLER_OUT_OF_MEMORY, currentFile).sendToTarget();
                                if (allSize == 0)
                                    stopReceive();
                                else if (allSize > 0)
                                    partType = PartType.receivedFileSize;
                                else
                                    Log.e("ERROR", "MainActivity.handler.HANDLER_RECEIVED_PART.receivedPart.allSize<0");
                            }
                            break;*/
                            System.arraycopy(buffer, 2, tmp, 0, size);
                            fileSize -= size;
                            allSize -= size;
                            FileManager.write(tmp);
                            mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_PROGRESS_INC).sendToTarget();
                            if (fileSize == 0) {
                                if (allSize == 0) {
                                    stopReceive();
                                } else if (allSize > 0)
                                    partType = PartType.receivedFileSize;
                                else
                                    Log.e("ERROR", "MainActivity.handler.HANDLER_RECEIVED_PART.receivedPart.allSize<0");
                            }
                            break;
                    }
                } else {
                    Log.e("ERROR", "Received.handler.error");
                }
            }
        };
    }

    private void stopReceive() {
        partType = PartType.receivedAllSize;

        mainActivity.bluetooth.isTransferring = false;

        if (fileSize != 0 && currentStatus != FileManager.FILE_CREATE_ERROR) {
            new File(currentFile).delete();
        }

        if (mainActivity.bluetooth.isServer) {
            FileManager.closeFileOutputStream();
            try {
                while (mainActivity.bluetooth.handlerLoadActivity == null) {
                    Thread.sleep(50);
                }
                if (!blocked)
                    mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_ACTIVITY_FINISH).sendToTarget();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}




