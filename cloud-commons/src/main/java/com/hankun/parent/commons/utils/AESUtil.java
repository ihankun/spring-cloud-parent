package com.hankun.parent.commons.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;

/**
 * @author hankun
 */
@Slf4j
public class AESUtil {

    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String SALT_KEY= "PISKun"; //自定义密钥密码
    private static final String ENCODING = "UTF-8";
    /**
     * 签名算法
     */
    public static final String SIGN_ALGORITHMS = "SHA1PRNG";

    /**
     * 加密
     * @param str 要加密的字符串
     * @return
     */
    public static String encrypt(String str){
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);// 创建密码器

            byte[] byteContent = str.getBytes("utf-8");

            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());// 初始化为加密模式的密码器

            byte[] result = cipher.doFinal(byteContent);// 加密

            return Base64.encodeBase64String(result);//通过Base64转码返回
        } catch (Exception ex) {
            log.error(ex.getMessage(),ex);
            return "";
        }

    }

    /**
     * 解密
     * @param str 要解密的字符串
     * @return
     */
    public static String decrypt(String str){
        try {
            //实例化
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);

            //使用密钥初始化，设置为解密模式
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey());

            //执行操作
            byte[] result = cipher.doFinal(Base64.decodeBase64(str));

            return new String(result, "utf-8");
        } catch (Exception ex) {
            log.error(ex.getMessage(),ex);
            return "";
        }
    }

    /**
     * 生成加密秘钥
     *
     * @return
     */
    private static SecretKeySpec getSecretKey() {
        //返回生成指定算法密钥生成器的 KeyGenerator 对象
        KeyGenerator kg = null;

        try {
            kg = KeyGenerator.getInstance(KEY_ALGORITHM);

            SecureRandom random = SecureRandom.getInstance(SIGN_ALGORITHMS);
            random.setSeed(SALT_KEY.getBytes(ENCODING));
            kg.init(128, random);
            //AES 要求密钥长度为 128
            //kg.init(128, new SecureRandom(SALT_KEY.getBytes()));

            //生成一个密钥
            SecretKey secretKey = kg.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);// 转换为AES专用密钥
        } catch (Exception ex) {
            log.error(ex.getMessage(),ex);
            return null;
        }
    }

    public static String getKey(){
        //返回生成指定算法密钥生成器的 KeyGenerator 对象
        KeyGenerator kg = null;

        try {
            kg = KeyGenerator.getInstance(KEY_ALGORITHM);

            //AES 要求密钥长度为 128
            kg.init(128, new SecureRandom(SALT_KEY.getBytes()));

            //生成一个密钥
            SecretKey secretKey = kg.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            //BASE64Encoder coder = new BASE64Encoder();
            //return coder.encode(enCodeFormat);
            java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
            return encoder.encodeToString(enCodeFormat);
        }catch (NoSuchAlgorithmException ex) {
            log.error(ex.getMessage(),ex);
            return "";
        }
    }


    public static void main(String[] args) {
        System.out.println(decrypt("1xZiCwi9Q4LpO//PKo4kSw=="));
        System.out.println(getKey());
        System.out.println(encrypt("m@ssuns0ft009"));
    }
}
