package com.example.sensordetection;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.naman14.androidlame.AndroidLame;
import com.naman14.androidlame.LameBuilder;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ActivateRecorder extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordTest";
    private static String fileName = null;
    private MediaRecorder recorder = null;
    private Socket mSocket;
    private String timestamp;
    private MediaPlayer player = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //make it always portrait
        setContentView(R.layout.activity_activate_recorder);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SensorApplication app = (SensorApplication) getApplication();
        mSocket = app.getSocket();

        // gets filename and sets save location in string filename
        fileName = getExternalCacheDir().getAbsolutePath();
        timestamp = new SimpleDateFormat("yyyyMMddHHmmss'.3gp'").format(new Date());
        fileName += "/audiorecordtest_";
        fileName += timestamp;

        startRecording();

    }

    // starts recording audio as player starts collection
    private void startRecording() {
        // initializing MediaRecorder
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        // test if recorder successfully created
        try {
            recorder.setMaxDuration(60000);
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "recorder prepare() failed");
        }

        // start recording
        recorder.start();

        // listen for stop recording
        mSocket.on("stop record", onRecStop);
    }

    // converts audio file to a byte array
    private byte[] getBytes(File f)
            throws IOException
    {
        byte[] buffer = new byte [1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(f);
        int read;
        while((read = fis.read(buffer)) != -1)
        {
            os.write(buffer, 0, read);
        }
        fis.close();
        os.close();
        return os.toByteArray();
    }

    // stops the recording when player stops playing music
    private void stopRecording() {
        mSocket.off("stop record", onRecStop);

        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException stopException) {
                recorder.reset();
                return;
            }

            recorder.release();
            recorder = null;
        }

        // obtaining unique filename to prevent overwriting
        String deviceName ;
        String fp = android.os.Build.FINGERPRINT;
        String[] fp_arr = fp.split("/");
        deviceName = fp_arr[4];
        deviceName = deviceName.substring(0, deviceName.indexOf(':'));
        deviceName += Build.MANUFACTURER;
        deviceName += "_" + timestamp;


        //convert audio file to byte array
        try {
            File fileToSend = new File(fileName);
            byte[] byteArr = getBytes(fileToSend);

            String filePath = Environment.getExternalStorageDirectory() + "/recording.mp3";
            FileOutputStream outputStream = new FileOutputStream(filePath);
//            addLog("creating short buffer array");
            int inSamplerate = 8000;
            short[] buffer = new short[inSamplerate * 2 * 60];
            byte[] mp3buffer = new byte[(int) (7200 + buffer.length * 2 * 1.25)];
            AndroidLame androidLame = new LameBuilder()
                    .setInSampleRate(inSamplerate)
                    .setOutChannels(1)
                    .setOutBitrate(32)
                    .setOutSampleRate(inSamplerate)
                    .build();

            for (byte b : byteArr ) {
                int bytesRead = (int) b;
                int bytesEncoded = androidLame.encode(buffer, buffer, bytesRead, mp3buffer);
                if (bytesEncoded > 0) {
                    outputStream.write(mp3buffer, 0, bytesEncoded);
                }
            }

            int outputMp3buf = androidLame.flush(mp3buffer);
            if (outputMp3buf > 0) {
                try {
//                    addLog("writing final mp3buffer to outputstream");
                    outputStream.write(mp3buffer, 0, outputMp3buf);
//                    addLog("closing output stream");
                    outputStream.close();
//                    updateStatus("Output recording saved in " + filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            androidLame.close();
            File mp3File = new File(filePath);
            byte[] byteArrMp3 = getBytes(mp3File);

            mSocket.emit("Send File", byteArrMp3, deviceName);
            //add another argument for recording name which should be the same name that's shown in main minus the brand name
        }
        catch (Exception e){
            Log.e(LOG_TAG, "No File Found");
        }

        // go to finish recording
        Intent recorderIntent = new Intent(this, FinishRecording.class);
        startActivity(recorderIntent);
        finish();
    }

    // emitter listener when received stop record event
    private Emitter.Listener onRecStop = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run(){
                    stopRecording();
                }
            });
        }
    };


    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        mSocket.off("stop record", onRecStop);
    }

}
