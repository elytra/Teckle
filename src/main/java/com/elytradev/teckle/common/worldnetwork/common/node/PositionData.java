package com.elytradev.teckle.common.worldnetwork.common.node;

import com.google.common.collect.Lists;

import java.util.*;
import java.util.function.Predicate;

public class PositionData {

    private Map<UUID, List<NodeContainer>> nodeContainers;

    public List<NodeContainer> getNodeContainers(UUID key) {
        return nodeContainers.get(key);
    }

    public boolean add(UUID key, NodeContainer value) {
        if (!nodeContainers.containsKey(key))
            nodeContainers.put(key, Lists.newArrayList());

        return nodeContainers.get(key).add(value);
    }

    public List<NodeContainer> removeNetwork(UUID key) {
        return nodeContainers.remove(key);
    }

    public boolean removeNodeContainer(UUID key, NodeContainer nodeContainer){
        if (!nodeContainers.containsKey(key))
            nodeContainers.put(key, Lists.newArrayList());

        return nodeContainers.get(key).remove(nodeContainer);
    }

    public Set<UUID> networkIDS() {
        return nodeContainers.keySet();
    }

    public Collection<List<NodeContainer>> allNodeContainers() {
        return nodeContainers.values();
    }

    public Set<Map.Entry<UUID, List<NodeContainer>>> entrySet() {
        return nodeContainers.entrySet();
    }

    public boolean removeIf(UUID key, Predicate<NodeContainer> predicate){
        return nodeContainers.get(key).removeIf(predicate);
    }
}
