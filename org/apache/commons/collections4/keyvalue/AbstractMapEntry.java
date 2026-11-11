package org.apache.commons.collections4.keyvalue;

import java.util.Map.Entry;

public abstract class AbstractMapEntry<K, V> extends AbstractKeyValue<K, V> implements Entry<K, V> {
   protected AbstractMapEntry(K key, V value) {
      super(key, value);
   }

   public V setValue(V value) {
      return (V)super.setValue(value);
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }

      if (!(obj instanceof Entry)) {
         return false;
      }

      Entry<?, ?> other = (Entry<?, ?>)obj;
      return (this.getKey() == null ? other.getKey() == null : this.getKey().equals(other.getKey()))
         && (this.getValue() == null ? other.getValue() == null : this.getValue().equals(other.getValue()));
   }

   public int hashCode() {
      return (this.getKey() == null ? 0 : this.getKey().hashCode()) ^ (this.getValue() == null ? 0 : this.getValue().hashCode());
   }
}
