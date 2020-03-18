package com.example.sensordetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

public class ConnectRecorder extends AppCompatActivity {

    private Socket mSocket;
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    // request recording permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //make it always portrait
        setContentView(R.layout.activity_connect_recorder);

        // keeps screen from sleeping to prevent disconnect
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SensorApplication app = (SensorApplication) getApplication();
        mSocket = app.getSocket();

        // request recording permissions
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        String deviceName = android.os.Build.MODEL;

        // tell server this phone is a recorder
        mSocket.emit("join recorder", deviceName);

        // listen for start record event from player
        mSocket.on("start record", onRecord);
    }

    private void letsRecord() {
        // stops listening to start record
        mSocket.off("start record", onRecord);
        Intent recorderIntent = new Intent(this, ActivateRecorder.class);
        startActivity(recorderIntent);
    }

    // emitter listener to start recording
    private Emitter.Listener onRecord = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run(){
                    letsRecord();
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.emit("leave recorder");
        mSocket.off("start record", onRecord);
    }
}
