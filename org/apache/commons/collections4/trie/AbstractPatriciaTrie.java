package org.apache.commons.collections4.trie;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.Map.Entry;
import org.apache.commons.collections4.OrderedMapIterator;
import org.apache.commons.collections4.trie.AbstractBitwiseTrie.BasicEntry;

abstract class AbstractPatriciaTrie<K, V> extends AbstractBitwiseTrie<K, V> {
   private static final long serialVersionUID = 5155253417231339498L;
   private transient AbstractPatriciaTrie.TrieEntry<K, V> root = new AbstractPatriciaTrie.TrieEntry<>(null, null, -1);
   private transient volatile Set<K> keySet;
   private transient volatile Collection<V> values;
   private transient volatile Set<Entry<K, V>> entrySet;
   private transient int size = 0;
   protected transient int modCount = 0;

   protected AbstractPatriciaTrie(KeyAnalyzer<? super K> keyAnalyzer) {
      super(keyAnalyzer);
   }

   protected AbstractPatriciaTrie(KeyAnalyzer<? super K> keyAnalyzer, Map<? extends K, ? extends V> map) {
      super(keyAnalyzer);
      this.putAll(map);
   }

   public void clear() {
      this.root.key = null;
      this.root.bitIndex = -1;
      this.root.value = null;
      this.root.parent = null;
      this.root.left = this.root;
      this.root.right = null;
      this.root.predecessor = this.root;
      this.size = 0;
      this.incrementModCount();
   }

   public int size() {
      return this.size;
   }

   void incrementSize() {
      this.size++;
      this.incrementModCount();
   }

   void decrementSize() {
      this.size--;
      this.incrementModCount();
   }

   private void incrementModCount() {
      this.modCount++;
   }

   public V put(K key, V value) {
      if (key == null) {
         throw new NullPointerException("Key cannot be null");
      }

      int lengthInBits = this.lengthInBits(key);
      if (lengthInBits == 0) {
         if (this.root.isEmpty()) {
            this.incrementSize();
         } else {
            this.incrementModCount();
         }

         return (V)this.root.setKeyValue(key, value);
      } else {
         AbstractPatriciaTrie.TrieEntry<K, V> found = this.getNearestEntryForKey(key, lengthInBits);
         if (this.compareKeys(key, found.key)) {
            if (found.isEmpty()) {
               this.incrementSize();
            } else {
               this.incrementModCount();
            }

            return (V)found.setKeyValue(key, value);
         } else {
            int bitIndex = this.bitIndex(key, found.key);
            if (!KeyAnalyzer.isOutOfBoundsIndex(bitIndex)) {
               if (KeyAnalyzer.isValidBitIndex(bitIndex)) {
                  AbstractPatriciaTrie.TrieEntry<K, V> t = new AbstractPatriciaTrie.TrieEntry<>(key, value, bitIndex);
                  this.addEntry(t, lengthInBits);
                  this.incrementSize();
                  return null;
               }

               if (KeyAnalyzer.isNullBitKey(bitIndex)) {
                  if (this.root.isEmpty()) {
                     this.incrementSize();
                  } else {
                     this.incrementModCount();
                  }

                  return (V)this.root.setKeyValue(key, value);
               }

               if (KeyAnalyzer.isEqualBitKey(bitIndex) && found != this.root) {
                  this.incrementModCount();
                  return (V)found.setKeyValue(key, value);
               }
            }

            throw new IllegalArgumentException("Failed to put: " + key + " -> " + value + ", " + bitIndex);
         }
      }
   }

   AbstractPatriciaTrie.TrieEntry<K, V> addEntry(AbstractPatriciaTrie.TrieEntry<K, V> entry, int lengthInBits) {
      AbstractPatriciaTrie.TrieEntry<K, V> current = this.root.left;
      AbstractPatriciaTrie.TrieEntry<K, V> path = this.root;

      while (current.bitIndex < entry.bitIndex && current.bitIndex > path.bitIndex) {
         path = current;
         if (!this.isBitSet(entry.key, current.bitIndex, lengthInBits)) {
            current = current.left;
         } else {
            current = current.right;
         }
      }

      entry.predecessor = entry;
      if (!this.isBitSet(entry.key, entry.bitIndex, lengthInBits)) {
         entry.left = entry;
         entry.right = current;
      } else {
         entry.left = current;
         entry.right = entry;
      }

      entry.parent = path;
      if (current.bitIndex >= entry.bitIndex) {
         current.parent = entry;
      }

      if (current.bitIndex <= path.bitIndex) {
         current.predecessor = entry;
      }

      if (path != this.root && this.isBitSet(entry.key, path.bitIndex, lengthInBits)) {
         path.right = entry;
      } else {
         path.left = entry;
      }

      return entry;
   }

   public V get(Object k) {
      AbstractPatriciaTrie.TrieEntry<K, V> entry = this.getEntry(k);
      return (V)(entry != null ? entry.getValue() : null);
   }

   AbstractPatriciaTrie.TrieEntry<K, V> getEntry(Object k) {
      K key = (K)this.castKey(k);
      if (key == null) {
         return null;
      }

      int lengthInBits = this.lengthInBits(key);
      AbstractPatriciaTrie.TrieEntry<K, V> entry = this.getNearestEntryForKey(key, lengthInBits);
      return !entry.isEmpty() && this.compareKeys(key, entry.key) ? entry : null;
   }

   public Entry<K, V> select(K key) {
      int lengthInBits = this.lengthInBits(key);
      AbstractPatriciaTrie.Reference<Entry<K, V>> reference = new AbstractPatriciaTrie.Reference<>();
      return !this.selectR(this.root.left, -1, key, lengthInBits, reference) ? reference.get() : null;
   }

   public K selectKey(K key) {
      Entry<K, V> entry = this.select(key);
      return (K)(entry == null ? null : entry.getKey());
   }

   public V selectValue(K key) {
      Entry<K, V> entry = this.select(key);
      return (V)(entry == null ? null : entry.getValue());
   }

   private boolean selectR(AbstractPatriciaTrie.TrieEntry<K, V> h, int bitIndex, K key, int lengthInBits, AbstractPatriciaTrie.Reference<Entry<K, V>> reference) {
      if (h.bitIndex <= bitIndex) {
         if (!h.isEmpty()) {
            reference.set(h);
            return false;
         } else {
            return true;
         }
      } else {
         if (!this.isBitSet(key, h.bitIndex, lengthInBits)) {
            if (this.selectR(h.left, h.bitIndex, key, lengthInBits, reference)) {
               return this.selectR(h.right, h.bitIndex, key, lengthInBits, reference);
            }
         } else if (this.selectR(h.right, h.bitIndex, key, lengthInBits, reference)) {
            return this.selectR(h.left, h.bitIndex, key, lengthInBits, reference);
         }

         return false;
      }
   }

