package com.willblaschko.android.alexavoicelibrary;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexavoicelibrary.utility.SigningKey;

/**
 * An application to handle all our initialization for the Alexa library before we
 * launch our VoiceLaunchActivity
 */
public class AlexaApplication extends Application {

    //Our Amazon application product ID, this is passed to the server when we authenticate


    //Our Application instance if we need to reference it directly
    private static AlexaApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        AlexaManager._appkey = "A8064DFC0B834B7FA5A2F92B005FB27A";
        AlexaManager._deviceid = "12345678";

        AlexaManager._token = FileUtils.readFileFromSd("access_token.txt");
        if(TextUtils.isEmpty(AlexaManager._token)){
            AlexaManager._token = "356388383";
            Toast.makeText(this,"do not get access token",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this," access token :"+AlexaManager._token,Toast.LENGTH_SHORT).show();
            Log.i("token = ", AlexaManager._token);
        }
        //if we run in DEBUG mode, we can get our signing key in the LogCat
        if(BuildConfig.DEBUG){
            Log.i("AlexaApplication", SigningKey.getCertificateMD5Fingerprint(this));
        }
    }

    /**
     * Return a reference to our mInstance instance
     * @return our current application instance, created in onCreate()
     */
    public static AlexaApplication getInstance(){
        return mInstance;
    }



}
