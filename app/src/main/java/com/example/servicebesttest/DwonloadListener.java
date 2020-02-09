package com.example.servicebesttest;

public interface DwonloadListener {

    void onProgress(int Progress);
    void onSuccess();
    void onFailed();
    void onPause();
    void onCanceled();
}
