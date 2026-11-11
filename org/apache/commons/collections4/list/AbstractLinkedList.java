package org.apache.commons.collections4.list;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.apache.commons.collections4.OrderedIterator;

public abstract class AbstractLinkedList<E> implements List<E> {
   transient AbstractLinkedList.Node<E> header;
   transient int size;
   transient int modCount;

   protected AbstractLinkedList() {
   }

   protected AbstractLinkedList(Collection<? extends E> coll) {
      this.init();
      this.addAll(coll);
   }

   protected void init() {
      this.header = this.createHeaderNode();
   }

   public int size() {
      return this.size;
   }

   public boolean isEmpty() {
      return this.size() == 0;
   }

   public E get(int index) {
      AbstractLinkedList.Node<E> node = this.getNode(index, false);
      return node.getValue();
   }

   public Iterator<E> iterator() {
      return this.listIterator();
   }

   public ListIterator<E> listIterator() {
      return new AbstractLinkedList.LinkedListIterator<>(this, 0);
   }

   public ListIterator<E> listIterator(int fromIndex) {
      return new AbstractLinkedList.LinkedListIterator<>(this, fromIndex);
   }

   public int indexOf(Object value) {
      int i = 0;

      for (AbstractLinkedList.Node<E> node = this.header.next; node != this.header; node = node.next) {
         if (this.isEqualValue(node.getValue(), value)) {
            return i;
         }

         i++;
      }

      return -1;
   }

   public int lastIndexOf(Object value) {
      int i = this.size - 1;

      for (AbstractLinkedList.Node<E> node = this.header.previous; node != this.header; node = node.previous) {
         if (this.isEqualValue(node.getValue(), value)) {
            return i;
         }

         i--;
      }

      return -1;
   }

   public boolean contains(Object value) {
      return this.indexOf(value) != -1;
   }

   public boolean containsAll(Collection<?> coll) {
      for (Object o : coll) {
         if (!this.contains(o)) {
            return false;
         }
      }

      return true;
   }

   public Object[] toArray() {
      return this.toArray(new Object[this.size]);
   }

   public <T> T[] toArray(T[] array) {
      if (array.length < this.size) {
         Class<?> componentType = array.getClass().getComponentType();
         array = (T[])Array.newInstance(componentType, this.size);
      }

      int i = 0;

      for (AbstractLinkedList.Node<E> node = this.header.next; node != this.header; i++) {
         array[i] = (T)node.getValue();
         node = node.next;
      }

      if (array.length > this.size) {
         array[this.size] = null;
      }

      return array;
   }

   public List<E> subList(int fromIndexInclusive, int toIndexExclusive) {
      return new AbstractLinkedList.LinkedSubList<E>(this, fromIndexInclusive, toIndexExclusive);
   }

   public boolean add(E value) {
      this.addLast(value);
      return true;
   }

   public void add(int index, E value) {
      AbstractLinkedList.Node<E> node = this.getNode(index, true);
      this.addNodeBefore(node, value);
   }

   public boolean addAll(Collection<? extends E> coll) {
      return this.addAll(this.size, coll);
   }

   public boolean addAll(int index, Collection<? extends E> coll) {
      AbstractLinkedList.Node<E> node = this.getNode(index, true);

      for (E e : coll) {
         this.addNodeBefore(node, e);
      }

      return true;
   }

   public E remove(int index) {
      AbstractLinkedList.Node<E> node = this.getNode(index, false);
      E oldValue = node.getValue();
      this.removeNode(node);
      return oldValue;
   }

   public boolean remove(Object value) {
      for (AbstractLinkedList.Node<E> node = this.header.next; node != this.header; node = node.next) {
         if (this.isEqualValue(node.getValue(), value)) {
            this.removeNode(node);
            return true;
         }
      }

      return false;
   }

   public boolean removeAll(Collection<?> coll) {
      boolean modified = false;
      Iterator<E> it = this.iterator();

      while (it.hasNext()) {
         if (coll.contains(it.next())) {
            it.remove();
            modified = true;
         }
      }

      return modified;
   }

   public boolean retainAll(Collection<?> coll) {
      boolean modified = false;
      Iterator<E> it = this.iterator();

      while (it.hasNext()) {
         if (!coll.contains(it.next())) {
            it.remove();
            modified = true;
         }
      }

      return modified;
   }

   public E set(int index, E value) {
      AbstractLinkedList.Node<E> node = this.getNode(index, false);
      E oldValue = node.getValue();
      this.updateNode(node, value);
      return oldValue;
   }

