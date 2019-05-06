package ru.demetrious.bluetoothdrop;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

class Received extends HandlerThread {
    final static int HANDLER_RECEIVED_PART = 0;
    static Handler handler;

    private MainActivity mainActivity;
    private String currentFile = "";
    private PartType partType = PartType.receivedAllSize;
    private long allSize;
    private int fileSize, receivedFileSize = 0;
    private byte[] buffer, tmp, file;
    private int size;

    enum PartType {
        receivedAllSize, receivedFileSize, receivedPart, receivedFileName
    }

    Received(MainActivity mainActivity, String name) {
        super(name);
        this.mainActivity = mainActivity;
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
                        if (mainActivity.send.send != null && mainActivity.send.send.isAlive()) {
                            mainActivity.bluetooth.transferDate.cancelClient();
                        }
                        handler.removeMessages(HANDLER_RECEIVED_PART);
                        stopReceive();
                        return;
                    }

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
                            file = new byte[fileSize];
                            mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_PROGRESS_FILE_CHG, (int) Math.ceil((double) fileSize / Bluetooth.PACKET_WIDTH), -1).sendToTarget();
                            partType = PartType.receivedFileName;
                            break;
                        case receivedFileName:
                            tmp = new byte[size];
                            System.arraycopy(buffer, 2, tmp, 0, size);
                            currentFile = new String(tmp, 0, size);
                            mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_STATUS_SET, currentFile).sendToTarget();
                            partType = PartType.receivedPart;
                            break;
                        case receivedPart:
                            System.arraycopy(buffer, 2, file, receivedFileSize, size);
                            receivedFileSize += size;
                            allSize -= size;
                            mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_PROGRESS_INC).sendToTarget();
                            if (fileSize == receivedFileSize) {
                                receivedFileSize = 0;
                                if (!Explorer.saveFile(file, currentFile))
                                    MainActivity.handler.obtainMessage(MainActivity.HANDLER_SAVE_FILE_ERROR, currentFile).sendToTarget();
                                if (allSize == 0)
                                    stopReceive();
                                else if (allSize > 0)
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
        receivedFileSize = 0;
        mainActivity.bluetooth.handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_ACTIVITY_FINISH).sendToTarget();
    }
}




