package org.apache.commons.collections4.map;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.collections4.KeyValue;

public final class StaticBucketMap<K, V> extends AbstractIterableMap<K, V> {
   private static final int DEFAULT_BUCKETS = 255;
   private final StaticBucketMap.Node<K, V>[] buckets;
   private final StaticBucketMap.Lock[] locks;

   public StaticBucketMap() {
      this(255);
   }

   public StaticBucketMap(int numBuckets) {
      int size = Math.max(17, numBuckets);
      if (size % 2 == 0) {
         size--;
      }

      this.buckets = new StaticBucketMap.Node[size];
      this.locks = new StaticBucketMap.Lock[size];

      for (int i = 0; i < size; i++) {
         this.locks[i] = new StaticBucketMap.Lock();
      }
   }

   private int getHash(Object key) {
      if (key == null) {
         return 0;
      }

      int hash = key.hashCode();
      hash += ~(hash << 15);
      hash ^= hash >>> 10;
      hash += hash << 3;
      hash ^= hash >>> 6;
      hash += ~(hash << 11);
      hash ^= hash >>> 16;
      hash %= this.buckets.length;
      return hash < 0 ? hash * -1 : hash;
   }

   public int size() {
      int cnt = 0;

      for (int i = 0; i < this.buckets.length; i++) {
         synchronized (this.locks[i]) {
            cnt += this.locks[i].size;
         }
      }

      return cnt;
   }

   public boolean isEmpty() {
      return this.size() == 0;
   }

   public V get(Object key) {
      int hash = this.getHash(key);
      synchronized (this.locks[hash]) {
         for (StaticBucketMap.Node<K, V> n = this.buckets[hash]; n != null; n = n.next) {
            if (n.key == key || n.key != null && n.key.equals(key)) {
               return n.value;
            }
         }

         return null;
      }
   }