   public void clear() {
      this.removeAllNodes();
   }

   public E getFirst() {
      AbstractLinkedList.Node<E> node = this.header.next;
      if (node == this.header) {
         throw new NoSuchElementException();
      } else {
         return node.getValue();
      }
   }

   public E getLast() {
      AbstractLinkedList.Node<E> node = this.header.previous;
      if (node == this.header) {
         throw new NoSuchElementException();
      } else {
         return node.getValue();
      }
   }

   public boolean addFirst(E o) {
      this.addNodeAfter(this.header, o);
      return true;
   }

   public boolean addLast(E o) {
      this.addNodeBefore(this.header, o);
      return true;
   }

   public E removeFirst() {
      AbstractLinkedList.Node<E> node = this.header.next;
      if (node == this.header) {
         throw new NoSuchElementException();
      }

      E oldValue = node.getValue();
      this.removeNode(node);
      return oldValue;
   }

   public E removeLast() {
      AbstractLinkedList.Node<E> node = this.header.previous;
      if (node == this.header) {
         throw new NoSuchElementException();
      }

      E oldValue = node.getValue();
      this.removeNode(node);
      return oldValue;
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }

      if (!(obj instanceof List)) {
         return false;
      }

      List<?> other = (List<?>)obj;
      if (other.size() != this.size()) {
         return false;
      }

      ListIterator<?> it1 = this.listIterator();
      ListIterator<?> it2 = other.listIterator();

      while (it1.hasNext() && it2.hasNext()) {
         Object o1 = it1.next();
         Object o2 = it2.next();
         if (o1 == null ? o2 != null : !o1.equals(o2)) {
            return false;
         }
      }

