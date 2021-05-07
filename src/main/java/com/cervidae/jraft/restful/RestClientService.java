package com.cervidae.jraft.restful;

import com.cervidae.jraft.msg.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
@Lazy
public class RestClientService {

    final RestTemplate template;
    final HttpHeaders headers = new HttpHeaders();

    public RestClientService() {
        var fact = new HttpComponentsClientHttpRequestFactory();
        fact.setConnectTimeout(3000);
        fact.setReadTimeout(3000);
        fact.setConnectionRequestTimeout(3000);
        this.template = new RestTemplate(fact);
        this.headers.setContentType(MediaType.APPLICATION_JSON);
    }

    public RequestVoteReply sendRequestVote(String url, RequestVoteRequest msg) throws ResourceAccessException {
        var reply = template.postForObject("http://" + url + "/rpc/vote",
                msg, RequestVoteReply.class);
        Assert.notNull(reply, "Reply is null?");
        return reply;
    }

    public AppendEntriesReply sendAppendEntries(String url, AppendEntriesRequest msg) throws ResourceAccessException {
        var reply = template.postForObject("http://" + url + "/rpc/entry",
                msg, AppendEntriesReply.class);
        Assert.notNull(reply, "Reply is null?");
        return reply;
    }

    public Response<?> post(String url, Object body) throws ResourceAccessException {
        var reply = template.postForObject("http://" + url, body, Response.class);
        Assert.notNull(reply, "Reply is null?");
        return reply;
    }

    public String getForString(String url) throws ResourceAccessException {
        var reply = template.getForObject("http://" + url, String.class);
        Assert.notNull(reply, "Reply is null?");
        return reply;
    }
}
