package com.example.sensordetection;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //to make it start on portrait

        setContentView(R.layout.activity_main);

        //Update device name here
        TextView textView = (TextView) findViewById(R.id.deviceNameTitle);
        String deviceName;
        String fp = android.os.Build.FINGERPRINT;
        String[] fp_arr = fp.split("/");
        deviceName = fp_arr[4];
        deviceName = deviceName.substring(0, deviceName.indexOf(':'));
        deviceName = Build.MANUFACTURER + "\n" + deviceName;
        textView.setText(deviceName); //set text for text view

        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new
                    StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    public void choosePlayer(View view){
        Intent playerIntent = new Intent(this, ConnectPlayer.class);
        startActivity(playerIntent);
    }

    public void chooseRecorder(View view){
        Intent recorderIntent = new Intent(this, ConnectRecorder.class);
        startActivity(recorderIntent);
    }

}