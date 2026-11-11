package org.apache.commons.collections4;

import java.util.Set;

public interface BidiMap<K, V> extends IterableMap<K, V> {
   V put(K var1, V var2);

   K getKey(Object var1);

   K removeValue(Object var1);

   BidiMap<V, K> inverseBidiMap();

   Set<V> values();
}
