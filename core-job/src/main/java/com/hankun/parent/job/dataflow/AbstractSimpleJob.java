package com.hankun.parent.job.dataflow;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.hankun.parent.commons.context.DomainContext;
import com.hankun.parent.commons.context.GrayContext;
import com.hankun.parent.commons.context.LoginUserContext;
import com.hankun.parent.commons.context.LoginUserInfo;
import com.hankun.parent.job.dataflow.domain.JobDomainItem;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 简单job(不支持分片)
 * 此方法将被定时执行。用于执行普通的定时任务，与Quartz原生接口相似，只是增加了弹性扩缩容和分片等功能，但是我们当前类的封装不支持分片。
 * 注意：在xml中配置 sharding-total-count=1 且部署本job在一台机器上就行 如果部署多台机器只有拿到shardingContext.getShardingItem() == 0的机器运行。
 * 可以用来做HA。
 * @author hankun
 */
@Slf4j
public abstract class AbstractSimpleJob extends AbstractJob implements SimpleJob {

    @Override
    public void execute(ShardingContext shardingContext) {

        if (!canProcess(shardingContext)) {
            return;
        }

        //mock灰度标识
        GrayContext.mock(String.valueOf(isGray()));

    }


    /**
     * 执行业务处理
     */
    private void tryExecute() {
        try {
            processJob();
        } catch (Exception e) {
            log.error("AbstractSimpleJob.execute,e={}", e);
        }
    }

    /**
     * job抽象方法
     */
    protected abstract void processJob();

    /**
     * 不进行分片 只有一片序号是0
     *
     * @param shardingContext 分片对象
     */
    private boolean canProcess(ShardingContext shardingContext) {
        return shardingContext.getShardingItem() == 0;
    }
}
