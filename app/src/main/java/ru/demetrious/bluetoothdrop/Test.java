package ru.demetrious.bluetoothdrop;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

class Test extends HandlerThread {
    final static int HANDLER_RECEIVED_PART = 0;
    static Handler handler;

    String currentFile = "";
    PartType partType = PartType.receivedAllSize;
    long allSize;
    int fileSize, receivedFileSize = 0;
    byte[] file;

    Test(String name) {
        super(name);
    }

    @Override
    protected void onLooperPrepared() {
        handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case HANDLER_RECEIVED_PART:
                        byte[] buffer = ((byte[]) msg.obj);

                        switch (partType) {
                            case receivedAllSize:
                                MainActivity.handler.obtainMessage(MainActivity.HANDLER_RECEIVED_START).sendToTarget();

                                //
                                int size = buffer[0]*256+buffer[1];
                                byte[] tmp = new byte[size];
                                System.arraycopy(buffer, 2, tmp, 0, size);
                                //
                                allSize = Long.parseLong(new String(tmp, 0, size));
                                Log.e("AllSizeReceived", String.valueOf(allSize));

                                try {
                                    while (LoadActivity.handler == null) {
                                        Thread.sleep(50);
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                assert LoadActivity.handler != null;
                                LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_PROGRESS_ALL_CHG, (int) Math.ceil((double) allSize/Bluetooth.PACKET_WIDTH), -1).sendToTarget();
                                partType = PartType.receivedFileSize;
                                break;
                            case receivedFileSize:
                                //
                                size = buffer[0]*256+buffer[1];
                                tmp = new byte[size];
                                System.arraycopy(buffer, 2, tmp, 0, size);
                                //
                                fileSize = Integer.parseInt(new String(tmp, 0, size));
                                file = new byte[fileSize];
                                LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_PROGRESS_FILE_CHG, (int) Math.ceil((double) fileSize/Bluetooth.PACKET_WIDTH), -1).sendToTarget();
                                partType = PartType.receivedFileName;
                                break;
                            case receivedFileName:
                                //
                                size = buffer[0]*256+buffer[1];
                                tmp = new byte[size];
                                System.arraycopy(buffer, 2, tmp, 0, size);
                                //
                                currentFile = new String(tmp, 0, size);
                                LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_STATUS_SET, currentFile).sendToTarget();
                                partType = PartType.receivedPart;
                                break;
                            case receivedPart:
                                assert file != null;
                                //
                                size = buffer[0]*256+buffer[1];
                                Log.e("SizeReceived", size + "." + buffer[0] + "." + buffer[1]);
                                System.arraycopy(buffer, 2, file, receivedFileSize, size);
                                //
                                receivedFileSize += size;
                                allSize-=size;
                                LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_PROGRESS_INC).sendToTarget();
                                if (fileSize == receivedFileSize) {
                                    if (!Explorer.saveFile(file, currentFile))
                                        Log.e("SaveFileReceived", "Error save File: \"" + currentFile + "\"");
                                    receivedFileSize = 0;
                                    if (allSize == 0) {
                                        partType = PartType.receivedAllSize;
                                        LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_ACTIVITY_FINISH).sendToTarget();
                                    } else if (allSize > 0) {
                                        partType = PartType.receivedFileSize;
                                    } else {
                                        Log.e("ERROR", "MainActivity.handler.HANDLER_RECEIVED_PART.receivedPart.allSize<0");
                                        //System.exit(45784574);
                                    }
                                }
                                break;
                        }
                        break;
                }
            }
        };
    }

    @Override
    public void run() {
        super.run();
    }
}




