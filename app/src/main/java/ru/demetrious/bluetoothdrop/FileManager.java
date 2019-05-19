package ru.demetrious.bluetoothdrop;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileManager {
    public final static byte FILE_EXCEPTION = -1;
    public final static byte FILE_CREATED = 0;
    public final static byte FILE_DIRECTORY_ERROR = 1;
    public final static byte FILE_CREATE_ERROR = 2;
    private static FileInputStream fileInputStream;
    private static FileOutputStream fileOutputStream;

    public static void openFileInputStream(File file) {
        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static byte openFileOutputStream(String filePath) {
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

    public static byte[] read(int length) {
        byte[] bytes = new byte[length];

        try {
            if (fileInputStream.read(bytes, 0, length) != length)
                Log.e("ERROR", "FileManager.read.notAllBytesRead");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static void write(byte[] bytes) {
        try {
            fileOutputStream.write(bytes, 0, bytes.length);
            fileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeFileInputStream() {
        try {
            Log.e("ERROR", "CloseFileInputStream:" + (fileInputStream != null));
            if (fileInputStream != null)
                fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeFileOutputStream() {
        try {
            Log.e("ERROR", "CloseFileOutputStream:" + (fileOutputStream != null));
            if (fileOutputStream != null)
                fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
