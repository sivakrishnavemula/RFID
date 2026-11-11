package org.apache.commons.collections4.map;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.collections4.Unmodifiable;
import org.apache.commons.collections4.iterators.AbstractIteratorDecorator;
import org.apache.commons.collections4.keyvalue.AbstractMapEntryDecorator;
import org.apache.commons.collections4.set.AbstractSetDecorator;

public final class UnmodifiableEntrySet<K, V> extends AbstractSetDecorator<Entry<K, V>> implements Unmodifiable {
   private static final long serialVersionUID = 1678353579659253473L;

   public static <K, V> Set<Entry<K, V>> unmodifiableEntrySet(Set<Entry<K, V>> set) {
      return (Set<Entry<K, V>>)(set instanceof Unmodifiable ? set : new UnmodifiableEntrySet(set));
   }

   private UnmodifiableEntrySet(Set<Entry<K, V>> set) {
      super(set);
   }

   public boolean add(Entry<K, V> object) {
      throw new UnsupportedOperationException();
   }

   public boolean addAll(Collection<? extends Entry<K, V>> coll) {
      throw new UnsupportedOperationException();
   }

   public void clear() {
      throw new UnsupportedOperationException();
   }

   public boolean remove(Object object) {
      throw new UnsupportedOperationException();
   }

   public boolean removeAll(Collection<?> coll) {
      throw new UnsupportedOperationException();
   }

   public boolean retainAll(Collection<?> coll) {
      throw new UnsupportedOperationException();
   }

   public Iterator<Entry<K, V>> iterator() {
      return new UnmodifiableEntrySet.UnmodifiableEntrySetIterator(this.decorated().iterator());
   }

   public Object[] toArray() {
      Object[] array = this.decorated().toArray();

      for (int i = 0; i < array.length; i++) {
         array[i] = new UnmodifiableEntrySet.UnmodifiableEntry((Entry<K, V>)array[i]);
      }

      return array;
   }

   public <T> T[] toArray(T[] array) {
      Object[] result = array;
      if (array.length > 0) {
         result = (Object[])Array.newInstance(array.getClass().getComponentType(), 0);
      }

      result = this.decorated().toArray(result);

      for (int i = 0; i < result.length; i++) {
         result[i] = new UnmodifiableEntrySet.UnmodifiableEntry((Entry<K, V>)result[i]);
      }

      if (result.length > array.length) {
         return (T[])result;
      }

      System.arraycopy(result, 0, array, 0, result.length);
      if (array.length > result.length) {
         array[result.length] = null;
      }

      return array;
   }

   private class UnmodifiableEntry extends AbstractMapEntryDecorator<K, V> {
      protected UnmodifiableEntry(Entry<K, V> entry) {
         super(entry);
      }

      public V setValue(V obj) {
         throw new UnsupportedOperationException();
      }
   }

   private class UnmodifiableEntrySetIterator extends AbstractIteratorDecorator<Entry<K, V>> {
      protected UnmodifiableEntrySetIterator(Iterator<Entry<K, V>> iterator) {
         super(iterator);
      }

      public Entry<K, V> next() {
         return UnmodifiableEntrySet.this.new UnmodifiableEntry((Entry<K, V>)this.getIterator().next());
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }
}
