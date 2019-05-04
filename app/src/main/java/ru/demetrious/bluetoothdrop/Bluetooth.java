package ru.demetrious.bluetoothdrop;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

class Bluetooth {
    final static int PACKET_WIDTH = 990;

    MainActivity mainActivity;

    Thread clientThread;
    Client client;
    Thread serverThread;
    Server server;

    BluetoothSocket clientSocket = null;
    BluetoothSocket serverSocket = null;

    TransferDate transferDate = null;

    boolean connected = false;

    Bluetooth(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        client = null;
        clientThread = null;
        server = null;
        serverThread = null;
    }

    boolean startServer() {
        if (mainActivity.friends.bluetoothAdapter.isEnabled()) {
            server = new Server();
            serverThread = new Thread(server, "Server");
            serverThread.setDaemon(true);
            serverThread.start();
            return true;
        }
        return false;
    }

    void connect(int position) {
        client = new Client(position);
        clientThread = new Thread(client, "Client");
        clientThread.start();
    }

    private class Client implements Runnable {
        int position;
        BluetoothDevice device;

        Client(int position) {
            this.position = position;
            synchronized (mainActivity.friendsElements) {
                this.device = mainActivity.friendsElements.get(position).getBluetoothDevice();
            }
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
                Log.e("ClientError", device.getAddress());
                stop();
                return;
            }

            //TODO add transfer manger for client
            transferDate = new TransferDate(clientSocket);
            new Thread(transferDate, "TransferDataClient").start();
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
                    //TODO add transfer manager for server
                    transferDate = new TransferDate(serverSocket);
                    new Thread(transferDate, "TransferDataServer").start();
                    stop();
                    break;
                }
            }
        }

        void stop() {
            try {
                serverAcceptSocket.close();
                //transferDate.stop();
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
        boolean wait = true;

        TransferDate(BluetoothSocket socket) {
            this.socket = socket;
            try {
                input = socket.getInputStream();
                output = socket.getOutputStream();
                MainActivity.handler.obtainMessage(MainActivity.HANDLER_CONNECTED).sendToTarget();
                Log.e("ClientConnect", socket.getRemoteDevice().getAddress());
                //
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.e("ClientConnect", String.valueOf(socket.getConnectionType()));
                }
                //
                connected = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[PACKET_WIDTH];
            int bytes = -1;

            while (true) {
                try {
                    buffer = new byte[buffer.length];
                    bytes = input.read(buffer);
                    //
                    Log.e("READ", FileManager.log(buffer, bytes) + "." + System.currentTimeMillis());
                    //
                    Test.handler.obtainMessage(Test.HANDLER_RECEIVED_PART, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        void write(byte[] bytes) {
            try {
                output.write(bytes);
                output.flush();
                //
                Log.e("SEND", FileManager.log(bytes, 0) + "." + System.currentTimeMillis());
                //
                LoadActivity.handler.obtainMessage(LoadActivity.HANDLER_PROGRESS_INC).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Вызывается для отключения
        void stop() {
            try {
                MainActivity.handler.obtainMessage(MainActivity.HANDLER_DISCONNECTED).sendToTarget();
                Log.e("ClientDisconnect", socket.getRemoteDevice().getAddress());
                connected = false;
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
