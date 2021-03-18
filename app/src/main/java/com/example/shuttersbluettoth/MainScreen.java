package com.example.shuttersbluettoth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainScreen extends AppCompatActivity {

    public static final int SELECT_DEVICE_REQUEST = 1;
    public static final int ENABLE_BT_ADAPTER_REQUEST = 2;

    TextView debugConsole;
    ToggleButton upButton;
    ToggleButton downButton;
    Button selectDeviceButton;
    Button connectButton;
    Button disconnectButton;

    // UUID dla urządzeń SPP (np. BTM222)
    UUID sppUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter phoneBtAdapter;
    BluetoothDevice btDevice = null;
    BluetoothSocket btSocket = null;
    String deviceName = null;
    String deviceMACAddress = null;
    OutputStream outStream = null;

    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;


    boolean isRaisingActive = false;
    boolean isLoweringActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

        upButton = (ToggleButton) findViewById(R.id.upButton);
        downButton = (ToggleButton) findViewById(R.id.downButton);
        selectDeviceButton = (Button) findViewById(R.id.selectDeviceButton);
        debugConsole = (TextView) findViewById(R.id.debugConsole);

        connectButton = (Button)  findViewById(R.id.connectButton);
        disconnectButton = (Button)  findViewById(R.id.disconnectButton);

        phoneBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!phoneBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT_ADAPTER_REQUEST);
        }

        upButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    debugConsole.setText(R.string.message_shutters_up_active);
                    downButton.setEnabled(false);
                    sendData('g');
                } else {
//                    debugConsole.setText(R.string.message_shutters_inactive);
                    printDebugMessage("");
                    downButton.setEnabled(true);
                    sendData('g');
                }
            }
        });

        downButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    debugConsole.setText(R.string.message_shutters_down_active);
                    upButton.setEnabled(false);
                    sendData('r');
                } else {
                    printDebugMessage("");
                    upButton.setEnabled(true);
                    sendData('r');
                }
            }
        });

        selectDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainScreen.this, SelectDeviceScreen.class);
//                startActivity(intent);
                startActivityForResult(intent, SELECT_DEVICE_REQUEST);
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToBtDevice();
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectFromDevice();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_DEVICE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
//                debugConsole.setText(data.getStringExtra("selectedDevice"));
                deviceName = data.getStringExtra("selectedDevice").split("\n")[0];
                deviceMACAddress = data.getStringExtra("selectedDevice").split("\n")[1];

                debugConsole.setText(deviceName + "; " + deviceMACAddress);
            }
            if (resultCode == Activity.RESULT_CANCELED) {

            }
        }
    }

    private void connectToBtDevice() {
        if (btDevice == null) {
            if (phoneBtAdapter.checkBluetoothAddress(deviceMACAddress)) {
                btDevice = phoneBtAdapter.getRemoteDevice(deviceMACAddress);
                try {
                    btSocket = btDevice.createRfcommSocketToServiceRecord(sppUUID);
                    btSocket.connect();
                    mmInputStream = btSocket.getInputStream();
//                    beginListenForData();
                } catch (IOException e) {
                    btDevice = null;
                }
            } else {

            }
        }
    }

    private void disconnectFromDevice() {
        if (btSocket != null) {
            if (btSocket.isConnected()) {
                try {
                    btSocket.close();

                } catch (IOException e) {

                } finally {
                    btSocket = null;
                    btDevice = null;
                }
            } else {

            }
        }
        else {

        }
    }

    private void sendData(char data) {
        if (btSocket != null) {
            try {
                outStream = btSocket.getOutputStream();
                outStream.write(data);
            } catch (IOException e) {

            }
        }
        else {

        }
    }

    private void printDebugMessage(String message) {
        debugConsole.setText(message);
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            debugConsole.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }
}