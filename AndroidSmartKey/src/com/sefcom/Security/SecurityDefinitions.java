package com.sefcom.Security;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.util.Log;

import com.sefcom.SmartKey.R;

public class SecurityDefinitions {

	public static boolean DEBUG_DISABLE_ENCRYPTION = true;
	public static boolean DEBUG_DISABLE_TIME_VERIFICATION = true;
	public static boolean DEBUG_MODE = false;
	private static final String PREFS_NAME = "SECUR_PREFS";
	private static byte[] SECRET_KEY = null;
	public static final String DEVICE_ID = "644c98e6-7e86-4bd1-a4f2-88fd9ed80407";
	public static final String SMARTKEY_ID = "240529c4-4dc1-4f06-b7c2-530b4f23135f";
	public static final String TEST_PASSWORD = "pass";

	// Unique UUID for this application
	// private static final UUID DEVICE_ID = UUID
	// .fromString("644c98e6-7e86-4bd1-a4f2-88fd9ed80407");
	// private String SHARED_SECRET_KEY;

	/**
	 * Retrieves Secret Key for AES Encryption If no key exists, creates one.
	 * 
	 * @param context
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public static byte[] getSecretKey(Context context)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {

		if (SECRET_KEY == null) {
			SECRET_KEY = readSecretKey(context);
		}
		return SECRET_KEY;
	}

	private static byte[] readSecretKey(Context context)
			throws UnsupportedEncodingException {
		String key = "";

		try {
			InputStream inputStream = context.getResources().openRawResource(
					R.raw.shared_secret_key);

			if (inputStream != null) {
				InputStreamReader inputStreamReader = new InputStreamReader(
						inputStream);
				BufferedReader bufferedReader = new BufferedReader(
						inputStreamReader);
				String receiveString = "";
				StringBuilder stringBuilder = new StringBuilder();

				while ((receiveString = bufferedReader.readLine()) != null) {
					stringBuilder.append(receiveString);
				}

				inputStream.close();
				key = stringBuilder.toString();
			}
		} catch (FileNotFoundException e) {
			Log.e("ERROR: login activity", "File not found: " + e.toString());
		} catch (IOException e) {
			Log.e("ERROR: login activity", "Can not read file: " + e.toString());
		}

		return key.getBytes("UTF-8");
	}

	// private static byte[] readSecretKey(Context context) {
	// String ret = "";
	//
	// try {
	// InputStream inputStream = context.getResources().openRawResource(
	// R.raw.shared_secret_key);
	//
	// if (inputStream != null) {
	// InputStreamReader inputStreamReader = new InputStreamReader(
	// inputStream);
	// BufferedReader bufferedReader = new BufferedReader(
	// inputStreamReader);
	// String receiveString = "";
	// StringBuilder stringBuilder = new StringBuilder();
	//
	// while ((receiveString = bufferedReader.readLine()) != null) {
	// stringBuilder.append(receiveString);
	// }
	//
	// inputStream.close();
	// ret = stringBuilder.toString();
	// }
	// } catch (FileNotFoundException e) {
	// Log.e("ERROR: login activity", "File not found: " + e.toString());
	// } catch (IOException e) {
	// Log.e("ERROR: login activity", "Can not read file: " + e.toString());
	// }
	//
	// return ret;
	// }

	//
	//
	// /**
	// *
	// * @param context
	// * @return
	// * @throws NoSuchAlgorithmException
	// * @throws UnsupportedEncodingException
	// */
	// public static byte[] getDeviceID(Context context)
	// throws NoSuchAlgorithmException, UnsupportedEncodingException {
	// if (DEBUG_MODE)
	// return "device_id".getBytes();
	//
	// SharedPreferences preferences = context.getApplicationContext()
	// .getSharedPreferences(PREFS_NAME, 0);
	// String value = preferences.getString(DEVICE_ID, null);
	// if (value == null) {
	// // the key does not exist
	// SharedPreferences.Editor editor = preferences.edit();
	// byte[] secretKey = keyGeneration();
	// value = new String(secretKey);
	// editor.putString(DEVICE_ID, value);
	// // Apply the edits!
	// editor.apply();
	// }
	//
	// return value.getBytes("UTF-8");
	// }
	//
	// /**
	// *
	// * @param context
	// * @return
	// * @throws NoSuchAlgorithmException
	// * @throws UnsupportedEncodingException
	// */
	// public static byte[] getSmartKeyID(Context context)
	// throws NoSuchAlgorithmException, UnsupportedEncodingException {
	// if (DEBUG_MODE)
	// return "smartKey_id".getBytes();
	//
	// SharedPreferences preferences = context.getApplicationContext()
	// .getSharedPreferences(PREFS_NAME, 0);
	// String value = preferences.getString(SMARTKEY_ID, null);
	// if (value == null) {
	// // the key does not exist
	//
	// // ERROR
	// // need to actually get it from smart key before using app
	// return null;
	// }
	//
	// return value.getBytes("UTF-8");
	// }
	//
	// /**
	// * Retrieves Secret Key for AES Encryption If no key exists, creates one.
	// *
	// * @param context
	// * @return
	// * @throws NoSuchAlgorithmException
	// * @throws UnsupportedEncodingException
	// */
	// public static byte[] getSecretKey(Context context)
	// throws NoSuchAlgorithmException, UnsupportedEncodingException {
	// if (DEBUG_MODE)
	// return "secret_key".getBytes();
	//
	// SharedPreferences preferences = context.getApplicationContext()
	// .getSharedPreferences(PREFS_NAME, 0);
	// String value = preferences.getString(SECRET_KEY, null);
	// if (value == null) {
	// // the key does not exist
	// SharedPreferences.Editor editor = preferences.edit();
	// byte[] secretKey = keyGeneration();
	// value = new String(secretKey);
	// editor.putString(SECRET_KEY, value);
	// // Apply the edits!
	// editor.apply();
	// }
	//
	// return value.getBytes("UTF-8");
	// }
	//
	// /**
	// *
	// * @return
	// * @throws NoSuchAlgorithmException
	// */
	// private static byte[] keyGeneration() throws NoSuchAlgorithmException {
	// byte[] keyStart = "this is a key".getBytes();
	// KeyGenerator kgen = KeyGenerator.getInstance("AES");
	// SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
	// sr.setSeed(keyStart);
	// kgen.init(128, sr); // 192 and 256 bits may not be available
	// SecretKey skey = kgen.generateKey();
	// byte[] key = skey.getEncoded();
	//
	// return key;
	// }
}
