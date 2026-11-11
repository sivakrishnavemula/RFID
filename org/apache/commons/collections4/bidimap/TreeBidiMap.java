package org.apache.commons.collections4.bidimap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.OrderedBidiMap;
import org.apache.commons.collections4.OrderedIterator;
import org.apache.commons.collections4.OrderedMapIterator;
import org.apache.commons.collections4.iterators.EmptyOrderedMapIterator;
import org.apache.commons.collections4.keyvalue.UnmodifiableMapEntry;

public class TreeBidiMap<K extends Comparable<K>, V extends Comparable<V>> implements OrderedBidiMap<K, V>, Serializable {
   private static final long serialVersionUID = 721969328361807L;
   private transient TreeBidiMap.Node<K, V>[] rootNode;
   private transient int nodeCount = 0;
   private transient int modifications = 0;
   private transient Set<K> keySet;
   private transient Set<V> valuesSet;
   private transient Set<Entry<K, V>> entrySet;
   private transient TreeBidiMap<K, V>.Inverse inverse = null;

   public TreeBidiMap() {
      this.rootNode = new TreeBidiMap.Node[2];
   }

   public TreeBidiMap(Map<? extends K, ? extends V> map) {
      this();
      this.putAll(map);
   }

   public int size() {
      return this.nodeCount;
   }

   public boolean isEmpty() {
      return this.nodeCount == 0;
   }

   public boolean containsKey(Object key) {
      checkKey(key);
      return this.lookupKey(key) != null;
   }

   public boolean containsValue(Object value) {
      checkValue(value);
      return this.lookupValue(value) != null;
   }

   public V get(Object key) {
      checkKey(key);
      TreeBidiMap.Node<K, V> node = this.lookupKey(key);
      return node == null ? null : node.getValue();
   }

   public V put(K key, V value) {
      V result = this.get(key);
      this.doPut(key, value);
      return result;
   }

   public void putAll(Map<? extends K, ? extends V> map) {
      for (Entry<? extends K, ? extends V> e : map.entrySet()) {
         this.put((K)e.getKey(), (V)e.getValue());
      }
   }

   public V remove(Object key) {
      return this.doRemoveKey(key);
   }

   public void clear() {
      this.modify();
      this.nodeCount = 0;
      this.rootNode[TreeBidiMap.DataElement.KEY.ordinal()] = null;
      this.rootNode[TreeBidiMap.DataElement.VALUE.ordinal()] = null;
   }

   public K getKey(Object value) {
      checkValue(value);
      TreeBidiMap.Node<K, V> node = this.lookupValue(value);
      return node == null ? null : node.getKey();
   }

   public K removeValue(Object value) {
      return this.doRemoveValue(value);
   }

   public K firstKey() {
      if (this.nodeCount == 0) {
         throw new NoSuchElementException("Map is empty");
      } else {
         return this.leastNode(this.rootNode[TreeBidiMap.DataElement.KEY.ordinal()], TreeBidiMap.DataElement.KEY).getKey();
      }
   }

   public K lastKey() {
      if (this.nodeCount == 0) {
         throw new NoSuchElementException("Map is empty");
      } else {
         return this.greatestNode(this.rootNode[TreeBidiMap.DataElement.KEY.ordinal()], TreeBidiMap.DataElement.KEY).getKey();
      }
   }

   public K nextKey(K key) {
      checkKey(key);
      TreeBidiMap.Node<K, V> node = this.nextGreater(this.lookupKey(key), TreeBidiMap.DataElement.KEY);
      return node == null ? null : node.getKey();
   }

   public K previousKey(K key) {
      checkKey(key);
      TreeBidiMap.Node<K, V> node = this.nextSmaller(this.lookupKey(key), TreeBidiMap.DataElement.KEY);
      return node == null ? null : node.getKey();
   }

   public Set<K> keySet() {
      if (this.keySet == null) {
         this.keySet = new TreeBidiMap.KeyView(TreeBidiMap.DataElement.KEY);
      }

      return this.keySet;
   }

   public Set<V> values() {
      if (this.valuesSet == null) {
         this.valuesSet = new TreeBidiMap.ValueView(TreeBidiMap.DataElement.KEY);
      }

      return this.valuesSet;
   }

   public Set<Entry<K, V>> entrySet() {
      if (this.entrySet == null) {
         this.entrySet = new TreeBidiMap.EntryView();
      }

      return this.entrySet;
   }

   public OrderedMapIterator<K, V> mapIterator() {
      return this.isEmpty() ? EmptyOrderedMapIterator.emptyOrderedMapIterator() : new TreeBidiMap.ViewMapIterator(TreeBidiMap.DataElement.KEY);
   }

   public OrderedBidiMap<V, K> inverseBidiMap() {
      if (this.inverse == null) {
         this.inverse = new TreeBidiMap.Inverse();
      }

      return this.inverse;
   }

   public boolean equals(Object obj) {
      return this.doEquals(obj, TreeBidiMap.DataElement.KEY);
   }

   public int hashCode() {
      return this.doHashCode(TreeBidiMap.DataElement.KEY);
   }

   public String toString() {
      return this.doToString(TreeBidiMap.DataElement.KEY);
   }

