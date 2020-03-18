package com.example.sensordetection;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

public class ConnectPlayer extends AppCompatActivity { //everyone does this

    private Socket mSocket;
    Button startCollection; //button
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //make it always portrait
        setContentView(R.layout.activity_connect_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        startCollection = findViewById(R.id.start_server_button);
        startCollection.setEnabled(false);

        tv = findViewById(R.id.recNum); //connect to textView
        tv.setText("0");    //at start set number to 0

        SensorApplication app = (SensorApplication) getApplication(); // initiate app as SensorApplication
        mSocket = app.getSocket(); // get global socket

        String deviceName = android.os.Build.MODEL;
        mSocket.emit("join player", deviceName); //send device name to server to print in server terminal
        mSocket.emit("ask for button");

        mSocket.on("update recorder number", updateRecNum); // socket now listens to "update recorder number"

        mSocket.on("enable button", enableButton);  // socket now listens to "enable button"
        mSocket.on("disable button", disableButton); // socket now listens to "disable button"
    }

    public void startProcess(View view){
        mSocket.emit("start collection");
        mSocket.on("start play", onPlay);
    }

    private void letsPlay() {
        mSocket.off("start play", onPlay); //stops listening to event "start play"
        Intent playerIntent = new Intent(this, ActivatePlayer.class);
        startActivity(playerIntent);
    }

    private void enableStartButton(){
        startCollection.setEnabled(true);
    }

    private void disableStartButton(){
        startCollection.setEnabled(false);
    }

    private void updateNum(String num){
        tv.setText(num);
        tv.invalidate();
    }

    //on event "update recorder number"
    private Emitter.Listener updateRecNum = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run(){
                    updateNum("" + args[0]); // args is the argument passed from server
                }
            });
        }
    };

    //on event "disable button"
    private Emitter.Listener disableButton = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run(){
                    disableStartButton();
                }
            });
        }
    };

    //on event "enable button"
    private Emitter.Listener enableButton = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run(){
                    enableStartButton();
                }
            });
        }
    };

    //on event "start play"
    private Emitter.Listener onPlay = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run(){
                    letsPlay();
                }
            });
        }
    };

    @Override
    protected void onDestroy() { //when back button is pressed or finished() is called; maybe?
        super.onDestroy();
        mSocket.emit("leave player");
        mSocket.off("start play", onPlay);
        mSocket.off("enable button", enableButton);
        mSocket.off("disable button", disableButton);
        mSocket.off("update recorder number", updateRecNum);
    }
}
