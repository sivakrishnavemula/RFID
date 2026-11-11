package org.apache.commons.collections4.iterators;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.ResettableIterator;

public class EntrySetMapIterator<K, V> implements MapIterator<K, V>, ResettableIterator<K> {
   private final Map<K, V> map;
   private Iterator<Entry<K, V>> iterator;
   private Entry<K, V> last;
   private boolean canRemove = false;

   public EntrySetMapIterator(Map<K, V> map) {
      this.map = map;
      this.iterator = map.entrySet().iterator();
   }

   public boolean hasNext() {
      return this.iterator.hasNext();
   }

   public K next() {
      this.last = (Entry<K, V>)this.iterator.next();
      this.canRemove = true;
      return (K)this.last.getKey();
   }

   public void remove() {
      if (!this.canRemove) {
         throw new IllegalStateException("Iterator remove() can only be called once after next()");
      }

      this.iterator.remove();
      this.last = null;
      this.canRemove = false;
   }

   public K getKey() {
      if (this.last == null) {
         throw new IllegalStateException("Iterator getKey() can only be called after next() and before remove()");
      } else {
         return (K)this.last.getKey();
      }
   }

   public V getValue() {
      if (this.last == null) {
         throw new IllegalStateException("Iterator getValue() can only be called after next() and before remove()");
      } else {
         return (V)this.last.getValue();
      }
   }

   public V setValue(V value) {
      if (this.last == null) {
         throw new IllegalStateException("Iterator setValue() can only be called after next() and before remove()");
      } else {
         return (V)this.last.setValue(value);
      }
   }

   public void reset() {
      this.iterator = this.map.entrySet().iterator();
      this.last = null;
      this.canRemove = false;
   }

   public String toString() {
      return this.last != null ? "MapIterator[" + this.getKey() + "=" + this.getValue() + "]" : "MapIterator[]";
   }
}