      return !it1.hasNext() && !it2.hasNext();
   }

   public int hashCode() {
      int hashCode = 1;

      for (E e : this) {
         hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
      }

      return hashCode;
   }

   public String toString() {
      if (this.size() == 0) {
         return "[]";
      }

      StringBuilder buf = new StringBuilder(16 * this.size());
      buf.append('[');
      Iterator<E> it = this.iterator();
      boolean hasNext = it.hasNext();

      while (hasNext) {
         Object value = it.next();
         buf.append(value == this ? "(this Collection)" : value);
         hasNext = it.hasNext();
         if (hasNext) {
            buf.append(", ");
         }
      }

      buf.append(']');
      return buf.toString();
   }

   protected boolean isEqualValue(Object value1, Object value2) {
      return value1 == value2 || value1 != null && value1.equals(value2);
   }

   protected void updateNode(AbstractLinkedList.Node<E> node, E value) {
      node.setValue(value);
   }

   protected AbstractLinkedList.Node<E> createHeaderNode() {
      return new AbstractLinkedList.Node<>();
   }

   protected AbstractLinkedList.Node<E> createNode(E value) {
      return new AbstractLinkedList.Node<>(value);
   }

   protected void addNodeBefore(AbstractLinkedList.Node<E> node, E value) {
      AbstractLinkedList.Node<E> newNode = this.createNode(value);
      this.addNode(newNode, node);
   }

   protected void addNodeAfter(AbstractLinkedList.Node<E> node, E value) {
      AbstractLinkedList.Node<E> newNode = this.createNode(value);
      this.addNode(newNode, node.next);
   }

   protected void addNode(AbstractLinkedList.Node<E> nodeToInsert, AbstractLinkedList.Node<E> insertBeforeNode) {
      nodeToInsert.next = insertBeforeNode;
      nodeToInsert.previous = insertBeforeNode.previous;
      insertBeforeNode.previous.next = nodeToInsert;
      insertBeforeNode.previous = nodeToInsert;
      this.size++;
      this.modCount++;
   }

   protected void removeNode(AbstractLinkedList.Node<E> node) {
      node.previous.next = node.next;
      node.next.previous = node.previous;
      this.size--;
      this.modCount++;
   }

   protected void removeAllNodes() {
      this.header.next = this.header;
      this.header.previous = this.header;
      this.size = 0;
      this.modCount++;
   }

   protected AbstractLinkedList.Node<E> getNode(int index, boolean endMarkerAllowed) throws IndexOutOfBoundsException {
      if (index < 0) {
         throw new IndexOutOfBoundsException("Couldn't get the node: index (" + index + ") less than zero.");
      }

      if (!endMarkerAllowed && index == this.size) {
         throw new IndexOutOfBoundsException("Couldn't get the node: index (" + index + ") is the size of the list.");
      }

      if (index > this.size) {
         throw new IndexOutOfBoundsException("Couldn't get the node: index (" + index + ") greater than the size of the list (" + this.size + ").");
      }

      AbstractLinkedList.Node<E> node;
      if (index < this.size / 2) {
         node = this.header.next;

         for (int currentIndex = 0; currentIndex < index; currentIndex++) {
            node = node.next;
         }
      } else {
         node = this.header;

         for (int currentIndex = this.size; currentIndex > index; currentIndex--) {
            node = node.previous;
         }
      }

      return node;
   }

   protected Iterator<E> createSubListIterator(AbstractLinkedList.LinkedSubList<E> subList) {
      return this.createSubListListIterator(subList, 0);
   }

   protected ListIterator<E> createSubListListIterator(AbstractLinkedList.LinkedSubList<E> subList, int fromIndex) {
      return new AbstractLinkedList.LinkedSubListIterator<>(subList, fromIndex);
   }

   protected void doWriteObject(ObjectOutputStream outputStream) throws IOException {
      outputStream.writeInt(this.size());

      for (E e : this) {
         outputStream.writeObject(e);
      }
   }

   protected void doReadObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
      this.init();
      int size = inputStream.readInt();

      for (int i = 0; i < size; i++) {
         this.add((E)inputStream.readObject());
      }
   }

   protected static class LinkedListIterator<E> implements ListIterator<E>, OrderedIterator<E> {
      protected final AbstractLinkedList<E> parent;
      protected AbstractLinkedList.Node<E> next;
      protected int nextIndex;
      protected AbstractLinkedList.Node<E> current;
      protected int expectedModCount;

      protected LinkedListIterator(AbstractLinkedList<E> parent, int fromIndex) throws IndexOutOfBoundsException {
         this.parent = parent;
         this.expectedModCount = parent.modCount;
         this.next = parent.getNode(fromIndex, true);
         this.nextIndex = fromIndex;
      }

      protected void checkModCount() {
         if (this.parent.modCount != this.expectedModCount) {
            throw new ConcurrentModificationException();
         }
      }

      protected AbstractLinkedList.Node<E> getLastNodeReturned() throws IllegalStateException {
         if (this.current == null) {
            throw new IllegalStateException();
         } else {
            return this.current;
         }
      }

      public boolean hasNext() {
         return this.next != this.parent.header;
      }

      public E next() {
         this.checkModCount();
         if (!this.hasNext()) {
            throw new NoSuchElementException("No element at index " + this.nextIndex + ".");
         }

         E value = this.next.getValue();
         this.current = this.next;
         this.next = this.next.next;
         this.nextIndex++;
         return value;
      }

      public boolean hasPrevious() {
         return this.next.previous != this.parent.header;
      }

      public E previous() {
         this.checkModCount();
         if (!this.hasPrevious()) {
            throw new NoSuchElementException("Already at start of list.");
         }

         this.next = this.next.previous;
         E value = this.next.getValue();
         this.current = this.next;
         this.nextIndex--;
         return value;
      }

      public int nextIndex() {
         return this.nextIndex;
      }

      public int previousIndex() {
         return this.nextIndex() - 1;
      }

      public void remove() {
         this.checkModCount();
         if (this.current == this.next) {
            this.next = this.next.next;
            this.parent.removeNode(this.getLastNodeReturned());
         } else {
            this.parent.removeNode(this.getLastNodeReturned());
            this.nextIndex--;
         }

         this.current = null;
         this.expectedModCount++;
      }

      public void set(E obj) {
         this.checkModCount();
         this.getLastNodeReturned().setValue(obj);
      }

      public void add(E obj) {
         this.checkModCount();
         this.parent.addNodeBefore(this.next, obj);
         this.current = null;
         this.nextIndex++;
         this.expectedModCount++;
      }
   }

   protected static class LinkedSubList<E> extends AbstractList<E> {
      AbstractLinkedList<E> parent;
      int offset;
      int size;
      int expectedModCount;

      protected LinkedSubList(AbstractLinkedList<E> parent, int fromIndex, int toIndex) {
         if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
         }

         if (toIndex > parent.size()) {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
         }

         if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
         }

         this.parent = parent;
         this.offset = fromIndex;
         this.size = toIndex - fromIndex;
         this.expectedModCount = parent.modCount;
      }

      public int size() {
         this.checkModCount();
         return this.size;
      }

      public E get(int index) {
         this.rangeCheck(index, this.size);
         this.checkModCount();
         return this.parent.get(index + this.offset);
      }

      public void add(int index, E obj) {
         this.rangeCheck(index, this.size + 1);
         this.checkModCount();
         this.parent.add(index + this.offset, obj);
         this.expectedModCount = this.parent.modCount;
         this.size++;
         this.modCount++;
      }

      public E remove(int index) {
         this.rangeCheck(index, this.size);
         this.checkModCount();
         E result = this.parent.remove(index + this.offset);
         this.expectedModCount = this.parent.modCount;
         this.size--;
         this.modCount++;
         return result;
      }

      public boolean addAll(Collection<? extends E> coll) {
         return this.addAll(this.size, coll);
      }

      public boolean addAll(int index, Collection<? extends E> coll) {
         this.rangeCheck(index, this.size + 1);
         int cSize = coll.size();
         if (cSize == 0) {
            return false;
         }

         this.checkModCount();
         this.parent.addAll(this.offset + index, coll);
         this.expectedModCount = this.parent.modCount;
         this.size += cSize;
         this.modCount++;
         return true;
      }

      public E set(int index, E obj) {
         this.rangeCheck(index, this.size);
         this.checkModCount();
         return this.parent.set(index + this.offset, obj);
      }

      public void clear() {
         this.checkModCount();
         Iterator<E> it = this.iterator();

         while (it.hasNext()) {
            it.next();
            it.remove();
         }
      }

      public Iterator<E> iterator() {
         this.checkModCount();
         return this.parent.createSubListIterator(this);
      }

      public ListIterator<E> listIterator(int index) {
         this.rangeCheck(index, this.size + 1);
         this.checkModCount();
         return this.parent.createSubListListIterator(this, index);
      }

      public List<E> subList(int fromIndexInclusive, int toIndexExclusive) {
         return new AbstractLinkedList.LinkedSubList<E>(this.parent, fromIndexInclusive + this.offset, toIndexExclusive + this.offset);
      }

      protected void rangeCheck(int index, int beyond) {
         if (index < 0 || index >= beyond) {
            throw new IndexOutOfBoundsException("Index '" + index + "' out of bounds for size '" + this.size + "'");
         }
      }

      protected void checkModCount() {
         if (this.parent.modCount != this.expectedModCount) {
            throw new ConcurrentModificationException();
         }
      }
   }

   protected static class LinkedSubListIterator<E> extends AbstractLinkedList.LinkedListIterator<E> {
      protected final AbstractLinkedList.LinkedSubList<E> sub;

      protected LinkedSubListIterator(AbstractLinkedList.LinkedSubList<E> sub, int startIndex) {
         super(sub.parent, startIndex + sub.offset);
         this.sub = sub;
      }

      @Override
      public boolean hasNext() {
         return this.nextIndex() < this.sub.size;
      }

      @Override
      public boolean hasPrevious() {
         return this.previousIndex() >= 0;
      }

      @Override
      public int nextIndex() {
         return super.nextIndex() - this.sub.offset;
      }

      @Override
      public void add(E obj) {
         super.add(obj);
         this.sub.expectedModCount = this.parent.modCount;
         this.sub.size++;
      }

      @Override
      public void remove() {
         super.remove();
         this.sub.expectedModCount = this.parent.modCount;
         this.sub.size--;
      }
   }

   protected static class Node<E> {
      protected AbstractLinkedList.Node<E> previous;
      protected AbstractLinkedList.Node<E> next;
      protected E value;

      protected Node() {
         this.previous = this;
         this.next = this;
      }

      protected Node(E value) {
         this.value = value;
      }

      protected Node(AbstractLinkedList.Node<E> previous, AbstractLinkedList.Node<E> next, E value) {
         this.previous = previous;
         this.next = next;
         this.value = value;
      }

      protected E getValue() {
         return this.value;
      }

      protected void setValue(E value) {
         this.value = value;
      }

      protected AbstractLinkedList.Node<E> getPreviousNode() {
         return this.previous;
      }

      protected void setPreviousNode(AbstractLinkedList.Node<E> previous) {
         this.previous = previous;
      }

      protected AbstractLinkedList.Node<E> getNextNode() {
         return this.next;
      }

      protected void setNextNode(AbstractLinkedList.Node<E> next) {
         this.next = next;
      }
   }
}
