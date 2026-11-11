package org.apache.commons.collections4.splitmap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.collections4.IterableGet;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.EntrySetToMapIteratorAdapter;

public class AbstractIterableGetMapDecorator<K, V> implements IterableGet<K, V> {
   transient Map<K, V> map;

   public AbstractIterableGetMapDecorator(Map<K, V> map) {
      if (map == null) {
         throw new NullPointerException("Map must not be null.");
      }

      this.map = map;
   }

   protected AbstractIterableGetMapDecorator() {
   }

   protected Map<K, V> decorated() {
      return this.map;
   }

   public boolean containsKey(Object key) {
      return this.decorated().containsKey(key);
   }

   public boolean containsValue(Object value) {
      return this.decorated().containsValue(value);
   }

   public Set<Entry<K, V>> entrySet() {
      return this.decorated().entrySet();
   }

   public V get(Object key) {
      return (V)this.decorated().get(key);
   }

   public V remove(Object key) {
      return (V)this.decorated().remove(key);
   }

   public boolean isEmpty() {
      return this.decorated().isEmpty();
   }

   public Set<K> keySet() {
      return this.decorated().keySet();
   }

   public int size() {
      return this.decorated().size();
   }

   public Collection<V> values() {
      return this.decorated().values();
   }

   public MapIterator<K, V> mapIterator() {
      return new EntrySetToMapIteratorAdapter(this.entrySet());
   }

   public boolean equals(Object object) {
      return object == this ? true : this.decorated().equals(object);
   }

   public int hashCode() {
      return this.decorated().hashCode();
   }

   public String toString() {
      return this.decorated().toString();
   }
}
