package org.apache.poi.hpsf;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.poi.util.CodePageUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public class CustomProperties implements Map<String, Object> {
   private static final POILogger LOG = POILogFactory.getLogger(CustomProperties.class);
   private final HashMap<Long, CustomProperty> props = new HashMap();
   private final TreeBidiMap<Long, String> dictionary = new TreeBidiMap();
   private boolean isPure = true;
   private int codepage = -1;

   public CustomProperty put(String name, CustomProperty cp) {
      if (name == null) {
         this.isPure = false;
         return null;
      }

      if (!name.equals(cp.getName())) {
         throw new IllegalArgumentException("Parameter \"name\" (" + name + ") and custom property's name (" + cp.getName() + ") do not match.");
      }

      this.checkCodePage(name);
      this.props.remove(this.dictionary.getKey(name));
      this.dictionary.put(cp.getID(), name);
      return (CustomProperty)this.props.put(cp.getID(), cp);
   }

   public Object put(String key, Object value) {
      int variantType;
      if (value instanceof String) {
         variantType = 30;
      } else if (value instanceof Short) {
         variantType = 2;
      } else if (value instanceof Integer) {
         variantType = 3;
      } else if (value instanceof Long) {
         variantType = 20;
      } else if (value instanceof Float) {
         variantType = 4;
      } else if (value instanceof Double) {
         variantType = 5;
      } else if (value instanceof Boolean) {
         variantType = 11;
      } else if (value instanceof BigInteger && ((BigInteger)value).bitLength() <= 64 && ((BigInteger)value).compareTo(BigInteger.ZERO) >= 0) {
         variantType = 21;
      } else {
         if (!(value instanceof Date)) {
            throw new IllegalStateException(
               "unsupported datatype - currently String,Short,Integer,Long,Float,Double,Boolean,BigInteger(unsigned long),Date can be processed."
            );
         }

         variantType = 64;
      }

      Property p = new Property(-1L, variantType, value);
      return this.put(new CustomProperty(p, key));
   }

   public Object get(Object key) {
      Long id = (Long)this.dictionary.getKey(key);
      CustomProperty cp = (CustomProperty)this.props.get(id);
      return cp != null ? cp.getValue() : null;
   }

   public CustomProperty remove(Object key) {
      Long id = (Long)this.dictionary.removeValue(key);
      return (CustomProperty)this.props.remove(id);
   }

   public int size() {
      return this.props.size();
   }

   public boolean isEmpty() {
      return this.props.isEmpty();
   }

   public void clear() {
      this.props.clear();
   }

   public int hashCode() {
      return this.props.hashCode();
   }

   public boolean equals(Object obj) {
      return obj instanceof CustomProperties && this.props.equals(((CustomProperties)obj).props);
   }

   public void putAll(Map<? extends String, ?> m) {
      for (Entry<? extends String, ?> me : m.entrySet()) {
         this.put((String)me.getKey(), me.getValue());
      }
   }

   public List<CustomProperty> properties() {
      List<CustomProperty> list = new ArrayList(this.props.size());

      for (Long l : this.dictionary.keySet()) {
         list.add(this.props.get(l));
      }

      return Collections.unmodifiableList(list);
   }

   public Collection<Object> values() {
      List<Object> list = new ArrayList(this.props.size());

      for (Long l : this.dictionary.keySet()) {
         list.add(((CustomProperty)this.props.get(l)).getValue());
      }

      return Collections.unmodifiableCollection(list);
   }

   public Set<Entry<String, Object>> entrySet() {
      Map<String, Object> set = new LinkedHashMap(this.props.size());

      for (Entry<Long, String> se : this.dictionary.entrySet()) {
         set.put(se.getValue(), ((CustomProperty)this.props.get(se.getKey())).getValue());
      }

      return Collections.unmodifiableSet(set.entrySet());
   }

   public Set keySet() {
      return Collections.unmodifiableSet(this.dictionary.values());
   }

   public Set<String> nameSet() {
      return Collections.unmodifiableSet(this.dictionary.values());
   }

   public Set<Long> idSet() {
      return Collections.unmodifiableSet(this.dictionary.keySet());
   }

   public void setCodepage(int codepage) {
      this.codepage = codepage;
   }

   public int getCodepage() {
      return this.codepage;
   }

   Map<Long, String> getDictionary() {
      return this.dictionary;
   }

   public boolean containsKey(Object key) {
      return key instanceof Long && this.dictionary.containsKey(key) || this.dictionary.containsValue(key);
   }

   public boolean containsValue(Object value) {
      if (value instanceof CustomProperty) {
         return this.props.containsValue(value);
      }

      for (CustomProperty cp : this.props.values()) {
         if (cp.getValue() == value) {
            return true;
         }
      }

      return false;
   }

   public boolean isPure() {
      return this.isPure;
   }

   public void setPure(boolean isPure) {
      this.isPure = isPure;
   }

   private Object put(CustomProperty customProperty) throws ClassCastException {
      String name = customProperty.getName();
      Long oldId = name == null ? null : (Long)this.dictionary.getKey(name);
      if (oldId != null) {
         customProperty.setID(oldId);
      } else {
         long lastKey = this.dictionary.isEmpty() ? 0L : (Long)this.dictionary.lastKey();
         long nextKey = Math.max(lastKey, 31L) + 1L;
         customProperty.setID(nextKey);
      }

      return this.put(name, customProperty);
   }

   private void checkCodePage(String value) {
      int cp = this.getCodepage();
      if (cp == -1) {
         cp = 1252;
      }

      if (cp != 1200) {
         String cps = "";

         try {
            cps = CodePageUtil.codepageToEncoding(cp, false);
         } catch (UnsupportedEncodingException e) {
            LOG.log(7, new Object[]{"Codepage '" + cp + "' can't be found."});
         }

         if (cps.isEmpty() || !Charset.forName(cps).newEncoder().canEncode(value)) {
            LOG.log(1, new Object[]{"Charset '" + cps + "' can't encode '" + value + "' - switching to unicode."});
            this.setCodepage(1200);
         }
      }
   }
}
