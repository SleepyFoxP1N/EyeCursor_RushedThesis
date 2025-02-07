package com.google.mediapipe.examples.facemesh;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class DataHolder {
    private static final DataHolder instance = new DataHolder();
    private MutableLiveData<String> data = new MutableLiveData<>();

    private DataHolder() {}

    public static DataHolder getInstance() {
        return instance;
    }

    public LiveData<String> getData() {
        return data;
    }

    public void setData(String newData) {
        data.setValue(newData);
    }
}

