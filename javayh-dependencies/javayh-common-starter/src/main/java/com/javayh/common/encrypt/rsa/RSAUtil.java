package com.javayh.common.encrypt.rsa;

import com.javayh.common.constant.EncryptConstantUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * <p>
 * 来自 zhao-baolin
 * </p>
 *
 * @version 1.0.0
 * @author Dylan-haiji
 * @since 2020/3/5
 */
@Slf4j
public class RSAUtil {

	/**
	 * 指定key的大小(64的整数倍,最小512位)
	 */
	private static int KEYSIZE = 1024;

	/** RSA最大加密明文大小 */
	private static final int MAX_ENCRYPT_BLOCK = 117;

	/** RSA最大解密密文大小 */
	private static final int MAX_DECRYPT_BLOCK = 128;

	/** 公钥模量 */
	public static String publicModulus = null;

	/** 公钥指数 */
	public static String publicExponent = null;

	/** 私钥模量 */
	public static String privateModulus = null;

	/** 私钥指数 */
	public static String privateExponent = null;

	private static KeyFactory keyFactory = null;

	static {
		try {
			keyFactory = KeyFactory.getInstance(EncryptConstantUtils.KEY_ALGORITHM);
		}
		catch (NoSuchAlgorithmException ex) {
			System.out.println(ex.getMessage());
		}
	}

	public RSAUtil() {
		try {
			generateKeyPairString(KEYSIZE);
		}
		catch (Exception e) {
			log.error("generateKeyPairString {}", e);
		}
	}

	public RSAUtil(int keySize) {
		try {
			generateKeyPairString(keySize);
		}
		catch (Exception e) {
			log.error("generateKeyPairString {}", e);
		}
	}

	/**
	 * <p>
	 * 生成密钥对字符串
	 * </p>
	 * @version 1.0.0
	 * @author Dylan-haiji
	 * @since 2020/3/5
	 * @param keySize
	 * @return void
	 */
	private void generateKeyPairString(int keySize) throws Exception {
		/** RSA算法要求有一个可信任的随机数源 */
		SecureRandom sr = new SecureRandom();
		/** 为RSA算法创建一个KeyPairGenerator对象 */
		KeyPairGenerator kpg = KeyPairGenerator
				.getInstance(EncryptConstantUtils.KEY_ALGORITHM);
		/** 利用上面的随机数据源初始化这个KeyPairGenerator对象 */
		kpg.initialize(keySize, sr);
		/** 生成密匙对 */
		KeyPair kp = kpg.generateKeyPair();
		/** 得到公钥 */
		Key publicKey = kp.getPublic();
		/** 得到私钥 */
		Key privateKey = kp.getPrivate();
		/** 用字符串将生成的密钥写入文件 获取算法 */
		String algorithm = publicKey.getAlgorithm();
		KeyFactory keyFact = KeyFactory.getInstance(algorithm);
		BigInteger prime = null;
		BigInteger exponent = null;
		RSAPublicKeySpec keySpec = keyFact.getKeySpec(publicKey, RSAPublicKeySpec.class);
		prime = keySpec.getModulus();
		exponent = keySpec.getPublicExponent();
		RSAUtil.publicModulus = HexUtil.bytes2Hex(prime.toByteArray());
		RSAUtil.publicExponent = HexUtil.bytes2Hex(exponent.toByteArray());

		RSAPrivateCrtKeySpec privateKeySpec = keyFact.getKeySpec(privateKey,
				RSAPrivateCrtKeySpec.class);
		BigInteger privateModulus = privateKeySpec.getModulus();
		BigInteger privateExponent = privateKeySpec.getPrivateExponent();
		RSAUtil.privateModulus = HexUtil.bytes2Hex(privateModulus.toByteArray());
		RSAUtil.privateExponent = HexUtil.bytes2Hex(privateExponent.toByteArray());
	}

	/**
	 * 根据给定的16进制系数和专用指数字符串构造一个RSA专用的公钥对象。
	 * @param hexModulus 系数。
	 * @param hexPublicExponent 专用指数。
	 * @return RSA专用公钥对象。
	 */
	public static RSAPublicKey getRSAPublicKey(String hexModulus,
			String hexPublicExponent) {
		if (isBlank(hexModulus) || isBlank(hexPublicExponent)) {
			log.error(
					"hexModulus and hexPublicExponent cannot be empty. return null(RSAPublicKey).");
			return null;
		}
		byte[] modulus = null;
		byte[] publicExponent = null;
		try {
			modulus = HexUtil.hex2Bytes(hexModulus);
			publicExponent = HexUtil.hex2Bytes(hexPublicExponent);
		}
		catch (Exception ex) {
			log.error(
					"hexModulus or hexPublicExponent value is invalid. return null(RSAPublicKey). {}",
					ex);

		}
		if (modulus != null && publicExponent != null) {
			return generateRSAPublicKey(modulus, publicExponent);
		}
		return null;
	}

