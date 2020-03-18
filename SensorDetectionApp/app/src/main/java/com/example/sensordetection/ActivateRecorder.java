package com.example.sensordetection;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.AudioRecord;
import android.media.AudioFormat;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ActivateRecorder extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordTest";
    private static String fileName = null;
    private String filePath;
    private MediaRecorder recorder;
    private int inSampleRate = 8000;
    private int minBuffer;
    private AudioRecord audioRecord;
    private AndroidLame androidLame;
    private Socket mSocket;
    private String timestamp;
    private MediaPlayer player;
    private FileOutputStream outputStream;
    private boolean isRecording = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //make it always portrait
        setContentView(R.layout.activity_activate_recorder);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SensorApplication app = (SensorApplication) getApplication();
        mSocket = app.getSocket();

        // gets filename and sets save location in string filename
        fileName = getExternalCacheDir().getAbsolutePath();
        timestamp = new SimpleDateFormat("yyyyMMddHHmmss'.3gp'").format(new Date());
        fileName += "/audiorecordtest_";
        fileName += timestamp;

        filePath = getExternalCacheDir().getAbsolutePath() + "/recording.mp3";
        startRecording();

    }

    // starts recording audio as player starts collection
    private void startRecording() {
        // initializing MediaRecorder
//        recorder = new MediaRecorder();
//        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        recorder.setOutputFile(fileName);
//        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        minBuffer = AudioRecord.getMinBufferSize(inSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                inSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2
        );

        short[] buffer = new short[inSampleRate * 2 * 2];

        byte[] mp3buffer = new byte[(int) (7200 + buffer.length * 2 * 1.25)];

        try {
            outputStream = new FileOutputStream(new File(filePath));
        } catch (FileNotFoundException e) {
            Log.d("File", "File not Found");
            e.printStackTrace();
        }

        androidLame = new LameBuilder()
                .setInSampleRate(inSampleRate)
                .setOutChannels(1)
                .setOutBitrate(32)
                .setOutSampleRate(inSampleRate)
                .build();
        System.out.println("Start Recording....");
        Log.d("Recording", "Start Recording...");

        audioRecord.startRecording();
        mSocket.on("stop record", onRecStop);

        int bytesRead = 0;

        while(isRecording) {
            bytesRead = audioRecord.read(buffer, 0, minBuffer);
            Log.d("Recording", "bytes read = " + bytesRead);

            if (bytesRead > 0) {
                int bytesEncoded = androidLame.encode(buffer, buffer, bytesRead, mp3buffer);

                if (bytesEncoded > 0) {
                    try {
                        Log.d("Recording", "Writing to File");
                        outputStream.write(mp3buffer, 0, bytesEncoded);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        int outputMp3Buff = androidLame.flush(mp3buffer);
        if (outputMp3Buff > 0) {
            try {
                outputStream.write(mp3buffer, 0, outputMp3Buff);
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        audioRecord.stop();
        audioRecord.release();
        androidLame.close();

        isRecording = false;

        // SENDING BYTE ARRAY TO SERVER
        String deviceName ;
        String fp = android.os.Build.FINGERPRINT;
        String[] fp_arr = fp.split("/");
        deviceName = fp_arr[4];
        deviceName = deviceName.substring(0, deviceName.indexOf(':'));
        deviceName += Build.MANUFACTURER;
        deviceName += "_" + timestamp;

        try {
            File fileToSend = new File(filePath);
            byte[] byteArr = getBytes(fileToSend);

            //add another argument for recording name which should be the same name that's shown in main minus the brand name
            mSocket.emit("Send File", byteArr, deviceName);
        }
        catch (Exception e){
            Log.e(LOG_TAG, "No File Found");
        }

        // go to finish recording
        Intent recorderIntent = new Intent(this, FinishRecording.class);
        startActivity(recorderIntent);
        finish();
    }

        // test if recorder successfully created
//        try {
//            recorder.setMaxDuration(60000);
//            recorder.prepare();
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "recorder prepare() failed");
//        }

        // start recording
//        recorder.start();

        // listen for stop recording
//        mSocket.on("stop record", onRecStop);


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
        isRecording = false;
        mSocket.off("stop record", onRecStop);
//        if (recorder != null) {
//            try {
//                recorder.stop();
//            } catch (RuntimeException stopException) {
//                recorder.reset();
//                return;
//            }
//
//            recorder.release();
//            recorder = null;
//        }

        // obtaining unique filename to prevent overwriting
//        String deviceName ;
//        String fp = android.os.Build.FINGERPRINT;
//        String[] fp_arr = fp.split("/");
//        deviceName = fp_arr[4];
//        deviceName = deviceName.substring(0, deviceName.indexOf(':'));
//        deviceName += Build.MANUFACTURER;
//        deviceName += "_" + timestamp;




        //convert audio file to byte array
//        try {
//            File fileToSend = new File(fileName);
//            byte[] byteArr = getBytes(fileToSend);
//
//            mSocket.emit("Send File", byteArrMp3, deviceName);
//            //add another argument for recording name which should be the same name that's shown in main minus the brand name
//        }
//        catch (Exception e){
//            Log.e(LOG_TAG, "No File Found");
//        }
//
//        // go to finish recording
//        Intent recorderIntent = new Intent(this, FinishRecording.class);
//        startActivity(recorderIntent);
//        finish();
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
