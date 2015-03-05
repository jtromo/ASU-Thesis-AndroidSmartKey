package com.sefcom.Launcher;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.sefcom.Bluetooth.BluetoothService;
import com.sefcom.Bluetooth.DeviceListActivity;
import com.sefcom.Security.EncryptionUtils;
import com.sefcom.Security.SecurityDefinitions;
import com.sefcom.SmartKey.MyApplication;
import com.sefcom.SmartKey.R;

public class HomeActivity extends Activity {
	// Debugging
	private static final String TAG = "SmartKey";
	private static final boolean D = true;

	private MyApplication app;

	private Timer myTimer;

	// String buffer for outgoing messages
	public StringBuffer mOutStringBuffer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Set up the window layout
		setContentView(R.layout.activity_home);

		app = (MyApplication) getApplication();

		// SHARED_SECRET_KEY = readSecretKey();

		// Get local Bluetooth adapter
		app.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (app.mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			authenticationResult(false, false);
			return;
		}

		PhoneStateListener phoneStateListener = new PhoneStateListener();
		TelephonyManager telephonymanager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		telephonymanager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_CALL_STATE);

		myTimer = new Timer();
		myTimer.schedule(new TimerTask() {
			public void run() {
				if (app.mBluetoothService == null) {
					setupConnection();
				} else {

					app.mBluetoothService.stop();
					setupConnection();
					// Attempt to connect to known devices
					// autoConnectToKnownDevice(MyApplication.KNOWN_DEVICE_MAC);
				}
			}
		}, 10000, 10000);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupConnection() will then be called during onActivityResult
		if (!app.mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent,
					MyApplication.REQUEST_ENABLE_BT);
			// Otherwise, setup the connection session
		} else {
			if (app.mBluetoothService == null)
				setupConnection();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (app.mBluetoothService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (app.mBluetoothService.getState() == BluetoothService.STATE_NONE) {
				// Start the Bluetooth services
				app.mBluetoothService.start();
			}
		}
	}
	
	// Disables the back button
		@Override
		public void onBackPressed() {
			myTimer.cancel();
			if (app.mBluetoothService != null)
				app.mBluetoothService.stop();
			super.onBackPressed();
		}

	public void authenticationResult(boolean success, boolean continuousAuth) {
		if (!success) {
			Toast.makeText(getApplicationContext(), "Authentication Failed",
					Toast.LENGTH_LONG).show();
			// Return to lock screen
			myTimer.cancel();
			app.mBluetoothService.stop();
			finish();
		} else {
			Toast.makeText(getApplicationContext(),
					"Successful Authentication", Toast.LENGTH_LONG).show();
		}
	}

	private void setupConnection() {
		Log.d(TAG, "setupConnection()");

		// Initialize the BluetoothService to perform bluetooth connections
		app.mBluetoothService = new BluetoothService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");

		// Attempt to connect to known devices
		autoConnectToKnownDevice(MyApplication.KNOWN_DEVICE_MAC);
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (app != null) {
			// Stop the Bluetooth services
			if (app.mBluetoothService != null)
				app.mBluetoothService.stop();
		}
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");

		// Start lockscreen service
		// startService(new Intent(this, LockScreenService.class));
	}

	private void sendAuthenticationChallenge() {
		sendEncryptedMessage(SecurityDefinitions.DEVICE_ID);
	}

	/**
	 * Sends an encrypted message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendEncryptedMessage(String message) {
		// Check that we're actually connected before trying anything
		if (app.mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothService to write
			byte[] sendUnencrypted = message.getBytes();

			try {
				byte[] sendEncrypted = null;

				// Enable/Disable Encryption
				if (SecurityDefinitions.DEBUG_DISABLE_ENCRYPTION) {
					sendEncrypted = sendUnencrypted;
				} else {
					sendEncrypted = EncryptionUtils.encryptData(
							getApplicationContext(), sendUnencrypted);
				}

				// Create timestamp for to verify the return is within a valid
				// time
				app.authStartTimestamp = System.currentTimeMillis() / 1000;

				if (SecurityDefinitions.DEBUG_MODE) {
					Toast.makeText(getApplicationContext(), "Challenge Sent",
							Toast.LENGTH_LONG).show();
				}
				app.mBluetoothService.write(sendEncrypted);

				// Reset out string buffer to zero and clear the edit text field
				mOutStringBuffer.setLength(0);
				// mOutEditText.setText(mOutStringBuffer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// The Handler that gets information back from the BluetoothService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MyApplication.MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothService.STATE_CONNECTED:
					break;
				case BluetoothService.STATE_CONNECTING:
					break;
				case BluetoothService.STATE_LISTEN:
				case BluetoothService.STATE_NONE:
					break;
				}
				break;
			case MyApplication.MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				break;
			case MyApplication.MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// decrypt
				try {
					byte[] unencrypted = null;

					if (SecurityDefinitions.DEBUG_DISABLE_ENCRYPTION) {
						unencrypted = readBuf;
					} else {
						unencrypted = EncryptionUtils.decryptData(
								getApplicationContext(), readBuf);
					}

					// construct a string from the valid bytes in the buffer
					String readMessage = new String(unencrypted, 0, msg.arg1);

					String[] readMessageArray = readMessage.split("/");
					if (!(readMessageArray.length == 2)) {
						Toast.makeText(getApplicationContext(),
								"Invalid response format", Toast.LENGTH_LONG)
								.show();
						return;
					}

					String readSmartKeyId = readMessageArray[0];
					String readTimestamp = readMessageArray[1];

					if (!readSmartKeyId.equals(SecurityDefinitions.SMARTKEY_ID)) {
						Toast.makeText(getApplicationContext(),
								"Challenge Failed: Invalid Smart Key ID",
								Toast.LENGTH_LONG).show();
						return;
					}

					String[] readTimestampArray = readTimestamp.split("\\.");
					if (!(readTimestampArray.length == 2)) {
						Toast.makeText(getApplicationContext(),
								"Invalid timestamp response format",
								Toast.LENGTH_LONG).show();
						return;
					}

					long authResponseTimestamp = Long.valueOf(
							readTimestampArray[0]).longValue();
					long timeDifferenceSeconds = Math.abs(authResponseTimestamp
							- app.authStartTimestamp);

					if (!SecurityDefinitions.DEBUG_DISABLE_TIME_VERIFICATION) {
						// Valid if less than 10 seconds
						if (timeDifferenceSeconds > 1) {
							Toast.makeText(
									getApplicationContext(),
									"Challenge Failed: Timestamp is not within acceptable limit",
									Toast.LENGTH_LONG).show();
							return;
						}
					}

					authenticationResult(true, true);

				} catch (Exception e) {
					e.printStackTrace();
				}

				break;
			case MyApplication.MESSAGE_DEVICE_NAME:
				// save the connected device's name
				app.mConnectedDeviceName = msg.getData().getString(
						MyApplication.DEVICE_NAME);
				if (SecurityDefinitions.DEBUG_MODE) {
					Toast.makeText(getApplicationContext(),
							"Connected to " + app.mConnectedDeviceName,
							Toast.LENGTH_SHORT).show();
				}

				sendAuthenticationChallenge();

				break;
			case MyApplication.MESSAGE_TOAST:
				String toastString = msg.getData().getString(
						MyApplication.TOAST);
				Toast.makeText(getApplicationContext(), toastString,
						Toast.LENGTH_SHORT).show();
				if (toastString.equals("Unable to connect device")) {
					authenticationResult(false, false);
				}
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case MyApplication.REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		case MyApplication.REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, false);
			}
			break;
		case MyApplication.REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a bluetooth session
				setupConnection();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				authenticationResult(false, false);
			}
		}
	}

	private void autoConnectToKnownDevice(String address) {
		BluetoothDevice device = app.mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		app.mBluetoothService.connect(device, true);
	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = app.mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		app.mBluetoothService.connect(device, secure);
	}
}

class StateListener extends PhoneStateListener {
	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		super.onCallStateChanged(state, incomingNumber);
		switch (state) {
		case TelephonyManager.CALL_STATE_RINGING:
			break;
		case TelephonyManager.CALL_STATE_OFFHOOK:
			System.out.println("call Activity off hook");
			// Finish lock screen activity
			// finish();
			break;
		case TelephonyManager.CALL_STATE_IDLE:
			break;
		}
	}
};
