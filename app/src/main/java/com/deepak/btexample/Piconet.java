package com.deepak.btexample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

/**
 * Created by KUMAR on 1/12/2017.
 */

public class Piconet {

    private final static String TAG = Piconet.class.getSimpleName();

    // Name for the SDP record when creating server socket
    private static final String PICONET = "ANDROID_PICONET_BLUETOOTH";

    private final BluetoothAdapter mBluetoothAdapter;

    // String: device address
    // BluetoothSocket: socket that represent a bluetooth connection
    private HashMap<String, BluetoothSocket> mBtSockets;

    // String: device address
    // Thread: thread for connection
    private HashMap<String, Thread> mBtConnectionThreads;

    private ArrayList<UUID> mUuidList;

    private ArrayList<String> mBtDeviceAddresses;

    private Context context;


    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Toast.makeText(context, msg.getData().getString("msg"), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        };
    };

    public Piconet(Context context) {
        this.context = context;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mBtSockets = new HashMap<String, BluetoothSocket>();
        mBtConnectionThreads = new HashMap<String, Thread>();
        mUuidList = new ArrayList<UUID>();
        mBtDeviceAddresses = new ArrayList<String>();

        // Allow up to 7 devices to connect to the server
        mUuidList.add(UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666"));
        mUuidList.add(UUID.fromString("54d1cc90-1169-11e2-892e-0800200c9a66"));
        mUuidList.add(UUID.fromString("6acffcb0-1169-11e2-892e-0800200c9a66"));
        mUuidList.add(UUID.fromString("7b977d20-1169-11e2-892e-0800200c9a66"));
        mUuidList.add(UUID.fromString("815473d0-1169-11e2-892e-0800200c9a66"));
        mUuidList.add(UUID.fromString("503c7434-bc23-11de-8a39-0800200c9a66"));
        mUuidList.add(UUID.fromString("503c7435-bc23-11de-8a39-0800200c9a66"));

        Thread connectionProvider = new Thread(new ConnectionProvider());
        connectionProvider.start();

    }



    public void startPiconet() {
        Log.d(TAG, " -- Looking devices -- ");
        // The devices must be already paired
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                .getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                // X , Y and Z are the Bluetooth name (ID) for each device you want to connect to
                if (device != null && (device.getName().equalsIgnoreCase("X") || device.getName().equalsIgnoreCase("Y")
                        || device.getName().equalsIgnoreCase("Z") || device.getName().equalsIgnoreCase("M"))) {
                    Log.d(TAG, " -- Device " + device.getName() + " found --");
                    BluetoothDevice remoteDevice = mBluetoothAdapter
                            .getRemoteDevice(device.getAddress());
                    connect(remoteDevice);
                }
            }
        } else {
            Toast.makeText(context, "No paired devices", Toast.LENGTH_SHORT).show();
        }
    }

    private class ConnectionProvider implements Runnable {
        @Override
        public void run() {
            try {
                for (int i=0; i<mUuidList.size(); i++) {
                    BluetoothServerSocket myServerSocket = mBluetoothAdapter
                            .listenUsingRfcommWithServiceRecord(PICONET, mUuidList.get(i));
                    Log.d(TAG, " ** Opened connection for uuid " + i + " ** ");

                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    Log.d(TAG, " ** Waiting connection for socket " + i + " ** ");
                    BluetoothSocket myBTsocket = myServerSocket.accept();
                    Log.d(TAG, " ** Socket accept for uuid " + i + " ** ");
                    try {
                        // Close the socket now that the
                        // connection has been made.
                        myServerSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, " ** IOException when trying to close serverSocket ** ");
                    }

                    if (myBTsocket != null) {
                        String address = myBTsocket.getRemoteDevice().getAddress();

                        mBtSockets.put(address, myBTsocket);
                        mBtDeviceAddresses.add(address);

                        Thread mBtConnectionThread = new Thread(new BluetoohConnection(myBTsocket));
                        mBtConnectionThread.start();

                        Log.i(TAG," ** Adding " + address + " in mBtDeviceAddresses ** ");
                        mBtConnectionThreads.put(address, mBtConnectionThread);
                    } else {
                        Log.e(TAG, " ** Can't establish connection ** ");
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, " ** IOException in ConnectionService:ConnectionProvider ** ", e);
            }
        }
    }

    private class BluetoohConnection implements Runnable {
        private String address;

        private final InputStream mmInStream;

        public BluetoohConnection(BluetoothSocket btSocket) {

            InputStream tmpIn = null;

            try {
                tmpIn = new DataInputStream(btSocket.getInputStream());
            } catch (IOException e) {
                Log.e(TAG, " ** IOException on create InputStream object ** ", e);
            }
            mmInStream = tmpIn;
        }
        @Override
        public void run() {
            byte[] buffer = new byte[1];
            String message = "";
            while (true) {

                try {
                    int readByte = mmInStream.read();
                    if (readByte == -1) {
                        Log.e(TAG, "Discarting message: " + message);
                        message = "";
                        continue;
                    }
                    buffer[0] = (byte) readByte;

                    if (readByte == 0) { // see terminateFlag on write method
                        onReceive(message);
                        message = "";
                    } else { // a message has been recieved
                        message += new String(buffer, 0, 1);
                    }
                } catch (IOException e) {
                    Log.e(TAG, " ** disconnected ** ", e);
                }

                mBtDeviceAddresses.remove(address);
                mBtSockets.remove(address);
                mBtConnectionThreads.remove(address);
            }
        }
    }

    /**
     * @param receiveMessage
     */
    private void onReceive(String receiveMessage) {
        if (receiveMessage != null && receiveMessage.length() > 0) {
            Log.i(TAG, " $$$$ " + receiveMessage + " $$$$ ");
            Bundle bundle = new Bundle();
            bundle.putString("msg", receiveMessage);
            Message message = new Message();
            message.what = 1;
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }

    /**
     * @param device
     * @param uuidToTry
     * @return
     */
    private BluetoothSocket getConnectedSocket(BluetoothDevice device, UUID uuidToTry) {
        BluetoothSocket myBtSocket;
        try {
            myBtSocket = device.createRfcommSocketToServiceRecord(uuidToTry);
            myBtSocket.connect();
            return myBtSocket;
        } catch (IOException e) {
            Log.e(TAG, "IOException in getConnectedSocket", e);
        }
        return null;
    }

    private void connect(BluetoothDevice device) {
        BluetoothSocket myBtSocket = null;
        String address = device.getAddress();
        BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(address);
        // Try to get connection through all uuids available
        for (int i = 0; i < mUuidList.size() && myBtSocket == null; i++) {
            // Try to get the socket 2 times for each uuid of the list
            for (int j = 0; j < 2 && myBtSocket == null; j++) {
                Log.d(TAG, " ** Trying connection..." + j + " with " + device.getName() + ", uuid " + i + "...** ");
                myBtSocket = getConnectedSocket(remoteDevice, mUuidList.get(i));
                if (myBtSocket == null) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "InterruptedException in connect", e);
                    }
                }
            }
        }
        if (myBtSocket == null) {
            Log.e(TAG, " ** Could not connect ** ");
            return;
        }
        Log.d(TAG, " ** Connection established with " + device.getName() +"! ** ");
        mBtSockets.put(address, myBtSocket);
        mBtDeviceAddresses.add(address);
        Thread mBluetoohConnectionThread = new Thread(new BluetoohConnection(myBtSocket));
        mBluetoohConnectionThread.start();
        mBtConnectionThreads.put(address, mBluetoohConnectionThread);

    }

    public void bluetoothBroadcastMessage(String message) {
        //send message to all except Id
        for (int i = 0; i < mBtDeviceAddresses.size(); i++) {
            sendMessage(mBtDeviceAddresses.get(i), message);
        }
    }

    private void sendMessage(String destination, String message) {
        BluetoothSocket myBsock = mBtSockets.get(destination);
        if (myBsock != null) {
            try {
                OutputStream outStream = myBsock.getOutputStream();
                final int pieceSize = 16;
                for (int i = 0; i < message.length(); i += pieceSize) {
                    byte[] send = message.substring(i,
                            Math.min(message.length(), i + pieceSize)).getBytes();
                    outStream.write(send);
                }
                // we put at the end of message a character to sinalize that message
                // was finished
                byte[] terminateFlag = new byte[1];
                terminateFlag[0] = 0; // ascii table value NULL (code 0)
                outStream.write(new byte[1]);
            } catch (IOException e) {
                Log.d(TAG, "line 278", e);
            }
        }
    }
}