   public boolean containsKey(Object k) {
      if (k == null) {
         return false;
      }

      K key = (K)this.castKey(k);
      int lengthInBits = this.lengthInBits(key);
      AbstractPatriciaTrie.TrieEntry<K, V> entry = this.getNearestEntryForKey(key, lengthInBits);
      return !entry.isEmpty() && this.compareKeys(key, entry.key);
   }

   public Set<Entry<K, V>> entrySet() {
      if (this.entrySet == null) {
         this.entrySet = new AbstractPatriciaTrie.EntrySet();
      }

      return this.entrySet;
   }

   public Set<K> keySet() {
      if (this.keySet == null) {
         this.keySet = new AbstractPatriciaTrie.KeySet();
      }

      return this.keySet;
   }

   public Collection<V> values() {
      if (this.values == null) {
         this.values = new AbstractPatriciaTrie.Values();
      }

      return this.values;
   }

   public V remove(Object k) {
      if (k == null) {
         return null;
      }

      K key = (K)this.castKey(k);
      int lengthInBits = this.lengthInBits(key);
      AbstractPatriciaTrie.TrieEntry<K, V> current = this.root.left;
      AbstractPatriciaTrie.TrieEntry<K, V> path = this.root;

      while (current.bitIndex > path.bitIndex) {
         path = current;
         if (!this.isBitSet(key, current.bitIndex, lengthInBits)) {
            current = current.left;
         } else {
            current = current.right;
         }
      }

      return !current.isEmpty() && this.compareKeys(key, current.key) ? this.removeEntry(current) : null;
   }

   AbstractPatriciaTrie.TrieEntry<K, V> getNearestEntryForKey(K key, int lengthInBits) {
      AbstractPatriciaTrie.TrieEntry<K, V> current = this.root.left;
      AbstractPatriciaTrie.TrieEntry<K, V> path = this.root;

      while (current.bitIndex > path.bitIndex) {
         path = current;
         if (!this.isBitSet(key, current.bitIndex, lengthInBits)) {
            current = current.left;
         } else {
            current = current.right;
         }
      }

      return current;
   }

   V removeEntry(AbstractPatriciaTrie.TrieEntry<K, V> h) {
      if (h != this.root) {
         if (h.isInternalNode()) {
            this.removeInternalEntry(h);
         } else {
            this.removeExternalEntry(h);
         }
      }

      this.decrementSize();
      return (V)h.setKeyValue(null, null);
   }

   private void removeExternalEntry(AbstractPatriciaTrie.TrieEntry<K, V> h) {
      if (h == this.root) {
         throw new IllegalArgumentException("Cannot delete root Entry!");
      }

      if (!h.isExternalNode()) {
         throw new IllegalArgumentException(h + " is not an external Entry!");
      }

      AbstractPatriciaTrie.TrieEntry<K, V> parent = h.parent;
      AbstractPatriciaTrie.TrieEntry<K, V> child = h.left == h ? h.right : h.left;
      if (parent.left == h) {
         parent.left = child;
      } else {
         parent.right = child;
      }

      if (child.bitIndex > parent.bitIndex) {
         child.parent = parent;
      } else {
         child.predecessor = parent;
      }
   }

   private void removeInternalEntry(AbstractPatriciaTrie.TrieEntry<K, V> h) {
      if (h == this.root) {
         throw new IllegalArgumentException("Cannot delete root Entry!");
      }

      if (!h.isInternalNode()) {
         throw new IllegalArgumentException(h + " is not an internal Entry!");
      }

      AbstractPatriciaTrie.TrieEntry<K, V> p = h.predecessor;
      p.bitIndex = h.bitIndex;
      AbstractPatriciaTrie.TrieEntry<K, V> parent = p.parent;
      AbstractPatriciaTrie.TrieEntry<K, V> child = p.left == h ? p.right : p.left;
      if (p.predecessor == p && p.parent != h) {
         p.predecessor = p.parent;
      }

      if (parent.left == p) {
         parent.left = child;
      } else {
         parent.right = child;
      }

      if (child.bitIndex > parent.bitIndex) {
         child.parent = parent;
      }

      if (h.left.parent == h) {
         h.left.parent = p;
      }

      if (h.right.parent == h) {
         h.right.parent = p;
      }

      if (h.parent.left == h) {
         h.parent.left = p;
      } else {
         h.parent.right = p;
      }

      p.parent = h.parent;
      p.left = h.left;
      p.right = h.right;
      if (isValidUplink(p.left, p)) {
         p.left.predecessor = p;
      }

      if (isValidUplink(p.right, p)) {
         p.right.predecessor = p;
      }
   }

   AbstractPatriciaTrie.TrieEntry<K, V> nextEntry(AbstractPatriciaTrie.TrieEntry<K, V> node) {
      return node == null ? this.firstEntry() : this.nextEntryImpl(node.predecessor, node, null);
   }

   AbstractPatriciaTrie.TrieEntry<K, V> nextEntryImpl(
      AbstractPatriciaTrie.TrieEntry<K, V> start, AbstractPatriciaTrie.TrieEntry<K, V> previous, AbstractPatriciaTrie.TrieEntry<K, V> tree
   ) {
      AbstractPatriciaTrie.TrieEntry<K, V> current = start;
      if (previous == null || start != previous.predecessor) {
         while (!current.left.isEmpty() && previous != current.left) {
            if (isValidUplink(current.left, current)) {
               return current.left;
            }

            current = current.left;
         }
      }

      if (current.isEmpty()) {
         return null;
      }

      if (current.right == null) {
         return null;
      }

      if (previous != current.right) {
         return isValidUplink(current.right, current) ? current.right : this.nextEntryImpl(current.right, previous, tree);
      }

      while (current == current.parent.right) {
         if (current == tree) {
            return null;
         }

         current = current.parent;
      }

      if (current == tree) {
         return null;
      } else if (current.parent.right == null) {
         return null;
      } else if (previous != current.parent.right && isValidUplink(current.parent.right, current.parent)) {
         return current.parent.right;
      } else {
         return current.parent.right == current.parent ? null : this.nextEntryImpl(current.parent.right, previous, tree);
      }
   }

   AbstractPatriciaTrie.TrieEntry<K, V> firstEntry() {
      return this.isEmpty() ? null : this.followLeft(this.root);
   }

   AbstractPatriciaTrie.TrieEntry<K, V> followLeft(AbstractPatriciaTrie.TrieEntry<K, V> node) {
      while (true) {
         AbstractPatriciaTrie.TrieEntry<K, V> child = node.left;
         if (child.isEmpty()) {
            child = node.right;
         }

         if (child.bitIndex <= node.bitIndex) {
            return child;
         }

         node = child;
      }
   }

