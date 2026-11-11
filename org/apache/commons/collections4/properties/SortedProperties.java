package org.apache.commons.collections4.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.collections4.iterators.IteratorEnumeration;

public class SortedProperties extends Properties {
   private static final long serialVersionUID = 1L;

   public synchronized Enumeration<Object> keys() {
      Set<Object> keySet = this.keySet();
      List<String> keys = new ArrayList(keySet.size());

      for (Object key : keySet) {
         keys.add(key.toString());
      }

      Collections.sort(keys);
      return new IteratorEnumeration(keys.iterator());
   }
}
