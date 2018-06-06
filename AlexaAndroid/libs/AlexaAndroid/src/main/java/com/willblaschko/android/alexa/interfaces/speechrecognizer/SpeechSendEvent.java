package com.willblaschko.android.alexa.interfaces.speechrecognizer;

import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.interfaces.SendEvent;
import com.willblaschko.android.alexa.okhttp3.MultipartBody;
import com.willblaschko.android.alexa.okhttp3.RequestBody;

import org.jetbrains.annotations.NotNull;
/**
 * Abstract class to extend {@link SendEvent} to automatically add the RequestBody with the correct type
 * and name, as well as the SpeechRecognizer {@link Event}
 *
 * @author will on 5/21/2016.
 */
public abstract class SpeechSendEvent extends SendEvent {

    @NotNull
    @Override
    protected String getEvent() {
        return Event.getSpeechRecognizerEvent();
    }

    @Override
    protected void addFormDataParts(MultipartBody.Builder builder){
        builder.addFormDataPart("audio", "speech.wav", getRequestBody());
    }

    @NotNull
    protected abstract RequestBody getRequestBody();
}
