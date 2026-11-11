package org.apache.commons.collections4;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.collections4.set.PredicatedNavigableSet;
import org.apache.commons.collections4.set.PredicatedSet;
import org.apache.commons.collections4.set.PredicatedSortedSet;
import org.apache.commons.collections4.set.TransformedNavigableSet;
import org.apache.commons.collections4.set.TransformedSet;
import org.apache.commons.collections4.set.TransformedSortedSet;
import org.apache.commons.collections4.set.UnmodifiableNavigableSet;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.collections4.set.UnmodifiableSortedSet;

public class SetUtils {
   public static final SortedSet EMPTY_SORTED_SET = UnmodifiableSortedSet.unmodifiableSortedSet(new TreeSet());

   public static <E> Set<E> emptySet() {
      return Collections.emptySet();
   }

   public static <E> SortedSet<E> emptySortedSet() {
      return EMPTY_SORTED_SET;
   }

   private SetUtils() {
   }

   public static <T> Set<T> emptyIfNull(Set<T> set) {
      return set == null ? Collections.emptySet() : set;
   }

   public static boolean isEqualSet(Collection<?> set1, Collection<?> set2) {
      if (set1 == set2) {
         return true;
      } else {
         return set1 != null && set2 != null && set1.size() == set2.size() ? set1.containsAll(set2) : false;
      }
   }

   public static <T> int hashCodeForSet(Collection<T> set) {
      if (set == null) {
         return 0;
      }

      int hashCode = 0;

      for (T obj : set) {
         if (obj != null) {
            hashCode += obj.hashCode();
         }
      }

      return hashCode;
   }

   public static <E> Set<E> newIdentityHashSet() {
      return Collections.newSetFromMap(new IdentityHashMap());
   }

   public static <E> Set<E> synchronizedSet(Set<E> set) {
      return Collections.synchronizedSet(set);
   }

   public static <E> Set<E> unmodifiableSet(Set<? extends E> set) {
      return UnmodifiableSet.unmodifiableSet(set);
   }

   public static <E> Set<E> predicatedSet(Set<E> set, Predicate<? super E> predicate) {
      return PredicatedSet.predicatedSet(set, predicate);
   }

   public static <E> Set<E> transformedSet(Set<E> set, Transformer<? super E, ? extends E> transformer) {
      return TransformedSet.transformingSet(set, transformer);
   }

   public static <E> Set<E> orderedSet(Set<E> set) {
      return ListOrderedSet.listOrderedSet(set);
   }

   public static <E> SortedSet<E> synchronizedSortedSet(SortedSet<E> set) {
      return Collections.synchronizedSortedSet(set);
   }

   public static <E> SortedSet<E> unmodifiableSortedSet(SortedSet<E> set) {
      return UnmodifiableSortedSet.unmodifiableSortedSet(set);
   }

   public static <E> SortedSet<E> predicatedSortedSet(SortedSet<E> set, Predicate<? super E> predicate) {
      return PredicatedSortedSet.predicatedSortedSet(set, predicate);
   }

   public static <E> SortedSet<E> transformedSortedSet(SortedSet<E> set, Transformer<? super E, ? extends E> transformer) {
      return TransformedSortedSet.transformingSortedSet(set, transformer);
   }

   public static <E> SortedSet<E> unmodifiableNavigableSet(NavigableSet<E> set) {
      return UnmodifiableNavigableSet.unmodifiableNavigableSet(set);
   }

   public static <E> SortedSet<E> predicatedNavigableSet(NavigableSet<E> set, Predicate<? super E> predicate) {
      return PredicatedNavigableSet.predicatedNavigableSet(set, predicate);
   }

   public static <E> SortedSet<E> transformedNavigableSet(NavigableSet<E> set, Transformer<? super E, ? extends E> transformer) {
      return TransformedNavigableSet.transformingNavigableSet(set, transformer);
   }

   public static <E> SetUtils.SetView<E> union(final Set<? extends E> a, final Set<? extends E> b) {
      if (a != null && b != null) {
         final SetUtils.SetView<E> bMinusA = difference(b, a);
         return new SetUtils.SetView<E>() {
            public boolean contains(Object o) {
               return a.contains(o) || b.contains(o);
            }

            @Override
            public Iterator<E> createIterator() {
               return IteratorUtils.chainedIterator(a.iterator(), bMinusA.iterator());
            }

            public boolean isEmpty() {
               return a.isEmpty() && b.isEmpty();
            }

            @Override
            public int size() {
               return a.size() + bMinusA.size();
            }
         };
      } else {
         throw new NullPointerException("Sets must not be null.");
      }
   }

   public static <E> SetUtils.SetView<E> difference(final Set<? extends E> a, final Set<? extends E> b) {
      if (a != null && b != null) {
         final Predicate<E> notContainedInB = new Predicate<E>() {
            public boolean evaluate(E object) {
               return !b.contains(object);
            }
         };
         return new SetUtils.SetView<E>() {
            public boolean contains(Object o) {
               return a.contains(o) && !b.contains(o);
            }

            @Override
            public Iterator<E> createIterator() {
               return IteratorUtils.filteredIterator(a.iterator(), notContainedInB);
            }
         };
      } else {
         throw new NullPointerException("Sets must not be null.");
      }
   }

   public static <E> SetUtils.SetView<E> intersection(final Set<? extends E> a, final Set<? extends E> b) {
      if (a != null && b != null) {
         final Predicate<E> containedInB = new Predicate<E>() {
            public boolean evaluate(E object) {
               return b.contains(object);
            }
         };
         return new SetUtils.SetView<E>() {
            public boolean contains(Object o) {
               return a.contains(o) && b.contains(o);
            }

            @Override
            public Iterator<E> createIterator() {
               return IteratorUtils.filteredIterator(a.iterator(), containedInB);
            }
         };
      } else {
         throw new NullPointerException("Sets must not be null.");
      }
   }

   public static <E> SetUtils.SetView<E> disjunction(final Set<? extends E> a, final Set<? extends E> b) {
      if (a != null && b != null) {
         final SetUtils.SetView<E> aMinusB = difference(a, b);
         final SetUtils.SetView<E> bMinusA = difference(b, a);
         return new SetUtils.SetView<E>() {
            public boolean contains(Object o) {
               return a.contains(o) ^ b.contains(o);
            }

            @Override
            public Iterator<E> createIterator() {
               return IteratorUtils.chainedIterator(aMinusB.iterator(), bMinusA.iterator());
            }

            public boolean isEmpty() {
               return aMinusB.isEmpty() && bMinusA.isEmpty();
            }

            @Override
            public int size() {
               return aMinusB.size() + bMinusA.size();
            }
         };
      } else {
         throw new NullPointerException("Sets must not be null.");
      }
   }

   public abstract static class SetView<E> extends AbstractSet<E> {
      public Iterator<E> iterator() {
         return IteratorUtils.unmodifiableIterator(this.createIterator());
      }

      protected abstract Iterator<E> createIterator();

      public int size() {
         return IteratorUtils.size(this.iterator());
      }

      public <S extends Set<E>> void copyInto(S set) {
         CollectionUtils.addAll(set, this);
      }

      public Set<E> toSet() {
         Set<E> set = new HashSet(this.size());
         this.copyInto(set);
         return set;
      }
   }
}
