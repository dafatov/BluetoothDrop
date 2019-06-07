package ru.demetrious.bluetoothdrop.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import ru.demetrious.bluetoothdrop.R;
import ru.demetrious.bluetoothdrop.activities.MainActivity;
import ru.demetrious.bluetoothdrop.friends.FriendsElement;

public class Bluetooth {
    final static int PACKET_WIDTH = 990;
    private Handler handlerLoadActivity;

    private MainActivity mainActivity;

    private Thread clientThread;
    private Client client;
    private Thread serverThread;
    private Server server;

    private BluetoothSocket clientSocket = null;

    private TransferDate transferDate = null;
    private boolean isServer = false;
    private boolean isTransferring = false;
    private final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            assert action != null;
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    boolean isBonded = false;
                    for (FriendsElement friendsElement : mainActivity.getFriendsElements()) {
                        if (friendsElement.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                            friendsElement.setOnline();
                            isBonded = true;
                            break;
                        }
                    }
                    if (!isBonded && device.getName() != null) {
                        boolean tmp = true;
                        for (int i = mainActivity.getFriends().getBluetoothAdapter().getBondedDevices().size(); i < mainActivity.getFriendsElements().size(); i++) {
                            if (device.getName().compareToIgnoreCase(mainActivity.getFriendsElements().get(i).getBluetoothDevice().getName()) < 0) {
                                mainActivity.getFriendsElements().add(i, new FriendsElement(device, true));
                                tmp = false;
                                break;
                            }
                        }
                        if (tmp)
                            mainActivity.getFriendsElements().add(new FriendsElement(device, true));
                    }
                    mainActivity.getFriendsElementAdapter().notifyDataSetChanged();
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON)) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            if (isTransferring)
                                transferDate.cancel();
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            server.stop();
                            if (mainActivity.getNavigation().getSelectedItemId() == R.id.navigation_friends) {
                                mainActivity.getNavigation().setSelectedItemId(R.id.navigation_explorer);
                                mainActivity.getExplorer().explorer();
                            }
                            break;
                        case BluetoothAdapter.STATE_ON:
                            startServer();
                            break;
                    }
                    break;
            }
        }
    };
    private BluetoothDevice device = null;

    public Bluetooth(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        client = null;
        clientThread = null;
        server = null;
        serverThread = null;
    }

    public void startServer() {
        if (mainActivity.getFriends().getBluetoothAdapter().isEnabled()) {
            server = new Server();
            serverThread = new Thread(server, "Server");
            serverThread.setDaemon(true);
            serverThread.start();
        }
    }

    public void connect(BluetoothDevice device) {
        client = new Client(device);
        clientThread = new Thread(client, "Client");
        clientThread.start();
    }

    Handler getHandlerLoadActivity() {
        return handlerLoadActivity;
    }

    public void setHandlerLoadActivity(Handler handlerLoadActivity) {
        this.handlerLoadActivity = handlerLoadActivity;
    }

    public TransferDate getTransferDate() {
        return transferDate;
    }

    boolean isServer() {
        return isServer;
    }

    public void setServer(boolean server) {
        isServer = server;
    }

    public boolean isTransferring() {
        return isTransferring;
    }

    public void setTransferring(boolean transferring) {
        isTransferring = transferring;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public BroadcastReceiver getBluetoothBroadcastReceiver() {
        return bluetoothBroadcastReceiver;
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
            mainActivity.getFriends().stopDiscovery();
            try {
                clientSocket.connect();
            } catch (IOException io) {
                stop();
                return;
            }
            transferDate = new TransferDate(clientSocket);
            new Thread(getTransferDate(), "TransferDataClient").start();
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

    public class Server implements Runnable {
        BluetoothServerSocket serverAcceptSocket = null;

        Server() {
            try {
                serverAcceptSocket = mainActivity.getFriends().getBluetoothAdapter().listenUsingRfcommWithServiceRecord(mainActivity.getString(R.string.app_name), UUID.fromString(mainActivity.getString(R.string.UUID)));
            } catch (IOException i) {
                i.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                BluetoothSocket serverSocket;
                try {
                    serverSocket = serverAcceptSocket.accept();
                } catch (IOException i) {
                    break;
                }

                if (serverSocket != null) {
                    transferDate = new TransferDate(serverSocket);
                    new Thread(getTransferDate(), "TransferDataServer").start();
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

    public class TransferDate implements Runnable {
        private final BluetoothSocket socket;
        private InputStream input = null;
        private OutputStream output = null;

        TransferDate(BluetoothSocket socket) {
            this.socket = socket;
            try {
                input = socket.getInputStream();
                output = socket.getOutputStream();
                device = socket.getRemoteDevice();
                MainActivity.getHandler().obtainMessage(MainActivity.HANDLER_CONNECTED).sendToTarget();
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
                    Received.getHandler().obtainMessage(Received.HANDLER_RECEIVED_PART, buffer).sendToTarget();
                } catch (IOException e) {
                    device = null;
                    MainActivity.getHandler().obtainMessage(MainActivity.HANDLER_DISCONNECTED).sendToTarget();
                    break;
                }
            }
        }

        void write(byte[] bytes) {
            try {
                output.write(bytes);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Вызывается для отключения соединения
        public void stop() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            if (isTransferring()) {
                if (isServer())
                    cancelServer();
                else
                    cancelClient();
                setTransferring(false);
            }
        }

        //Вызвается для отключения передачи данных
        private void cancelClient() {
            mainActivity.getSend().stop();
            try {
                mainActivity.getSend().getSend().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            write(new byte[]{0, 0});
        }

        private void cancelServer() {
            write(new byte[]{0, 0});
        }
    }
}
