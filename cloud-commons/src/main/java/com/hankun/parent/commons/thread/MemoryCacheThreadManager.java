package com.hankun.parent.commons.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author hankun
 */
@Slf4j
@Component
public class MemoryCacheThreadManager implements ApplicationContextAware {

    ApplicationContext context;

    private Map<String, Long> lastTimeMap = new HashMap<>(16);

    @PostConstruct
    public void init() {
        Map<String, MemoryCacheRunnable> map = context.getBeansOfType(MemoryCacheRunnable.class);
        if (map == null || map.size() == 0) {
            return;
        }

        List<MemoryCacheRunnable> share = new ArrayList<>();
        List<MemoryCacheRunnable> standalone = new ArrayList<>();

        for (Map.Entry<String, MemoryCacheRunnable> entry : map.entrySet()) {
            MemoryCacheRunnable runnable = entry.getValue();
            boolean open = runnable.open();
            if (!open) {
                continue;
            }
            boolean stand = runnable.standalone();
            if (stand) {
                standalone.add(runnable);
                continue;
            }
            share.add(runnable);
        }

        if (share != null && share.size() > 0) {
            runShare(share);
        }

        if (standalone != null && standalone.size() > 0) {
            runStandalone(standalone);
        }

    }

    private void runShare(List<MemoryCacheRunnable> list) {

        NamedThreadFactory factory = new NamedThreadFactory("MemoryCacheRunnable-Share");
        Thread thread = factory.newThread(() -> {

            log.info("MemoryCacheThreadManager.runShare.start,list={}", list);

            while (true) {

                for (MemoryCacheRunnable runnable : list) {

                    String runableName = runnable.getClass().getSimpleName();

                    Long lastTime = lastTimeMap.get(runableName);
                    int interval = runnable.interval();
                    if (lastTime == null || (System.currentTimeMillis() - lastTime) >= (interval * 1000)) {
                        try {
                            //runnable.run();
                            lastTimeMap.put(runableName, System.currentTimeMillis());
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

        });
        thread.start();
    }

    private void runStandalone(List<MemoryCacheRunnable> list) {

        for (MemoryCacheRunnable runnable : list) {

            String runableName = runnable.getClass().getSimpleName();

            NamedThreadFactory factory = new NamedThreadFactory("MemoryCacheRunnable-Standalone-" + runableName);

            factory.newThread(() -> {

                log.info("MemoryCacheThreadManager.runStandalone.start,name={}", runableName);

                while (true) {

                    try {
                        //runnable.run();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }

                    //休眠
                    try {
                        TimeUnit.SECONDS.sleep(runnable.interval());
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }).start();

        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}
