package com.hankun.parent.job.dataflow;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.dataflow.DataflowJob;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hankun.parent.commons.context.DomainContext;
import com.hankun.parent.commons.context.GrayContext;
import com.hankun.parent.commons.context.LoginUserContext;
import com.hankun.parent.commons.context.LoginUserInfo;
import com.hankun.parent.job.dataflow.domain.JobDomainItem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 流式job (不支持分片)
 * 如果是流式处理数据(streaming-process="true")，fetchData方法的返回值只有为null或长度为空时，作业才会停止执行，否则作业会一直运行下去；
 * 非流式处理数据(缺省配置 streaming-process="false")则只会在每次作业执行过程中执行一次fetchData方法和processData方法，即完成本次作业。
 * 这里采用线程池并发执行
 * 注意：在xml中配置sharding-total-count=1 且部署本job在一台机器上就行，如果部署多台机器只有拿到shardingContext.getShardingItem() == 0的机器运行。可以做HA
 * 修改线程数的方式：重写getThreadCount()方法
 * @author hankun
 */
@Data
@Slf4j
public abstract class AbstractOneItemJob<T> extends AbstractJob implements DataflowJob<JobDataWrapper<T>> {

    private ThreadFactory namedThreadFactory =
            new ThreadFactoryBuilder().setNameFormat("thread-call-runner-%d").build();
    private ExecutorService exec = new ThreadPoolExecutor(10, 20, 200L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), namedThreadFactory);

    private int threadCount = 1;

    @Override
    public List<JobDataWrapper<T>> fetchData(ShardingContext shardingContext) {

        if (!canFetchData(shardingContext)) {
            return null;
        }

        List<JobDataWrapper<T>> list = new ArrayList<>(16);
        return list;
    }

    /**
     * 自定义前期数据方法
     *
     * @return List返回处理数据
     */
    public abstract List<T> fetchData();

    /**
     * 自定义后期数据方法
     *
     * @param shardingContext 分片对象
     * @param data            数据
     */
    public abstract void processData(ShardingContext shardingContext, T data);


    private boolean canFetchData(ShardingContext shardingContext) {

        int item = shardingContext.getShardingItem();

        return item == 0;
    }

    @Override
    public void processData(ShardingContext shardingContext, List<JobDataWrapper<T>> list) {

        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        int size = Math.max(getThreadCount(), 1);

        int calSize = list.size() / size;

        //当list 条数不够线程数时 则coungdown计数用list数量
        if (calSize == 0) {
            calSize = 1;
            size = list.size();
        }

        List<List<JobDataWrapper<T>>> lists = Lists.partition(list, calSize);

        CountDownLatch latch = new CountDownLatch(size);

        for (List<JobDataWrapper<T>> oneList : lists) {
            processPartition(shardingContext, oneList, latch);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private void processPartition(final ShardingContext shardingContext, final List<JobDataWrapper<T>> list, final CountDownLatch latch) {
        exec.execute(() -> {

            //mock灰度标识
            GrayContext.mock(String.valueOf(isGray()));
            for (JobDataWrapper<T> t : list) {
                T data = t.getData();
                JobDomainItem domain = t.getDomain();
                if (domain != null) {
                    //模拟域名信息
                    DomainContext.mock(domain.getHost());
                    //模拟用户信息
                    LoginUserInfo userInfo = new LoginUserInfo();
                    userInfo.setOrgId(domain.getOrgId());
                    LoginUserContext.mock(userInfo);
                }
                log.info("AbstractOneItemJob.processPartition,domain={}", domain);
                tryExecute(shardingContext, data);
            }
            latch.countDown();
        });
    }

    /**
     * 捕获数据
     *
     * @return
     */
    private List<T> tryFetch() {
        try {
            List<T> list = fetchData();
            return list;
        } catch (Exception e) {
            log.error("AbstractOneItemJob.fetch,e={}", e);
            return null;
        }
    }

    /**
     * 处理数据
     *
     * @param shardingContext
     * @param data
     */
    private void tryExecute(ShardingContext shardingContext, T data) {
        try {
            processData(shardingContext, data);
        } catch (Exception e) {
            log.error("AbstractOneItemJob.execute.exception,e={},data={}", e, data);
        }
    }
}
