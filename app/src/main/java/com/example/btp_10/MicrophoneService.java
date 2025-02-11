package com.example.btp_10;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;

public class MicrophoneService extends Service {

    private static final int SAMPLE_RATE = 44100; // Sampling rate (44.1kHz)
    private static final int BUFFER_SIZE = 1024; // Buffer size for audio data
    private static final int THRESHOLD = 1000; // Threshold for detecting microphone activity (you can adjust this based on testing)

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;

    public MicrophoneService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Initialize and start the microphone detection
        startMicrophoneDetection();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMicrophoneDetection();
    }

    private void startMicrophoneDetection() {
        // Check if the device supports microphone input
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e("MicrophoneService", "Failed to get minimum buffer size");
            return;
        }

        // Initialize the AudioRecord instance to read audio data
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d("MicrophoneService", "Permission not provided");
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);

        // Start the audio recording in a separate thread
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                readAudioData();
            }
        });
        recordingThread.start();
        audioRecord.startRecording();
        Log.d("MicrophoneService", "Microphone detection started");
    }

    private void stopMicrophoneDetection() {
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            Log.d("MicrophoneService", "Microphone detection stopped");
        }
    }

    private void readAudioData() {
        // Create a buffer to read audio data into
        short[] audioBuffer = new short[BUFFER_SIZE];

        while (isRecording) {
            // Read audio data from the microphone
            int numberOfShorts = audioRecord.read(audioBuffer, 0, audioBuffer.length);
            if (numberOfShorts > 0) {
                // Calculate the amplitude (volume) of the audio data
                double amplitude = calculateAmplitude(audioBuffer);
                Log.d("MicrophoneService", "Amplitude: " + amplitude);

                // Check if the amplitude exceeds the threshold
                if (amplitude > THRESHOLD) {
                    Log.d("MicrophoneService", "Microphone activity detected");
                } else {
                    Log.d("MicrophoneService", "No microphone activity");
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
        // This service does not support binding
        return null;
    }
}
