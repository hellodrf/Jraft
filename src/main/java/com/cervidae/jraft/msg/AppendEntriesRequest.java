package com.cervidae.jraft.msg;

import com.cervidae.jraft.node.LogEntry;
import com.cervidae.jraft.node.RaftNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class AppendEntriesRequest extends Message {

    public final String type = "AppendEntriesRequest";

    public final Class<? extends Message> replyClass = AppendEntriesReply.class;

    int term;
    int leaderID;
    int prevLogIndex;
    int prevLogTerm;
    int leaderCommit;
    List<LogEntry> entries;

    public AppendEntriesRequest(int term, int leaderID, int prevLogIndex,
                                int prevLogTerm, int leaderCommit, List<LogEntry> entries) {
        this.term = term;
        this.leaderID = leaderID;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.leaderCommit = leaderCommit;
        this.entries = entries;
    }

    public AppendEntriesRequest(RaftNode node, List<LogEntry> entries) {
        this.term = node.getCurrentTerm().get();
        this.leaderID = node.getId();
        if (node.getLogEntries().size()==0) {
            this.prevLogIndex = -1;
            this.prevLogTerm = -1;
        } else {
            this.prevLogIndex = node.getLogEntries().size()-1;
            this.prevLogTerm = node.getLogEntries().get(node.getLogEntries().size()-1).getTerm();
        }
        this.leaderCommit = node.getLastCommitted();
        this.entries = entries;
        this.source = node.getId();
    }

    @Override
    public String toString() {
        return "AppendEntriesRequest{" +
                "term=" + term +
                ", leaderID=" + leaderID +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", leaderCommit=" + leaderCommit +
                ", entries=" + entries +
                '}';
    }
}
