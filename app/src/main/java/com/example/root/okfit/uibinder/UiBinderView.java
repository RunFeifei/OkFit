package com.example.root.okfit.uibinder;


import com.example.crnetwork.error.ErrorCode;
import com.example.crnetwork.response.RequestException;

/**
 * Created by Lei Guoting on 17-3-19.
 */
public interface UiBinderView {
    void onUiBinderStart(int type);

    void onUiBinderError(RequestException exception);

    void onUiBinderEnd(int type);
}
