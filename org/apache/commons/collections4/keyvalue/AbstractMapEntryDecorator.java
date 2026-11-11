package org.apache.commons.collections4.keyvalue;

import java.util.Map.Entry;
import org.apache.commons.collections4.KeyValue;

public abstract class AbstractMapEntryDecorator<K, V> implements Entry<K, V>, KeyValue<K, V> {
   private final Entry<K, V> entry;

   public AbstractMapEntryDecorator(Entry<K, V> entry) {
      if (entry == null) {
         throw new NullPointerException("Map Entry must not be null.");
      }

      this.entry = entry;
   }

   protected Entry<K, V> getMapEntry() {
      return this.entry;
   }

   public K getKey() {
      return (K)this.entry.getKey();
   }

   public V getValue() {
      return (V)this.entry.getValue();
   }

   public V setValue(V object) {
      return (V)this.entry.setValue(object);
   }

   public boolean equals(Object object) {
      return object == this ? true : this.entry.equals(object);
   }

   public int hashCode() {
      return this.entry.hashCode();
   }

   public String toString() {
      return this.entry.toString();
   }
}
