package org.apache.commons.collections4.collection;

import java.util.Collection;
import java.util.HashMap;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.map.MultiValueMap;

public class IndexedCollection<K, C> extends AbstractCollectionDecorator<C> {
   private static final long serialVersionUID = -5512610452568370038L;
   private final Transformer<C, K> keyTransformer;
   private final MultiMap<K, C> index;
   private final boolean uniqueIndex;

   public static <K, C> IndexedCollection<K, C> uniqueIndexedCollection(Collection<C> coll, Transformer<C, K> keyTransformer) {
      return new IndexedCollection<>(coll, keyTransformer, MultiValueMap.multiValueMap(new HashMap()), true);
   }

   public static <K, C> IndexedCollection<K, C> nonUniqueIndexedCollection(Collection<C> coll, Transformer<C, K> keyTransformer) {
      return new IndexedCollection<>(coll, keyTransformer, MultiValueMap.multiValueMap(new HashMap()), false);
   }

   public IndexedCollection(Collection<C> coll, Transformer<C, K> keyTransformer, MultiMap<K, C> map, boolean uniqueIndex) {
      super(coll);
      this.keyTransformer = keyTransformer;
      this.index = map;
      this.uniqueIndex = uniqueIndex;
      this.reindex();
   }

   public boolean add(C object) {
      boolean added = super.add(object);
      if (added) {
         this.addToIndex(object);
      }

      return added;
   }

   public boolean addAll(Collection<? extends C> coll) {
      boolean changed = false;

      for (C c : coll) {
         changed |= this.add(c);
      }

      return changed;
   }

   public void clear() {
      super.clear();
      this.index.clear();
   }

   public boolean contains(Object object) {
      return this.index.containsKey(this.keyTransformer.transform(object));
   }

   public boolean containsAll(Collection<?> coll) {
      for (Object o : coll) {
         if (!this.contains(o)) {
            return false;
         }
      }

      return true;
   }

   public C get(K key) {
      Collection<C> coll = (Collection<C>)this.index.get(key);
      return (C)(coll == null ? null : coll.iterator().next());
   }

   public Collection<C> values(K key) {
      return (Collection<C>)this.index.get(key);
   }

   public void reindex() {
      this.index.clear();

      for (C c : this.decorated()) {
         this.addToIndex(c);
      }
   }

   public boolean remove(Object object) {
      boolean removed = super.remove(object);
      if (removed) {
         this.removeFromIndex((C)object);
      }

      return removed;
   }

   public boolean removeAll(Collection<?> coll) {
      boolean changed = false;

      for (Object o : coll) {
         changed |= this.remove(o);
      }

      return changed;
   }

   public boolean retainAll(Collection<?> coll) {
      boolean changed = super.retainAll(coll);
      if (changed) {
         this.reindex();
      }

      return changed;
   }

   private void addToIndex(C object) {
      K key = (K)this.keyTransformer.transform(object);
      if (this.uniqueIndex && this.index.containsKey(key)) {
         throw new IllegalArgumentException("Duplicate key in uniquely indexed collection.");
      }

      this.index.put(key, object);
   }

   private void removeFromIndex(C object) {
      this.index.remove(this.keyTransformer.transform(object));
   }
}
