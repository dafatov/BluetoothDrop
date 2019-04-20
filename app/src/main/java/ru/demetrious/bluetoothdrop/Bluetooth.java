package ru.demetrious.bluetoothdrop;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

class Bluetooth {
    MainActivity mainActivity;

    Thread clientThread;
    Client client;
    Thread serverThread;
    Server server;

    BluetoothSocket clientSocket = null;
    BluetoothSocket serverSocket = null;

    Bluetooth(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        client = new Client();
        clientThread = new Thread(client, "Client");
        clientThread.setDaemon(true);

        server = new Server();
        serverThread = new Thread(server, "Server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    class Client implements Runnable {
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
                stop();
                return;
            }

            //TODO add transfer manger for client
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
                    serverSocket = serverAcceptSocket.accept();
                } catch (IOException i) {
                    i.printStackTrace();
                    break;
                }

                if (serverSocket != null) {
                    //TODO add transfer manager for server
                    stop();
                    break;
                }
            }
        }

        void stop() {
            try {
                serverAcceptSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