   private void doPut(K key, V value) {
      checkKeyAndValue(key, value);
      this.doRemoveKey(key);
      this.doRemoveValue(value);
      TreeBidiMap.Node<K, V> node = this.rootNode[TreeBidiMap.DataElement.KEY.ordinal()];
      if (node == null) {
         TreeBidiMap.Node<K, V> root = new TreeBidiMap.Node<>(key, value);
         this.rootNode[TreeBidiMap.DataElement.KEY.ordinal()] = root;
         this.rootNode[TreeBidiMap.DataElement.VALUE.ordinal()] = root;
         this.grow();
      } else {
         while (true) {
            int cmp = compare(key, node.getKey());
            if (cmp == 0) {
               throw new IllegalArgumentException("Cannot store a duplicate key (\"" + key + "\") in this Map");
            }

            if (cmp < 0) {
               if (node.getLeft(TreeBidiMap.DataElement.KEY) == null) {
                  TreeBidiMap.Node<K, V> newNode = new TreeBidiMap.Node<>(key, value);
                  this.insertValue(newNode);
                  node.setLeft(newNode, TreeBidiMap.DataElement.KEY);
                  newNode.setParent(node, TreeBidiMap.DataElement.KEY);
                  this.doRedBlackInsert(newNode, TreeBidiMap.DataElement.KEY);
                  this.grow();
                  break;
               }

               node = node.getLeft(TreeBidiMap.DataElement.KEY);
            } else {
               if (node.getRight(TreeBidiMap.DataElement.KEY) == null) {
                  TreeBidiMap.Node<K, V> newNode = new TreeBidiMap.Node<>(key, value);
                  this.insertValue(newNode);
                  node.setRight(newNode, TreeBidiMap.DataElement.KEY);
                  newNode.setParent(node, TreeBidiMap.DataElement.KEY);
                  this.doRedBlackInsert(newNode, TreeBidiMap.DataElement.KEY);
                  this.grow();
                  break;
               }

               node = node.getRight(TreeBidiMap.DataElement.KEY);
            }
         }
      }
   }

   private V doRemoveKey(Object key) {
      TreeBidiMap.Node<K, V> node = this.lookupKey(key);
      if (node == null) {
         return null;
      }

      this.doRedBlackDelete(node);
      return node.getValue();
   }

   private K doRemoveValue(Object value) {
      TreeBidiMap.Node<K, V> node = this.lookupValue(value);
      if (node == null) {
         return null;
      }

      this.doRedBlackDelete(node);
      return node.getKey();
   }

   private <T extends Comparable<T>> TreeBidiMap.Node<K, V> lookup(Object data, TreeBidiMap.DataElement dataElement) {
      TreeBidiMap.Node<K, V> rval = null;
      TreeBidiMap.Node<K, V> node = this.rootNode[dataElement.ordinal()];

      while (node != null) {
         int cmp = compare((T)data, (T)node.getData(dataElement));
         if (cmp == 0) {
            rval = node;
            break;
         }

         node = cmp < 0 ? node.getLeft(dataElement) : node.getRight(dataElement);
      }

      return rval;
   }

   private TreeBidiMap.Node<K, V> lookupKey(Object key) {
      return this.lookup(key, TreeBidiMap.DataElement.KEY);
   }

   private TreeBidiMap.Node<K, V> lookupValue(Object value) {
      return this.lookup(value, TreeBidiMap.DataElement.VALUE);
   }

   private TreeBidiMap.Node<K, V> nextGreater(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
      TreeBidiMap.Node<K, V> rval;
      if (node == null) {
         rval = null;
      } else if (node.getRight(dataElement) != null) {
         rval = this.leastNode(node.getRight(dataElement), dataElement);
      } else {
         TreeBidiMap.Node<K, V> parent = node.getParent(dataElement);

         for (TreeBidiMap.Node<K, V> child = node; parent != null && child == parent.getRight(dataElement); parent = parent.getParent(dataElement)) {
            child = parent;
         }

         rval = parent;
      }

      return rval;
   }

   private TreeBidiMap.Node<K, V> nextSmaller(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
      TreeBidiMap.Node<K, V> rval;
      if (node == null) {
         rval = null;
      } else if (node.getLeft(dataElement) != null) {
         rval = this.greatestNode(node.getLeft(dataElement), dataElement);
      } else {
         TreeBidiMap.Node<K, V> parent = node.getParent(dataElement);

         for (TreeBidiMap.Node<K, V> child = node; parent != null && child == parent.getLeft(dataElement); parent = parent.getParent(dataElement)) {
            child = parent;
         }

         rval = parent;
      }

      return rval;
   }

   private static <T extends Comparable<T>> int compare(T o1, T o2) {
      return o1.compareTo(o2);
   }

   private TreeBidiMap.Node<K, V> leastNode(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
      TreeBidiMap.Node<K, V> rval = node;
      if (rval != null) {
         while (rval.getLeft(dataElement) != null) {
            rval = rval.getLeft(dataElement);
         }
      }

      return rval;
   }

   private TreeBidiMap.Node<K, V> greatestNode(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
      TreeBidiMap.Node<K, V> rval = node;
      if (rval != null) {
         while (rval.getRight(dataElement) != null) {
            rval = rval.getRight(dataElement);
         }
      }

      return rval;
   }

   private void copyColor(TreeBidiMap.Node<K, V> from, TreeBidiMap.Node<K, V> to, TreeBidiMap.DataElement dataElement) {
      if (to != null) {
         if (from == null) {
            to.setBlack(dataElement);
         } else {
            to.copyColor(from, dataElement);
         }
      }
   }

   private static boolean isRed(TreeBidiMap.Node<?, ?> node, TreeBidiMap.DataElement dataElement) {
      return node != null && node.isRed(dataElement);
   }

   private static boolean isBlack(TreeBidiMap.Node<?, ?> node, TreeBidiMap.DataElement dataElement) {
      return node == null || node.isBlack(dataElement);
   }

   private static void makeRed(TreeBidiMap.Node<?, ?> node, TreeBidiMap.DataElement dataElement) {
      if (node != null) {
         node.setRed(dataElement);
      }
   }

   private static void makeBlack(TreeBidiMap.Node<?, ?> node, TreeBidiMap.DataElement dataElement) {
      if (node != null) {
         node.setBlack(dataElement);
      }
   }

   private TreeBidiMap.Node<K, V> getGrandParent(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
      return this.getParent(this.getParent(node, dataElement), dataElement);
   }

   private TreeBidiMap.Node<K, V> getParent(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
      return node == null ? null : node.getParent(dataElement);
   }

   private TreeBidiMap.Node<K, V> getRightChild(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
      return node == null ? null : node.getRight(dataElement);
   }

   private TreeBidiMap.Node<K, V> getLeftChild(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
      return node == null ? null : node.getLeft(dataElement);
   }

