package org.apache.commons.collections4.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class BoundedIterator<E> implements Iterator<E> {
   private final Iterator<? extends E> iterator;
   private final long offset;
   private final long max;
   private long pos;

   public BoundedIterator(Iterator<? extends E> iterator, long offset, long max) {
      if (iterator == null) {
         throw new NullPointerException("Iterator must not be null");
      }

      if (offset < 0L) {
         throw new IllegalArgumentException("Offset parameter must not be negative.");
      }

      if (max < 0L) {
         throw new IllegalArgumentException("Max parameter must not be negative.");
      }

      this.iterator = iterator;
      this.offset = offset;
      this.max = max;
      this.pos = 0L;
      this.init();
   }

   private void init() {
      while (this.pos < this.offset && this.iterator.hasNext()) {
         this.iterator.next();
         this.pos++;
      }
   }

   public boolean hasNext() {
      return !this.checkBounds() ? false : this.iterator.hasNext();
   }

   private boolean checkBounds() {
      return this.pos - this.offset + 1L <= this.max;
   }

   public E next() {
      if (!this.checkBounds()) {
         throw new NoSuchElementException();
      }

      E next = (E)this.iterator.next();
      this.pos++;
      return next;
   }

   public void remove() {
      if (this.pos <= this.offset) {
         throw new IllegalStateException("remove() can not be called before calling next()");
      }

      this.iterator.remove();
   }
}
