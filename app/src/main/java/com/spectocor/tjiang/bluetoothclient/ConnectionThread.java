package com.spectocor.tjiang.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.UUID;

public class ConnectionThread implements Runnable {

    private Context context;
    public Thread thread;
    private static ObjectInputStream ois = null;
    private static boolean stopped;
    private static boolean statusConnected = false;
    private static boolean cleaning = false;

    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothSocket socket = null;
    public static MainActivity.MyHandler mHandler = null;

    private static final UUID SOCKET_UUID = UUID.fromString("4e5d48e0-75df-11e3-981f-0800200c9a66");
    private static final String SERVER_ADDRESS = "20:6E:9C:5C:55:44";   // slim phone
    //    private static final String SERVER_ADDRESS = "64:77:91:82:1F:3B";   // fat phone
    public static final int CL_STATUS = 4;
    public static final int MSG_COUNTER = 1;

    ConnectionThread(MainActivity.MyHandler m) {
        thread = new Thread(this, "ConnThread");
        stopped = true;
        mHandler = m;
    }

    public boolean isRunning() {
        return !stopped;
    }

    public static void isConnected() {
        mHandler.sendMessage(mHandler.obtainMessage(CL_STATUS, statusConnected));
    }

    public void setContext(Context c) {
        context = c;
    }

    public static void log(String str) {
        Log.d("ConnThread", str);
    }

    @Override
    public void run() {
        try {
            stopped = false;
            if (checkSupport()) {
                enableBluetooth();

                // TODO: better waiting
                while (!bluetoothAdapter.isEnabled()) {
                    Thread.sleep(1000);
                    log("waiting for enableBluetooth()");
                }
                pairToDevice();
            }
        } catch (Exception e) {
            log("could not create channel / connect to socket / create DataInputStream");
            clean();
        }
        clean();
    }

    private boolean checkSupport() {
        log("entering checkSupport()");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            log("bluetooth not supported");
            return false;
        }
        return true;
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            log("entering enableBluetooth()");
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            turnOnIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(turnOnIntent);
        } else
            log("bluetooth already enabled");
    }

    private void pairToDevice() throws IOException, InterruptedException {
        log("entering pairToDevice()");

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(SERVER_ADDRESS);
        socket = bluetoothDevice.createRfcommSocketToServiceRecord(SOCKET_UUID);
        bluetoothAdapter.cancelDiscovery();
        socket.connect();
        log("socket connected");

        statusConnected = true;
        isConnected();

//        ds = socket.getInputStream();
//        short current;
        while (!stopped) {
            try {
//                current = ds.readShort();
//                log(String.valueOf(current));
//                ConnectionThread.mHandler.sendMessage(ConnectionThread.mHandler.obtainMessage(MSG_COUNTER, current));

//                // TODO: available for reading?
//                int packetLen = ds.available();
//                byte[] b = new byte[packetLen];
//                int actualBytes = ds.read(b);
//                log("available packetLen" + packetLen + " actual bytes: " + actualBytes);
//
//                if (actualBytes != 0) {
//                    Object obj = bytesToObj(b);
//                    if (obj instanceof Short[]) {
//                        log("short[] object!!");
//                        Short[] packet = (Short[]) obj;
//                    } else {
//                        log("not short[] object...");
//                    }
//                } else {
//                    log("actual bytes is 0 nothing is done");
//                }
                ois = new ObjectInputStream(socket.getInputStream());
                // block until read
                Object obj = ois.readObject();
                if (obj instanceof Short[]) {
                    Short[] packet = (Short[]) obj;
                    for (short item : packet) {
                        log(String.valueOf(item));
                        ConnectionThread.mHandler.sendMessage(ConnectionThread.mHandler.obtainMessage(MSG_COUNTER, item));
                    }
                } else {
                    log("something's wrong");
                }
            } catch (Exception e) {
                log("reached end of stream");
            }
        }
    }

    private Object bytesToObj(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bIn = new ByteArrayInputStream(bytes);
        ObjectInputStream oIn = new ObjectInputStream(bIn);
        return oIn.readObject();
    }

    public static void clean() {
        stopped = true;
        if (!cleaning) {
            cleaning = true;
            log("cleaning...");

            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    log("could not close DataInputStream");
                }
                ois = null;
                log("DS cleaned");
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log("socket could not be closed");
                }
                socket = null;
                log("socket cleaned");
            }

            statusConnected = false;
            isConnected();

            MainActivity.clean();
            cleaning = false;
        }
    }
}