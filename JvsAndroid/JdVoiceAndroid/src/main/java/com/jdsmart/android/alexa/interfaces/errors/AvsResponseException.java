package com.jdsmart.android.alexa.interfaces.errors;

import com.jdsmart.android.alexa.data.Directive;
import com.jdsmart.android.alexa.interfaces.AvsItem;

/**
 * Created by will on 6/26/2016.
 */

public class AvsResponseException extends AvsItem {
    Directive directive;
    public AvsResponseException(Directive directive) {
        super(null);
        this.directive = directive;
    }

    public Directive getDirective() {
        return directive;
    }
}
