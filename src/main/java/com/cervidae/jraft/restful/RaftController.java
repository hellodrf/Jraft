package com.cervidae.jraft.restful;

import com.alibaba.fastjson.JSON;
import com.cervidae.jraft.model.Account;
import com.cervidae.jraft.model.Command;
import com.cervidae.jraft.node.LocalRaftContext;
import com.cervidae.jraft.node.LogEntry;
import com.cervidae.jraft.node.RaftContext;
import com.cervidae.jraft.node.RaftNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@Log4j2
@RequestMapping("/raft")
@Api("RaftController")
public class RaftController implements ApplicationContextAware {

    private final RaftContext context;
    private ApplicationContext applicationContext;

    @Autowired
    public RaftController(RaftContext context) {
        this.context = context;
    }

    // GET localhost:8080/raft/kill?n=1
    @GetMapping(value = "/kill", params = {"n"})
    @ApiOperation("/killNode")
    @ApiImplicitParam(name = "n", value = "n", required = true, paramType = "query", dataType = "int")
    public String kill(@RequestParam int n) {
        if (context instanceof LocalRaftContext) {
            context.getNodes().get(n).shutdown();
        } else {
            context.shutdown();
        }
        log.warn("Killed node " + n);
        return info();
    }

    // GET localhost:8080/raft/info
    @GetMapping(value = "/info")
    public String info() {
        var builder = new StringBuilder();
        context.getNodes().forEach((x)-> builder.append(x.toString()).append(System.lineSeparator()));
        return builder.toString();
    }

    @GetMapping(value = "/start", params = {"n"})
    public String start(@RequestParam int n) {
        context.getNodes().set(n, applicationContext.getBean(RaftNode.class));
        context.getNodes().get(n).start();
        log.warn("Started node " + n);
        return info();
    }

    @Override
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }




    @GetMapping(value = "/check_balance")
    public Response checkBalance(@RequestParam("userId") String userId) {
        if(userId == null){
            return Response.fail("user does not exist");
        }

        log.info("check balance : {}", userId);
        Account account = context.getAccount(userId);
        if(account==null){
            return Response.fail("user does not exist");
        }else {
            return Response.success(account);
        }
    }

    @PostMapping(value = "/create_account")
    public Response createAccount(@RequestBody Map<String, Object> requestMap) {
        if(requestMap == null){
            return Response.fail("error input");
        }

        Command command = JSON.parseObject(JSON.toJSONString(requestMap),Command.class);
        if(command.getUserId() == null || command.getOperate() == null){
            return Response.fail("userId null");
        }

        Account account = context.getAccount(command.getUserId());
        if(account!=null){
            return Response.fail("user exists");
        }else {
            LogEntry logEntry = new LogEntry(context.getLeader().getCurrentTerm().intValue(),command);
            if(-1 == context.newEntry(logEntry)){
                return Response.fail("server error");
            }
            return Response.success("account created");
        }
    }


    @PostMapping(value = "/deposit")
    public Response deposit(@RequestBody Map<String, Object> requestMap) {
        if(requestMap == null){
            return Response.fail("error input");
        }

        Command command = JSON.parseObject(JSON.toJSONString(requestMap),Command.class);
        if(command.getUserId() == null || command.getOperate() ==null || command.getNumber() == null){
            return Response.fail("userId null");
        }

        Account account = context.getAccount(command.getUserId());
        if(account==null){
            return Response.fail("user does not exist");
        }else {
            LogEntry logEntry = new LogEntry(context.getLeader().getCurrentTerm().intValue(),command);
            if(-1 == context.newEntry(logEntry)){
                return Response.fail("server error");
            }
            return Response.success("deposit done");
        }
    }


    @PostMapping(value = "/withdraw")
    public Response withdraw(@RequestBody Map<String, Object> requestMap) {
        if(requestMap == null){
            return Response.fail("error input");
        }

        Command command = JSON.parseObject(JSON.toJSONString(requestMap),Command.class);
        if(command.getUserId() == null || command.getOperate() ==null || command.getNumber() == null){
            return Response.fail("userId null");
        }

        Account account = context.getAccount(command.getUserId());
        if(account==null){
            return Response.fail("user does not exist");
        }else {
            LogEntry logEntry = new LogEntry(context.getLeader().getCurrentTerm().intValue(),command);
            if(-1 == context.newEntry(logEntry)){
                return Response.fail("server error");
            }
            return Response.success("withdraw done");
        }
    }

}