   private void rotateLeft(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
      TreeBidiMap.Node<K, V> rightChild = node.getRight(dataElement);
      node.setRight(rightChild.getLeft(dataElement), dataElement);
      if (rightChild.getLeft(dataElement) != null) {
         rightChild.getLeft(dataElement).setParent(node, dataElement);
      }

      rightChild.setParent(node.getParent(dataElement), dataElement);
      if (node.getParent(dataElement) == null) {
         this.rootNode[dataElement.ordinal()] = rightChild;
      } else if (node.getParent(dataElement).getLeft(dataElement) == node) {
         node.getParent(dataElement).setLeft(rightChild, dataElement);
      } else {
         node.getParent(dataElement).setRight(rightChild, dataElement);
      }

      rightChild.setLeft(node, dataElement);
      node.setParent(rightChild, dataElement);
   }

   private void rotateRight(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
      TreeBidiMap.Node<K, V> leftChild = node.getLeft(dataElement);
      node.setLeft(leftChild.getRight(dataElement), dataElement);
      if (leftChild.getRight(dataElement) != null) {
         leftChild.getRight(dataElement).setParent(node, dataElement);
      }

      leftChild.setParent(node.getParent(dataElement), dataElement);
      if (node.getParent(dataElement) == null) {
         this.rootNode[dataElement.ordinal()] = leftChild;
      } else if (node.getParent(dataElement).getRight(dataElement) == node) {
         node.getParent(dataElement).setRight(leftChild, dataElement);
      } else {
         node.getParent(dataElement).setLeft(leftChild, dataElement);
      }

      leftChild.setRight(node, dataElement);
      node.setParent(leftChild, dataElement);
   }

   private void doRedBlackInsert(TreeBidiMap.Node<K, V> insertedNode, TreeBidiMap.DataElement dataElement) {
      TreeBidiMap.Node<K, V> currentNode = insertedNode;
      makeRed(currentNode, dataElement);

      while (currentNode != null && currentNode != this.rootNode[dataElement.ordinal()] && isRed(currentNode.getParent(dataElement), dataElement)) {
         if (currentNode.isLeftChild(dataElement)) {
            TreeBidiMap.Node<K, V> y = this.getRightChild(this.getGrandParent(currentNode, dataElement), dataElement);
            if (isRed(y, dataElement)) {
               makeBlack(this.getParent(currentNode, dataElement), dataElement);
               makeBlack(y, dataElement);
               makeRed(this.getGrandParent(currentNode, dataElement), dataElement);
               currentNode = this.getGrandParent(currentNode, dataElement);
            } else {
               if (currentNode.isRightChild(dataElement)) {
                  currentNode = this.getParent(currentNode, dataElement);
                  this.rotateLeft(currentNode, dataElement);
               }

               makeBlack(this.getParent(currentNode, dataElement), dataElement);
               makeRed(this.getGrandParent(currentNode, dataElement), dataElement);
               if (this.getGrandParent(currentNode, dataElement) != null) {
                  this.rotateRight(this.getGrandParent(currentNode, dataElement), dataElement);
               }
            }
         } else {
            TreeBidiMap.Node<K, V> y = this.getLeftChild(this.getGrandParent(currentNode, dataElement), dataElement);
            if (isRed(y, dataElement)) {
               makeBlack(this.getParent(currentNode, dataElement), dataElement);
               makeBlack(y, dataElement);
               makeRed(this.getGrandParent(currentNode, dataElement), dataElement);
               currentNode = this.getGrandParent(currentNode, dataElement);
            } else {
               if (currentNode.isLeftChild(dataElement)) {
                  currentNode = this.getParent(currentNode, dataElement);
                  this.rotateRight(currentNode, dataElement);
               }

               makeBlack(this.getParent(currentNode, dataElement), dataElement);
               makeRed(this.getGrandParent(currentNode, dataElement), dataElement);
               if (this.getGrandParent(currentNode, dataElement) != null) {
                  this.rotateLeft(this.getGrandParent(currentNode, dataElement), dataElement);
               }
            }
         }
      }

      makeBlack(this.rootNode[dataElement.ordinal()], dataElement);
   }

   private void doRedBlackDelete(TreeBidiMap.Node<K, V> deletedNode) {
      for (TreeBidiMap.DataElement dataElement : TreeBidiMap.DataElement.values()) {
         if (deletedNode.getLeft(dataElement) != null && deletedNode.getRight(dataElement) != null) {
            this.swapPosition(this.nextGreater(deletedNode, dataElement), deletedNode, dataElement);
         }

         TreeBidiMap.Node<K, V> replacement = deletedNode.getLeft(dataElement) != null ? deletedNode.getLeft(dataElement) : deletedNode.getRight(dataElement);
         if (replacement != null) {
            replacement.setParent(deletedNode.getParent(dataElement), dataElement);
            if (deletedNode.getParent(dataElement) == null) {
               this.rootNode[dataElement.ordinal()] = replacement;
            } else if (deletedNode == deletedNode.getParent(dataElement).getLeft(dataElement)) {
               deletedNode.getParent(dataElement).setLeft(replacement, dataElement);
            } else {
               deletedNode.getParent(dataElement).setRight(replacement, dataElement);
            }

            deletedNode.setLeft(null, dataElement);
            deletedNode.setRight(null, dataElement);
            deletedNode.setParent(null, dataElement);
            if (isBlack(deletedNode, dataElement)) {
               this.doRedBlackDeleteFixup(replacement, dataElement);
            }
         } else if (deletedNode.getParent(dataElement) == null) {
            this.rootNode[dataElement.ordinal()] = null;
         } else {
            if (isBlack(deletedNode, dataElement)) {
               this.doRedBlackDeleteFixup(deletedNode, dataElement);
            }

            if (deletedNode.getParent(dataElement) != null) {
               if (deletedNode == deletedNode.getParent(dataElement).getLeft(dataElement)) {
                  deletedNode.getParent(dataElement).setLeft(null, dataElement);
               } else {
                  deletedNode.getParent(dataElement).setRight(null, dataElement);
               }

               deletedNode.setParent(null, dataElement);
            }
         }
      }

      this.shrink();
   }

