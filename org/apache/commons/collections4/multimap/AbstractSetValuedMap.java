package org.apache.commons.collections4.multimap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.SetValuedMap;

public abstract class AbstractSetValuedMap<K, V> extends AbstractMultiValuedMap<K, V> implements SetValuedMap<K, V> {
   protected AbstractSetValuedMap() {
   }

   protected AbstractSetValuedMap(Map<K, ? extends Set<V>> map) {
      super(map);
   }

   protected Map<K, Set<V>> getMap() {
      return super.getMap();
   }

   protected abstract Set<V> createCollection();

   public Set<V> get(K key) {
      return this.wrappedCollection(key);
   }

   Set<V> wrappedCollection(K key) {
      return new AbstractSetValuedMap.WrappedSet(key);
   }

   public Set<V> remove(Object key) {
      return SetUtils.emptyIfNull((Set)this.getMap().remove(key));
   }

   private class WrappedSet extends AbstractMultiValuedMap<K, V>.WrappedCollection implements Set<V> {
      public WrappedSet(K key) {
         super(AbstractSetValuedMap.this, key);
      }

      public boolean equals(Object other) {
         Set<V> set = (Set<V>)this.getMapping();
         if (set == null) {
            return Collections.emptySet().equals(other);
         }

         if (!(other instanceof Set)) {
            return false;
         }

         Set<?> otherSet = (Set<?>)other;
         return SetUtils.isEqualSet(set, otherSet);
      }

      public int hashCode() {
         Set<V> set = (Set<V>)this.getMapping();
         return SetUtils.hashCodeForSet(set);
      }
   }
}
