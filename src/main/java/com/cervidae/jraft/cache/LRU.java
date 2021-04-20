package com.cervidae.jraft.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LRU<K,V> {

    static class Node<K,V> {
        K key;
        V value;
        Node<K,V> previousNode;
        Node<K,V> nextNode;
    }

    Map<K, Node<K,V>> storage = new HashMap<>();

    long capacity;
    ReentrantReadWriteLock mutex = new ReentrantReadWriteLock();

    Node<K,V> head;
    Node<K,V> tail;

    public LRU(long capacity) {
        this.capacity = capacity;
        this.head = new Node<>();
        this.tail = new Node<>();
        head.nextNode = tail;
        tail.previousNode = head;
    }

    public void put(K key, V value) {
        mutex.writeLock().lock();
        try {
            while (storage.size() >= capacity) {
                this.trimOneHead();
            }
            var node = new Node<K, V>();
            attachToTail(node);
            storage.put(key, node);
        } finally {
            mutex.writeLock().unlock();
        }
    }

    public V get(K key) {
        mutex.writeLock().lock();
        try {
            var node = storage.get(key);
            if (node == null) {
                return null;
            }
            attachToTail(node);
            return node.value;
        } finally {
            mutex.writeLock().unlock();
        }
    }

    public void remove(K key) {
        mutex.writeLock().lock();
        try {
            var node = storage.get(key);
            if (node == null) {
                return;
            }
            node.nextNode.previousNode = node.previousNode;
            node.previousNode.nextNode = node.nextNode;
            storage.remove(key);
        } finally {
            mutex.writeLock().unlock();
        }
    }

    void trimOneHead() {
        var node = head.nextNode;
        if (node == tail) {
            return;
        }
        head.nextNode = node.nextNode;
        node.nextNode.previousNode = head;
        storage.remove(node.key);
    }

    void attachToTail(Node<K,V> node) {
        node.previousNode = tail.previousNode;
        tail.previousNode.nextNode = node;
        node.nextNode = tail;
        tail.previousNode = node;
    }
}
