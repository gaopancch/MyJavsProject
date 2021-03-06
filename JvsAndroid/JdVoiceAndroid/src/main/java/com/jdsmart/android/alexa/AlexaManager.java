package com.jdsmart.android.alexa;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.jdsmart.android.alexa.callbacks.AsyncCallback;
import com.jdsmart.android.alexa.callbacks.AuthorizationCallback;
import com.jdsmart.android.alexa.data.Event;
import com.jdsmart.android.alexa.interfaces.AvsException;
import com.jdsmart.android.alexa.interfaces.AvsItem;
import com.jdsmart.android.alexa.interfaces.AvsResponse;
import com.jdsmart.android.alexa.interfaces.GenericSendEvent;
import com.jdsmart.android.alexa.interfaces.audioplayer.AvsPlayAudioItem;
import com.jdsmart.android.alexa.interfaces.response.ResponseParser;
import com.jdsmart.android.alexa.interfaces.speechrecognizer.SpeechSendAudio;
import com.jdsmart.android.alexa.interfaces.speechrecognizer.SpeechSendText;
import com.jdsmart.android.alexa.interfaces.speechrecognizer.SpeechSendVoice;
import com.jdsmart.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;
import com.jdsmart.android.alexa.okhttp3.Call;
import com.jdsmart.android.alexa.okhttp3.Response;
import com.jdsmart.android.alexa.requestbody.DataRequestBody;
import com.jdsmart.android.alexa.service.DownChannelService;
import com.jdsmart.android.alexa.system.AndroidSystemHandler;
import com.jdsmart.android.alexa.utility.Util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import okio.BufferedSink;
import speechutils.TtsProvider;

import static com.jdsmart.android.alexa.AuthorizationManager.createCodeVerifier;
import static com.jdsmart.android.alexa.interfaces.response.ResponseParser.getBoundary;
import static com.jdsmart.android.alexa.utility.Util.IDENTIFIER;

/**
 * The overarching instance that handles all the state when requesting intents to the Alexa Voice Service servers, it creates all the required instances and confirms that users are logged in
 * and authenticated before allowing them to send intents.
 *
 * Beyond initialization, mostly it supplies wrapped helper functions to the other classes to assure authentication state.
 */
public class AlexaManager {

    public static final boolean needTokenCheck = false;//true时候每次请求都需要token验证，登录状态验证
    public static String _token = "tokentest";
    public static String _deviceid="gaopan27";
    public static String _appkey = "9988jjhdhdhd";
    private static final String TAG = "AlexaManager";
    private static final String KEY_URL_ENDPOINT = "url_endpoint";

    private static AlexaManager mInstance;
    private static AndroidSystemHandler mAndroidSystemHandler;
    private AuthorizationManager mAuthorizationManager;
    private SpeechSendVoice mSpeechSendVoice;
    private SpeechSendText mSpeechSendText;
    private SpeechSendAudio mSpeechSendAudio;
    private VoiceHelper mVoiceHelper;
    private String urlEndpoint;
    private Context mContext;
    private boolean mIsRecording = false;
    private TtsProvider ttsProvider;

