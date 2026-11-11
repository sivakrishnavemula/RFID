package org.apache.commons.collections4.map;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.collections4.iterators.AbstractIteratorDecorator;
import org.apache.commons.collections4.keyvalue.AbstractMapEntryDecorator;
import org.apache.commons.collections4.set.AbstractSetDecorator;

abstract class AbstractInputCheckedMapDecorator<K, V> extends AbstractMapDecorator<K, V> {
   protected AbstractInputCheckedMapDecorator() {
   }

   protected AbstractInputCheckedMapDecorator(Map<K, V> map) {
      super(map);
   }

   protected abstract V checkSetValue(V var1);

   protected boolean isSetValueChecking() {
      return true;
   }

   public Set<Entry<K, V>> entrySet() {
      return (Set<Entry<K, V>>)(this.isSetValueChecking() ? new AbstractInputCheckedMapDecorator.EntrySet(this.map.entrySet(), this) : this.map.entrySet());
   }

   private class EntrySet extends AbstractSetDecorator<Entry<K, V>> {
      private static final long serialVersionUID = 4354731610923110264L;
      private final AbstractInputCheckedMapDecorator<K, V> parent;

      protected EntrySet(Set<Entry<K, V>> set, AbstractInputCheckedMapDecorator<K, V> parent) {
         super(set);
         this.parent = parent;
      }

      public Iterator<Entry<K, V>> iterator() {
         return AbstractInputCheckedMapDecorator.this.new EntrySetIterator(this.decorated().iterator(), this.parent);
      }

      public Object[] toArray() {
         Object[] array = this.decorated().toArray();

         for (int i = 0; i < array.length; i++) {
            array[i] = AbstractInputCheckedMapDecorator.this.new MapEntry((Entry<K, V>)array[i], this.parent);
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
            result[i] = AbstractInputCheckedMapDecorator.this.new MapEntry((Entry<K, V>)result[i], this.parent);
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
   }

   private class EntrySetIterator extends AbstractIteratorDecorator<Entry<K, V>> {
      private final AbstractInputCheckedMapDecorator<K, V> parent;

      protected EntrySetIterator(Iterator<Entry<K, V>> iterator, AbstractInputCheckedMapDecorator<K, V> parent) {
         super(iterator);
         this.parent = parent;
      }

      public Entry<K, V> next() {
         Entry<K, V> entry = (Entry<K, V>)this.getIterator().next();
         return AbstractInputCheckedMapDecorator.this.new MapEntry(entry, this.parent);
      }
   }

   private class MapEntry extends AbstractMapEntryDecorator<K, V> {
      private final AbstractInputCheckedMapDecorator<K, V> parent;

      protected MapEntry(Entry<K, V> entry, AbstractInputCheckedMapDecorator<K, V> parent) {
         super(entry);
         this.parent = parent;
      }

      public V setValue(V value) {
         value = this.parent.checkSetValue(value);
         return (V)this.getMapEntry().setValue(value);
      }
   }
}
