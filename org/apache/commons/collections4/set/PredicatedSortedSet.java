package org.apache.commons.collections4.set;

import java.util.Comparator;
import java.util.SortedSet;
import org.apache.commons.collections4.Predicate;

public class PredicatedSortedSet<E> extends PredicatedSet<E> implements SortedSet<E> {
   private static final long serialVersionUID = -9110948148132275052L;

   public static <E> PredicatedSortedSet<E> predicatedSortedSet(SortedSet<E> set, Predicate<? super E> predicate) {
      return new PredicatedSortedSet<>(set, predicate);
   }

   protected PredicatedSortedSet(SortedSet<E> set, Predicate<? super E> predicate) {
      super(set, predicate);
   }

   protected SortedSet<E> decorated() {
      return (SortedSet<E>)super.decorated();
   }

   public Comparator<? super E> comparator() {
      return this.decorated().comparator();
   }

   public E first() {
      return (E)this.decorated().first();
   }

   public E last() {
      return (E)this.decorated().last();
   }

   public SortedSet<E> subSet(E fromElement, E toElement) {
      SortedSet<E> sub = this.decorated().subSet(fromElement, toElement);
      return new PredicatedSortedSet<>(sub, this.predicate);
   }

   public SortedSet<E> headSet(E toElement) {
      SortedSet<E> head = this.decorated().headSet(toElement);
      return new PredicatedSortedSet<>(head, this.predicate);
   }

   public SortedSet<E> tailSet(E fromElement) {
      SortedSet<E> tail = this.decorated().tailSet(fromElement);
      return new PredicatedSortedSet<>(tail, this.predicate);
   }
}
