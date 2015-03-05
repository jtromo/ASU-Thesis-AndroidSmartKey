package com.sefcom.Security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;

public class EncryptionUtils {

	public static byte[] encryptData(Context context, byte[] targetData) throws Exception {
		if (SecurityDefinitions.DEBUG_DISABLE_ENCRYPTION)
			return targetData;
		
		// byte[] stringBytes = key.getBytes("UTF-8");

		byte[] secretKey = SecurityDefinitions.getSecretKey(context);

		// encrypt
		byte[] encryptedData = encrypt(secretKey, targetData);

		return encryptedData;
	}

	public static byte[] decryptData(Context context, byte[] targetData) throws Exception {
		if (SecurityDefinitions.DEBUG_DISABLE_ENCRYPTION)
			return targetData;
		
		// byte[] encryptedKey = s.getBytes("UTF-8");

		byte[] secretKey = SecurityDefinitions.getSecretKey(context);

		// decrypt
		byte[] decryptedKey = decrypt(secretKey, targetData);

		return decryptedKey;
	}

	private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
		if (SecurityDefinitions.DEBUG_DISABLE_ENCRYPTION)
			return raw;
		
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES/CBC/PKCS5Padding");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//				getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		byte[] encrypted = cipher.doFinal(clear);
		return encrypted;
	}

	private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
		if (SecurityDefinitions.DEBUG_DISABLE_ENCRYPTION)
			return raw;
		
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		byte[] decrypted = cipher.doFinal(encrypted);
		return decrypted;
	}
}
