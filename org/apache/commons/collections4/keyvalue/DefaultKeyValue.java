package org.apache.commons.collections4.keyvalue;

import java.util.Map.Entry;
import org.apache.commons.collections4.KeyValue;

public class DefaultKeyValue<K, V> extends AbstractKeyValue<K, V> {
   public DefaultKeyValue() {
      super(null, null);
   }

   public DefaultKeyValue(K key, V value) {
      super(key, value);
   }

   public DefaultKeyValue(KeyValue<? extends K, ? extends V> pair) {
      super(pair.getKey(), pair.getValue());
   }

   public DefaultKeyValue(Entry<? extends K, ? extends V> entry) {
      super(entry.getKey(), entry.getValue());
   }

   public K setKey(K key) {
      if (key == this) {
         throw new IllegalArgumentException("DefaultKeyValue may not contain itself as a key.");
      } else {
         return (K)super.setKey(key);
      }
   }

   public V setValue(V value) {
      if (value == this) {
         throw new IllegalArgumentException("DefaultKeyValue may not contain itself as a value.");
      } else {
         return (V)super.setValue(value);
      }
   }

   public Entry<K, V> toMapEntry() {
      return new DefaultMapEntry(this);
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }

      if (!(obj instanceof DefaultKeyValue)) {
         return false;
      }

      DefaultKeyValue<?, ?> other = (DefaultKeyValue<?, ?>)obj;
      return (this.getKey() == null ? other.getKey() == null : this.getKey().equals(other.getKey()))
         && (this.getValue() == null ? other.getValue() == null : this.getValue().equals(other.getValue()));
   }

   public int hashCode() {
      return (this.getKey() == null ? 0 : this.getKey().hashCode()) ^ (this.getValue() == null ? 0 : this.getValue().hashCode());
   }
}
