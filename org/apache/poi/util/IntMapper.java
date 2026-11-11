package org.apache.poi.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class IntMapper<T> {
   private List<T> elements;
   private Map<T, Integer> valueKeyMap;
   private static final int _default_size = 10;

   public IntMapper() {
      this(10);
   }

   public IntMapper(int initialCapacity) {
      this.elements = new ArrayList(initialCapacity);
      this.valueKeyMap = new HashMap(initialCapacity);
   }

   public boolean add(T value) {
      int index = this.elements.size();
      this.elements.add(value);
      this.valueKeyMap.put(value, index);
      return true;
   }

   public int size() {
      return this.elements.size();
   }

   public T get(int index) {
      return (T)this.elements.get(index);
   }

   public int getIndex(T o) {
      Integer i = (Integer)this.valueKeyMap.get(o);
      return i == null ? -1 : i;
   }

   public Iterator<T> iterator() {
      return this.elements.iterator();
   }
}
