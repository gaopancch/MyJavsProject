package com.jdsmart.android.alexa.requestbody;

import com.jdsmart.android.alexa.okhttp3.MediaType;
import com.jdsmart.android.alexa.okhttp3.RequestBody;

/**
 * An implemented class that automatically fills in the required MediaType for the {@link RequestBody} that is sent
 * in the {@link com.jdsmart.android.alexa.interfaces.SendEvent} class.
 *
 * @author will on 5/28/2016.
 */
public abstract class DataRequestBody extends RequestBody {
    @Override
    public MediaType contentType() {
        return MediaType.parse("application/octet-stream");
    }
}
