package com.cervidae.jraft.node;

import com.cervidae.jraft.async.AsyncService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Log4j2
@ConfigurationProperties(prefix="cervidae.jraft")
public class RaftConfiguration {

    private Boolean isLocalCluster;

    private int clusterSize;

    private int clusteredId;

    @Value("#{'${cervidae.jraft.clusteredIPs}'.split(',')}")
    private String[] clusteredIPs;

    private final AsyncService asyncService;

    private final StateMachine stateMachine;

    @Autowired
    public RaftConfiguration(AsyncService asyncService, StateMachine stateMachine) {
        this.asyncService = asyncService;
        this.stateMachine = stateMachine;
    }

    @Bean
    @Primary
    public RaftContext createRaftContext() {
        if (isLocalCluster) {
            return new LocalRaftContext(asyncService, clusterSize, stateMachine);
        } else {
            return new ClusteredRaftContext(asyncService, clusterSize, clusteredId, clusteredIPs, stateMachine);
        }
    }
}
