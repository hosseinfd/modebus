package com.example.ifasanatmodebusproject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

public class MainActivity extends Activity {

    FT_Device ftDevice = null;
    D2xxManager ftdid2xx;
    int DevCount = -1;
    EditText writeText;
    TextView infoText;
    TextView readText, textLog;
    OpticPort op;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button loopbackBtn = (Button) findViewById(R.id.button_id);
        Button refreshBtn = (Button) findViewById(R.id.refreshBtn);
        infoText = findViewById(R.id.text);
        readText = findViewById(R.id.textt);
        writeText = findViewById(R.id.edText);
        textLog = findViewById(R.id.textLog);

        try {
            ftdid2xx = D2xxManager.getInstance(this);
            op = new OpticPort(MainActivity.this, ftdid2xx);
        } catch (D2xxManager.D2xxException e) {
            e.printStackTrace();
        }

        loopbackBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                int count = op.ConnectOpticPort();
                if (count > 0) op.Connect("Factory");
                else new Exception();
            }
        });

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RefreshDeviceInformation(v);
            }
        });
    }

    public void RefreshDeviceInformation(View view) {
        try {
            GetDeviceInformation();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            String s = e.getMessage();
            if (s != null) {
                infoText.setText(s);
            }
            e.printStackTrace();
        }
    }

    public void GetDeviceInformation() throws InterruptedException {

        int devCount = 0;

        devCount = ftdid2xx.createDeviceInfoList(this);

        Log.i("FtdiModeControl",
                "Device number = " + Integer.toString(devCount));
        if (devCount > 0) {
            D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
            ftdid2xx.getDeviceInfoList(devCount, deviceList);

            // deviceList[0] = ftdid2xx.getDeviceInfoListDetail(0);

            infoText.setText("Number of Devices: "
                    + Integer.toString(devCount));

            if (deviceList[0].serialNumber == null) {
                readText.setText("Device Serial Number: " + deviceList[0].serialNumber + "(No Serial Number)");
            } else {
                readText.setText("Device Serial Number: " + deviceList[0].serialNumber);
            }

            if (deviceList[0].description == null) {
                infoText.setText("Device Description: " + deviceList[0].description + "(No Description)");
            } else {
                infoText.setText("Device Description: " + deviceList[0].description);
            }
            infoText.setText("Device Location: "+ Integer.toString(deviceList[0].location));

            infoText.setText("Device ID: " + Integer.toString(deviceList[0].id));
            switch (deviceList[0].type) {
                case D2xxManager.FT_DEVICE_232B:
                    infoText.setText("Device Name : FT232B device");
                    break;

                case D2xxManager.FT_DEVICE_8U232AM:
                    infoText.setText("Device Name : FT8U232AM device");
                    break;

                case D2xxManager.FT_DEVICE_UNKNOWN:
                    infoText.setText("Device Name : Unknown device");
                    break;

                case D2xxManager.FT_DEVICE_2232:
                    infoText.setText("Device Name : FT2232 device");
                    break;

                case D2xxManager.FT_DEVICE_232R:
                    infoText.setText("Device Name : FT232R device");
                    break;

                case D2xxManager.FT_DEVICE_2232H:
                    infoText.setText("Device Name : FT2232H device");
                    break;

                case D2xxManager.FT_DEVICE_4232H:
                    infoText.setText("Device Name : FT4232H device");
                    break;

                case D2xxManager.FT_DEVICE_232H:
                    infoText.setText("Device Name : FT232H device");
                    break;
                case D2xxManager.FT_DEVICE_X_SERIES:
                    infoText.setText("Device Name : FTDI X_SERIES");
                    break;
                default:
                    infoText.setText("Device Name : FT232B device");
                    break;
            }
        } else {
            infoText.setText("Number of devices: 0");
        }

    }

}
