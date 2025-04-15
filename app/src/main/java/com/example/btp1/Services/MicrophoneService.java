package com.example.btp1.Services;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.btp1.DataRepository;

public class MicrophoneService extends Service {

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 1024;
    private static final int THRESHOLD = 500;
    private static final int DETECTION_DURATION = 30 * 1000; // 30 seconds
    private static final int INTERVAL = 5 * 60 * 1000; // 5 minutes

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private final String TAG = "Logs";
    private final Handler handler = new Handler();

    private final Runnable detectionRunnable = new Runnable() {
        @Override
        public void run() {
            startMicrophoneDetection();

            // Stop detection after 30 seconds
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopMicrophoneDetection();
                    handler.postDelayed(detectionRunnable, INTERVAL); // Schedule next detection in 5 minutes
                }
            }, DETECTION_DURATION);
        }
    };

    public MicrophoneService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.post(detectionRunnable);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMicrophoneDetection();
        handler.removeCallbacks(detectionRunnable);
    }

    private void startMicrophoneDetection() {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Failed to get minimum buffer size");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission not provided");
            return;
        }

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        isRecording = true;

        new Thread(this::readAudioData).start();
        audioRecord.startRecording();
        Log.d(TAG, "Microphone detection started");
    }

    private void stopMicrophoneDetection() {
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            Log.d(TAG, "Microphone detection stopped");
        }
    }

    private void readAudioData() {
        short[] audioBuffer = new short[BUFFER_SIZE];

        while (isRecording) {
            int numberOfShorts = audioRecord.read(audioBuffer, 0, audioBuffer.length);
            if (numberOfShorts > 0) {
                double amplitude = calculateAmplitude(audioBuffer);
                if (amplitude > THRESHOLD) {
                    String entry = "Microphone activity detected";
                    Log.d(TAG, entry);
                    DataRepository.getInstance().addMicrophoneData(entry);
//                    Log.d(TAG, "Microphone activity detected");
                }else{
                    String entry = "Microphone not detected";
                    Log.d(TAG, entry);
                    DataRepository.getInstance().addMicrophoneData(entry);
//                    Log.d(TAG,"Microphone not detected");
                }
            }
        }
    }

    private double calculateAmplitude(short[] audioBuffer) {
        double sum = 0;
        for (short sample : audioBuffer) {
            sum += Math.abs(sample);
        }
        return sum / audioBuffer.length;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}