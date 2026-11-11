package org.apache.commons.collections4;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.collections4.collection.UnmodifiableCollection;
import org.apache.commons.collections4.iterators.UnmodifiableMapIterator;
import org.apache.commons.collections4.map.EntrySetToMapIteratorAdapter;
import org.apache.commons.collections4.map.UnmodifiableEntrySet;
import org.apache.commons.collections4.set.UnmodifiableSet;

public class SplitMapUtils {
   private SplitMapUtils() {
   }

   public static <K, V> IterableMap<K, V> readableMap(Get<K, V> get) {
      if (get == null) {
         throw new NullPointerException("Get must not be null");
      } else if (get instanceof Map) {
         return get instanceof IterableMap ? (IterableMap)get : MapUtils.iterableMap((Map)get);
      } else {
         return new SplitMapUtils.WrappedGet<>(get);
      }
   }

   public static <K, V> Map<K, V> writableMap(Put<K, V> put) {
      if (put == null) {
         throw new NullPointerException("Put must not be null");
      } else {
         return put instanceof Map ? (Map)put : new SplitMapUtils.WrappedPut<>(put);
      }
   }

   private static class WrappedGet<K, V> implements IterableMap<K, V>, Unmodifiable {
      private final Get<K, V> get;

      private WrappedGet(Get<K, V> get) {
         this.get = get;
      }

      public void clear() {
         throw new UnsupportedOperationException();
      }

      public boolean containsKey(Object key) {
         return this.get.containsKey(key);
      }

      public boolean containsValue(Object value) {
         return this.get.containsValue(value);
      }

      public Set<Entry<K, V>> entrySet() {
         return UnmodifiableEntrySet.unmodifiableEntrySet(this.get.entrySet());
      }

      public boolean equals(Object arg0) {
         return arg0 == this ? true : arg0 instanceof SplitMapUtils.WrappedGet && ((SplitMapUtils.WrappedGet)arg0).get.equals(this.get);
      }

      public V get(Object key) {
         return (V)this.get.get(key);
      }

      public int hashCode() {
         return "WrappedGet".hashCode() << 4 | this.get.hashCode();
      }

      public boolean isEmpty() {
         return this.get.isEmpty();
      }

      public Set<K> keySet() {
         return UnmodifiableSet.unmodifiableSet(this.get.keySet());
      }

      public V put(K key, V value) {
         throw new UnsupportedOperationException();
      }

      public void putAll(Map<? extends K, ? extends V> t) {
         throw new UnsupportedOperationException();
      }

      public V remove(Object key) {
         return (V)this.get.remove(key);
      }

      public int size() {
         return this.get.size();
      }

      public Collection<V> values() {
         return UnmodifiableCollection.unmodifiableCollection(this.get.values());
      }

      public MapIterator<K, V> mapIterator() {
         MapIterator<K, V> it;
         if (this.get instanceof IterableGet) {
            it = ((IterableGet)this.get).mapIterator();
         } else {
            it = new EntrySetToMapIteratorAdapter(this.get.entrySet());
         }

         return UnmodifiableMapIterator.unmodifiableMapIterator(it);
      }
   }

   private static class WrappedPut<K, V> implements Map<K, V>, Put<K, V> {
      private final Put<K, V> put;

      private WrappedPut(Put<K, V> put) {
         this.put = put;
      }

      public void clear() {
         this.put.clear();
      }

      public boolean containsKey(Object key) {
         throw new UnsupportedOperationException();
      }

      public boolean containsValue(Object value) {
         throw new UnsupportedOperationException();
      }

      public Set<Entry<K, V>> entrySet() {
         throw new UnsupportedOperationException();
      }

      public boolean equals(Object obj) {
         return obj == this ? true : obj instanceof SplitMapUtils.WrappedPut && ((SplitMapUtils.WrappedPut)obj).put.equals(this.put);
      }

      public V get(Object key) {
         throw new UnsupportedOperationException();
      }

      public int hashCode() {
         return "WrappedPut".hashCode() << 4 | this.put.hashCode();
      }

      public boolean isEmpty() {
         throw new UnsupportedOperationException();
      }

      public Set<K> keySet() {
         throw new UnsupportedOperationException();
      }

      public V put(K key, V value) {
         return (V)this.put.put(key, value);
      }

      public void putAll(Map<? extends K, ? extends V> t) {
         this.put.putAll(t);
      }

      public V remove(Object key) {
         throw new UnsupportedOperationException();
      }

      public int size() {
         throw new UnsupportedOperationException();
      }

      public Collection<V> values() {
         throw new UnsupportedOperationException();
      }
   }
}
