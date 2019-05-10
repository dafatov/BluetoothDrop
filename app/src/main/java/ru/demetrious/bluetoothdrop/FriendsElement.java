package ru.demetrious.bluetoothdrop;

import android.bluetooth.BluetoothDevice;

class FriendsElement {
    private BluetoothDevice bluetoothDevice;
    private boolean isOnline;

    FriendsElement(BluetoothDevice bluetoothDevice, boolean isOnline) {
        this.bluetoothDevice = bluetoothDevice;
        this.isOnline = isOnline;
    }

    BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    boolean isOnline() {
        return isOnline;
    }

    void setOnline(boolean online) {
        isOnline = online;
    }
}