	/**
	 * 根据给定的系数和专用指数构造一个RSA专用的公钥对象。
	 * @param modulus 系数。
	 * @param publicExponent 专用指数。
	 * @return RSA专用公钥对象。
	 */
	public static RSAPublicKey generateRSAPublicKey(byte[] modulus,
			byte[] publicExponent) {
		RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(new BigInteger(modulus),
				new BigInteger(publicExponent));
		try {
			return (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
		}
		catch (InvalidKeySpecException ex) {
			log.error("RSAPublicKeySpec is unavailable. {}", ex);

		}
		catch (NullPointerException ex) {
			log.error(
					"RSAUtils#KEY_FACTORY is null, can not generate KeyFactory instance. {}",
					ex);
		}
		return null;
	}

	/**
	 * 根据给定的16进制系数和专用指数字符串构造一个RSA专用的私钥对象。
	 * @param hexModulus 系数。
	 * @param hexPrivateExponent 专用指数。
	 * @return RSA专用私钥对象。
	 */
	public static RSAPrivateKey getRSAPrivateKey(String hexModulus,
			String hexPrivateExponent) {
		if (isBlank(hexModulus) || isBlank(hexPrivateExponent)) {
			System.out.println(
					"hexModulus and hexPrivateExponent cannot be empty. RSAPrivateKey value is null to return.");
			return null;
		}
		byte[] modulus = null;
		byte[] privateExponent = null;
		try {
			modulus = HexUtil.hex2Bytes(hexModulus);
			privateExponent = HexUtil.hex2Bytes(hexPrivateExponent);
		}
		catch (Exception ex) {
			System.out.println();
			log.error(
					"hexModulus or hexPrivateExponent value is invalid. return null(RSAPrivateKey). {}",
					ex);
		}
		if (modulus != null && privateExponent != null) {
			return generateRSAPrivateKey(modulus, privateExponent);
		}
		return null;
	}

	/**
	 * 根据给定的系数和专用指数构造一个RSA专用的私钥对象。
	 * @param modulus 系数。
	 * @param privateExponent 专用指数。
	 * @return RSA专用私钥对象。
	 */
	public static RSAPrivateKey generateRSAPrivateKey(byte[] modulus,
			byte[] privateExponent) {
		RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(new BigInteger(modulus),
				new BigInteger(privateExponent));
		try {
			return (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
		}
		catch (InvalidKeySpecException ex) {
			log.error("RSAPrivateKeySpec is unavailable. {}", ex);
		}
		catch (NullPointerException ex) {
			log.error(
					"RSAUtils#KEY_FACTORY is null, can not generate KeyFactory instance.",
					ex);
		}
		return null;
	}

	/**
	 * 使用给定的公钥加密给定的字符串。
	 * @param key 给定的公钥。
	 * @param plaintext 字符串。
	 * @return 给定字符串的密文。
	 */
	public static String encryptString(Key key, String plaintext) {
		if (key == null || plaintext == null) {
			return null;
		}
		byte[] data = plaintext.getBytes();
		try {
			byte[] en_data = encrypt(key, data);
			return new String(Base64.encodeBase64String(en_data));
		}
		catch (Exception ex) {

		}
		return null;
	}

	/**
	 * 使用指定的公钥加密数据。
	 * @param key 给定的公钥。
	 * @param data 要加密的数据。
	 * @return 加密后的数据。
	 */

	public static byte[] encrypt(Key key, byte[] data) throws Exception {
		Cipher ci = Cipher.getInstance(EncryptConstantUtils.ALGORITHM);
		ci.init(Cipher.ENCRYPT_MODE, key);
		int inputLen = data.length;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int offSet = 0;
		byte[] cache;
		int i = 0;
		// 对数据分段加密
		while (inputLen - offSet > 0) {
			if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
				cache = ci.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
			}
			else {
				cache = ci.doFinal(data, offSet, inputLen - offSet);
			}
			out.write(cache, 0, cache.length);
			i++;
			offSet = i * MAX_ENCRYPT_BLOCK;
		}
		byte[] encryptedData = out.toByteArray();
		out.close();
		return encryptedData;
	}

	/**
	 * 使用给定的公钥解密给定的字符串。
	 * @param key 给定的公钥
	 * @param encrypttext 密文
	 * @return 原文字符串。
	 */
	public static String decryptString(Key key, String encrypttext) {
		if (key == null || isBlank(encrypttext)) {
			return null;
		}
		try {
			byte[] en_data = Base64.decodeBase64(encrypttext);
			byte[] data = decrypt(key, en_data);
			return new String(data);
		}
		catch (Exception ex) {
			System.out.println(String.format("\"%s\" Decryption failed. Cause: %s",
					encrypttext, ex.getCause().getMessage()));
		}
		return null;
	}

	/**
	 * 使用指定的公钥解密数据。
	 * @param key 指定的公钥
	 * @param data 要解密的数据
	 * @return 原数据
	 * @throws Exception
	 */
	public static byte[] decrypt(Key key, byte[] data) throws Exception {
		Cipher ci = Cipher.getInstance(EncryptConstantUtils.ALGORITHM);
		ci.init(Cipher.DECRYPT_MODE, key);
		int inputLen = data.length;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int offSet = 0;
		byte[] cache;
		int i = 0;
		// 对数据分段解密
		while (inputLen - offSet > 0) {
			if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
				cache = ci.doFinal(data, offSet, MAX_DECRYPT_BLOCK);
			}
			else {
				cache = ci.doFinal(data, offSet, inputLen - offSet);
			}
			out.write(cache, 0, cache.length);
			i++;
			offSet = i * MAX_DECRYPT_BLOCK;
		}
		byte[] decryptedData = out.toByteArray();
		out.close();
		return decryptedData;
	}

	/**
	 * 判断非空字符串
	 * @param cs 待判断的CharSequence序列
	 * @return 是否非空
	 */
	private static boolean isBlank(final CharSequence cs) {
		int strLen;
		if (cs == null || (strLen = cs.length()) == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if (Character.isWhitespace(cs.charAt(i)) == false) {
				return false;
			}
		}
		return true;
	}

	public static PublicKey getPublicKey(String key) throws Exception {
		byte[] keyBytes;
		keyBytes = Base64.decodeBase64(key);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey publicKey = keyFactory.generatePublic(keySpec);
		return publicKey;
	}

	public static PrivateKey getPrivateKey(String key) throws Exception {
		byte[] keyBytes;
		keyBytes = Base64.decodeBase64(key);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
		return privateKey;
	}

}