   public boolean containsKey(Object key) {
      int hash = this.getHash(key);
      synchronized (this.locks[hash]) {
         for (StaticBucketMap.Node<K, V> n = this.buckets[hash]; n != null; n = n.next) {
            if (n.key == key || n.key != null && n.key.equals(key)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean containsValue(Object value) {
      for (int i = 0; i < this.buckets.length; i++) {
         synchronized (this.locks[i]) {
            for (StaticBucketMap.Node<K, V> n = this.buckets[i]; n != null; n = n.next) {
               if (n.value == value || n.value != null && n.value.equals(value)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public V put(K key, V value) {
      int hash = this.getHash(key);
      synchronized (this.locks[hash]) {
         StaticBucketMap.Node<K, V> n = this.buckets[hash];
         if (n == null) {
            n = new StaticBucketMap.Node<>();
            n.key = key;
            n.value = value;
            this.buckets[hash] = n;
            this.locks[hash].size++;
            return null;
         }

         for (StaticBucketMap.Node<K, V> next = n; next != null; next = next.next) {
            n = next;
            if (n.key == key || n.key != null && n.key.equals(key)) {
               V returnVal = n.value;
               n.value = value;
               return returnVal;
            }
         }

         StaticBucketMap.Node<K, V> newNode = new StaticBucketMap.Node<>();
         newNode.key = key;
         newNode.value = value;
         n.next = newNode;
         this.locks[hash].size++;
         return null;
      }
   }

   public V remove(Object key) {
      int hash = this.getHash(key);
      synchronized (this.locks[hash]) {
         StaticBucketMap.Node<K, V> n = this.buckets[hash];
         StaticBucketMap.Node<K, V> prev = null;

         while (n != null) {
            if (n.key == key || n.key != null && n.key.equals(key)) {
               if (null == prev) {
                  this.buckets[hash] = n.next;
               } else {
                  prev.next = n.next;
               }

               this.locks[hash].size--;
               return n.value;
            }

            prev = n;
            n = n.next;
         }

         return null;
      }
   }

   public Set<K> keySet() {
      return new StaticBucketMap.KeySet();
   }

   public Collection<V> values() {
      return new StaticBucketMap.Values();
   }

   public Set<Entry<K, V>> entrySet() {
      return new StaticBucketMap.EntrySet();
   }

   public void putAll(Map<? extends K, ? extends V> map) {
      for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
         this.put((K)entry.getKey(), (V)entry.getValue());
      }
   }

   public void clear() {
      for (int i = 0; i < this.buckets.length; i++) {
         StaticBucketMap.Lock lock = this.locks[i];
         synchronized (lock) {
            this.buckets[i] = null;
            lock.size = 0;
         }
      }
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }

      if (!(obj instanceof Map)) {
         return false;
      }

      Map<?, ?> other = (Map<?, ?>)obj;
      return this.entrySet().equals(other.entrySet());
   }

   public int hashCode() {
      int hashCode = 0;

      for (int i = 0; i < this.buckets.length; i++) {
         synchronized (this.locks[i]) {
            for (StaticBucketMap.Node<K, V> n = this.buckets[i]; n != null; n = n.next) {
               hashCode += n.hashCode();
            }
         }
      }

      return hashCode;
   }

   public void atomic(Runnable r) {
      if (r == null) {
         throw new NullPointerException();
      }

      this.atomic(r, 0);
   }

   private void atomic(Runnable r, int bucket) {
      if (bucket >= this.buckets.length) {
         r.run();
      } else {
         synchronized (this.locks[bucket]) {
            this.atomic(r, bucket + 1);
         }
      }
   }

   private class BaseIterator {
      private final ArrayList<Entry<K, V>> current = new ArrayList();
      private int bucket;
      private Entry<K, V> last;

      private BaseIterator() {
      }

      public boolean hasNext() {
         if (this.current.size() > 0) {
            return true;
         }

         while (this.bucket < StaticBucketMap.this.buckets.length) {
            synchronized (StaticBucketMap.this.locks[this.bucket]) {
               for (StaticBucketMap.Node<K, V> n = StaticBucketMap.this.buckets[this.bucket]; n != null; n = n.next) {
                  this.current.add(n);
               }

               this.bucket++;
               if (this.current.size() > 0) {
                  return true;
               }
            }
         }

         return false;
      }

      protected Entry<K, V> nextEntry() {
         if (!this.hasNext()) {
            throw new NoSuchElementException();
         }

         this.last = (Entry<K, V>)this.current.remove(this.current.size() - 1);
         return this.last;
      }

      public void remove() {
         if (this.last == null) {
            throw new IllegalStateException();
         }

         StaticBucketMap.this.remove(this.last.getKey());
         this.last = null;
      }
   }

   private class EntryIterator extends StaticBucketMap<K, V>.BaseIterator implements Iterator<Entry<K, V>> {
      private EntryIterator() {
      }

      public Entry<K, V> next() {
         return this.nextEntry();
      }
   }

   private class EntrySet extends AbstractSet<Entry<K, V>> {
      private EntrySet() {
      }

      public int size() {
         return StaticBucketMap.this.size();
      }

      public void clear() {
         StaticBucketMap.this.clear();
      }

      public Iterator<Entry<K, V>> iterator() {
         return StaticBucketMap.this.new EntryIterator();
      }

      public boolean contains(Object obj) {
         Entry<?, ?> entry = (Entry<?, ?>)obj;
         int hash = StaticBucketMap.this.getHash(entry.getKey());
         synchronized (StaticBucketMap.this.locks[hash]) {
            for (StaticBucketMap.Node<K, V> n = StaticBucketMap.this.buckets[hash]; n != null; n = n.next) {
               if (n.equals(entry)) {
                  return true;
               }
            }

            return false;
         }
      }

      public boolean remove(Object obj) {
         if (!(obj instanceof Entry)) {
            return false;
         }

         Entry<?, ?> entry = (Entry<?, ?>)obj;
         int hash = StaticBucketMap.this.getHash(entry.getKey());
         synchronized (StaticBucketMap.this.locks[hash]) {
            for (StaticBucketMap.Node<K, V> n = StaticBucketMap.this.buckets[hash]; n != null; n = n.next) {
               if (n.equals(entry)) {
                  StaticBucketMap.this.remove(n.getKey());
                  return true;
               }
            }

            return false;
         }
      }
   }

   private class KeyIterator extends StaticBucketMap<K, V>.BaseIterator implements Iterator<K> {
      private KeyIterator() {
      }

      public K next() {
         return (K)this.nextEntry().getKey();
      }
   }

   private class KeySet extends AbstractSet<K> {
      private KeySet() {
      }

      public int size() {
         return StaticBucketMap.this.size();
      }

      public void clear() {
         StaticBucketMap.this.clear();
      }

      public Iterator<K> iterator() {
         return StaticBucketMap.this.new KeyIterator();
      }

      public boolean contains(Object obj) {
         return StaticBucketMap.this.containsKey(obj);
      }

      public boolean remove(Object obj) {
         int hash = StaticBucketMap.this.getHash(obj);
         synchronized (StaticBucketMap.this.locks[hash]) {
            for (StaticBucketMap.Node<K, V> n = StaticBucketMap.this.buckets[hash]; n != null; n = n.next) {
               Object k = n.getKey();
               if (k == obj || k != null && k.equals(obj)) {
                  StaticBucketMap.this.remove(k);
                  return true;
               }
            }

            return false;
         }
      }
   }

   private static final class Lock {
      public int size;

      private Lock() {
      }
   }

   private static final class Node<K, V> implements Entry<K, V>, KeyValue<K, V> {
      protected K key;
      protected V value;
      protected StaticBucketMap.Node<K, V> next;

      private Node() {
      }

      public K getKey() {
         return this.key;
      }

      public V getValue() {
         return this.value;
      }

      public int hashCode() {
         return (this.key == null ? 0 : this.key.hashCode()) ^ (this.value == null ? 0 : this.value.hashCode());
      }

      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }

         if (!(obj instanceof Entry)) {
            return false;
         }

         Entry<?, ?> e2 = (Entry<?, ?>)obj;
         return (this.key == null ? e2.getKey() == null : this.key.equals(e2.getKey()))
            && (this.value == null ? e2.getValue() == null : this.value.equals(e2.getValue()));
      }

      public V setValue(V obj) {
         V retVal = this.value;
         this.value = obj;
         return retVal;
      }
   }

   private class ValueIterator extends StaticBucketMap<K, V>.BaseIterator implements Iterator<V> {
      private ValueIterator() {
      }

      public V next() {
         return (V)this.nextEntry().getValue();
      }
   }

   private class Values extends AbstractCollection<V> {
      private Values() {
      }

      public int size() {
         return StaticBucketMap.this.size();
      }

      public void clear() {
         StaticBucketMap.this.clear();
      }

      public Iterator<V> iterator() {
         return StaticBucketMap.this.new ValueIterator();
      }
   }
}
