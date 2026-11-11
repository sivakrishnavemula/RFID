package org.apache.commons.collections4.trie;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map.Entry;
import org.apache.commons.collections4.Trie;

public abstract class AbstractBitwiseTrie<K, V> extends AbstractMap<K, V> implements Trie<K, V>, Serializable {
   private static final long serialVersionUID = 5826987063535505652L;
   private final KeyAnalyzer<? super K> keyAnalyzer;

   protected AbstractBitwiseTrie(KeyAnalyzer<? super K> keyAnalyzer) {
      if (keyAnalyzer == null) {
         throw new NullPointerException("keyAnalyzer");
      }

      this.keyAnalyzer = keyAnalyzer;
   }

   protected KeyAnalyzer<? super K> getKeyAnalyzer() {
      return this.keyAnalyzer;
   }

   public String toString() {
      StringBuilder buffer = new StringBuilder();
      buffer.append("Trie[").append(this.size()).append("]={\n");

      for (Entry<K, V> entry : this.entrySet()) {
         buffer.append("  ").append(entry).append("\n");
      }

      buffer.append("}\n");
      return buffer.toString();
   }

   final K castKey(Object key) {
      return (K)key;
   }

   final int lengthInBits(K key) {
      return key == null ? 0 : this.keyAnalyzer.lengthInBits(key);
   }

   final int bitsPerElement() {
      return this.keyAnalyzer.bitsPerElement();
   }

   final boolean isBitSet(K key, int bitIndex, int lengthInBits) {
      return key == null ? false : this.keyAnalyzer.isBitSet(key, bitIndex, lengthInBits);
   }

   final int bitIndex(K key, K foundKey) {
      return this.keyAnalyzer.bitIndex(key, 0, this.lengthInBits(key), foundKey, 0, this.lengthInBits(foundKey));
   }

   final boolean compareKeys(K key, K other) {
      if (key == null) {
         return other == null;
      } else {
         return other == null ? false : this.keyAnalyzer.compare(key, other) == 0;
      }
   }

   static boolean compare(Object a, Object b) {
      return a == null ? b == null : a.equals(b);
   }

   abstract static class BasicEntry<K, V> implements Entry<K, V>, Serializable {
      private static final long serialVersionUID = -944364551314110330L;
      protected K key;
      protected V value;

      public BasicEntry(K key) {
         this.key = key;
      }

      public BasicEntry(K key, V value) {
         this.key = key;
         this.value = value;
      }

      public V setKeyValue(K key, V value) {
         this.key = key;
         return this.setValue(value);
      }

      public K getKey() {
         return this.key;
      }

      public V getValue() {
         return this.value;
      }

      public V setValue(V value) {
         V previous = this.value;
         this.value = value;
         return previous;
      }

      public int hashCode() {
         return (this.getKey() == null ? 0 : this.getKey().hashCode()) ^ (this.getValue() == null ? 0 : this.getValue().hashCode());
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         }

         if (!(o instanceof Entry)) {
            return false;
         }

         Entry<?, ?> other = (Entry<?, ?>)o;
         return AbstractBitwiseTrie.compare(this.key, other.getKey()) && AbstractBitwiseTrie.compare(this.value, other.getValue());
      }

      public String toString() {
         return this.key + "=" + this.value;
      }
   }
}
