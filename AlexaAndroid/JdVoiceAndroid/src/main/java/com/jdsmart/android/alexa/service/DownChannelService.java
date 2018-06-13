package com.jdsmart.android.alexa.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jdsmart.android.alexa.AlexaManager;
import com.jdsmart.android.alexa.TokenManager;
import com.jdsmart.android.alexa.callbacks.ImplAsyncCallback;
import com.jdsmart.android.alexa.connection.ClientUtil;
import com.jdsmart.android.alexa.data.Directive;
import com.jdsmart.android.alexa.data.Event;
import com.jdsmart.android.alexa.interfaces.AvsItem;
import com.jdsmart.android.alexa.interfaces.AvsResponse;
import com.jdsmart.android.alexa.interfaces.response.ResponseParser;
import com.jdsmart.android.alexa.okhttp3.Call;
import com.jdsmart.android.alexa.okhttp3.Callback;
import com.jdsmart.android.alexa.okhttp3.OkHttpClient;
import com.jdsmart.android.alexa.okhttp3.Request;
import com.jdsmart.android.alexa.okhttp3.Response;
import com.jdsmart.android.alexa.system.AndroidSystemHandler;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import okio.BufferedSource;

/**
 * @author will on 4/27/2016.
 */
public class DownChannelService extends Service {

    private static final String TAG = "DownChannelService";

    private AlexaManager alexaManager;
    private Call currentCall;
    private AndroidSystemHandler handler;
    private Handler runnableHandler;
    private Runnable pingRunnable;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Launched");
        alexaManager = AlexaManager.getInstance(this);
        handler = AndroidSystemHandler.getInstance(this);

        runnableHandler = new Handler(Looper.getMainLooper());
        pingRunnable = new Runnable() {
            @Override
            public void run() {
                if(AlexaManager.needTokenCheck) {
                    TokenManager.getAccessToken(alexaManager.getAuthorizationManager().getAmazonAuthorizationManager(), DownChannelService.this, new TokenManager.TokenCallback() {
                        @Override
                        public void onSuccess(String token) {
                            Log.i(TAG, "Sending heartbeat");
                            final Request request = new Request.Builder()
                                    .url(alexaManager.getPingUrl())
                                    .get()
                                    .addHeader("Authorization", "Bearer " + token)
                                    .build();
                            Log.i("LogUtils", "DownChannelService pingRunnable url =" + alexaManager.getPingUrl());
                            ClientUtil.getTLS12OkHttpClient()
                                    .newCall(request)
                                    .enqueue(new Callback() {
                                        @Override
                                        public void onFailure(Call call, IOException e) {
                                            Log.i("LogUtils", "Sending heartbeat onFailure e="+e.getMessage());
                                        }

                                        @Override
                                        public void onResponse(Call call, Response response) throws IOException {
//                                            Log.i("LogUtils", "Sending heartbeat ping success");
                                            runnableHandler.postDelayed(pingRunnable, 1 * 60 * 1000);
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(Throwable e) {

                        }
                    });
                }else{
                    Log.i("LogUtils", "Sending heartbeat--");
                    final Request request = new Request.Builder()
                            .url(alexaManager.getPingUrl())
                            .get()
                            .addHeader("Authorization", AlexaManager._token)
                            .addHeader("device-id",AlexaManager._deviceid)
                            .addHeader("app-key",AlexaManager._appkey)
                            .build();
                    ClientUtil.getTLS12OkHttpClient()
                            .newCall(request)
                            .enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    Log.i("LogUtils", "Sending heartbeat onFailure e="+e.getMessage());
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
//                                    Log.i("LogUtils", "ping success response = "+response.toString());
                                    runnableHandler.postDelayed(pingRunnable, 1 * 60 * 1000);
                                }
                            });
                }
            }
        };
        
        openDownChannel();

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(currentCall != null){
            currentCall.cancel();
        }
        runnableHandler.removeCallbacks(pingRunnable);
    }


    private void openDownChannel(){
        if(AlexaManager.needTokenCheck) {
            TokenManager.getAccessToken(alexaManager.getAuthorizationManager().getAmazonAuthorizationManager(), DownChannelService.this, new TokenManager.TokenCallback() {
                @Override
                public void onSuccess(String token) {

                    OkHttpClient downChannelClient = ClientUtil.getTLS12OkHttpClient();
                    final Request request = new Request.Builder()
                            .url(alexaManager.getDirectivesUrl())
                            .get()
                            .addHeader("Authorization", "Bearer " + token)
                            .build();
                    Log.i("LogUtils", "DownChannelService openDownChannel url =" + alexaManager.getDirectivesUrl());
                    currentCall = downChannelClient.newCall(request);
                    currentCall.enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {

                            alexaManager.sendEvent(Event.getSynchronizeStateEvent(), new ImplAsyncCallback<AvsResponse, Exception>() {
                                @Override
                                public void success(AvsResponse result) {
                                    handler.handleItems(result);
                                    runnableHandler.post(pingRunnable);
                                }
                            });

                            BufferedSource bufferedSource = response.body().source();

                            while (!bufferedSource.exhausted()) {
                                String line = bufferedSource.readUtf8Line();
                                try {
                                    Directive directive = ResponseParser.getDirective(line);
                                    handler.handleDirective(directive);

                                    //surface to our UI if it's up
                                    try {
                                        AvsItem item = ResponseParser.parseDirective(directive);
                                        EventBus.getDefault().post(item);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Bad line e ="+e.getMessage());
                                }
                            }

                        }
                    });

                }

                @Override
                public void onFailure(Throwable e) {
                    e.printStackTrace();
                }
            });
        }else{
//            Http2Connection
            OkHttpClient downChannelClient = ClientUtil.getTLS12OkHttpClient();
            final Request request = new Request.Builder()
                    .url(alexaManager.getDirectivesUrl())
                    .get()
                    .addHeader("Authorization", AlexaManager._token)
                    .addHeader("device-id",AlexaManager._deviceid)
                    .addHeader("app-key",AlexaManager._appkey)
                    .build();
            Log.i("LogUtils", "DownChannelService openDownChannel request =" + request.toString());
            currentCall = downChannelClient.newCall(request);
            currentCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("LogUtils", "onFailure e ="+e.getMessage());
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.i("LogUtils", "downChannelClient onResponse"+response);
//                    alexaManager.sendEvent(Event.getSynchronizeStateEvent(), new ImplAsyncCallback<AvsResponse, Exception>() {
//                        @Override
//                        public void success(AvsResponse result) {
////                            handler.handleItems(result);
//                            Log.i("LogUtils", "downChannelClient result ="+result);
//                            runnableHandler.post(pingRunnable);
//                        }
//                    });
                    runnableHandler.post(pingRunnable);
                    BufferedSource bufferedSource = response.body().source();
                    while (!bufferedSource.exhausted()) {
                        String line = bufferedSource.readUtf8Line();
                        try {
                            Directive directive = ResponseParser.getDirective(line);
                            handler.handleDirective(directive);

                            //surface to our UI if it's up
                            try {
                                AvsItem item = ResponseParser.parseDirective(directive);
                                EventBus.getDefault().post(item);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "256 Bad line e ="+e.getMessage());
                        }
                    }

                }
            });
        }
    }

}
