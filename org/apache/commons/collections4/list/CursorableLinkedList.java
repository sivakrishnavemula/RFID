package org.apache.commons.collections4.list;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.collections4.list.AbstractLinkedList.LinkedListIterator;
import org.apache.commons.collections4.list.AbstractLinkedList.LinkedSubList;
import org.apache.commons.collections4.list.AbstractLinkedList.Node;

public class CursorableLinkedList<E> extends AbstractLinkedList<E> implements Serializable {
   private static final long serialVersionUID = 8836393098519411393L;
   private transient List<WeakReference<CursorableLinkedList.Cursor<E>>> cursors;

   public CursorableLinkedList() {
      this.init();
   }

   public CursorableLinkedList(Collection<? extends E> coll) {
      super(coll);
   }

   protected void init() {
      super.init();
      this.cursors = new ArrayList();
   }

   public Iterator<E> iterator() {
      return super.listIterator(0);
   }

   public ListIterator<E> listIterator() {
      return this.cursor(0);
   }

   public ListIterator<E> listIterator(int fromIndex) {
      return this.cursor(fromIndex);
   }

   public CursorableLinkedList.Cursor<E> cursor() {
      return this.cursor(0);
   }

   public CursorableLinkedList.Cursor<E> cursor(int fromIndex) {
      CursorableLinkedList.Cursor<E> cursor = new CursorableLinkedList.Cursor<>(this, fromIndex);
      this.registerCursor(cursor);
      return cursor;
   }

   protected void updateNode(Node<E> node, E value) {
      super.updateNode(node, value);
      this.broadcastNodeChanged(node);
   }

   protected void addNode(Node<E> nodeToInsert, Node<E> insertBeforeNode) {
      super.addNode(nodeToInsert, insertBeforeNode);
      this.broadcastNodeInserted(nodeToInsert);
   }

   protected void removeNode(Node<E> node) {
      super.removeNode(node);
      this.broadcastNodeRemoved(node);
   }

   protected void removeAllNodes() {
      if (this.size() > 0) {
         Iterator<E> it = this.iterator();

         while (it.hasNext()) {
            it.next();
            it.remove();
         }
      }
   }

   protected void registerCursor(CursorableLinkedList.Cursor<E> cursor) {
      Iterator<WeakReference<CursorableLinkedList.Cursor<E>>> it = this.cursors.iterator();

      while (it.hasNext()) {
         WeakReference<CursorableLinkedList.Cursor<E>> ref = (WeakReference<CursorableLinkedList.Cursor<E>>)it.next();
         if (ref.get() == null) {
            it.remove();
         }
      }

      this.cursors.add(new WeakReference(cursor));
   }

   protected void unregisterCursor(CursorableLinkedList.Cursor<E> cursor) {
      Iterator<WeakReference<CursorableLinkedList.Cursor<E>>> it = this.cursors.iterator();

      while (it.hasNext()) {
         WeakReference<CursorableLinkedList.Cursor<E>> ref = (WeakReference<CursorableLinkedList.Cursor<E>>)it.next();
         CursorableLinkedList.Cursor<E> cur = (CursorableLinkedList.Cursor<E>)ref.get();
         if (cur == null) {
            it.remove();
         } else if (cur == cursor) {
            ref.clear();
            it.remove();
            break;
         }
      }
   }

   protected void broadcastNodeChanged(Node<E> node) {
      Iterator<WeakReference<CursorableLinkedList.Cursor<E>>> it = this.cursors.iterator();

      while (it.hasNext()) {
         WeakReference<CursorableLinkedList.Cursor<E>> ref = (WeakReference<CursorableLinkedList.Cursor<E>>)it.next();
         CursorableLinkedList.Cursor<E> cursor = (CursorableLinkedList.Cursor<E>)ref.get();
         if (cursor == null) {
            it.remove();
         } else {
            cursor.nodeChanged(node);
         }
      }
   }

