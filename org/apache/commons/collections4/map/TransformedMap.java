package org.apache.commons.collections4.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections4.Transformer;

public class TransformedMap<K, V> extends AbstractInputCheckedMapDecorator<K, V> implements Serializable {
   private static final long serialVersionUID = 7023152376788900464L;
   protected final Transformer<? super K, ? extends K> keyTransformer;
   protected final Transformer<? super V, ? extends V> valueTransformer;

   public static <K, V> TransformedMap<K, V> transformingMap(
      Map<K, V> map, Transformer<? super K, ? extends K> keyTransformer, Transformer<? super V, ? extends V> valueTransformer
   ) {
      return new TransformedMap<>(map, keyTransformer, valueTransformer);
   }

   public static <K, V> TransformedMap<K, V> transformedMap(
      Map<K, V> map, Transformer<? super K, ? extends K> keyTransformer, Transformer<? super V, ? extends V> valueTransformer
   ) {
      TransformedMap<K, V> decorated = new TransformedMap<>(map, keyTransformer, valueTransformer);
      if (map.size() > 0) {
         Map<K, V> transformed = decorated.transformMap(map);
         decorated.clear();
         decorated.decorated().putAll(transformed);
      }

      return decorated;
   }

   protected TransformedMap(Map<K, V> map, Transformer<? super K, ? extends K> keyTransformer, Transformer<? super V, ? extends V> valueTransformer) {
      super(map);
      this.keyTransformer = keyTransformer;
      this.valueTransformer = valueTransformer;
   }

   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      out.writeObject(this.map);
   }

   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      this.map = (Map)in.readObject();
   }

   protected K transformKey(K object) {
      return (K)(this.keyTransformer == null ? object : this.keyTransformer.transform(object));
   }

   protected V transformValue(V object) {
      return (V)(this.valueTransformer == null ? object : this.valueTransformer.transform(object));
   }

   protected Map<K, V> transformMap(Map<? extends K, ? extends V> map) {
      if (map.isEmpty()) {
         return (Map<K, V>)map;
      }

      Map<K, V> result = new LinkedMap(map.size());

      for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
         result.put(this.transformKey((K)entry.getKey()), this.transformValue((V)entry.getValue()));
      }

      return result;
   }

   protected V checkSetValue(V value) {
      return (V)this.valueTransformer.transform(value);
   }

   protected boolean isSetValueChecking() {
      return this.valueTransformer != null;
   }

   public V put(K key, V value) {
      key = this.transformKey(key);
      value = this.transformValue(value);
      return (V)this.decorated().put(key, value);
   }

   public void putAll(Map<? extends K, ? extends V> mapToCopy) {
      mapToCopy = this.transformMap(mapToCopy);
      this.decorated().putAll(mapToCopy);
   }
}
