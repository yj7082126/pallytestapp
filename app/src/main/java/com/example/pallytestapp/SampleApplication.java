package com.example.pallytestapp;

import android.app.Application;

import com.microsoft.CloudServices;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Use application's context to initialize CloudServices!
        CloudServices.initialize(this);
    }
}
