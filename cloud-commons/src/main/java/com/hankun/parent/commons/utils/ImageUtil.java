package com.hankun.parent.commons.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

/**
 * @author hankun
 */
public class ImageUtil {

    public static String imageToBase64(String imagePath){
        InputStream in = null;
        byte[] data = null;
        //读取图片字节数组
        try
        {
            in = new FileInputStream(imagePath);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        }
        catch (IOException e)
        {
            //throw new CustomException(e.getMessage());
        }
        Base64.Encoder encoder = Base64.getEncoder();
        //返回Base64编码过的字节数组字符串
        return encoder.encodeToString(data);
    }

    public static void base64ToImage(String base64Str,String imagePath){
        //对字节数组字符串进行Base64解码并生成图片
        //BASE64Decoder decoder = new BASE64Decoder();
        Base64.Decoder decoder = Base64.getDecoder();
        try
        {
            //Base64解码
            //byte[] b = decoder.decodeBuffer(base64Str);
            byte[] b = decoder.decode(base64Str);
            for(int i=0;i<b.length;++i)
            {
                if(b[i]<0)
                {//调整异常数据
                    b[i]+=256;
                }
            }
            //生成jpeg图片
            OutputStream out = new FileOutputStream(imagePath);
            out.write(b);
            out.flush();
            out.close();
        }
        catch (Exception e)
        {
            //throw new CustomException(e.getMessage());
        }
    }

    public static String getImageBinary(String imagePath) {
        File file = new File(imagePath);
        BufferedImage bi;
        try {
            bi = ImageIO.read(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "jpg", baos);  //经测试转换的图片是格式这里就什么格式，否则会失真
            byte[] bytes = baos.toByteArray();
            Base64.Encoder encoder = Base64.getEncoder();
            return encoder.encodeToString(bytes).trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
