package org.shield.webserver.configuration;

import java.util.ArrayList;
import java.util.List;

public class ResponseWrapper {
    private String notary;
    private List<String> nodes;

    public ResponseWrapper() {
        nodes = new ArrayList<>();
    }

    public String getNotary() {
        return notary;
    }

    public void setNotary(String notary) {
        this.notary = notary;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }

    public void addNode(String node){
        this.nodes.add(node);
    }
}
