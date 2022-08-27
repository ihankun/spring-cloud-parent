package com.hankun.parent.commons.id;

import lombok.extern.slf4j.Slf4j;

/**
 * @author hankun
 */
@Slf4j
public class BusinessCodeGenerator {

    private static final String SPLIT = "_";

    public static class BusinessCodeGeneratorHolder {
        public static final BusinessCodeGenerator HOLDER = new BusinessCodeGenerator();
    }

    public static BusinessCodeGenerator ins() {
        return BusinessCodeGeneratorHolder.HOLDER;
    }


    public String generator(String prefix) {
        Long generator = IdGenerator.ins().generator();
        StringBuilder builder = new StringBuilder(prefix);
        builder.append(SPLIT).append(generator);
        return builder.toString();
    }

    public static void main(String[] args) {

        int length = 100;
        for (int i = 0; i < length; i++) {
            String id = BusinessCodeGenerator.ins().generator("test");
            log.info("BusinessCodeGenerator.main.;[id]={}", id);
        }


    }
}
