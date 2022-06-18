package com.hankun.parent.db.exceptions;

/**
 * @author hankun
 */
public class CommonDbException extends RuntimeException{

    public CommonDbException(String message){
        super(message);
    }

    public CommonDbException(Throwable throwable){
        super(throwable);
    }

    public CommonDbException(String message, Throwable throwable){
        super(message, throwable);
    }
}
