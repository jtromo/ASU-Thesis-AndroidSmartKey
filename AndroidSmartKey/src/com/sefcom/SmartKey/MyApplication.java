package com.sefcom.SmartKey;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;

import com.sefcom.Bluetooth.BluetoothService;

public class MyApplication extends Application {

	public static final String KNOWN_DEVICE_MAC = "00:02:72:C6:B1:FC";

	// Message types sent from the BluetoothService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	public static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	public static final int REQUEST_ENABLE_BT = 3;

	// Name of the connected device
	public String mConnectedDeviceName = null;
	
	// Local Bluetooth adapter
	public BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the Bluetooth services
	public BluetoothService mBluetoothService = null;

	// Timestamp at the start of the protocol.
	// Verify difference between send and return are close
	public long authStartTimestamp = 0;

	@Override
	public void onCreate() {
		super.onCreate();
	}
}
