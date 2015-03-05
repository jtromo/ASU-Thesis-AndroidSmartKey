package com.sefcom.Services;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.sefcom.LockScreen.LockScreenActivity;

public class LockScreenService extends Service {
	private static String TAG = "LockScreenService";
	BroadcastReceiver mReceiver;

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	@Override
	public void onCreate() {
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		mReceiver = new LockScreenReceiver();
		registerReceiver(mReceiver, filter);
		super.onCreate();

		Toast.makeText(this, "The new Service was Created", Toast.LENGTH_LONG)
				.show();

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				// Intent dialogIntent = new Intent(getBaseContext(),
				// LockScreenActivity.class);
				// dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				// getApplication().startActivity(dialogIntent);

				// Toast.makeText(this, "Test", Toast.LENGTH_LONG).show();
			}
		}, 0, 1000);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Toast.makeText(this, " Service Started", Toast.LENGTH_LONG).show();

	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();

		unregisterReceiver(mReceiver);
	}

}