   public Comparator<? super K> comparator() {
      return this.getKeyAnalyzer();
   }

   public K firstKey() {
      if (this.size() == 0) {
         throw new NoSuchElementException();
      } else {
         return (K)this.firstEntry().getKey();
      }
   }

   public K lastKey() {
      AbstractPatriciaTrie.TrieEntry<K, V> entry = this.lastEntry();
      if (entry != null) {
         return (K)entry.getKey();
      } else {
         throw new NoSuchElementException();
      }
   }

   public K nextKey(K key) {
      if (key == null) {
         throw new NullPointerException();
      } else {
         AbstractPatriciaTrie.TrieEntry<K, V> entry = this.getEntry(key);
         if (entry != null) {
            AbstractPatriciaTrie.TrieEntry<K, V> nextEntry = this.nextEntry(entry);
            return (K)(nextEntry != null ? nextEntry.getKey() : null);
         } else {
            return null;
         }
      }
   }

   public K previousKey(K key) {
      if (key == null) {
         throw new NullPointerException();
      } else {
         AbstractPatriciaTrie.TrieEntry<K, V> entry = this.getEntry(key);
         if (entry != null) {
            AbstractPatriciaTrie.TrieEntry<K, V> prevEntry = this.previousEntry(entry);
            return (K)(prevEntry != null ? prevEntry.getKey() : null);
         } else {
            return null;
         }
      }
   }

   public OrderedMapIterator<K, V> mapIterator() {
      return new AbstractPatriciaTrie.TrieMapIterator();
   }

   public SortedMap<K, V> prefixMap(K key) {
      return this.getPrefixMapByBits(key, 0, this.lengthInBits(key));
   }

   private SortedMap<K, V> getPrefixMapByBits(K key, int offsetInBits, int lengthInBits) {
      int offsetLength = offsetInBits + lengthInBits;
      if (offsetLength > this.lengthInBits(key)) {
         throw new IllegalArgumentException(offsetInBits + " + " + lengthInBits + " > " + this.lengthInBits(key));
      } else {
         return (SortedMap<K, V>)(offsetLength == 0 ? this : new AbstractPatriciaTrie.PrefixRangeMap(key, offsetInBits, lengthInBits));
      }
   }

   public SortedMap<K, V> headMap(K toKey) {
      return new AbstractPatriciaTrie.RangeEntryMap(null, toKey);
   }

