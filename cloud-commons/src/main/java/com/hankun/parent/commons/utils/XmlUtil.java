package com.hankun.parent.commons.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.*;

/**
 * @author hankun
 */
public class XmlUtil {

    private static String[] TEXT_FIELD= {"REPORT_USER_PIC","CHECK_USER_PIC"};

    public static Set<String> stringSet = new HashSet<>();

    public static String getXmlByMap(List<Map<String,Object>> mapList) throws IOException, SQLException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<xml>");
        for(Map<String,Object> map : mapList){
            stringBuilder.append("<row>");
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String xmlValue = "";
                if(judgeTextField(entry.getKey())){
                    xmlValue = ClobToString((Clob)entry.getValue());
                } else {
                    xmlValue = entry.getValue().toString();
                }
                stringBuilder.append("<"+entry.getKey()+">"+replaceSpecialCharacter(xmlValue)+"</"+entry.getKey()+">");
                //System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            }
            stringBuilder.append("</row>");
        }
        stringBuilder.append("</xml>");
        return stringBuilder.toString();
    }

    private static String replaceSpecialCharacter(String str){
        return str.replaceAll("&","&amp;")
                .replaceAll(">","&gt; ")
                .replaceAll("<","&lt;")
                .replaceAll("\"","&quot; ")
                .replaceAll("'","&apos; ");
    }

    public static String replaceSpecialCharacterGrf(String str){
        return str.replaceAll("&","&amp;").replaceAll("<","&lt;");
    }

    private static String ClobToString(Clob clob) throws SQLException, IOException {
        String ret = "";
        Reader read= clob.getCharacterStream();
        BufferedReader br = new BufferedReader(read);
        String s = br.readLine();
        StringBuffer sb = new StringBuffer();
        while (s != null) {
            sb.append(s);
            sb.append("\r\n");
            s = br.readLine();
        }
        ret = sb.toString();
        if(br != null){
            br.close();
        }
        if(read != null){
            read.close();
        }
        return ret;
    }

    private static Boolean judgeTextField(String field){
        Set<String> set = new HashSet<>(Arrays.asList(TEXT_FIELD));
        return set.contains(field);
    }
}
