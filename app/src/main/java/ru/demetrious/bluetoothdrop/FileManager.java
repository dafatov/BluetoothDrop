package ru.demetrious.bluetoothdrop;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

class FileManager {
    final static byte FILE_EXCEPTION = -1;
    final static byte FILE_CREATED = 0;
    final static byte FILE_DIRECTORY_ERROR = 1;
    final static byte FILE_CREATE_ERROR = 2;
    private static FileInputStream fileInputStream;
    private static FileOutputStream fileOutputStream;

    static void openFileInputStream(File file) {
        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    static byte openFileOutputStream(String filePath) {
        File file = new File(filePath);

        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) return 1;
        }
        try {
            if (!file.createNewFile()) return 2;
            fileOutputStream = new FileOutputStream(file);
            return 0;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    static byte[] read(int length) {
        byte[] bytes = new byte[length];

        try {
            fileInputStream.read(bytes, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    static void write(byte[] bytes) {
        try {
            fileOutputStream.write(bytes, 0, bytes.length);
            fileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void closeFileInputStream() {
        try {
            Log.e("ERROR", "CloseFileInputStream:" + (fileInputStream != null));
            if (fileInputStream != null)
                fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void closeFileOutputStream() {
        try {
            Log.e("ERROR", "CloseFileOutputStream:" + (fileOutputStream != null));
            if (fileOutputStream != null)
                fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
