package org.apache.commons.collections4;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public interface MultiValuedMap<K, V> {
   int size();

   boolean isEmpty();

   boolean containsKey(Object var1);

   boolean containsValue(Object var1);

   boolean containsMapping(Object var1, Object var2);

   Collection<V> get(K var1);

   boolean put(K var1, V var2);

   boolean putAll(K var1, Iterable<? extends V> var2);

   boolean putAll(Map<? extends K, ? extends V> var1);

   boolean putAll(MultiValuedMap<? extends K, ? extends V> var1);

   Collection<V> remove(Object var1);

   boolean removeMapping(Object var1, Object var2);

   void clear();

   Collection<Entry<K, V>> entries();

   MultiSet<K> keys();

   Set<K> keySet();

   Collection<V> values();

   Map<K, Collection<V>> asMap();

   MapIterator<K, V> mapIterator();
}
