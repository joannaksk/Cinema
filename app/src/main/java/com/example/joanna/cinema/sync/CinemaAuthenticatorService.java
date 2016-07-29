package com.example.joanna.cinema.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by joanna on 29/07/16.
 */
public class CinemaAuthenticatorService extends Service {
    // Instance field that stores the authenticator object
    private com.example.joanna.cinema.sync.CinemaAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new com.example.joanna.cinema.sync.CinemaAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
