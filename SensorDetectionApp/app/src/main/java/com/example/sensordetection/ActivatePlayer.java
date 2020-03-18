package com.example.sensordetection;

import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import com.github.nkzawa.socketio.client.Socket;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;

public class ActivatePlayer extends AppCompatActivity {

    private Socket mSocket;
    MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //make it always portrait
        setContentView(R.layout.activity_activate_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SensorApplication app = (SensorApplication) getApplication();   //get app
        mSocket = app.getSocket();      //get socket

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSocket.emit("stop collection");
            }
        }, 55000);
    }

    //this function is connected to the 'Play' button
    public void play(View v) {
        if (player == null) {
            player = MediaPlayer.create(this, R.raw.song); // initialize media player
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) { //i don't know what this does tbh
                    stopPlayer();
                    mSocket.emit("stop collection");
                }
            });
        }
        player.start();
    }

    //this function is connected to the 'Pause' button
    public void pause(View v) {
        if (player != null) {
            player.pause();
        }
    }

    //this function is connected to the 'Stop' button
    public void stop(View v) {
        stopPlayer();
        mSocket.emit("stop collection");
        finish();
    }

    //this function stops the whole process; recording audio in other phones
    private void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;  //delete player
            Toast.makeText(this, "MediaPlayer released", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() { //called if and only if back button on the phone or call finished(); maybe the same as onDestroy()
        super.onStop();
        stopPlayer();   // just to be make sure
    }
}
