package org.apache.commons.collections4.set;

import java.util.Iterator;
import java.util.NavigableSet;

public abstract class AbstractNavigableSetDecorator<E> extends AbstractSortedSetDecorator<E> implements NavigableSet<E> {
   private static final long serialVersionUID = 20150528L;

   protected AbstractNavigableSetDecorator() {
   }

   protected AbstractNavigableSetDecorator(NavigableSet<E> set) {
      super(set);
   }

   protected NavigableSet<E> decorated() {
      return (NavigableSet<E>)super.decorated();
   }

   public E lower(E e) {
      return (E)this.decorated().lower(e);
   }

   public E floor(E e) {
      return (E)this.decorated().floor(e);
   }

   public E ceiling(E e) {
      return (E)this.decorated().ceiling(e);
   }

   public E higher(E e) {
      return (E)this.decorated().higher(e);
   }

   public E pollFirst() {
      return (E)this.decorated().pollFirst();
   }

   public E pollLast() {
      return (E)this.decorated().pollLast();
   }

   public NavigableSet<E> descendingSet() {
      return this.decorated().descendingSet();
   }

   public Iterator<E> descendingIterator() {
      return this.decorated().descendingIterator();
   }

   public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
      return this.decorated().subSet(fromElement, fromInclusive, toElement, toInclusive);
   }

   public NavigableSet<E> headSet(E toElement, boolean inclusive) {
      return this.decorated().headSet(toElement, inclusive);
   }

   public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      return this.decorated().tailSet(fromElement, inclusive);
   }
}
