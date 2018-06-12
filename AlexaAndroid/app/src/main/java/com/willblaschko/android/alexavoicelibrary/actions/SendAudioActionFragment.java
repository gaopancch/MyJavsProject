package com.willblaschko.android.alexavoicelibrary.actions;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.willblaschko.android.alexa.VoiceHelper;
import com.willblaschko.android.alexa.interfaces.AvsException;
import com.willblaschko.android.alexa.requestbody.DataRequestBody;
import com.willblaschko.android.alexavoicelibrary.BuildConfig;
import com.willblaschko.android.alexavoicelibrary.FileUtils;
import com.willblaschko.android.alexavoicelibrary.R;
import com.willblaschko.android.recorderview.RecorderView;

import java.io.IOException;

import ee.ioc.phon.android.speechutils.AudioRecorder;
import ee.ioc.phon.android.speechutils.RawAudioRecorder;
import okio.BufferedSink;


/**
 * @author will on 5/30/2016.
 */

public class SendAudioActionFragment extends BaseListenerFragment {

    private static final String TAG = "SendAudioActionFragment";

    private final static int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int AUDIO_RATE = 16000;
    private RawAudioRecorder recorder;
    private RecorderView recorderView;
    private int bufferSize;// = AudioTrack.getMinBufferSize(16000,AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    private AudioTrack audioTrack;//= new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
    VoiceHelper voiceHelper;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_action_audio, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recorderView = (RecorderView) view.findViewById(R.id.recorder);
        bufferSize = AudioTrack.getMinBufferSize(16000,AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recorderView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recorder == null) {
                    recodeBytes = null;
                    startListening();
                }else{
                    stopListening();
                }
            }
        });
        view.findViewById(R.id.button_speak).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioTrack= new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                audioTrack.play();
                audioTrack.write(recodeBytes, 0, recodeBytes.length);
//                 voiceHelper = VoiceHelper.getInstance(getContext());
//                String text = "... " + "白日依山尽" + " ...";
//                voiceHelper.getSpeechFromText(text, new VoiceHelper.SpeechFromTextCallback() {
//                    @Override
//                    public void onSuccess(byte[] data) {
//                        Log.i("LogUtils", "onSuccess ");
//                        audioTrack= new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
//                        audioTrack.play();
//                        audioTrack.write(data, 0, data.length);
////                        audioTrack.write(recodeBytes, 0, recodeBytes.length);
//                    }
//
//                    @Override
//                    public void onError(Exception e) {
//                        Log.i("LogUtils", "onError = "+e.getMessage());
//                    }
//                });

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.RECORD_AUDIO)) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
                }
            }

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //tear down our recorder on stop
        if(recorder != null){
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    @Override
    public void startListening() {
        if(recorder == null){
            recorder = new RawAudioRecorder(AUDIO_RATE);
        }
        recorder.start();
        alexaManager.sendAudioRequest(requestBody, getRequestCallback());
    }

    byte[] recodeBytes = null;
    byte[] recdeBytesTem = null;
    private DataRequestBody requestBody = new DataRequestBody() {
        /**
         * @param sink
         * @throws IOException
         */
        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            Log.i("LogUtils", "requestBody writeTo ");
            while (recorder != null && recorder.getState() != AudioRecorder.State.ERROR
                    && !recorder.isPausing()) {
                if(recorder != null) {
                    final float rmsdb = recorder.getRmsdb();
                    if(recorderView != null) {
                        recorderView.post(new Runnable() {
                            @Override
                            public void run() {
                                recorderView.setRmsdbLevel(rmsdb);
                            }
                        });
                    }
                    if(sink != null && recorder != null) {
                        byte[] consumeRecordBytes = recorder.consumeRecording();
//                        if(consumeRecordBytes.length<=320&&consumeRecordBytes.length>0){
                        if(consumeRecordBytes.length>0) {
                            if(recodeBytes == null){
                                recodeBytes = consumeRecordBytes;
                            }else{
                                recdeBytesTem = recodeBytes;
                                recodeBytes = new byte[recdeBytesTem.length + consumeRecordBytes.length];
                                System.arraycopy(recdeBytesTem,0,recodeBytes,0,recdeBytesTem.length);
                                System.arraycopy(consumeRecordBytes,0,recodeBytes,recdeBytesTem.length,consumeRecordBytes.length);
                            }
                            android.util.Log.i("LogUtils", "bytes.length = "+consumeRecordBytes.length);
                            sink.write(consumeRecordBytes);
                            sink.flush();
                        }
                    }
                }

                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            stopListening();
        }

    };

    /**
     *
     */
    private void stopListening(){
        if(recorder != null) {
            byte[] data1 = recorder.getCompleteRecordingAsWav();
//        byte[] data2 = recodeBytes;
            FileUtils.playWav(data1,"aaSpeech.wav",false);
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    @Override
    protected String getTitle() {
        return getString(R.string.fragment_action_send_audio);
    }

    @Override
    protected int getRawCode() {
        return R.raw.code_audio;
    }


}
