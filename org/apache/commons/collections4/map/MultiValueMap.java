package org.apache.commons.collections4.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.FunctorException;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.iterators.EmptyIterator;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.collections4.iterators.LazyIteratorChain;
import org.apache.commons.collections4.iterators.TransformIterator;

@Deprecated
public class MultiValueMap<K, V> extends AbstractMapDecorator<K, Object> implements MultiMap<K, V>, Serializable {
   private static final long serialVersionUID = -2214159910087182007L;
   private final Factory<? extends Collection<V>> collectionFactory;
   private transient Collection<V> valuesView;

   public static <K, V> MultiValueMap<K, V> multiValueMap(Map<K, ? super Collection<V>> map) {
      return multiValueMap(map, ArrayList.class);
   }

   public static <K, V, C extends Collection<V>> MultiValueMap<K, V> multiValueMap(Map<K, ? super C> map, Class<C> collectionClass) {
      return new MultiValueMap<>(map, new MultiValueMap.ReflectionFactory<>(collectionClass));
   }

   public static <K, V, C extends Collection<V>> MultiValueMap<K, V> multiValueMap(Map<K, ? super C> map, Factory<C> collectionFactory) {
      return new MultiValueMap<>(map, collectionFactory);
   }

   public MultiValueMap() {
      this(new HashMap(), new MultiValueMap.ReflectionFactory(ArrayList.class));
   }

   protected <C extends Collection<V>> MultiValueMap(Map<K, ? super C> map, Factory<C> collectionFactory) {
      super(map);
      if (collectionFactory == null) {
         throw new IllegalArgumentException("The factory must not be null");
      }

      this.collectionFactory = collectionFactory;
   }

   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      out.writeObject(this.map);
   }

   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      this.map = (Map)in.readObject();
   }

   public void clear() {
      this.decorated().clear();
   }

   public boolean removeMapping(Object key, Object value) {
      Collection<V> valuesForKey = this.getCollection(key);
      if (valuesForKey == null) {
         return false;
      }

      boolean removed = valuesForKey.remove(value);
      if (!removed) {
         return false;
      }

      if (valuesForKey.isEmpty()) {
         this.remove(key);
      }

      return true;
   }

   public boolean containsValue(Object value) {
      Set<Entry<K, Object>> pairs = this.decorated().entrySet();
      if (pairs != null) {
         for (Entry<K, Object> entry : pairs) {
            if (((Collection)entry.getValue()).contains(value)) {
               return true;
            }
         }
      }

      return false;
   }

   public Object put(K key, Object value) {
      boolean result = false;
      Collection<V> coll = this.getCollection(key);
      if (coll == null) {
         coll = this.createCollection(1);
         coll.add(value);
         if (coll.size() > 0) {
            this.decorated().put(key, coll);
            result = true;
         }
      } else {
         result = coll.add(value);
      }

      return result ? value : null;
   }

   public void putAll(Map<? extends K, ?> map) {
      if (map instanceof MultiMap) {
         for (Entry<? extends K, Object> entry : ((MultiMap)map).entrySet()) {
            this.putAll((K)entry.getKey(), (Collection<V>)entry.getValue());
         }
      } else {
         for (Entry<? extends K, ?> entry : map.entrySet()) {
            this.put((K)entry.getKey(), entry.getValue());
         }
      }
   }

   public Set<Entry<K, Object>> entrySet() {
      return super.entrySet();
   }

   public Collection<Object> values() {
      Collection<V> vs = this.valuesView;
      return vs != null ? vs : (this.valuesView = new MultiValueMap.Values());
   }

   public boolean containsValue(Object key, Object value) {
      Collection<V> coll = this.getCollection(key);
      return coll == null ? false : coll.contains(value);
   }

   public Collection<V> getCollection(Object key) {
      return (Collection<V>)this.decorated().get(key);
   }

   public int size(Object key) {
      Collection<V> coll = this.getCollection(key);
      return coll == null ? 0 : coll.size();
   }

   public boolean putAll(K key, Collection<V> values) {
      if (values != null && values.size() != 0) {
         boolean result = false;
         Collection<V> coll = this.getCollection(key);
         if (coll == null) {
            coll = this.createCollection(values.size());
            coll.addAll(values);
            if (coll.size() > 0) {
               this.decorated().put(key, coll);
               result = true;
            }
         } else {
            result = coll.addAll(values);
         }

         return result;
      } else {
         return false;
      }
   }

   public Iterator<V> iterator(Object key) {
      return !this.containsKey(key) ? EmptyIterator.emptyIterator() : new MultiValueMap.ValuesIterator(key);
   }

   public Iterator<Entry<K, V>> iterator() {
      Collection<K> allKeys = new ArrayList(this.keySet());
      final Iterator<K> keyIterator = allKeys.iterator();
      return new LazyIteratorChain<Entry<K, V>>() {
         protected Iterator<? extends Entry<K, V>> nextIterator(int count) {
            if (!keyIterator.hasNext()) {
               return null;
            }

            final K key = (K)keyIterator.next();
            Transformer<V, Entry<K, V>> transformer = new Transformer<V, Entry<K, V>>() {
               public Entry<K, V> transform(final V input) {
                  return new Entry<K, V>() {
                     public K getKey() {
                        return key;
                     }

                     public V getValue() {
                        return input;
                     }

                     public V setValue(V value) {
                        throw new UnsupportedOperationException();
                     }
                  };
               }
            };
            return new TransformIterator(MultiValueMap.this.new ValuesIterator(key), transformer);
         }
      };
   }

   public int totalSize() {
      int total = 0;

      for (Object v : this.decorated().values()) {
         total += CollectionUtils.size(v);
      }

      return total;
   }

   protected Collection<V> createCollection(int size) {
      return (Collection<V>)this.collectionFactory.create();
   }

   private static class ReflectionFactory<T extends Collection<?>> implements Factory<T>, Serializable {
      private static final long serialVersionUID = 2986114157496788874L;
      private final Class<T> clazz;

      public ReflectionFactory(Class<T> clazz) {
         this.clazz = clazz;
      }

      public T create() {
         try {
            return (T)this.clazz.newInstance();
         } catch (Exception ex) {
            throw new FunctorException("Cannot instantiate class: " + this.clazz, ex);
         }
      }

      private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
         is.defaultReadObject();
         if (this.clazz != null && !Collection.class.isAssignableFrom(this.clazz)) {
            throw new UnsupportedOperationException();
         }
      }
   }

   private class Values extends AbstractCollection<V> {
      private Values() {
      }

      public Iterator<V> iterator() {
         IteratorChain<V> chain = new IteratorChain();

         for (K k : MultiValueMap.this.keySet()) {
            chain.addIterator(MultiValueMap.this.new ValuesIterator(k));
         }

         return chain;
      }

      public int size() {
         return MultiValueMap.this.totalSize();
      }

      public void clear() {
         MultiValueMap.this.clear();
      }
   }

   private class ValuesIterator implements Iterator<V> {
      private final Object key;
      private final Collection<V> values;
      private final Iterator<V> iterator;

      public ValuesIterator(Object key) {
         this.key = key;
         this.values = MultiValueMap.this.getCollection(key);
         this.iterator = this.values.iterator();
      }

      public void remove() {
         this.iterator.remove();
         if (this.values.isEmpty()) {
            MultiValueMap.this.remove(this.key);
         }
      }

      public boolean hasNext() {
         return this.iterator.hasNext();
      }

      public V next() {
         return (V)this.iterator.next();
      }
   }
}
