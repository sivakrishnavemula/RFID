package org.apache.commons.collections4.list;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import org.apache.commons.collections4.list.AbstractLinkedList.Node;

public class NodeCachingLinkedList<E> extends AbstractLinkedList<E> implements Serializable {
   private static final long serialVersionUID = 6897789178562232073L;
   private static final int DEFAULT_MAXIMUM_CACHE_SIZE = 20;
   private transient Node<E> firstCachedNode;
   private transient int cacheSize;
   private int maximumCacheSize;

   public NodeCachingLinkedList() {
      this(20);
   }

   public NodeCachingLinkedList(Collection<? extends E> coll) {
      super(coll);
      this.maximumCacheSize = 20;
   }

   public NodeCachingLinkedList(int maximumCacheSize) {
      this.maximumCacheSize = maximumCacheSize;
      this.init();
   }

   protected int getMaximumCacheSize() {
      return this.maximumCacheSize;
   }

   protected void setMaximumCacheSize(int maximumCacheSize) {
      this.maximumCacheSize = maximumCacheSize;
      this.shrinkCacheToMaximumSize();
   }

   protected void shrinkCacheToMaximumSize() {
      while (this.cacheSize > this.maximumCacheSize) {
         this.getNodeFromCache();
      }
   }

   protected Node<E> getNodeFromCache() {
      if (this.cacheSize == 0) {
         return null;
      }

      Node<E> cachedNode = this.firstCachedNode;
      this.firstCachedNode = cachedNode.next;
      cachedNode.next = null;
      this.cacheSize--;
      return cachedNode;
   }

   protected boolean isCacheFull() {
      return this.cacheSize >= this.maximumCacheSize;
   }

   protected void addNodeToCache(Node<E> node) {
      if (!this.isCacheFull()) {
         Node<E> nextCachedNode = this.firstCachedNode;
         node.previous = null;
         node.next = nextCachedNode;
         node.setValue(null);
         this.firstCachedNode = node;
         this.cacheSize++;
      }
   }

   protected Node<E> createNode(E value) {
      Node<E> cachedNode = this.getNodeFromCache();
      if (cachedNode == null) {
         return super.createNode(value);
      }

      cachedNode.setValue(value);
      return cachedNode;
   }

   protected void removeNode(Node<E> node) {
      super.removeNode(node);
      this.addNodeToCache(node);
   }

   protected void removeAllNodes() {
      int numberOfNodesToCache = Math.min(this.size, this.maximumCacheSize - this.cacheSize);
      Node<E> node = this.header.next;

      for (int currentIndex = 0; currentIndex < numberOfNodesToCache; currentIndex++) {
         Node<E> oldNode = node;
         node = node.next;
         this.addNodeToCache(oldNode);
      }

      super.removeAllNodes();
   }

   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      this.doWriteObject(out);
   }

   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      this.doReadObject(in);
   }
}