   private void doRedBlackDeleteFixup(TreeBidiMap.Node<K, V> replacementNode, TreeBidiMap.DataElement dataElement) {
      TreeBidiMap.Node<K, V> currentNode = replacementNode;

      while (currentNode != this.rootNode[dataElement.ordinal()] && isBlack(currentNode, dataElement)) {
         if (currentNode.isLeftChild(dataElement)) {
            TreeBidiMap.Node<K, V> siblingNode = this.getRightChild(this.getParent(currentNode, dataElement), dataElement);
            if (isRed(siblingNode, dataElement)) {
               makeBlack(siblingNode, dataElement);
               makeRed(this.getParent(currentNode, dataElement), dataElement);
               this.rotateLeft(this.getParent(currentNode, dataElement), dataElement);
               siblingNode = this.getRightChild(this.getParent(currentNode, dataElement), dataElement);
            }

            if (isBlack(this.getLeftChild(siblingNode, dataElement), dataElement) && isBlack(this.getRightChild(siblingNode, dataElement), dataElement)) {
               makeRed(siblingNode, dataElement);
               currentNode = this.getParent(currentNode, dataElement);
            } else {
               if (isBlack(this.getRightChild(siblingNode, dataElement), dataElement)) {
                  makeBlack(this.getLeftChild(siblingNode, dataElement), dataElement);
                  makeRed(siblingNode, dataElement);
                  this.rotateRight(siblingNode, dataElement);
                  siblingNode = this.getRightChild(this.getParent(currentNode, dataElement), dataElement);
               }

               this.copyColor(this.getParent(currentNode, dataElement), siblingNode, dataElement);
               makeBlack(this.getParent(currentNode, dataElement), dataElement);
               makeBlack(this.getRightChild(siblingNode, dataElement), dataElement);
               this.rotateLeft(this.getParent(currentNode, dataElement), dataElement);
               currentNode = this.rootNode[dataElement.ordinal()];
            }
         } else {
            TreeBidiMap.Node<K, V> siblingNode = this.getLeftChild(this.getParent(currentNode, dataElement), dataElement);
            if (isRed(siblingNode, dataElement)) {
               makeBlack(siblingNode, dataElement);
               makeRed(this.getParent(currentNode, dataElement), dataElement);
               this.rotateRight(this.getParent(currentNode, dataElement), dataElement);
               siblingNode = this.getLeftChild(this.getParent(currentNode, dataElement), dataElement);
            }

            if (isBlack(this.getRightChild(siblingNode, dataElement), dataElement) && isBlack(this.getLeftChild(siblingNode, dataElement), dataElement)) {
               makeRed(siblingNode, dataElement);
               currentNode = this.getParent(currentNode, dataElement);
            } else {
               if (isBlack(this.getLeftChild(siblingNode, dataElement), dataElement)) {
                  makeBlack(this.getRightChild(siblingNode, dataElement), dataElement);
                  makeRed(siblingNode, dataElement);
                  this.rotateLeft(siblingNode, dataElement);
                  siblingNode = this.getLeftChild(this.getParent(currentNode, dataElement), dataElement);
               }

               this.copyColor(this.getParent(currentNode, dataElement), siblingNode, dataElement);
               makeBlack(this.getParent(currentNode, dataElement), dataElement);
               makeBlack(this.getLeftChild(siblingNode, dataElement), dataElement);
               this.rotateRight(this.getParent(currentNode, dataElement), dataElement);
               currentNode = this.rootNode[dataElement.ordinal()];
            }
         }
      }

      makeBlack(currentNode, dataElement);
   }

   private void swapPosition(TreeBidiMap.Node<K, V> x, TreeBidiMap.Node<K, V> y, TreeBidiMap.DataElement dataElement) {
      TreeBidiMap.Node<K, V> xFormerParent = x.getParent(dataElement);
      TreeBidiMap.Node<K, V> xFormerLeftChild = x.getLeft(dataElement);
      TreeBidiMap.Node<K, V> xFormerRightChild = x.getRight(dataElement);
      TreeBidiMap.Node<K, V> yFormerParent = y.getParent(dataElement);
      TreeBidiMap.Node<K, V> yFormerLeftChild = y.getLeft(dataElement);
      TreeBidiMap.Node<K, V> yFormerRightChild = y.getRight(dataElement);
      boolean xWasLeftChild = x.getParent(dataElement) != null && x == x.getParent(dataElement).getLeft(dataElement);
      boolean yWasLeftChild = y.getParent(dataElement) != null && y == y.getParent(dataElement).getLeft(dataElement);
      if (x == yFormerParent) {
         x.setParent(y, dataElement);
         if (yWasLeftChild) {
            y.setLeft(x, dataElement);
            y.setRight(xFormerRightChild, dataElement);
         } else {
            y.setRight(x, dataElement);
            y.setLeft(xFormerLeftChild, dataElement);
         }
      } else {
         x.setParent(yFormerParent, dataElement);
         if (yFormerParent != null) {
            if (yWasLeftChild) {
               yFormerParent.setLeft(x, dataElement);
            } else {
               yFormerParent.setRight(x, dataElement);
            }
         }

         y.setLeft(xFormerLeftChild, dataElement);
         y.setRight(xFormerRightChild, dataElement);
      }

      if (y == xFormerParent) {
         y.setParent(x, dataElement);
         if (xWasLeftChild) {
            x.setLeft(y, dataElement);
            x.setRight(yFormerRightChild, dataElement);
         } else {
            x.setRight(y, dataElement);
            x.setLeft(yFormerLeftChild, dataElement);
         }
      } else {
         y.setParent(xFormerParent, dataElement);
         if (xFormerParent != null) {
            if (xWasLeftChild) {
               xFormerParent.setLeft(y, dataElement);
            } else {
               xFormerParent.setRight(y, dataElement);
            }
         }

         x.setLeft(yFormerLeftChild, dataElement);
         x.setRight(yFormerRightChild, dataElement);
      }

      if (x.getLeft(dataElement) != null) {
         x.getLeft(dataElement).setParent(x, dataElement);
      }

      if (x.getRight(dataElement) != null) {
         x.getRight(dataElement).setParent(x, dataElement);
      }

      if (y.getLeft(dataElement) != null) {
         y.getLeft(dataElement).setParent(y, dataElement);
      }

      if (y.getRight(dataElement) != null) {
         y.getRight(dataElement).setParent(y, dataElement);
      }

      x.swapColors(y, dataElement);
      if (this.rootNode[dataElement.ordinal()] == x) {
         this.rootNode[dataElement.ordinal()] = y;
      } else if (this.rootNode[dataElement.ordinal()] == y) {
         this.rootNode[dataElement.ordinal()] = x;
      }
   }

