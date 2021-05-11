package com.cervidae.jraft.restful;

import com.cervidae.jraft.config.AsyncService;
import com.cervidae.jraft.node.RaftConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnExpression("${cervidae.jraft.isMonitor:false}")
public class MonitorService {

    /**
     * External Services
     */
    final AsyncService asyncService;
    final RestClientService restClientService;
    final RaftConfig config;

    @Autowired
    public MonitorService(AsyncService asyncService, RestClientService restClientService, RaftConfig config) {
        this.asyncService = asyncService;
        this.restClientService = restClientService;
        this.config = config;
    }

    /**
     * Broadcast a POST request to all nodes, and return when response is success
     * Particularly helpful if you want to send the request to Leader
     * Make sure **only leader** will reply with success, followers should reply with fail
     * @param item url item, ex. /raft/info
     * @param body request body
     * @param <T> type of request body
     * @return response
     */
    public <T> Response<?> broadcastToLeader(String item, T body) {
        Response<?> reply = null;
        for (String url: config.getClusteredUrls()) {
            try {
                var response = restClientService.post(url + item, body);
                if (response.getSuccess() == 1) {
                    reply = response;
                    break;
                }
            } catch (Exception ignored) {}
        }
        Assert.notNull(reply, "Reply is null?");
        return reply;
    }

    /**
     * Broadcast a GET request to all nodes, and return EVERYONE's response in a list
     * @param item url item, ex. /raft/info
     * @return response (list of string)
     */
    public List<String> broadcastForString(String item) {
        List<String> replies = new ArrayList<>();
        for (String url: config.getClusteredUrls()) {
            String response = "TIME_OUT";
            try {
                response = restClientService.getForString(url + item);
            } catch (Exception ignored) {}
            replies.add(response);
        }
        return replies;
    }

    public List<String> getClusterStatus() {
        return broadcastForString("/raft/info");
    }
}
