package com.willblaschko.android.alexa.interfaces.speechrecognizer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.VoiceHelper;
import com.willblaschko.android.alexa.interfaces.AvsException;
import com.willblaschko.android.alexa.okhttp3.RequestBody;
import com.willblaschko.android.alexa.requestbody.DataRequestBody;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import okio.BufferedSink;

/**
 * A subclass of {@link SpeechSendEvent} that allows an arbitrary text string to be sent to the AVS servers, translated through Google's text to speech engine
 * This speech is rendered using the VoiceHelper utility class, and is done on whatever thread this call is running
 */
public class SpeechSendText extends SpeechSendEvent {

    private final static String TAG = "SpeechSendText";

    long start = 0;

    /**
     * Use VoiceHelper utility to create an audio file from arbitrary text using Text-To-Speech to be passed to the AVS servers
     * @param context local/application context
     * @param url the URL to which we're sending the AVS post
     * @param accessToken our user's access token for the server
     * @param text the text we want to translate into speech
     * @param callback our event callbacks
     * @throws IOException
     */
    public void sendText(final Context context, final String url, final String accessToken, String text,
                         final AlexaManager.AsyncEventHandler callback) throws IOException {

        if(callback != null){
            callback.start();
        }

        Log.i(TAG, "Starting SpeechSendText procedure");
        start = System.currentTimeMillis();

        //add a pause to the end to be better understood
        if(!TextUtils.isEmpty(text)){
            text = "... " + text + " ...";
        }

        final String input = text;


        //call the parent class's prepareConnection() in order to prepare our URL POST
        prepareConnection(url, accessToken);

        //get our VoiceHelper and use an async callback to get the data and send it off to the AVS server via completePost()
        VoiceHelper voiceHelper = VoiceHelper.getInstance(context);
        voiceHelper.getSpeechFromText(input, new VoiceHelper.SpeechFromTextCallback() {
            @Override
            public void onSuccess(final byte[] data){

                Log.i("LogUtils", "We have audio data ");
//                AudioTrack audioTrack= new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT, AudioTrack.getMinBufferSize(16000,AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
//                audioTrack.play();
//                audioTrack.write(data, 0, data.length);
                try {
                    mOutputStream.write(data);
                    Log.i(TAG, "Audio sent");
                    if(callback != null) {
                        callback.success(completePost());
                        callback.complete();
                    }

                } catch (IOException e) {
                    onError(e);
                } catch (AvsException e) {
                    onError(e);
                }
            }


            @Override
            public void onError(Exception e) {
                if(callback != null){
                    callback.failure(e);
                    callback.complete();
                }
            }
        });

    }


    @NotNull
    @Override
    protected RequestBody getRequestBody() {
        return new DataRequestBody() {
            /**
             * @param sink
             * @throws IOException
             */
            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                byte[] data = mOutputStream.toByteArray();
//                AudioTrack audioTrack= new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT, AudioTrack.getMinBufferSize(16000,AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
//                audioTrack.play();
//                audioTrack.write(data, 0, data.length);
//                playWav(data,"aaSpeechaaa.wav",false);
                sink.write(data);
                //这种方式，云端asr解析出来的是空字符串，但是我们传过去的格式是正确的
            }
        };
    }

//    public static void playWav(byte[] wavSoundByteArray, String fileName,boolean isPlay) {
//        try {
//            // create temp file that will hold byte array
//            File tempWav = new File(Environment.getExternalStorageDirectory(),
//                    fileName);
//            tempWav.deleteOnExit();
//            FileOutputStream fos = new FileOutputStream(tempWav);
//            fos.write(wavSoundByteArray);
//            fos.close();
//
//            // Tried reusing instance of media player
//            // but that resulted in system crashes...
//            if(isPlay) {
//                MediaPlayer mediaPlayer = new MediaPlayer();
//
//                // Tried passing path directly, but kept getting
//                // "Prepare failed.: status=0x1"
//                // so using file descriptor instead
//                FileInputStream fis = new FileInputStream(tempWav);
//                mediaPlayer.setDataSource(fis.getFD());
//
//                mediaPlayer.prepare();
//                mediaPlayer.start();
//            }
//        } catch (IOException ex) {
//            String s = ex.toString();
//            Log.i("gaopan123"," exception s= "+s);
//            ex.printStackTrace();
//        }
//    }
}
