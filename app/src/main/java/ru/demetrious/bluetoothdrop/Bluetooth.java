package ru.demetrious.bluetoothdrop;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

class Bluetooth {
    final static int PACKET_WIDTH = 990;
    Handler handlerLoadActivity;

    private MainActivity mainActivity;

    private Thread clientThread;
    private Client client;
    private Thread serverThread;
    Server server;

    private BluetoothSocket clientSocket = null;
    private BluetoothSocket serverSocket = null;

    TransferDate transferDate = null;
    Thread transfer = null;
    boolean isServer = false;
    boolean isTransferring = false;

    BluetoothDevice device = null;

    Bluetooth(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        client = null;
        clientThread = null;
        server = null;
        serverThread = null;
    }

    void startServer() {
        if (mainActivity.friends.bluetoothAdapter.isEnabled()) {
            server = new Server();
            serverThread = new Thread(server, "Server");
            serverThread.setDaemon(true);
            serverThread.start();
        }
    }

    void connect(BluetoothDevice device) {
        client = new Client(device);
        clientThread = new Thread(client, "Client");
        clientThread.start();
    }

    private class Client implements Runnable {
        Client(BluetoothDevice device) {
            try {
                clientSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(mainActivity.getString(R.string.UUID)));
            } catch (IOException i) {
                i.printStackTrace();
            }
        }

        @Override
        public void run() {
            if (mainActivity.friends.bluetoothAdapter.isDiscovering())
                mainActivity.friends.bluetoothAdapter.cancelDiscovery();
            try {
                clientSocket.connect();
            } catch (IOException io) {
                //Log.e("ClientError", tmp.getAddress());
                stop();
                return;
            }

            transferDate = new TransferDate(clientSocket);
            transfer = new Thread(transferDate, "TransferDataClient");
            transfer.start();
            server.stop();
        }

        void stop() {
            try {
                clientSocket.close();
            } catch (IOException i) {
                i.printStackTrace();
            }
        }
    }

    class Server implements Runnable {
        BluetoothServerSocket serverAcceptSocket = null;

        Server() {
            try {
                serverAcceptSocket = mainActivity.friends.bluetoothAdapter.listenUsingRfcommWithServiceRecord(mainActivity.getString(R.string.app_name), UUID.fromString(mainActivity.getString(R.string.UUID)));
            } catch (IOException i) {
                i.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Log.e("Server", "Is start");
                    serverSocket = serverAcceptSocket.accept();
                } catch (IOException i) {
                    break;
                }

                if (serverSocket != null) {
                    transferDate = new TransferDate(serverSocket);
                    transfer = new Thread(transferDate, "TransferDataServer");
                    transfer.start();
                    stop();
                    break;
                }
            }
        }

        void stop() {
            try {
                serverAcceptSocket.close();
                Log.e("Server", "Is stop");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class TransferDate implements Runnable {
        private final BluetoothSocket socket;
        private InputStream input = null;
        private OutputStream output = null;

        TransferDate(BluetoothSocket socket) {
            this.socket = socket;
            try {
                input = socket.getInputStream();
                output = socket.getOutputStream();
                device = socket.getRemoteDevice();
                MainActivity.handler.obtainMessage(MainActivity.HANDLER_CONNECTED).sendToTarget();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.e("MaxReceivePacketSize", String.valueOf(socket.getMaxReceivePacketSize()));
                    Log.e("MaxTransmitPacketSize", String.valueOf(socket.getMaxTransmitPacketSize()));
                }
                Log.e("ClientConnect", socket.getRemoteDevice().getAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[PACKET_WIDTH];

            while (true) {
                try {
                    buffer = new byte[buffer.length];
                    input.read(buffer);
                    Received.handler.obtainMessage(Received.HANDLER_RECEIVED_PART, buffer).sendToTarget();
                    //
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[");
                    for (int i = 0; i < buffer.length; i++) {
                        stringBuilder.append(buffer[i]);
                        if (i == buffer.length - 1) stringBuilder.append("]");
                        else stringBuilder.append(",");
                    }
                    //Log.e("PACKET", stringBuilder.toString());
                    //
                } catch (IOException e) {
                    Log.e("ClientDisconnect", socket.getRemoteDevice().getAddress());
                    device = null;
                    MainActivity.handler.obtainMessage(MainActivity.HANDLER_DISCONNECTED).sendToTarget();
                    break;
                }
            }
        }

        void write(byte[] bytes) {
            try {
                output.write(bytes);
                output.flush();
                handlerLoadActivity.obtainMessage(LoadActivity.HANDLER_PROGRESS_INC).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Вызывается для отключения соединения
        void stop() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void cancel() {
            if (isTransferring) {
                if (isServer)
                    cancelServer();
                else
                    cancelClient();
                isTransferring = false;
            }
        }

        //Вызвается для отключения передачи данных
        private void cancelClient() {
            mainActivity.send.stop();
            try {
                mainActivity.send.send.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            write(new byte[]{0, 0});
            Log.e("Bluetooth", "ClientStop");
        }

        private void cancelServer() {
            write(new byte[]{0, 0});
            Log.e("Bluetooth", "ServerStop");
        }
    }
}
