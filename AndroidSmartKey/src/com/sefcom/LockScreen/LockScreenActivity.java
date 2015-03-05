/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sefcom.LockScreen;

import java.lang.reflect.Method;

import android.app.ActionBar;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sefcom.Bluetooth.BluetoothService;
import com.sefcom.Bluetooth.DeviceListActivity;
import com.sefcom.Launcher.HomeActivity;
import com.sefcom.Security.EncryptionUtils;
import com.sefcom.Security.SecurityDefinitions;
import com.sefcom.SmartKey.MyApplication;
import com.sefcom.SmartKey.R;

/**
 * This is the main Activity that displays the current authentication session.
 */
public class LockScreenActivity extends Activity {
	// Debugging
	private static final String TAG = "SmartKey";
	private static final boolean D = true;

	private MyApplication app;

	// Array adapter for the conversation thread
	public ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	public StringBuffer mOutStringBuffer;

	// Layout Views
	private ListView mConversationView;
	private EditText mPasswordEditText;
	private Button mConnectButton;
	private Button mResendButton;
	private Button mPasswordButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Set up the window layout
		setContentView(R.layout.main);

		app = (MyApplication) getApplication();

		// SHARED_SECRET_KEY = readSecretKey();

		// Get local Bluetooth adapter
		app.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (app.mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			return;
		}

		PhoneStateListener phoneStateListener = new PhoneStateListener();
		TelephonyManager telephonymanager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		telephonymanager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_CALL_STATE);

		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);

		// Initialize the compose field with a listener for the return key
		mPasswordEditText = (EditText) findViewById(R.id.edit_text_password);
		mPasswordEditText.setOnEditorActionListener(mWriteListener);

		// Initialize the connect button with a listener that for click events
		mConnectButton = (Button) findViewById(R.id.button_connect);
		mConnectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (app.mBluetoothService != null)
					app.mBluetoothService.stop();
				
				setupConnection();
				// Attempt to connect to known devices
//				autoConnectToKnownDevice(MyApplication.KNOWN_DEVICE_MAC);
			}
		});

		// Initialize the resend button with a listener that for click events
		mResendButton = (Button) findViewById(R.id.button_resend);
		mResendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendAuthenticationChallenge();
			}
		});

		// Initialize the send button with a listener that for click events
		mPasswordButton = (Button) findViewById(R.id.button_password);
		mPasswordButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Dismiss if password is correct
				TextView view = (TextView) findViewById(R.id.edit_text_password);
				String message = view.getText().toString();
				if (message.equals(SecurityDefinitions.TEST_PASSWORD)) {
					view.setText("");
					authenticationResult(true, false);
				} else {
					Toast.makeText(getApplicationContext(),
							"Incorrect Password", Toast.LENGTH_LONG).show();
				}
			}
		});

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");

		// If BT is not on, request that it be enabled.
		// setupConnection() will then be called during onActivityResult
		if (!app.mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent,
					MyApplication.REQUEST_ENABLE_BT);
			// Otherwise, setup the connection session
		} else {
			setupConnection();
		}
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

		if (app.mBluetoothService != null)
			app.mBluetoothService.stop();
		
		setupConnection();
		
		