   private static void checkNonNullComparable(Object o, TreeBidiMap.DataElement dataElement) {
      if (o == null) {
         throw new NullPointerException(dataElement + " cannot be null");
      }

      if (!(o instanceof Comparable)) {
         throw new ClassCastException(dataElement + " must be Comparable");
      }
   }

   private static void checkKey(Object key) {
      checkNonNullComparable(key, TreeBidiMap.DataElement.KEY);
   }

   private static void checkValue(Object value) {
      checkNonNullComparable(value, TreeBidiMap.DataElement.VALUE);
   }

   private static void checkKeyAndValue(Object key, Object value) {
      checkKey(key);
      checkValue(value);
   }

   private void modify() {
      this.modifications++;
   }

   private void grow() {
      this.modify();
      this.nodeCount++;
   }

   private void shrink() {
      this.modify();
      this.nodeCount--;
   }

   private void insertValue(TreeBidiMap.Node<K, V> newNode) throws IllegalArgumentException {
      TreeBidiMap.Node<K, V> node = this.rootNode[TreeBidiMap.DataElement.VALUE.ordinal()];

      while (true) {
         int cmp = compare(newNode.getValue(), node.getValue());
         if (cmp == 0) {
            throw new IllegalArgumentException("Cannot store a duplicate value (\"" + newNode.getData(TreeBidiMap.DataElement.VALUE) + "\") in this Map");
         }

         if (cmp < 0) {
            if (node.getLeft(TreeBidiMap.DataElement.VALUE) == null) {
               node.setLeft(newNode, TreeBidiMap.DataElement.VALUE);
               newNode.setParent(node, TreeBidiMap.DataElement.VALUE);
               this.doRedBlackInsert(newNode, TreeBidiMap.DataElement.VALUE);
               break;
            }

            node = node.getLeft(TreeBidiMap.DataElement.VALUE);
         } else {
            if (node.getRight(TreeBidiMap.DataElement.VALUE) == null) {
               node.setRight(newNode, TreeBidiMap.DataElement.VALUE);
               newNode.setParent(node, TreeBidiMap.DataElement.VALUE);
               this.doRedBlackInsert(newNode, TreeBidiMap.DataElement.VALUE);
               break;
            }

            node = node.getRight(TreeBidiMap.DataElement.VALUE);
         }
      }
   }

   private boolean doEquals(Object obj, TreeBidiMap.DataElement dataElement) {
      if (obj == this) {
         return true;
      }

      if (!(obj instanceof Map)) {
         return false;
      }

      Map<?, ?> other = (Map<?, ?>)obj;
      if (other.size() != this.size()) {
         return false;
      }

      if (this.nodeCount > 0) {
         try {
            MapIterator<?, ?> it = this.getMapIterator(dataElement);

            while (it.hasNext()) {
               Object key = it.next();
               Object value = it.getValue();
               if (!value.equals(other.get(key))) {
                  return false;
               }
            }
         } catch (ClassCastException ex) {
            return false;
         } catch (NullPointerException ex) {
            return false;
         }
      }

      return true;
   }

   private int doHashCode(TreeBidiMap.DataElement dataElement) {
      int total = 0;
      if (this.nodeCount > 0) {
         MapIterator<?, ?> it = this.getMapIterator(dataElement);

         while (it.hasNext()) {
            Object key = it.next();
            Object value = it.getValue();
            total += key.hashCode() ^ value.hashCode();
         }
      }

      return total;
   }

   private String doToString(TreeBidiMap.DataElement dataElement) {
      if (this.nodeCount == 0) {
         return "{}";
      }

      StringBuilder buf = new StringBuilder(this.nodeCount * 32);
      buf.append('{');
      MapIterator<?, ?> it = this.getMapIterator(dataElement);
      boolean hasNext = it.hasNext();

      while (hasNext) {
         Object key = it.next();
         Object value = it.getValue();
         buf.append(key == this ? "(this Map)" : key).append('=').append(value == this ? "(this Map)" : value);
         hasNext = it.hasNext();
         if (hasNext) {
            buf.append(", ");
         }
      }

      buf.append('}');
      return buf.toString();
   }

   private MapIterator<?, ?> getMapIterator(TreeBidiMap.DataElement dataElement) {
      switch (dataElement) {
         case KEY:
            return new TreeBidiMap.ViewMapIterator(TreeBidiMap.DataElement.KEY);
         case VALUE:
            return new TreeBidiMap.InverseViewMapIterator(TreeBidiMap.DataElement.VALUE);
         default:
            throw new IllegalArgumentException();
      }
   }

   private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      this.rootNode = new TreeBidiMap.Node[2];
      int size = stream.readInt();

