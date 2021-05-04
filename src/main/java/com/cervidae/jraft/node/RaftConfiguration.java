package com.cervidae.jraft.node;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@Log4j2
@Data
@ConfigurationProperties(prefix="cervidae.jraft")
public class RaftConfiguration implements ApplicationContextAware {

    private Boolean isLocalCluster;

    private int clusterSize;

    private int clusteredId;

    final TimeUnit GLOBAL_TIMEUNIT = TimeUnit.MILLISECONDS;

    final long MAX_ELECTION_DELAY = 660L;

    final long MIN_ELECTION_DELAY = 550L;

    final long HEARTBEAT_FREQUENCY = 110L;

    private String clusteredUrls;

    private ApplicationContext applicationContext;

    private RaftContext raftContext;

    @Override
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    @Primary
    public RaftContext createRaftContext() {
        if (raftContext == null) {
            if (isLocalCluster) {
                 raftContext = applicationContext.getBean(LocalRaftContext.class);
            } else {
                 raftContext = applicationContext.getBean(ClusteredRaftContext.class);
            }
        }
        if (!raftContext.isRunning()) {
            raftContext.start();
        }
        return raftContext;
    }

}
