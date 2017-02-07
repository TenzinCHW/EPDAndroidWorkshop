package com.meow.hanwei.epdandroidworkshop;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MY_APP_DEBUG_TAG"; // just the Tag that error messages will
    // contain when written to from this app

    private Handler mHandler = new Handler(); // handler that gets info from Bluetooth service

    /**
     * Bluetooth adapter
     */
    BluetoothAdapter mBluetoothAdapter;

    /**
     * Bluetooth devices
     */
    Set<BluetoothDevice> pairedDevices; // Set of devices that are paired to phone
    // (i.e. are bonded)

    ArrayList<String> nameList = new ArrayList();   // namelist of paired/in-range devices for adapter

    ArrayAdapter adapter;   // adapter for listview
    ListView list;   // to display names of devices

    ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private ConnectedThread connectedThread;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                if (!nameList.contains(deviceName)) {
                    devices.add(device);
                    nameList.add(deviceName);
                    adapter.notifyDataSetChanged();
                }
//                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, nameList);
        list = (ListView) findViewById(R.id.devices);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mBluetoothAdapter.isEnabled()) {
                    String devName = (String) list.getItemAtPosition(i);
                    for (BluetoothDevice bt :
                            devices) {
                        if (bt.getName().equals(devName)) {
                            connectedThread = new ConnectedThread(bt);
                            Toast.makeText(MainActivity.this, "MADE CONNECTION", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Bluetooth is not on", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try {
            Method getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);

            ParcelUuid[] uuids = (ParcelUuid[]) getUuidsMethod.invoke(mBluetoothAdapter, null);
            for (ParcelUuid uuid : uuids) {
                Log.d(TAG, "UUID: " + uuid.getUuid().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void toggleBT(View view) {
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, 0);
                Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
            } else {
                mBluetoothAdapter.disable();
                Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth not supported", Toast.LENGTH_LONG).show();
        }
    }

    protected void getPaired(View view) {
        if (mBluetoothAdapter.isEnabled()) {
            pairedDevices = mBluetoothAdapter.getBondedDevices();
            nameList.clear();
            devices.clear();
            for (BluetoothDevice bt : pairedDevices) {
                if (!nameList.contains(bt.getName())) {
                    nameList.add(bt.getName());
                    adapter.notifyDataSetChanged();
                    devices.add(bt);
                    System.out.println(bt.getName() + ": ");
                    for (ParcelUuid meow :
                            bt.getUuids()) {
                        System.out.println(meow.getUuid());
                    }
                }
            }
            Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Bluetooth is not on", Toast.LENGTH_SHORT).show();
        }
    }

    protected void discover(View view) {
        if (mBluetoothAdapter.isDiscovering()) {
            boolean discovering = mBluetoothAdapter.cancelDiscovery();
            if (discovering) {
                Toast.makeText(MainActivity.this, "Stopping discovery", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Could not start discovery", Toast.LENGTH_LONG).show();
            }
        } else {
            mBluetoothAdapter.startDiscovery();
            Toast.makeText(MainActivity.this, "Started discovering", Toast.LENGTH_LONG).show();
        }
    }

    protected void sendUp(View view) {
        try {
            connectedThread.write("0".getBytes());
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Cannot send", Toast.LENGTH_SHORT).show();
        }
    }

    protected void sendDown(View view) {
        try {
            connectedThread.write("1".getBytes());
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Cannot send", Toast.LENGTH_SHORT).show();
        }
    }

    protected void sendLeft(View view) {
        try {
            connectedThread.write("LEFT".getBytes());
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Cannot send", Toast.LENGTH_SHORT).show();
        }
    }

    protected void sendRight(View view) {
        try {
            connectedThread.write("RIGHT".getBytes());
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Cannot send", Toast.LENGTH_SHORT).show();
        }
    }

    protected void closeConnection(View view) {
        try {
            connectedThread.cancel();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "No active connection", Toast.LENGTH_SHORT).show();
        }
    }

    private interface MessageConstants {
        int MESSAGE_READ = 0;
        int MESSAGE_WRITE = 1;
        int MESSAGE_TOAST = 2;
        // ... (Add other message types here as needed.)
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        protected ConnectedThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string.

                // tmp = device.createRfcommSocketToServiceRecord(mmDevice.getUuids()[0].getUuid());
                // This gets the first item from the array returned by getUuids().

                // 00001101 represents the COM/Serial port for bluetooth.
                tmp = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                tmp.connect();
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = mmSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        protected void write(byte[] msgbytes) {
            try {
                mmOutStream.write(msgbytes);    // This is the only call that's actually sending
                // data to the connected bluetooth device

                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        protected void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}
