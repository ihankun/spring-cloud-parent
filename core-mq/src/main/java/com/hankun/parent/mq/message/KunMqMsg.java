package com.hankun.parent.mq.message;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hankun.parent.commons.context.LoginUserInfo;
import lombok.Data;

import java.io.Serializable;

/**
 * @author hankun
 */
@Data
public class KunMqMsg implements Serializable {

    private String topic;

    private String tag;

    private String telnet;

    private String messageId;

    private LoginUserInfo loginUserInfo;

    private Object data;

    private String gray;


    public String serialize() {
        String jsonString = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            jsonString = mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return jsonString;
    }

    public static KunMqMsg deserialize(String data) {
        return JSON.parseObject(data, KunMqMsg.class);
    }
}
