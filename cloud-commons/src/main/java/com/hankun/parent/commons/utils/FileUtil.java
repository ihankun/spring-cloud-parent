package com.hankun.parent.commons.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class FileUtil {

    /**
     * 功能描述: 判断文件路径格式是否正确
     */
    public static Boolean matchFilePath(String filePath){
        return filePath.matches("^[A-z]:\\\\(.+?\\\\)*$");
    }

    /**
     * 功能描述: 判断文件是否存在
     */
    public static void judeFileExists(File file) {
        if (file.exists()) {
            System.out.println("file exists");
        } else {
            System.out.println("file not exists, create it ...");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 功能描述: 判断文件夹是否存在
     */
    public static void judeDirExists(String fielPath) {
        File file = new File(fielPath);
        if (!file.exists()) {
            //file.isDirectory()
            file.mkdirs();
        }
    }

    /**
     * 功能描述: 输入流转base64
     */
    public static String inputSteamToBase64Str(InputStream in){
        byte[] data = null;
        try {
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(data);
    }

    /**
     * 功能描述: 将文件转为base64
     */
    public static String encryptToBase64(String filePath) {
        if (filePath == null) {
            return null;
        }
        try {
            byte[] b = Files.readAllBytes(Paths.get(filePath));
            Base64.Encoder encoder = Base64.getEncoder();
            return encoder.encodeToString(b);
            //return Base64.getEncoder().encodeToString(b);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 功能描述: base64生成文件
     */
    public static String decryptByBase64(String base64, String filePath) {
        if (base64 == null && filePath == null) {
            return "生成文件失败，请给出相应的数据。";
        }
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] buffer = decoder.decode(base64);
            //byte[] buffer = new BASE64Decoder().decodeBuffer(base64);
            FileOutputStream out = new FileOutputStream(filePath);
            out.write(buffer);
            out.close();
            //Files.write(Paths.get(filePath), Base64.getDecoder().decode(base64), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "指定路径下生成文件成功！";
    }
}
