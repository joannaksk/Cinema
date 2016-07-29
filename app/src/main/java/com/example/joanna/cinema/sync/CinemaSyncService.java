package com.example.joanna.cinema.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by joanna on 29/07/16.
 */
public class CinemaSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static CinemaSyncAdapter sCinemaSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("CinemaSyncService", "onCreate - CinemaSyncService");
        synchronized (sSyncAdapterLock) {
            if (sCinemaSyncAdapter == null) {
                sCinemaSyncAdapter = new CinemaSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sCinemaSyncAdapter.getSyncAdapterBinder();
    }
}