      for (int i = 0; i < size; i++) {
         K k = (K)stream.readObject();
         V v = (V)stream.readObject();
         this.put(k, v);
      }
   }

   private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeInt(this.size());

      for (Entry<K, V> entry : this.entrySet()) {
         stream.writeObject(entry.getKey());
         stream.writeObject(entry.getValue());
      }
   }

   enum DataElement {
      KEY("key"),
      VALUE("value");

      private final String description;

      DataElement(String description) {
         this.description = description;
      }

      public String toString() {
         return this.description;
      }
   }

   class EntryView extends TreeBidiMap<K, V>.View<Entry<K, V>> {
      EntryView() {
         super(TreeBidiMap.DataElement.KEY);
      }

      public boolean contains(Object obj) {
         if (!(obj instanceof Entry)) {
            return false;
         }

         Entry<?, ?> entry = (Entry<?, ?>)obj;
         Object value = entry.getValue();
         TreeBidiMap.Node<K, V> node = TreeBidiMap.this.lookupKey(entry.getKey());
         return node != null && node.getValue().equals(value);
      }

      public boolean remove(Object obj) {
         if (!(obj instanceof Entry)) {
            return false;
         } else {
            Entry<?, ?> entry = (Entry<?, ?>)obj;
            Object value = entry.getValue();
            TreeBidiMap.Node<K, V> node = TreeBidiMap.this.lookupKey(entry.getKey());
            if (node != null && node.getValue().equals(value)) {
               TreeBidiMap.this.doRedBlackDelete(node);
               return true;
            } else {
               return false;
            }
         }
      }

      public Iterator<Entry<K, V>> iterator() {
         return TreeBidiMap.this.new ViewMapEntryIterator();
      }
   }

   class Inverse implements OrderedBidiMap<V, K> {
      private Set<V> inverseKeySet;
      private Set<K> inverseValuesSet;
      private Set<Entry<V, K>> inverseEntrySet;

      public int size() {
         return TreeBidiMap.this.size();
      }

      public boolean isEmpty() {
         return TreeBidiMap.this.isEmpty();
      }

      public K get(Object key) {
         return TreeBidiMap.this.getKey(key);
      }

      public V getKey(Object value) {
         return TreeBidiMap.this.get(value);
      }

      public boolean containsKey(Object key) {
         return TreeBidiMap.this.containsValue(key);
      }

      public boolean containsValue(Object value) {
         return TreeBidiMap.this.containsKey(value);
      }

      public V firstKey() {
         if (TreeBidiMap.this.nodeCount == 0) {
            throw new NoSuchElementException("Map is empty");
         } else {
            return TreeBidiMap.this.leastNode(TreeBidiMap.this.rootNode[TreeBidiMap.DataElement.VALUE.ordinal()], TreeBidiMap.DataElement.VALUE).getValue();
         }
      }

      public V lastKey() {
         if (TreeBidiMap.this.nodeCount == 0) {
            throw new NoSuchElementException("Map is empty");
         } else {
            return TreeBidiMap.this.greatestNode(TreeBidiMap.this.rootNode[TreeBidiMap.DataElement.VALUE.ordinal()], TreeBidiMap.DataElement.VALUE).getValue();
         }
      }

      public V nextKey(V key) {
         TreeBidiMap.checkKey(key);
         TreeBidiMap.Node<K, V> node = TreeBidiMap.this.nextGreater(TreeBidiMap.this.lookup(key, TreeBidiMap.DataElement.VALUE), TreeBidiMap.DataElement.VALUE);
         return node == null ? null : node.getValue();
      }

      public V previousKey(V key) {
         TreeBidiMap.checkKey(key);
         TreeBidiMap.Node<K, V> node = TreeBidiMap.this.nextSmaller(TreeBidiMap.this.lookup(key, TreeBidiMap.DataElement.VALUE), TreeBidiMap.DataElement.VALUE);
         return node == null ? null : node.getValue();
      }

      public K put(V key, K value) {
         K result = (K)this.get(key);
         TreeBidiMap.this.doPut(value, key);
         return result;
      }

      public void putAll(Map<? extends V, ? extends K> map) {
         for (Entry<? extends V, ? extends K> e : map.entrySet()) {
            this.put((V)e.getKey(), (K)e.getValue());
         }
      }

      public K remove(Object key) {
         return TreeBidiMap.this.removeValue(key);
      }

      public V removeValue(Object value) {
         return TreeBidiMap.this.remove(value);
      }

      public void clear() {
         TreeBidiMap.this.clear();
      }

      public Set<V> keySet() {
         if (this.inverseKeySet == null) {
            this.inverseKeySet = TreeBidiMap.this.new ValueView(TreeBidiMap.DataElement.VALUE);
         }

         return this.inverseKeySet;
      }

      public Set<K> values() {
         if (this.inverseValuesSet == null) {
            this.inverseValuesSet = TreeBidiMap.this.new KeyView(TreeBidiMap.DataElement.VALUE);
         }

         return this.inverseValuesSet;
      }

      public Set<Entry<V, K>> entrySet() {
         if (this.inverseEntrySet == null) {
            this.inverseEntrySet = TreeBidiMap.this.new InverseEntryView();
         }

         return this.inverseEntrySet;
      }

      public OrderedMapIterator<V, K> mapIterator() {
         return this.isEmpty() ? EmptyOrderedMapIterator.emptyOrderedMapIterator() : TreeBidiMap.this.new InverseViewMapIterator(TreeBidiMap.DataElement.VALUE);
      }

      public OrderedBidiMap<K, V> inverseBidiMap() {
         return TreeBidiMap.this;
      }

      public boolean equals(Object obj) {
         return TreeBidiMap.this.doEquals(obj, TreeBidiMap.DataElement.VALUE);
      }

      public int hashCode() {
         return TreeBidiMap.this.doHashCode(TreeBidiMap.DataElement.VALUE);
      }

      public String toString() {
         return TreeBidiMap.this.doToString(TreeBidiMap.DataElement.VALUE);
      }
   }

   class InverseEntryView extends TreeBidiMap<K, V>.View<Entry<V, K>> {
      InverseEntryView() {
         super(TreeBidiMap.DataElement.VALUE);
      }

      public boolean contains(Object obj) {
         if (!(obj instanceof Entry)) {
            return false;
         }

         Entry<?, ?> entry = (Entry<?, ?>)obj;
         Object value = entry.getValue();
         TreeBidiMap.Node<K, V> node = TreeBidiMap.this.lookupValue(entry.getKey());
         return node != null && node.getKey().equals(value);
      }

      public boolean remove(Object obj) {
         if (!(obj instanceof Entry)) {
            return false;
         } else {
            Entry<?, ?> entry = (Entry<?, ?>)obj;
            Object value = entry.getValue();
            TreeBidiMap.Node<K, V> node = TreeBidiMap.this.lookupValue(entry.getKey());
            if (node != null && node.getKey().equals(value)) {
               TreeBidiMap.this.doRedBlackDelete(node);
               return true;
            } else {
               return false;
            }
         }
      }

      public Iterator<Entry<V, K>> iterator() {
         return TreeBidiMap.this.new InverseViewMapEntryIterator();
      }
   }

   class InverseViewMapEntryIterator extends TreeBidiMap<K, V>.ViewIterator implements OrderedIterator<Entry<V, K>> {
      InverseViewMapEntryIterator() {
         super(TreeBidiMap.DataElement.VALUE);
      }

      public Entry<V, K> next() {
         return this.createEntry(this.navigateNext());
      }

      public Entry<V, K> previous() {
         return this.createEntry(this.navigatePrevious());
      }

      private Entry<V, K> createEntry(TreeBidiMap.Node<K, V> node) {
         return new UnmodifiableMapEntry(node.getValue(), node.getKey());
      }
   }

   class InverseViewMapIterator extends TreeBidiMap<K, V>.ViewIterator implements OrderedMapIterator<V, K> {
      public InverseViewMapIterator(TreeBidiMap.DataElement orderType) {
         super(orderType);
      }

      public V getKey() {
         if (this.lastReturnedNode == null) {
            throw new IllegalStateException("Iterator getKey() can only be called after next() and before remove()");
         } else {
            return this.lastReturnedNode.getValue();
         }
      }

      public K getValue() {
         if (this.lastReturnedNode == null) {
            throw new IllegalStateException("Iterator getValue() can only be called after next() and before remove()");
         } else {
            return this.lastReturnedNode.getKey();
         }
      }

      public K setValue(K obj) {
         throw new UnsupportedOperationException();
      }

      public V next() {
         return (V)this.navigateNext().getValue();
      }

      public V previous() {
         return (V)this.navigatePrevious().getValue();
      }
   }

   class KeyView extends TreeBidiMap<K, V>.View<K> {
      public KeyView(TreeBidiMap.DataElement orderType) {
         super(orderType);
      }

      public Iterator<K> iterator() {
         return TreeBidiMap.this.new ViewMapIterator(this.orderType);
      }

      public boolean contains(Object obj) {
         TreeBidiMap.checkNonNullComparable(obj, TreeBidiMap.DataElement.KEY);
         return TreeBidiMap.this.lookupKey(obj) != null;
      }

      public boolean remove(Object o) {
         return TreeBidiMap.this.doRemoveKey(o) != null;
      }
   }

   static class Node<K extends Comparable<K>, V extends Comparable<V>> implements Entry<K, V>, KeyValue<K, V> {
      private final K key;
      private final V value;
      private final TreeBidiMap.Node<K, V>[] leftNode;
      private final TreeBidiMap.Node<K, V>[] rightNode;
      private final TreeBidiMap.Node<K, V>[] parentNode;
      private final boolean[] blackColor;
      private int hashcodeValue;
      private boolean calculatedHashCode;

      Node(K key, V value) {
         this.key = key;
         this.value = value;
         this.leftNode = new TreeBidiMap.Node[2];
         this.rightNode = new TreeBidiMap.Node[2];
         this.parentNode = new TreeBidiMap.Node[2];
         this.blackColor = new boolean[]{true, true};
         this.calculatedHashCode = false;
      }

      private Object getData(TreeBidiMap.DataElement dataElement) {
         switch (dataElement) {
            case KEY:
               return this.getKey();
            case VALUE:
               return this.getValue();
            default:
               throw new IllegalArgumentException();
         }
      }

      private void setLeft(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
         this.leftNode[dataElement.ordinal()] = node;
      }

      private TreeBidiMap.Node<K, V> getLeft(TreeBidiMap.DataElement dataElement) {
         return this.leftNode[dataElement.ordinal()];
      }

      private void setRight(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
         this.rightNode[dataElement.ordinal()] = node;
      }

      private TreeBidiMap.Node<K, V> getRight(TreeBidiMap.DataElement dataElement) {
         return this.rightNode[dataElement.ordinal()];
      }

      private void setParent(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
         this.parentNode[dataElement.ordinal()] = node;
      }

      private TreeBidiMap.Node<K, V> getParent(TreeBidiMap.DataElement dataElement) {
         return this.parentNode[dataElement.ordinal()];
      }

      private void swapColors(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
         this.blackColor[dataElement.ordinal()] ^= node.blackColor[dataElement.ordinal()];
         node.blackColor[dataElement.ordinal()] ^= this.blackColor[dataElement.ordinal()];
         this.blackColor[dataElement.ordinal()] ^= node.blackColor[dataElement.ordinal()];
      }

      private boolean isBlack(TreeBidiMap.DataElement dataElement) {
         return this.blackColor[dataElement.ordinal()];
      }

      private boolean isRed(TreeBidiMap.DataElement dataElement) {
         return !this.blackColor[dataElement.ordinal()];
      }

      private void setBlack(TreeBidiMap.DataElement dataElement) {
         this.blackColor[dataElement.ordinal()] = true;
      }

      private void setRed(TreeBidiMap.DataElement dataElement) {
         this.blackColor[dataElement.ordinal()] = false;
      }

      private void copyColor(TreeBidiMap.Node<K, V> node, TreeBidiMap.DataElement dataElement) {
         this.blackColor[dataElement.ordinal()] = node.blackColor[dataElement.ordinal()];
      }

      private boolean isLeftChild(TreeBidiMap.DataElement dataElement) {
         return this.parentNode[dataElement.ordinal()] != null && this.parentNode[dataElement.ordinal()].leftNode[dataElement.ordinal()] == this;
      }

      private boolean isRightChild(TreeBidiMap.DataElement dataElement) {
         return this.parentNode[dataElement.ordinal()] != null && this.parentNode[dataElement.ordinal()].rightNode[dataElement.ordinal()] == this;
      }

      public K getKey() {
         return this.key;
      }

      public V getValue() {
         return this.value;
      }

      public V setValue(V ignored) throws UnsupportedOperationException {
         throw new UnsupportedOperationException("Map.Entry.setValue is not supported");
      }

      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }

         if (!(obj instanceof Entry)) {
            return false;
         }

         Entry<?, ?> e = (Entry<?, ?>)obj;
         return this.getKey().equals(e.getKey()) && this.getValue().equals(e.getValue());
      }

      public int hashCode() {
         if (!this.calculatedHashCode) {
            this.hashcodeValue = this.getKey().hashCode() ^ this.getValue().hashCode();
            this.calculatedHashCode = true;
         }

         return this.hashcodeValue;
      }
   }

   class ValueView extends TreeBidiMap<K, V>.View<V> {
      public ValueView(TreeBidiMap.DataElement orderType) {
         super(orderType);
      }

      public Iterator<V> iterator() {
         return TreeBidiMap.this.new InverseViewMapIterator(this.orderType);
      }

      public boolean contains(Object obj) {
         TreeBidiMap.checkNonNullComparable(obj, TreeBidiMap.DataElement.VALUE);
         return TreeBidiMap.this.lookupValue(obj) != null;
      }

      public boolean remove(Object o) {
         return TreeBidiMap.this.doRemoveValue(o) != null;
      }
   }

   abstract class View<E> extends AbstractSet<E> {
      final TreeBidiMap.DataElement orderType;

      View(TreeBidiMap.DataElement orderType) {
         this.orderType = orderType;
      }

      public int size() {
         return TreeBidiMap.this.size();
      }

      public void clear() {
         TreeBidiMap.this.clear();
      }
   }

   abstract class ViewIterator {
      private final TreeBidiMap.DataElement orderType;
      TreeBidiMap.Node<K, V> lastReturnedNode;
      private TreeBidiMap.Node<K, V> nextNode;
      private TreeBidiMap.Node<K, V> previousNode;
      private int expectedModifications;

      ViewIterator(TreeBidiMap.DataElement orderType) {
         this.orderType = orderType;
         this.expectedModifications = TreeBidiMap.this.modifications;
         this.nextNode = TreeBidiMap.this.leastNode(TreeBidiMap.this.rootNode[orderType.ordinal()], orderType);
         this.lastReturnedNode = null;
         this.previousNode = null;
      }

      public final boolean hasNext() {
         return this.nextNode != null;
      }

      protected TreeBidiMap.Node<K, V> navigateNext() {
         if (this.nextNode == null) {
            throw new NoSuchElementException();
         }

         if (TreeBidiMap.this.modifications != this.expectedModifications) {
            throw new ConcurrentModificationException();
         }

         this.lastReturnedNode = this.nextNode;
         this.previousNode = this.nextNode;
         this.nextNode = TreeBidiMap.this.nextGreater(this.nextNode, this.orderType);
         return this.lastReturnedNode;
      }

      public boolean hasPrevious() {
         return this.previousNode != null;
      }

      protected TreeBidiMap.Node<K, V> navigatePrevious() {
         if (this.previousNode == null) {
            throw new NoSuchElementException();
         }

         if (TreeBidiMap.this.modifications != this.expectedModifications) {
            throw new ConcurrentModificationException();
         }

         this.nextNode = this.lastReturnedNode;
         if (this.nextNode == null) {
            this.nextNode = TreeBidiMap.this.nextGreater(this.previousNode, this.orderType);
         }

         this.lastReturnedNode = this.previousNode;
         this.previousNode = TreeBidiMap.this.nextSmaller(this.previousNode, this.orderType);
         return this.lastReturnedNode;
      }

      public final void remove() {
         if (this.lastReturnedNode == null) {
            throw new IllegalStateException();
         }

         if (TreeBidiMap.this.modifications != this.expectedModifications) {
            throw new ConcurrentModificationException();
         }

         TreeBidiMap.this.doRedBlackDelete(this.lastReturnedNode);
         this.expectedModifications++;
         this.lastReturnedNode = null;
         if (this.nextNode == null) {
            this.previousNode = TreeBidiMap.this.greatestNode(TreeBidiMap.this.rootNode[this.orderType.ordinal()], this.orderType);
         } else {
            this.previousNode = TreeBidiMap.this.nextSmaller(this.nextNode, this.orderType);
         }
      }
   }

   class ViewMapEntryIterator extends TreeBidiMap<K, V>.ViewIterator implements OrderedIterator<Entry<K, V>> {
      ViewMapEntryIterator() {
         super(TreeBidiMap.DataElement.KEY);
      }

      public Entry<K, V> next() {
         return this.navigateNext();
      }

      public Entry<K, V> previous() {
         return this.navigatePrevious();
      }
   }

   class ViewMapIterator extends TreeBidiMap<K, V>.ViewIterator implements OrderedMapIterator<K, V> {
      ViewMapIterator(TreeBidiMap.DataElement orderType) {
         super(orderType);
      }

      public K getKey() {
         if (this.lastReturnedNode == null) {
            throw new IllegalStateException("Iterator getKey() can only be called after next() and before remove()");
         } else {
            return this.lastReturnedNode.getKey();
         }
      }

      public V getValue() {
         if (this.lastReturnedNode == null) {
            throw new IllegalStateException("Iterator getValue() can only be called after next() and before remove()");
         } else {
            return this.lastReturnedNode.getValue();
         }
      }

      public V setValue(V obj) {
         throw new UnsupportedOperationException();
      }

      public K next() {
         return (K)this.navigateNext().getKey();
      }

      public K previous() {
         return (K)this.navigatePrevious().getKey();
      }
   }
}
