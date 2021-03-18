package com.example.shuttersbluettoth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SelectDeviceScreen extends AppCompatActivity {

    BluetoothAdapter btAdapter;
    ListView deviceList;
    TextView selectDeviceTitle;
    ArrayAdapter<String> deviceAdapter;

    String aList[] = {
            "List item 1",
            "List item 2",
            "List item 3",
            "List item 4",
            "List item 5",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_device_screen);

        selectDeviceTitle = (TextView) findViewById(R.id.selectDeviceTitle);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        ArrayList<String> pairedDeviceList = new ArrayList<String>();
        // MAKE SURE TO TURN ON THE BT ADAPTER IN YOUR PHONE -> ASK FOR TURNING IT ON AUTOMATICALLY IF IT'S OFF
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, MainScreen.ENABLE_BT_ADAPTER_REQUEST);
        }

        Set<BluetoothDevice> pairedDevicesSet = btAdapter.getBondedDevices();
        if (pairedDevicesSet.size() >= 1) {
            for (BluetoothDevice device : pairedDevicesSet) {
                pairedDeviceList.add(device.getName() + "\n" + device.getAddress());
            }
        }



        deviceList = (ListView) findViewById(R.id.deviceList);
//        deviceAdapter = new ArrayAdapter<String>(this, R.layout.custom_text_item, aList);
        deviceAdapter = new ArrayAdapter<String>(this, R.layout.custom_text_item, pairedDeviceList);
        deviceList.setAdapter(deviceAdapter);

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String deviceString = (String) parent.getAdapter().getItem(position);
//                selectDeviceTitle.setText(item);
                Intent intent = new Intent(SelectDeviceScreen.this, MainScreen.class);
                intent.putExtra("selectedDevice", deviceString);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }
}