   protected void broadcastNodeRemoved(Node<E> node) {
      Iterator<WeakReference<CursorableLinkedList.Cursor<E>>> it = this.cursors.iterator();

      while (it.hasNext()) {
         WeakReference<CursorableLinkedList.Cursor<E>> ref = (WeakReference<CursorableLinkedList.Cursor<E>>)it.next();
         CursorableLinkedList.Cursor<E> cursor = (CursorableLinkedList.Cursor<E>)ref.get();
         if (cursor == null) {
            it.remove();
         } else {
            cursor.nodeRemoved(node);
         }
      }
   }

   protected void broadcastNodeInserted(Node<E> node) {
      Iterator<WeakReference<CursorableLinkedList.Cursor<E>>> it = this.cursors.iterator();

      while (it.hasNext()) {
         WeakReference<CursorableLinkedList.Cursor<E>> ref = (WeakReference<CursorableLinkedList.Cursor<E>>)it.next();
         CursorableLinkedList.Cursor<E> cursor = (CursorableLinkedList.Cursor<E>)ref.get();
         if (cursor == null) {
            it.remove();
         } else {
            cursor.nodeInserted(node);
         }
      }
   }

   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      this.doWriteObject(out);
   }

   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      this.doReadObject(in);
   }

   protected ListIterator<E> createSubListListIterator(LinkedSubList<E> subList, int fromIndex) {
      CursorableLinkedList.SubCursor<E> cursor = new CursorableLinkedList.SubCursor<>(subList, fromIndex);
      this.registerCursor(cursor);
      return cursor;
   }

   public static class Cursor<E> extends LinkedListIterator<E> {
      boolean valid = true;
      boolean nextIndexValid = true;
      boolean currentRemovedByAnother = false;

      protected Cursor(CursorableLinkedList<E> parent, int index) {
         super(parent, index);
         this.valid = true;
      }

      public void remove() {
         if (this.current != null || !this.currentRemovedByAnother) {
            this.checkModCount();
            this.parent.removeNode(this.getLastNodeReturned());
         }

         this.currentRemovedByAnother = false;
      }

      public void add(E obj) {
         super.add(obj);
         this.next = this.next.next;
      }

      public int nextIndex() {
         if (!this.nextIndexValid) {
            if (this.next == this.parent.header) {
               this.nextIndex = this.parent.size();
            } else {
               int pos = 0;

               for (Node<E> temp = this.parent.header.next; temp != this.next; temp = temp.next) {
                  pos++;
               }

               this.nextIndex = pos;
            }

            this.nextIndexValid = true;
         }

         return this.nextIndex;
      }

      protected void nodeChanged(Node<E> node) {
      }

      protected void nodeRemoved(Node<E> node) {
         if (node == this.next && node == this.current) {
            this.next = node.next;
            this.current = null;
            this.currentRemovedByAnother = true;
         } else if (node == this.next) {
            this.next = node.next;
            this.currentRemovedByAnother = false;
         } else if (node == this.current) {
            this.current = null;
            this.currentRemovedByAnother = true;
            this.nextIndex--;
         } else {
            this.nextIndexValid = false;
            this.currentRemovedByAnother = false;
         }
      }

      protected void nodeInserted(Node<E> node) {
         if (node.previous == this.current) {
            this.next = node;
         } else if (this.next.previous == node) {
            this.next = node;
         } else {
            this.nextIndexValid = false;
         }
      }

      protected void checkModCount() {
         if (!this.valid) {
            throw new ConcurrentModificationException("Cursor closed");
         }
      }

      public void close() {
         if (this.valid) {
            ((CursorableLinkedList)this.parent).unregisterCursor(this);
            this.valid = false;
         }
      }
   }

   protected static class SubCursor<E> extends CursorableLinkedList.Cursor<E> {
      protected final LinkedSubList<E> sub;

      protected SubCursor(LinkedSubList<E> sub, int index) {
         super((CursorableLinkedList<E>)sub.parent, index + sub.offset);
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
}