//		// Performing this check in onResume() covers the case in which BT was
//		// not enabled during onStart(), so we were paused to enable it...
//		// onResume() will be called when ACTION_REQUEST_ENABLE activity
//		// returns.
//		if (app.mBluetoothService != null) {
//			// Only if the state is STATE_NONE, do we know that we haven't
//			// started already
//			if (app.mBluetoothService.getState() == BluetoothService.STATE_NONE) {
//				// Start the Bluetooth services
//				app.mBluetoothService.start();
//			} else {
//				// Attempt to connect to known devices
//				// autoConnectToKnownDevice(MyApplication.KNOWN_DEVICE_MAC);
//			}
//		}
	}

	public void authenticationResult(boolean success, boolean continuousAuth) {
		if (success) {
			// Stop the Bluetooth services
			if (app.mBluetoothService != null)
				app.mBluetoothService.stop();
			
			// Open Home activity
			Intent i = new Intent(this, HomeActivity.class);
			startActivity(i);
		}
	}

	// Disables the back button
	@Override
	public void onBackPressed() {
		return;
	}

	// Prevents status bar from being used
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		try {
			if (!hasFocus) {
				Object service = getSystemService("statusbar");
				Class<?> statusbarManager = Class
						.forName("android.app.StatusBarManager");
				Method collapse = statusbarManager.getMethod("collapse");
				collapse.setAccessible(true);
				collapse.invoke(service);
			}
		} catch (Exception e) {
			Log.e(TAG, "onWindowFocusChanged - " + e.getCause());
		}
	}

	private void setupConnection() {
		Log.d(TAG, "setupConnection()");

		// // Initialize the array adapter for the conversation thread
		// mConversationArrayAdapter = new ArrayAdapter<String>(this,
		// R.layout.message);
		// mConversationView = (ListView) findViewById(R.id.in);
		// mConversationView.setAdapter(mConversationArrayAdapter);
		//
		// // Initialize the compose field with a listener for the return key
		// mPasswordEditText = (EditText) findViewById(R.id.edit_text_password);
		// mPasswordEditText.setOnEditorActionListener(mWriteListener);
		//
		// // Initialize the connect button with a listener that for click
		// events
		// mConnectButton = (Button) findViewById(R.id.button_connect);
		// mConnectButton.setOnClickListener(new OnClickListener() {
		// public void onClick(View v) {
		// // Attempt to connect to known devices
		// autoConnectToKnownDevice(MyApplication.KNOWN_DEVICE_MAC);
		// }
		// });
		//
		// // Initialize the resend button with a listener that for click events
		// mResendButton = (Button) findViewById(R.id.button_resend);
		// mResendButton.setOnClickListener(new OnClickListener() {
		// public void onClick(View v) {
		// sendAuthenticationChallenge();
		// }
		// });
		//
		// // Initialize the send button with a listener that for click events
		// mPasswordButton = (Button) findViewById(R.id.button_password);
		// mPasswordButton.setOnClickListener(new OnClickListener() {
		// public void onClick(View v) {
		// // Dismiss if password is correct
		// TextView view = (TextView) findViewById(R.id.edit_text_password);
		// String message = view.getText().toString();
		// if (message.equals(SecurityDefinitions.TEST_PASSWORD)) {
		// view.setText("");
		// authenticationResult(true, false);
		// } else {
		// Toast.makeText(getApplicationContext(),
		// "Incorrect Password", Toast.LENGTH_LONG).show();
		// }
		// }
		// });
		//
		//
		//
		// // Initialize the buffer for outgoing messages
		// mOutStringBuffer = new StringBuffer("");

		// Initialize the BluetoothService to perform bluetooth connections
		app.mBluetoothService = new BluetoothService(this, mHandler);

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

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (app.mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
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
				mConversationArrayAdapter.add("Start Timestamp: "
						+ app.authStartTimestamp);

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

	// The action listener for the EditText widget, to listen for the return key
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				if (message.equals(SecurityDefinitions.TEST_PASSWORD)) {
					authenticationResult(true, false);
				} else {
					Toast.makeText(getApplicationContext(),
							"Incorrect Password", Toast.LENGTH_LONG).show();
				}
			}
			if (D)
				Log.i(TAG, "END onEditorAction");
			return true;
		}
	};

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		if (actionBar != null)
			actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		if (actionBar != null)
			actionBar.setSubtitle(subTitle);
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
					setStatus(getString(R.string.title_connected_to,
							app.mConnectedDeviceName));
					mConversationArrayAdapter.clear();
					break;
				case BluetoothService.STATE_CONNECTING:
					setStatus(R.string.title_connecting);
					break;
				case BluetoothService.STATE_LISTEN:
				case BluetoothService.STATE_NONE:
					setStatus(R.string.title_not_connected);
					break;
				}
				break;
			case MyApplication.MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				mConversationArrayAdapter.add("Me:  " + writeMessage);
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

					mConversationArrayAdapter.add(app.mConnectedDeviceName
							+ ":  \nSmartKeyID: " + readSmartKeyId
							+ "\nResponse Timestamp: " + readTimestamp);

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
					mConversationArrayAdapter.add("Response Timestamp: "
							+ authResponseTimestamp);
					long timeDifferenceSeconds = Math.abs(authResponseTimestamp
							- app.authStartTimestamp);

					mConversationArrayAdapter.add("Time Difference (seconds): "
							+ timeDifferenceSeconds);

					if (!SecurityDefinitions.DEBUG_DISABLE_TIME_VERIFICATION) {
						// Valid if less than 10 seconds
						if (timeDifferenceSeconds > 1) {
							Toast.makeText(
									getApplicationContext(),
									"Challenge Failed: Timestamp is not within acceptable limit",
									Toast.LENGTH_LONG).show();
							return;
						}
					} else {
						mConversationArrayAdapter.add("Debug: Timestamp not validated");
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
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(MyApplication.TOAST),
						Toast.LENGTH_SHORT).show();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.secure_connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent,
					MyApplication.REQUEST_CONNECT_DEVICE_SECURE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
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
