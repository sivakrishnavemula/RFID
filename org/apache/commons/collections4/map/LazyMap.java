package org.apache.commons.collections4.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.functors.FactoryTransformer;

public class LazyMap<K, V> extends AbstractMapDecorator<K, V> implements Serializable {
   private static final long serialVersionUID = 7990956402564206740L;
   protected final Transformer<? super K, ? extends V> factory;

   public static <K, V> LazyMap<K, V> lazyMap(Map<K, V> map, Factory<? extends V> factory) {
      return new LazyMap<>(map, factory);
   }

   public static <V, K> LazyMap<K, V> lazyMap(Map<K, V> map, Transformer<? super K, ? extends V> factory) {
      return new LazyMap<>(map, factory);
   }

   protected LazyMap(Map<K, V> map, Factory<? extends V> factory) {
      super(map);
      if (factory == null) {
         throw new NullPointerException("Factory must not be null");
      }

      this.factory = FactoryTransformer.factoryTransformer(factory);
   }

   protected LazyMap(Map<K, V> map, Transformer<? super K, ? extends V> factory) {
      super(map);
      if (factory == null) {
         throw new NullPointerException("Factory must not be null");
      }

      this.factory = factory;
   }

   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      out.writeObject(this.map);
   }

   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      this.map = (Map)in.readObject();
   }

   public V get(Object key) {
      if (!this.map.containsKey(key)) {
         K castKey = (K)key;
         V value = (V)this.factory.transform(castKey);
         this.map.put(castKey, value);
         return value;
      } else {
         return (V)this.map.get(key);
      }
   }
}