    private AlexaManager(Context context, String productId){
        mContext = context.getApplicationContext();
         ttsProvider = new TtsProvider(mContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
//                Log.i("LogUtils","onInit status = "+status);
            }
        });
        if(productId == null){
            productId = context.getString(R.string.alexa_product_id);
        }
        if(needTokenCheck) {
            urlEndpoint = Util.getPreferences(context).getString(KEY_URL_ENDPOINT, context.getString(R.string.alexa_api));
        }else{
            urlEndpoint = Util.getPreferences(context).getString(KEY_URL_ENDPOINT, context.getString(R.string.jdsmart_api));
        }

        mAuthorizationManager = new AuthorizationManager(mContext, productId);
        mAndroidSystemHandler = AndroidSystemHandler.getInstance(context);
        //AlexaManager初始化时候就开启了DownChannelService服务
        Intent stickyIntent = new Intent(context, DownChannelService.class);
        context.startService(stickyIntent);

        if(!Util.getPreferences(mContext).contains(IDENTIFIER)){
            Util.getPreferences(mContext)
                    .edit()
                    .putString(IDENTIFIER, createCodeVerifier(30))
                    .apply();
        }
    }

    /**
     * Return an instance of AlexaManager
     *
     * @param context application context
     * @return AlexaManager instance
     */
    public static AlexaManager getInstance(Context context){
        return getInstance(context, null);
    }

    /**
     * Return an instance of AlexaManager
     *
     * Deprecated: use @getInstance(Context) instead and set R.string.alexa_product_id in your application resources,
     * this change was made to properly support the DownChannelService
     *
     * @param context application context
     * @param productId AVS product id
     * @return AlexaManager instance
     */
    @Deprecated
    public static AlexaManager getInstance(Context context, String productId){
        if(mInstance == null){
            mInstance = new AlexaManager(context, productId);
        }
        return mInstance;
    }

    public AuthorizationManager getAuthorizationManager(){
        return mAuthorizationManager;
    }

    public void setUrlEndpoint(String url){
        urlEndpoint = url;
        Util.getPreferences(mContext)
                .edit()
                .putString(KEY_URL_ENDPOINT, url)
                .apply();
    }


    public SpeechSendVoice getSpeechSendVoice(){
        if(mSpeechSendVoice == null){
            mSpeechSendVoice = new SpeechSendVoice();
        }
        return mSpeechSendVoice;
    }

    public SpeechSendText getSpeechSendText(){
        if(mSpeechSendText == null){
            mSpeechSendText = new SpeechSendText();
        }
        return mSpeechSendText;
    }

    public SpeechSendAudio getSpeechSendAudio(){
        if(mSpeechSendAudio == null){
            mSpeechSendAudio = new SpeechSendAudio();
        }
        return mSpeechSendAudio;
    }

    public VoiceHelper getVoiceHelper(){
        if(mVoiceHelper == null){
            mVoiceHelper = VoiceHelper.getInstance(mContext);
        }
        return mVoiceHelper;
    }

    /**
     * Check if the user is logged in to the Amazon service, uses an async callback with a boolean to return response
     * @param callback state callback
     */
    public void checkLoggedIn(@NotNull final AsyncCallback<Boolean, Throwable> callback){
        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {

            }
            @Override
            public void success(Boolean result) {
                callback.success(result);
            }

            @Override
            public void failure(Throwable error) {
                callback.failure(error);
            }

            @Override
            public void complete() {

            }
        });
    }

    /**
     * Send a log in request to the Amazon Authentication Manager
     * @param callback state callback
     */
    public void logIn(@Nullable final AuthorizationCallback callback){
        //check if we're already logged in
        mAuthorizationManager.checkLoggedIn(mContext, new AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {

            }

            @Override
            public void success(Boolean result) {
                //if we are, return a success
                if(result){
                    if(callback != null){
                        callback.onSuccess();
                    }
                }else{
                    //otherwise start the authorization process
                    mAuthorizationManager.authorizeUser(callback);
                }
            }

            @Override
            public void failure(Throwable error) {
                if(callback != null) {
                    callback.onError(new Exception(error));
                }
            }

            @Override
            public void complete() {

            }
        });

    }


    /**
     * Send a synchronize state {@link Event} request to Alexa Servers to retrieve pending {@link com.jdsmart.android.alexa.data.Directive}
     * See: {@link #sendEvent(String, AsyncCallback)}
     * @param callback state callback
     */
    public void sendSynchronizeStateEvent(@Nullable final AsyncCallback<AvsResponse, Exception> callback){
        sendEvent(Event.getSynchronizeStateEvent(), callback);
    }

    /**
     * Helper function to check if we're currently recording
     * @return
     */
    public boolean isRecording(){
        return mIsRecording;
    }


    /**
     * Send a text string request to the AVS server, this is run through Text-To-Speech to create the raw audio file needed by the AVS server.
     *
     * This allows the developer to pre/post-pend or send any arbitrary text to the server, versus the startRecording()/stopRecording() combination which
     * expects input from the user. This operation, because of the extra steps is generally slower than the above option.
     *
     * @param text the arbitrary text that we want to send to the AVS server
     * @param callback the state change callback
     */
    public void sendTextRequest(final String text, @Nullable final AsyncCallback<AvsResponse, Exception> callback){
        if(needTokenCheck) {
            //check if the user is already logged in
            mAuthorizationManager.checkLoggedIn(mContext, new ImplCheckLoggedInCallback() {
                @Override
                public void success(Boolean result) {
                    if (result) {
                        //if the user is logged in

                        //set our URL
                        final String url = getEventsUrl();
                        //do this off the main thread
                        new AsyncTask<Void, Void, AvsResponse>() {
                            @Override
                            protected AvsResponse doInBackground(Void... params) {
                                //get our access token
                                TokenManager.getAccessToken(mAuthorizationManager.getAmazonAuthorizationManager(), mContext, new TokenManager.TokenCallback() {
                                    @Override
                                    public void onSuccess(String token) {

                                        try {
                                            getSpeechSendText().sendText(mContext, url, token, text, new AsyncEventHandler(AlexaManager.this, callback));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            //bubble up the error
                                            if (callback != null) {
                                                callback.failure(e);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onFailure(Throwable e) {

                                    }
                                });
                                return null;
                            }


                            @Override
                            protected void onPostExecute(AvsResponse avsResponse) {
                                super.onPostExecute(avsResponse);
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        //if the user is not logged in, log them in and then call the function again
                        logIn(new ImplAuthorizationCallback<AvsResponse>(callback) {

                            @Override
                            public void onSuccess() {
                                //call our function again
                                sendTextRequest(text, callback);
                            }

                        });
                    }
                }

            });
        }else{
            Log.i("LogUtils"," sendText  text = "+text);
            try {
                getSpeechSendText().sendText(mContext, getEventsUrl(), AlexaManager._token, text, new AsyncEventHandler(AlexaManager.this, callback));
            } catch (Exception e) {
                e.printStackTrace();
                //bubble up the error
                if (callback != null) {
                    callback.failure(e);
                }
            }
        }
    }


    /**
     * Send raw audio data to the Alexa servers, this is a more advanced option to bypass other issues (like only one item being able to use the mic at a time).
     *
     * @param data the audio data that we want to send to the AVS server
     * @param callback the state change callback
     */
    public void sendAudioRequest(final byte[] data, @Nullable final AsyncCallback<AvsResponse, Exception> callback){
        sendAudioRequest(new DataRequestBody() {
            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(data);
            }
        }, callback);
    }

    /**
     * Send streamed raw audio data to the Alexa servers, this is a more advanced option to bypass other issues (like only one item being able to use the mic at a time).
     *
     * @param requestBody a request body that incorporates either a static byte[] write to the BufferedSink or a streamed, managed byte[] data source
     * @param callback the state change callback
     */
    public void sendAudioRequest(final DataRequestBody requestBody, @Nullable final AsyncCallback<AvsResponse, Exception> callback){
        if(!AlexaManager.needTokenCheck) {
            new AsyncTask<Void, Void, AvsResponse>() {
                @Override
                protected AvsResponse doInBackground(Void... params) {
                    try {
                        Log.i("LogUtils", "sendAudioRequest 360");
                        getSpeechSendAudio().sendAudio(getEventsUrl(), AlexaManager._token, requestBody, new AsyncEventHandler(AlexaManager.this, callback));
                    } catch (IOException e) {
                        e.printStackTrace();
                        //bubble up the error
                        if (callback != null) {
                            callback.failure(e);
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(AvsResponse avsResponse) {
                    super.onPostExecute(avsResponse);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            //check if the user is already logged in
            mAuthorizationManager.checkLoggedIn(mContext, new ImplCheckLoggedInCallback() {

                @Override
                public void success(Boolean result) {
                    if (result) {
                        //if the user is logged in

                        //set our URL
                        final String url = getEventsUrl();
                        //get our access token
                        TokenManager.getAccessToken(mAuthorizationManager.getAmazonAuthorizationManager(), mContext, new TokenManager.TokenCallback() {
                            @Override
                            public void onSuccess(final String token) {
                                //do this off the main thread
                                new AsyncTask<Void, Void, AvsResponse>() {
                                    @Override
                                    protected AvsResponse doInBackground(Void... params) {
                                        try {
                                            getSpeechSendAudio().sendAudio(url, token, requestBody, new AsyncEventHandler(AlexaManager.this, callback));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            //bubble up the error
                                            if (callback != null) {
                                                callback.failure(e);
                                            }
                                        }
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(AvsResponse avsResponse) {
                                        super.onPostExecute(avsResponse);
                                    }
                                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }

                            @Override
                            public void onFailure(Throwable e) {

                            }
                        });
                    } else {
                        //if the user is not logged in, log them in and then call the function again
                        logIn(new ImplAuthorizationCallback<AvsResponse>(callback) {
                            @Override
                            public void onSuccess() {
                                //call our function again
                                sendAudioRequest(requestBody, callback);
                            }
                        });
                    }
                }

            });
        }
    }

    public void cancelAudioRequest() {
        //check if the user is already logged in
        mAuthorizationManager.checkLoggedIn(mContext, new ImplCheckLoggedInCallback() {
            @Override
            public void success(Boolean result) {
                if (result) {
                    //if the user is logged in
                    getSpeechSendAudio().cancelRequest();
                }
            }

        });
    }

    /** Send a confirmation to the Alexa server that the device volume has been changed in response to a directive
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param volume volume as reported by the {@link com.jdsmart.android.alexa.interfaces.speaker.AvsAdjustVolumeItem} Directive
     * @param isMute report whether the device is currently muted
     * @param callback state callback
     */
    public void sendVolumeChangedEvent(final long volume, final boolean isMute, @Nullable final AsyncCallback<AvsResponse, Exception> callback){
        sendEvent(Event.getVolumeChangedEvent(volume, isMute), callback);
    }


    /** Send a confirmation to the Alexa server that the mute state has been changed in response to a directive
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param isMute mute state as reported by the {@link com.jdsmart.android.alexa.interfaces.speaker.AvsSetMuteItem} Directive
     * @param callback
     */
    public void sendMutedEvent(final boolean isMute, @Nullable final AsyncCallback<AvsResponse, Exception> callback){
        sendEvent(Event.getMuteEvent(isMute), callback);
    }

    /**
     * Send confirmation that the device has timed out without receiving a speech request when expected
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param callback
     */
    public void sendExpectSpeechTimeoutEvent(final AsyncCallback<AvsResponse, Exception> callback){
        sendEvent(Event.getExpectSpeechTimedOutEvent(), callback);
    }



    /**
     * Send an event to indicate that playback of a speech item has started
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param item our speak item
     * @param callback
     */
    public void sendPlaybackStartedEvent(AvsItem item, long milliseconds, final AsyncCallback<AvsResponse, Exception> callback) {
        if (item == null) {
            return;
        }
        String event;
        if (isAudioPlayItem(item)) {
            event = Event.getPlaybackStartedEvent(item.getToken(), milliseconds);
        } else {
            event = Event.getSpeechStartedEvent(item.getToken());
        }

        sendEvent(event, callback);
    }

    /**
     * Send an event to indicate that playback of a speech item has finished
     * See: {@link #sendEvent(String, AsyncCallback)}
     *
     * @param item our speak item
     * @param callback
     */
    public void sendPlaybackFinishedEvent(AvsItem item, final AsyncCallback<AvsResponse, Exception> callback){
        if (item == null) {
            return;
        }
        String event;
        if (isAudioPlayItem(item)) {
            event = Event.getPlaybackFinishedEvent(item.getToken());
        } else {
            event = Event.getSpeechFinishedEvent(item.getToken());
        }
        sendEvent(event, callback);
    }


    /**
     * Send an event to indicate that playback of an item has nearly finished
     *
     * @param item our speak/playback item
     * @param callback
     */
    public void sendPlaybackNearlyFinishedEvent(AvsPlayAudioItem item, long milliseconds, final AsyncCallback<AvsResponse, Exception> callback){
        if (item == null) {
            return;
        }
        String event = Event.getPlaybackNearlyFinishedEvent(item.getToken(), milliseconds);

        sendEvent(event, callback);
    }

    /**
     * Send a generic event to the AVS server, this is generated using {@link com.jdsmart.android.alexa.data.Event.Builder}
     * @param event the string JSON event
     * @param callback
     */
    public void sendEvent(final String event, final AsyncCallback<AvsResponse, Exception> callback){
        if(AlexaManager.needTokenCheck) {
            //check if the user is already logged in
            mAuthorizationManager.checkLoggedIn(mContext, new ImplCheckLoggedInCallback() {

                @Override
                public void success(Boolean result) {
                    if (result) {
                        //if the user is logged in

                        //set our URL
                        final String url = getEventsUrl();
                        //get our access token
                        TokenManager.getAccessToken(mAuthorizationManager.getAmazonAuthorizationManager(), mContext, new TokenManager.TokenCallback() {
                            @Override
                            public void onSuccess(final String token) {
                                //do this off the main thread
                                new AsyncTask<Void, Void, AvsResponse>() {
                                    @Override
                                    protected AvsResponse doInBackground(Void... params) {
                                        Log.i(TAG, event);
                                        new GenericSendEvent(url, token, event, new AsyncEventHandler(AlexaManager.this, callback));
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(AvsResponse avsResponse) {
                                        super.onPostExecute(avsResponse);
                                    }
                                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }

                            @Override
                            public void onFailure(Throwable e) {

                            }
                        });
                    } else {
                        //if the user is not logged in, log them in and then call the function again
                        logIn(new ImplAuthorizationCallback<AvsResponse>(callback) {
                            @Override
                            public void onSuccess() {
                                //call our function again
                                sendEvent(event, callback);
                            }
                        });
                    }
                }

            });
        }else {
            new AsyncTask<Void, Void, AvsResponse>() {
                @Override
                protected AvsResponse doInBackground(Void... params) {
                    Log.i(TAG, event);
                    new GenericSendEvent(getEventsUrl(), AlexaManager._token, event, new AsyncEventHandler(AlexaManager.this, callback));
                    return null;
                }

                @Override
                protected void onPostExecute(AvsResponse avsResponse) {
                    super.onPostExecute(avsResponse);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private boolean isAudioPlayItem (AvsItem item) {
        return item != null && (item instanceof AvsPlayAudioItem || !(item instanceof AvsSpeakItem));
    }


    public String getUrlEndpoint(){
        return urlEndpoint;
    }

    public String getPingUrl(){
        return new StringBuilder()
                .append(getUrlEndpoint())
                .append("/ping")
                .toString();
    }

    public String getEventsUrl(){
        return new StringBuilder()
                .append(getUrlEndpoint())
                .append("/")
                .append(mContext.getString(R.string.alexa_api_version))
                .append("/")
                .append("events")
                .toString();
    }

    public String getDirectivesUrl(){
        return new StringBuilder()
                .append(getUrlEndpoint())
                .append("/")
                .append(mContext.getString(R.string.alexa_api_version))
                .append("/")
                .append("directives")
                .toString();
    }



    public class AsyncEventHandler implements AsyncCallback<Call, Exception>{

        AsyncCallback<AvsResponse, Exception> callback;
        AlexaManager manager;

        public AsyncEventHandler(AlexaManager manager, AsyncCallback<AvsResponse, Exception> callback){
            this.callback = callback;
            this.manager = manager;
        }

        @Override
        public void start() {
            if (callback != null) {
                callback.start();
            }
        }

        @Override
        public void success(Call currentCall) {
            Log.i("LogUtils","AlexaManager AsyncEventHandler success ");
            try {
                Response response = currentCall.execute();
                Log.i("LogUtils","AsyncEventHandler success response="+response.toString());
                if(response.code() == HttpURLConnection.HTTP_NO_CONTENT){
//                    ttsProvider.say("服务端说没收到发的数据，你是在逗我吗？");
                }

                final AvsResponse items = response.code() == HttpURLConnection.HTTP_NO_CONTENT ? new AvsResponse() :
                        ResponseParser.parseResponse(response.body().byteStream(), getBoundary(response));

                response.body().close();

                mAndroidSystemHandler.handleItems(items);

                if (callback != null) {
                    callback.success(items);
                }
            } catch (Exception e) {
                ttsProvider.say("异常，请重试");
                Log.i("LogUtils","AsyncEventHandler exception ="+e.getMessage());
                if (!currentCall.isCanceled()) {
                    if (callback != null) {
                        callback.failure(e);
                    }
                }
            }
        }

        @Override
        public void failure(Exception error) {
            //bubble up the error
            Log.i("LogUtils","AsyncEventHandler failure error ="+error.getMessage());
            if (callback != null) {
                callback.failure(error);
            }
        }

        @Override
        public void complete() {
            Log.i("LogUtils","AsyncEventHandler complete");
            if (callback != null) {
                callback.complete();
            }
            manager.mSpeechSendAudio = null;
            manager.mSpeechSendVoice = null;
            manager.mSpeechSendText = null;
        }
    }

    private abstract static class ImplAuthorizationCallback<E> implements AuthorizationCallback{

        AsyncCallback<E, Exception> callback;

        public ImplAuthorizationCallback(AsyncCallback<E, Exception> callback){
            this.callback = callback;
        }

        @Override
        public void onCancel() {

        }

        @Override
        public void onError(Exception error) {
            if (callback != null) {
                //bubble up the error
                callback.failure(error);
            }
        }
    }

    private abstract static class ImplCheckLoggedInCallback implements AsyncCallback<Boolean, Throwable>{

        @Override
        public void start() {

        }


        @Override
        public void failure(Throwable error) {

        }

        @Override
        public void complete() {

        }
    }
}