   public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return new AbstractPatriciaTrie.RangeEntryMap(fromKey, toKey);
   }

   public SortedMap<K, V> tailMap(K fromKey) {
      return new AbstractPatriciaTrie.RangeEntryMap(fromKey, null);
   }

   AbstractPatriciaTrie.TrieEntry<K, V> higherEntry(K key) {
      int lengthInBits = this.lengthInBits(key);
      if (lengthInBits == 0) {
         if (!this.root.isEmpty()) {
            return this.size() > 1 ? this.nextEntry(this.root) : null;
         } else {
            return this.firstEntry();
         }
      } else {
         AbstractPatriciaTrie.TrieEntry<K, V> found = this.getNearestEntryForKey(key, lengthInBits);
         if (this.compareKeys(key, found.key)) {
            return this.nextEntry(found);
         }

         int bitIndex = this.bitIndex(key, found.key);
         if (KeyAnalyzer.isValidBitIndex(bitIndex)) {
            AbstractPatriciaTrie.TrieEntry<K, V> added = new AbstractPatriciaTrie.TrieEntry<>(key, null, bitIndex);
            this.addEntry(added, lengthInBits);
            this.incrementSize();
            AbstractPatriciaTrie.TrieEntry<K, V> ceil = this.nextEntry(added);
            this.removeEntry(added);
            this.modCount -= 2;
            return ceil;
         }

         if (KeyAnalyzer.isNullBitKey(bitIndex)) {
            if (!this.root.isEmpty()) {
               return this.firstEntry();
            } else {
               return this.size() > 1 ? this.nextEntry(this.firstEntry()) : null;
            }
         } else if (KeyAnalyzer.isEqualBitKey(bitIndex)) {
            return this.nextEntry(found);
         } else {
            throw new IllegalStateException("invalid lookup: " + key);
         }
      }
   }

   AbstractPatriciaTrie.TrieEntry<K, V> ceilingEntry(K key) {
      int lengthInBits = this.lengthInBits(key);
      if (lengthInBits == 0) {
         return !this.root.isEmpty() ? this.root : this.firstEntry();
      } else {
         AbstractPatriciaTrie.TrieEntry<K, V> found = this.getNearestEntryForKey(key, lengthInBits);
         if (this.compareKeys(key, found.key)) {
            return found;
         } else {
            int bitIndex = this.bitIndex(key, found.key);
            if (KeyAnalyzer.isValidBitIndex(bitIndex)) {
               AbstractPatriciaTrie.TrieEntry<K, V> added = new AbstractPatriciaTrie.TrieEntry<>(key, null, bitIndex);
               this.addEntry(added, lengthInBits);
               this.incrementSize();
               AbstractPatriciaTrie.TrieEntry<K, V> ceil = this.nextEntry(added);
               this.removeEntry(added);
               this.modCount -= 2;
               return ceil;
            } else if (KeyAnalyzer.isNullBitKey(bitIndex)) {
               return !this.root.isEmpty() ? this.root : this.firstEntry();
            } else if (KeyAnalyzer.isEqualBitKey(bitIndex)) {
               return found;
            } else {
               throw new IllegalStateException("invalid lookup: " + key);
            }
         }
      }
   }

   AbstractPatriciaTrie.TrieEntry<K, V> lowerEntry(K key) {
      int lengthInBits = this.lengthInBits(key);
      if (lengthInBits == 0) {
         return null;
      } else {
         AbstractPatriciaTrie.TrieEntry<K, V> found = this.getNearestEntryForKey(key, lengthInBits);
         if (this.compareKeys(key, found.key)) {
            return this.previousEntry(found);
         } else {
            int bitIndex = this.bitIndex(key, found.key);
            if (KeyAnalyzer.isValidBitIndex(bitIndex)) {
               AbstractPatriciaTrie.TrieEntry<K, V> added = new AbstractPatriciaTrie.TrieEntry<>(key, null, bitIndex);
               this.addEntry(added, lengthInBits);
               this.incrementSize();
               AbstractPatriciaTrie.TrieEntry<K, V> prior = this.previousEntry(added);
               this.removeEntry(added);
               this.modCount -= 2;
               return prior;
            } else if (KeyAnalyzer.isNullBitKey(bitIndex)) {
               return null;
            } else if (KeyAnalyzer.isEqualBitKey(bitIndex)) {
               return this.previousEntry(found);
            } else {
               throw new IllegalStateException("invalid lookup: " + key);
            }
         }
      }
   }

   AbstractPatriciaTrie.TrieEntry<K, V> floorEntry(K key) {
      int lengthInBits = this.lengthInBits(key);
      if (lengthInBits == 0) {
         return !this.root.isEmpty() ? this.root : null;
      } else {
         AbstractPatriciaTrie.TrieEntry<K, V> found = this.getNearestEntryForKey(key, lengthInBits);
         if (this.compareKeys(key, found.key)) {
            return found;
         } else {
            int bitIndex = this.bitIndex(key, found.key);
            if (KeyAnalyzer.isValidBitIndex(bitIndex)) {
               AbstractPatriciaTrie.TrieEntry<K, V> added = new AbstractPatriciaTrie.TrieEntry<>(key, null, bitIndex);
               this.addEntry(added, lengthInBits);
               this.incrementSize();
               AbstractPatriciaTrie.TrieEntry<K, V> floor = this.previousEntry(added);
               this.removeEntry(added);
               this.modCount -= 2;
               return floor;
            } else if (KeyAnalyzer.isNullBitKey(bitIndex)) {
               return !this.root.isEmpty() ? this.root : null;
            } else if (KeyAnalyzer.isEqualBitKey(bitIndex)) {
               return found;
            } else {
               throw new IllegalStateException("invalid lookup: " + key);
            }
         }
      }
   }

   AbstractPatriciaTrie.TrieEntry<K, V> subtree(K prefix, int offsetInBits, int lengthInBits) {
      AbstractPatriciaTrie.TrieEntry<K, V> current = this.root.left;
      AbstractPatriciaTrie.TrieEntry<K, V> path = this.root;

      while (current.bitIndex > path.bitIndex && lengthInBits > current.bitIndex) {
         path = current;
         if (!this.isBitSet(prefix, offsetInBits + current.bitIndex, offsetInBits + lengthInBits)) {
            current = current.left;
         } else {
            current = current.right;
         }
      }

      AbstractPatriciaTrie.TrieEntry<K, V> entry = current.isEmpty() ? path : current;
      if (entry.isEmpty()) {
         return null;
      }

      int endIndexInBits = offsetInBits + lengthInBits;
      if (entry == this.root && this.lengthInBits(entry.getKey()) < endIndexInBits) {
         return null;
      }

      if (this.isBitSet(prefix, endIndexInBits - 1, endIndexInBits) != this.isBitSet(entry.key, lengthInBits - 1, this.lengthInBits(entry.key))) {
         return null;
      }

      int bitIndex = this.getKeyAnalyzer().bitIndex(prefix, offsetInBits, lengthInBits, entry.key, 0, this.lengthInBits(entry.getKey()));
      return bitIndex >= 0 && bitIndex < lengthInBits ? null : entry;
   }

   AbstractPatriciaTrie.TrieEntry<K, V> lastEntry() {
      return this.followRight(this.root.left);
   }

   AbstractPatriciaTrie.TrieEntry<K, V> followRight(AbstractPatriciaTrie.TrieEntry<K, V> node) {
      if (node.right == null) {
         return null;
      }

      while (node.right.bitIndex > node.bitIndex) {
         node = node.right;
      }

      return node.right;
   }

   AbstractPatriciaTrie.TrieEntry<K, V> previousEntry(AbstractPatriciaTrie.TrieEntry<K, V> start) {
      if (start.predecessor == null) {
         throw new IllegalArgumentException("must have come from somewhere!");
      }

      if (start.predecessor.right == start) {
         return isValidUplink(start.predecessor.left, start.predecessor) ? start.predecessor.left : this.followRight(start.predecessor.left);
      }

      AbstractPatriciaTrie.TrieEntry<K, V> node = start.predecessor;

      while (node.parent != null && node == node.parent.left) {
         node = node.parent;
      }

      if (node.parent == null) {
         return null;
      }

      if (isValidUplink(node.parent.left, node.parent)) {
         if (node.parent.left == this.root) {
            return this.root.isEmpty() ? null : this.root;
         } else {
            return node.parent.left;
         }
      } else {
         return this.followRight(node.parent.left);
      }
   }

   AbstractPatriciaTrie.TrieEntry<K, V> nextEntryInSubtree(AbstractPatriciaTrie.TrieEntry<K, V> node, AbstractPatriciaTrie.TrieEntry<K, V> parentOfSubtree) {
      return node == null ? this.firstEntry() : this.nextEntryImpl(node.predecessor, node, parentOfSubtree);
   }

   static boolean isValidUplink(AbstractPatriciaTrie.TrieEntry<?, ?> next, AbstractPatriciaTrie.TrieEntry<?, ?> from) {
      return next != null && next.bitIndex <= from.bitIndex && !next.isEmpty();
   }

   private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      this.root = new AbstractPatriciaTrie.TrieEntry<>(null, null, -1);
      int size = stream.readInt();

      for (int i = 0; i < size; i++) {
         K k = (K)stream.readObject();
         V v = (V)stream.readObject();
         this.put(k, v);
      }
   }

   private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeInt(this.size());

      for (Entry<K, V> entry : this.entrySet()) {
         stream.writeObject(entry.getKey());
         stream.writeObject(entry.getValue());
      }
   }

   private class EntrySet extends AbstractSet<Entry<K, V>> {
      private EntrySet() {
      }

      public Iterator<Entry<K, V>> iterator() {
         return new AbstractPatriciaTrie.EntrySet.EntryIterator();
      }

      public boolean contains(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         }

         AbstractPatriciaTrie.TrieEntry<K, V> candidate = AbstractPatriciaTrie.this.getEntry(((Entry)o).getKey());
         return candidate != null && candidate.equals(o);
      }

      public boolean remove(Object obj) {
         if (!(obj instanceof Entry)) {
            return false;
         }

         if (!this.contains(obj)) {
            return false;
         }

         Entry<?, ?> entry = (Entry<?, ?>)obj;
         AbstractPatriciaTrie.this.remove(entry.getKey());
         return true;
      }

      public int size() {
         return AbstractPatriciaTrie.this.size();
      }

      public void clear() {
         AbstractPatriciaTrie.this.clear();
      }

      private class EntryIterator extends AbstractPatriciaTrie<K, V>.TrieIterator<Entry<K, V>> {
         private EntryIterator() {
         }

         public Entry<K, V> next() {
            return this.nextEntry();
         }
      }
   }

   private class KeySet extends AbstractSet<K> {
      private KeySet() {
      }

      public Iterator<K> iterator() {
         return new AbstractPatriciaTrie.KeySet.KeyIterator();
      }

      public int size() {
         return AbstractPatriciaTrie.this.size();
      }

      public boolean contains(Object o) {
         return AbstractPatriciaTrie.this.containsKey(o);
      }

      public boolean remove(Object o) {
         int size = this.size();
         AbstractPatriciaTrie.this.remove(o);
         return size != this.size();
      }

      public void clear() {
         AbstractPatriciaTrie.this.clear();
      }

      private class KeyIterator extends AbstractPatriciaTrie<K, V>.TrieIterator<K> {
         private KeyIterator() {
         }

         public K next() {
            return (K)this.nextEntry().getKey();
         }
      }
   }

   private final class PrefixRangeEntrySet extends AbstractPatriciaTrie<K, V>.RangeEntrySet {
      private final AbstractPatriciaTrie<K, V>.PrefixRangeMap delegate;
      private AbstractPatriciaTrie.TrieEntry<K, V> prefixStart;
      private int expectedModCount = 0;

      public PrefixRangeEntrySet(AbstractPatriciaTrie<K, V>.PrefixRangeMap delegate) {
         super(delegate);
         this.delegate = delegate;
      }

      @Override
      public int size() {
         return this.delegate.fixup();
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
         if (AbstractPatriciaTrie.this.modCount != this.expectedModCount) {
            this.prefixStart = AbstractPatriciaTrie.this.subtree((K)this.delegate.prefix, this.delegate.offsetInBits, this.delegate.lengthInBits);
            this.expectedModCount = AbstractPatriciaTrie.this.modCount;
         }

         if (this.prefixStart == null) {
            Set<Entry<K, V>> empty = Collections.emptySet();
            return empty.iterator();
         } else {
            return (Iterator<Entry<K, V>>)(this.delegate.lengthInBits > this.prefixStart.bitIndex
               ? new AbstractPatriciaTrie.PrefixRangeEntrySet.SingletonIterator(this.prefixStart)
               : new AbstractPatriciaTrie.PrefixRangeEntrySet.EntryIterator(
                  this.prefixStart, (K)this.delegate.prefix, this.delegate.offsetInBits, this.delegate.lengthInBits
               ));
         }
      }

      private final class EntryIterator extends AbstractPatriciaTrie<K, V>.TrieIterator<Entry<K, V>> {
         private final Object prefix;
         private final int offset;
         private final int lengthInBits;
         private boolean lastOne;
         private AbstractPatriciaTrie.TrieEntry<K, V> subtree;

         EntryIterator(AbstractPatriciaTrie.TrieEntry<K, V> startScan, K prefix, int offset, int lengthInBits) {
            this.subtree = startScan;
            this.next = AbstractPatriciaTrie.this.followLeft(startScan);
            this.prefix = prefix;
            this.offset = offset;
            this.lengthInBits = lengthInBits;
         }

         public Entry<K, V> next() {
            Entry<K, V> entry = this.nextEntry();
            if (this.lastOne) {
               this.next = null;
            }

            return entry;
         }

         @Override
         protected AbstractPatriciaTrie.TrieEntry<K, V> findNext(AbstractPatriciaTrie.TrieEntry<K, V> prior) {
            return AbstractPatriciaTrie.this.nextEntryInSubtree(prior, this.subtree);
         }

         @Override
         public void remove() {
            boolean needsFixing = false;
            int bitIdx = this.subtree.bitIndex;
            if (this.current == this.subtree) {
               needsFixing = true;
            }

            super.remove();
            if (bitIdx != this.subtree.bitIndex || needsFixing) {
               this.subtree = AbstractPatriciaTrie.this.subtree((K)this.prefix, this.offset, this.lengthInBits);
            }

            if (this.lengthInBits >= this.subtree.bitIndex) {
               this.lastOne = true;
            }
         }
      }

      private final class SingletonIterator implements Iterator<Entry<K, V>> {
         private final AbstractPatriciaTrie.TrieEntry<K, V> entry;
         private int hit = 0;

         public SingletonIterator(AbstractPatriciaTrie.TrieEntry<K, V> entry) {
            this.entry = entry;
         }

         public boolean hasNext() {
            return this.hit == 0;
         }

         public Entry<K, V> next() {
            if (this.hit != 0) {
               throw new NoSuchElementException();
            }

            this.hit++;
            return this.entry;
         }

         public void remove() {
            if (this.hit != 1) {
               throw new IllegalStateException();
            }

            this.hit++;
            AbstractPatriciaTrie.this.removeEntry(this.entry);
         }
      }
   }

   private class PrefixRangeMap extends AbstractPatriciaTrie<K, V>.RangeMap {
      private final Object prefix;
      private final int offsetInBits;
      private final int lengthInBits;
      private Object fromKey = null;
      private Object toKey = null;
      private transient int expectedModCount = 0;
      private int size = -1;

      private PrefixRangeMap(K prefix, int offsetInBits, int lengthInBits) {
         this.prefix = prefix;
         this.offsetInBits = offsetInBits;
         this.lengthInBits = lengthInBits;
      }

      private int fixup() {
         if (this.size == -1 || AbstractPatriciaTrie.this.modCount != this.expectedModCount) {
            Iterator<Entry<K, V>> it = super.entrySet().iterator();
            this.size = 0;
            Entry<K, V> entry = null;
            if (it.hasNext()) {
               entry = (Entry<K, V>)it.next();
               this.size = 1;
            }

            this.fromKey = entry == null ? null : entry.getKey();
            if (this.fromKey != null) {
               AbstractPatriciaTrie.TrieEntry<K, V> prior = AbstractPatriciaTrie.this.previousEntry((AbstractPatriciaTrie.TrieEntry<K, V>)entry);
               this.fromKey = prior == null ? null : prior.getKey();
            }

            for (this.toKey = this.fromKey; it.hasNext(); entry = (Entry<K, V>)it.next()) {
               this.size++;
            }

            this.toKey = entry == null ? null : entry.getKey();
            if (this.toKey != null) {
               Entry<K, V> var4 = AbstractPatriciaTrie.this.nextEntry((AbstractPatriciaTrie.TrieEntry<K, V>)entry);
               this.toKey = var4 == null ? null : var4.getKey();
            }

            this.expectedModCount = AbstractPatriciaTrie.this.modCount;
         }

         return this.size;
      }

      public K firstKey() {
         this.fixup();
         Entry<K, V> e = null;
         AbstractPatriciaTrie.TrieEntry var3;
         if (this.fromKey == null) {
            var3 = AbstractPatriciaTrie.this.firstEntry();
         } else {
            var3 = AbstractPatriciaTrie.this.higherEntry((K)this.fromKey);
         }

         K first = (K)(var3 != null ? var3.getKey() : null);
         if (var3 != null && AbstractPatriciaTrie.this.getKeyAnalyzer().isPrefix(this.prefix, this.offsetInBits, this.lengthInBits, first)) {
            return first;
         } else {
            throw new NoSuchElementException();
         }
      }

      public K lastKey() {
         this.fixup();
         Entry<K, V> e = null;
         AbstractPatriciaTrie.TrieEntry var3;
         if (this.toKey == null) {
            var3 = AbstractPatriciaTrie.this.lastEntry();
         } else {
            var3 = AbstractPatriciaTrie.this.lowerEntry((K)this.toKey);
         }

         K last = (K)(var3 != null ? var3.getKey() : null);
         if (var3 != null && AbstractPatriciaTrie.this.getKeyAnalyzer().isPrefix(this.prefix, this.offsetInBits, this.lengthInBits, last)) {
            return last;
         } else {
            throw new NoSuchElementException();
         }
      }

      @Override
      protected boolean inRange(K key) {
         return AbstractPatriciaTrie.this.getKeyAnalyzer().isPrefix(this.prefix, this.offsetInBits, this.lengthInBits, key);
      }

      @Override
      protected boolean inRange2(K key) {
         return this.inRange(key);
      }

      @Override
      protected boolean inFromRange(K key, boolean forceInclusive) {
         return AbstractPatriciaTrie.this.getKeyAnalyzer().isPrefix(this.prefix, this.offsetInBits, this.lengthInBits, key);
      }

      @Override
      protected boolean inToRange(K key, boolean forceInclusive) {
         return AbstractPatriciaTrie.this.getKeyAnalyzer().isPrefix(this.prefix, this.offsetInBits, this.lengthInBits, key);
      }

      @Override
      protected Set<Entry<K, V>> createEntrySet() {
         return AbstractPatriciaTrie.this.new PrefixRangeEntrySet(this);
      }

      @Override
      public K getFromKey() {
         return (K)this.fromKey;
      }

      @Override
      public K getToKey() {
         return (K)this.toKey;
      }

      @Override
      public boolean isFromInclusive() {
         return false;
      }

      @Override
      public boolean isToInclusive() {
         return false;
      }

      @Override
      protected SortedMap<K, V> createRangeMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
         return AbstractPatriciaTrie.this.new RangeEntryMap(fromKey, fromInclusive, toKey, toInclusive);
      }

      public void clear() {
         Iterator<Entry<K, V>> it = AbstractPatriciaTrie.this.entrySet().iterator();
         Set<K> currentKeys = this.keySet();

         while (it.hasNext()) {
            if (currentKeys.contains(((Entry)it.next()).getKey())) {
               it.remove();
            }
         }
      }
   }

   private class RangeEntryMap extends AbstractPatriciaTrie<K, V>.RangeMap {
      private final Object fromKey;
      private final Object toKey;
      private final boolean fromInclusive;
      private final boolean toInclusive;

      protected RangeEntryMap(K fromKey, K toKey) {
         this(fromKey, true, toKey, false);
      }

      protected RangeEntryMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
         if (fromKey == null && toKey == null) {
            throw new IllegalArgumentException("must have a from or to!");
         }

         if (fromKey != null && toKey != null && AbstractPatriciaTrie.this.getKeyAnalyzer().compare(fromKey, toKey) > 0) {
            throw new IllegalArgumentException("fromKey > toKey");
         }

         this.fromKey = fromKey;
         this.fromInclusive = fromInclusive;
         this.toKey = toKey;
         this.toInclusive = toInclusive;
      }

      public K firstKey() {
         Entry<K, V> e = null;
         AbstractPatriciaTrie.TrieEntry var3;
         if (this.fromKey == null) {
            var3 = AbstractPatriciaTrie.this.firstEntry();
         } else if (this.fromInclusive) {
            var3 = AbstractPatriciaTrie.this.ceilingEntry((K)this.fromKey);
         } else {
            var3 = AbstractPatriciaTrie.this.higherEntry((K)this.fromKey);
         }

         K first = (K)(var3 != null ? var3.getKey() : null);
         if (var3 != null && (this.toKey == null || this.inToRange((boolean)first, false))) {
            return first;
         } else {
            throw new NoSuchElementException();
         }
      }

      public K lastKey() {
         Entry<K, V> e;
         if (this.toKey == null) {
            e = AbstractPatriciaTrie.this.lastEntry();
         } else if (this.toInclusive) {
            e = AbstractPatriciaTrie.this.floorEntry((K)this.toKey);
         } else {
            e = AbstractPatriciaTrie.this.lowerEntry((K)this.toKey);
         }

         K last = (K)(e != null ? e.getKey() : null);
         if (e != null && (this.fromKey == null || this.inFromRange((boolean)last, false))) {
            return last;
         } else {
            throw new NoSuchElementException();
         }
      }

      @Override
      protected Set<Entry<K, V>> createEntrySet() {
         return AbstractPatriciaTrie.this.new RangeEntrySet(this);
      }

      @Override
      public K getFromKey() {
         return (K)this.fromKey;
      }

      @Override
      public K getToKey() {
         return (K)this.toKey;
      }

      @Override
      public boolean isFromInclusive() {
         return this.fromInclusive;
      }

      @Override
      public boolean isToInclusive() {
         return this.toInclusive;
      }

      @Override
      protected SortedMap<K, V> createRangeMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
         return AbstractPatriciaTrie.this.new RangeEntryMap(fromKey, fromInclusive, toKey, toInclusive);
      }
   }

   private class RangeEntrySet extends AbstractSet<Entry<K, V>> {
      private final AbstractPatriciaTrie<K, V>.RangeMap delegate;
      private transient int size = -1;
      private transient int expectedModCount;

      public RangeEntrySet(AbstractPatriciaTrie<K, V>.RangeMap delegate) {
         if (delegate == null) {
            throw new NullPointerException("delegate");
         }

         this.delegate = delegate;
      }

      public Iterator<Entry<K, V>> iterator() {
         K fromKey = this.delegate.getFromKey();
         K toKey = this.delegate.getToKey();
         AbstractPatriciaTrie.TrieEntry<K, V> first = null;
         if (fromKey == null) {
            first = AbstractPatriciaTrie.this.firstEntry();
         } else {
            first = AbstractPatriciaTrie.this.ceilingEntry(fromKey);
         }

         AbstractPatriciaTrie.TrieEntry<K, V> last = null;
         if (toKey != null) {
            last = AbstractPatriciaTrie.this.ceilingEntry(toKey);
         }

         return new AbstractPatriciaTrie.RangeEntrySet.EntryIterator(first, last);
      }

      public int size() {
         if (this.size == -1 || this.expectedModCount != AbstractPatriciaTrie.this.modCount) {
            this.size = 0;
            Iterator<?> it = this.iterator();

            while (it.hasNext()) {
               this.size++;
               it.next();
            }

            this.expectedModCount = AbstractPatriciaTrie.this.modCount;
         }

         return this.size;
      }

      public boolean isEmpty() {
         return !this.iterator().hasNext();
      }

      public boolean contains(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         }

         Entry<K, V> entry = (Entry<K, V>)o;
         K key = (K)entry.getKey();
         if (!this.delegate.inRange(key)) {
            return false;
         }

         AbstractPatriciaTrie.TrieEntry<K, V> node = AbstractPatriciaTrie.this.getEntry(key);
         return node != null && AbstractBitwiseTrie.compare(node.getValue(), entry.getValue());
      }

      public boolean remove(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         } else {
            Entry<K, V> entry = (Entry<K, V>)o;
            K key = (K)entry.getKey();
            if (!this.delegate.inRange(key)) {
               return false;
            } else {
               AbstractPatriciaTrie.TrieEntry<K, V> node = AbstractPatriciaTrie.this.getEntry(key);
               if (node != null && AbstractBitwiseTrie.compare(node.getValue(), entry.getValue())) {
                  AbstractPatriciaTrie.this.removeEntry(node);
                  return true;
               } else {
                  return false;
               }
            }
         }
      }

      private final class EntryIterator extends AbstractPatriciaTrie<K, V>.TrieIterator<Entry<K, V>> {
         private final Object excludedKey;

         private EntryIterator(AbstractPatriciaTrie.TrieEntry<K, V> first, AbstractPatriciaTrie.TrieEntry<K, V> last) {
            super(first);
            this.excludedKey = last != null ? last.getKey() : null;
         }

         @Override
         public boolean hasNext() {
            return this.next != null && !AbstractBitwiseTrie.compare(this.next.key, this.excludedKey);
         }

         public Entry<K, V> next() {
            if (this.next != null && !AbstractBitwiseTrie.compare(this.next.key, this.excludedKey)) {
               return this.nextEntry();
            } else {
               throw new NoSuchElementException();
            }
         }
      }
   }

   private abstract class RangeMap extends AbstractMap<K, V> implements SortedMap<K, V> {
      private transient volatile Set<Entry<K, V>> entrySet;

      private RangeMap() {
      }

      protected abstract Set<Entry<K, V>> createEntrySet();

      protected abstract K getFromKey();

      protected abstract boolean isFromInclusive();

      protected abstract K getToKey();

      protected abstract boolean isToInclusive();

      public Comparator<? super K> comparator() {
         return AbstractPatriciaTrie.this.comparator();
      }

      public boolean containsKey(Object key) {
         return !this.inRange((K)AbstractPatriciaTrie.this.castKey(key)) ? false : AbstractPatriciaTrie.this.containsKey(key);
      }

      public V remove(Object key) {
         return !this.inRange((K)AbstractPatriciaTrie.this.castKey(key)) ? null : AbstractPatriciaTrie.this.remove(key);
      }

      public V get(Object key) {
         return !this.inRange((K)AbstractPatriciaTrie.this.castKey(key)) ? null : AbstractPatriciaTrie.this.get(key);
      }

      public V put(K key, V value) {
         if (!this.inRange(key)) {
            throw new IllegalArgumentException("Key is out of range: " + key);
         } else {
            return AbstractPatriciaTrie.this.put(key, value);
         }
      }

      public Set<Entry<K, V>> entrySet() {
         if (this.entrySet == null) {
            this.entrySet = this.createEntrySet();
         }

         return this.entrySet;
      }

      public SortedMap<K, V> subMap(K fromKey, K toKey) {
         if (!this.inRange2(fromKey)) {
            throw new IllegalArgumentException("FromKey is out of range: " + fromKey);
         } else if (!this.inRange2(toKey)) {
            throw new IllegalArgumentException("ToKey is out of range: " + toKey);
         } else {
            return (SortedMap<K, V>)this.createRangeMap((boolean)fromKey, this.isFromInclusive(), (boolean)toKey, this.isToInclusive());
         }
      }

      public SortedMap<K, V> headMap(K toKey) {
         if (!this.inRange2(toKey)) {
            throw new IllegalArgumentException("ToKey is out of range: " + toKey);
         } else {
            return (SortedMap<K, V>)this.createRangeMap((boolean)this.getFromKey(), this.isFromInclusive(), (boolean)toKey, this.isToInclusive());
         }
      }

      public SortedMap<K, V> tailMap(K fromKey) {
         if (!this.inRange2(fromKey)) {
            throw new IllegalArgumentException("FromKey is out of range: " + fromKey);
         } else {
            return (SortedMap<K, V>)this.createRangeMap((boolean)fromKey, this.isFromInclusive(), (boolean)this.getToKey(), this.isToInclusive());
         }
      }

      protected boolean inRange(K key) {
         K fromKey = (K)this.getFromKey();
         K toKey = (K)this.getToKey();
         return (fromKey == null || this.inFromRange((boolean)key, false)) && (toKey == null || this.inToRange((boolean)key, false));
      }

      protected boolean inRange2(K key) {
         K fromKey = (K)this.getFromKey();
         K toKey = (K)this.getToKey();
         return (fromKey == null || this.inFromRange((boolean)key, false)) && (toKey == null || this.inToRange((boolean)key, true));
      }

      protected boolean inFromRange(K key, boolean forceInclusive) {
         K fromKey = (K)this.getFromKey();
         boolean fromInclusive = this.isFromInclusive();
         int ret = AbstractPatriciaTrie.this.getKeyAnalyzer().compare(key, fromKey);
         return !fromInclusive && !forceInclusive ? ret > 0 : ret >= 0;
      }

      protected boolean inToRange(K key, boolean forceInclusive) {
         K toKey = (K)this.getToKey();
         boolean toInclusive = this.isToInclusive();
         int ret = AbstractPatriciaTrie.this.getKeyAnalyzer().compare(key, toKey);
         return !toInclusive && !forceInclusive ? ret < 0 : ret <= 0;
      }

      protected abstract SortedMap<K, V> createRangeMap(K var1, boolean var2, K var3, boolean var4);
   }

   private static class Reference<E> {
      private E item;

      private Reference() {
      }

      public void set(E item) {
         this.item = item;
      }

      public E get() {
         return this.item;
      }
   }

   protected static class TrieEntry<K, V> extends BasicEntry<K, V> {
      private static final long serialVersionUID = 4596023148184140013L;
      protected int bitIndex;
      protected AbstractPatriciaTrie.TrieEntry<K, V> parent;
      protected AbstractPatriciaTrie.TrieEntry<K, V> left;
      protected AbstractPatriciaTrie.TrieEntry<K, V> right;
      protected AbstractPatriciaTrie.TrieEntry<K, V> predecessor;

      public TrieEntry(K key, V value, int bitIndex) {
         super(key, value);
         this.bitIndex = bitIndex;
         this.parent = null;
         this.left = this;
         this.right = null;
         this.predecessor = this;
      }

      public boolean isEmpty() {
         return this.key == null;
      }

      public boolean isInternalNode() {
         return this.left != this && this.right != this;
      }

      public boolean isExternalNode() {
         return !this.isInternalNode();
      }

      public String toString() {
         StringBuilder buffer = new StringBuilder();
         if (this.bitIndex == -1) {
            buffer.append("RootEntry(");
         } else {
            buffer.append("Entry(");
         }

         buffer.append("key=").append(this.getKey()).append(" [").append(this.bitIndex).append("], ");
         buffer.append("value=").append(this.getValue()).append(", ");
         if (this.parent != null) {
            if (this.parent.bitIndex == -1) {
               buffer.append("parent=").append("ROOT");
            } else {
               buffer.append("parent=").append(this.parent.getKey()).append(" [").append(this.parent.bitIndex).append("]");
            }
         } else {
            buffer.append("parent=").append("null");
         }

         buffer.append(", ");
         if (this.left != null) {
            if (this.left.bitIndex == -1) {
               buffer.append("left=").append("ROOT");
            } else {
               buffer.append("left=").append(this.left.getKey()).append(" [").append(this.left.bitIndex).append("]");
            }
         } else {
            buffer.append("left=").append("null");
         }

         buffer.append(", ");
         if (this.right != null) {
            if (this.right.bitIndex == -1) {
               buffer.append("right=").append("ROOT");
            } else {
               buffer.append("right=").append(this.right.getKey()).append(" [").append(this.right.bitIndex).append("]");
            }
         } else {
            buffer.append("right=").append("null");
         }

         buffer.append(", ");
         if (this.predecessor != null) {
            if (this.predecessor.bitIndex == -1) {
               buffer.append("predecessor=").append("ROOT");
            } else {
               buffer.append("predecessor=").append(this.predecessor.getKey()).append(" [").append(this.predecessor.bitIndex).append("]");
            }
         }

         buffer.append(")");
         return buffer.toString();
      }
   }

   abstract class TrieIterator<E> implements Iterator<E> {
      protected int expectedModCount;
      protected AbstractPatriciaTrie.TrieEntry<K, V> next;
      protected AbstractPatriciaTrie.TrieEntry<K, V> current;

      protected TrieIterator() {
         this.expectedModCount = AbstractPatriciaTrie.this.modCount;
         this.next = AbstractPatriciaTrie.this.nextEntry(null);
      }

      protected TrieIterator(AbstractPatriciaTrie.TrieEntry<K, V> firstEntry) {
         this.expectedModCount = AbstractPatriciaTrie.this.modCount;
         this.next = firstEntry;
      }

      protected AbstractPatriciaTrie.TrieEntry<K, V> nextEntry() {
         if (this.expectedModCount != AbstractPatriciaTrie.this.modCount) {
            throw new ConcurrentModificationException();
         }

         AbstractPatriciaTrie.TrieEntry<K, V> e = this.next;
         if (e == null) {
            throw new NoSuchElementException();
         }

         this.next = this.findNext(e);
         this.current = e;
         return e;
      }

      protected AbstractPatriciaTrie.TrieEntry<K, V> findNext(AbstractPatriciaTrie.TrieEntry<K, V> prior) {
         return AbstractPatriciaTrie.this.nextEntry(prior);
      }

      public boolean hasNext() {
         return this.next != null;
      }

      public void remove() {
         if (this.current == null) {
            throw new IllegalStateException();
         }

         if (this.expectedModCount != AbstractPatriciaTrie.this.modCount) {
            throw new ConcurrentModificationException();
         }

         AbstractPatriciaTrie.TrieEntry<K, V> node = this.current;
         this.current = null;
         AbstractPatriciaTrie.this.removeEntry(node);
         this.expectedModCount = AbstractPatriciaTrie.this.modCount;
      }
   }

   private class TrieMapIterator extends AbstractPatriciaTrie<K, V>.TrieIterator<K> implements OrderedMapIterator<K, V> {
      protected AbstractPatriciaTrie.TrieEntry<K, V> previous;

      private TrieMapIterator() {
      }

      public K next() {
         return (K)this.nextEntry().getKey();
      }

      public K getKey() {
         if (this.current == null) {
            throw new IllegalStateException();
         } else {
            return (K)this.current.getKey();
         }
      }

      public V getValue() {
         if (this.current == null) {
            throw new IllegalStateException();
         } else {
            return (V)this.current.getValue();
         }
      }

      public V setValue(V value) {
         if (this.current == null) {
            throw new IllegalStateException();
         } else {
            return (V)this.current.setValue(value);
         }
      }

      public boolean hasPrevious() {
         return this.previous != null;
      }

      public K previous() {
         return (K)this.previousEntry().getKey();
      }

      @Override
      protected AbstractPatriciaTrie.TrieEntry<K, V> nextEntry() {
         AbstractPatriciaTrie.TrieEntry<K, V> nextEntry = super.nextEntry();
         this.previous = nextEntry;
         return nextEntry;
      }

      protected AbstractPatriciaTrie.TrieEntry<K, V> previousEntry() {
         if (this.expectedModCount != AbstractPatriciaTrie.this.modCount) {
            throw new ConcurrentModificationException();
         }

         AbstractPatriciaTrie.TrieEntry<K, V> e = this.previous;
         if (e == null) {
            throw new NoSuchElementException();
         }

         this.previous = AbstractPatriciaTrie.this.previousEntry(e);
         this.next = this.current;
         this.current = e;
         return this.current;
      }
   }

   private class Values extends AbstractCollection<V> {
      private Values() {
      }

      public Iterator<V> iterator() {
         return new AbstractPatriciaTrie.Values.ValueIterator();
      }

      public int size() {
         return AbstractPatriciaTrie.this.size();
      }

      public boolean contains(Object o) {
         return AbstractPatriciaTrie.this.containsValue(o);
      }

      public void clear() {
         AbstractPatriciaTrie.this.clear();
      }

      public boolean remove(Object o) {
         Iterator<V> it = this.iterator();

         while (it.hasNext()) {
            V value = (V)it.next();
            if (AbstractBitwiseTrie.compare(value, o)) {
               it.remove();
               return true;
            }
         }

         return false;
      }

      private class ValueIterator extends AbstractPatriciaTrie<K, V>.TrieIterator<V> {
         private ValueIterator() {
         }

         public V next() {
            return (V)this.nextEntry().getValue();
         }
      }
   }
}
