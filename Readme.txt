JRaft Banking Application

I. Information:
    This application uses SpringBoot for IOC injection and Restful API endpoints.
    To start a cluster, you might need to modify node config in /src/main/resources,
        then start the RaftNodes and a Monitor.


II. Package structure (/src/main/java/) & important classes:
1. jraft/JraftApplication.class
    SpringBoot Application Entrypoint.
2. jraft/config
    SpringBoot Configuration classes: AsyncExecutors, SwaggerUI.
3. jraft/msg
    Raft Message classes: AppendEntriesRequest/Reply, RequestVoteRequest/Reply.
4. jraft/node
    Our implementation of the Raft Algorithm.
    a. RaftNode
        A Raft node, supports election and consensus.
    b. RaftContext
        Context of the Raft cluster, allowing nodes to interact with each other.
    c. LogEntry
        Class of the log entries in raft node.
5. jraft/restful
    Restful API endpoints for the cluster nodes.
    a. MonitorController
        Endpoints for the Monitor.
    b. RaftController
        Endpoints for the RaftNodes.
    c. RPCController
        RPC Endpoints for the RaftNodes.
6. jraft/statemachine
    Key-Value state-machine classes for the Raft Algorithm: HashMap, Redis.


III. Configuration files
Location: /src/main/resources
Usage: for example to start RaftNode 1, run the SpringBoot application with "remote1" profile.

application-remote{0-2}.properties
    Configuration file for the RaftNode 0, 1, and 2

application-remoteMon.properties
    Configuration file for the Monitor