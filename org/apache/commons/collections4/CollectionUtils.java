package org.apache.commons.collections4;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.collection.PredicatedCollection;
import org.apache.commons.collections4.collection.SynchronizedCollection;
import org.apache.commons.collections4.collection.TransformedCollection;
import org.apache.commons.collections4.collection.UnmodifiableBoundedCollection;
import org.apache.commons.collections4.collection.UnmodifiableCollection;
import org.apache.commons.collections4.functors.TruePredicate;
import org.apache.commons.collections4.iterators.CollatingIterator;
import org.apache.commons.collections4.iterators.PermutationIterator;

public class CollectionUtils {
   public static final Collection EMPTY_COLLECTION = Collections.emptyList();

   private CollectionUtils() {
   }

   public static <T> Collection<T> emptyCollection() {
      return EMPTY_COLLECTION;
   }

   public static <T> Collection<T> emptyIfNull(Collection<T> collection) {
      return collection == null ? emptyCollection() : collection;
   }

   public static <O> Collection<O> union(Iterable<? extends O> a, Iterable<? extends O> b) {
      CollectionUtils.SetOperationCardinalityHelper<O> helper = new CollectionUtils.SetOperationCardinalityHelper<>(a, b);

      for (O obj : helper) {
         helper.setCardinality(obj, helper.max(obj));
      }

      return helper.list();
   }

   public static <O> Collection<O> intersection(Iterable<? extends O> a, Iterable<? extends O> b) {
      CollectionUtils.SetOperationCardinalityHelper<O> helper = new CollectionUtils.SetOperationCardinalityHelper<>(a, b);

      for (O obj : helper) {
         helper.setCardinality(obj, helper.min(obj));
      }

      return helper.list();
   }

   public static <O> Collection<O> disjunction(Iterable<? extends O> a, Iterable<? extends O> b) {
      CollectionUtils.SetOperationCardinalityHelper<O> helper = new CollectionUtils.SetOperationCardinalityHelper<>(a, b);

      for (O obj : helper) {
         helper.setCardinality(obj, helper.max(obj) - helper.min(obj));
      }

      return helper.list();
   }

   public static <O> Collection<O> subtract(Iterable<? extends O> a, Iterable<? extends O> b) {
      Predicate<O> p = TruePredicate.truePredicate();
      return subtract(a, b, p);
   }

   public static <O> Collection<O> subtract(Iterable<? extends O> a, Iterable<? extends O> b, Predicate<O> p) {
      ArrayList<O> list = new ArrayList();
      HashBag<O> bag = new HashBag();

      for (O element : b) {
         if (p.evaluate(element)) {
            bag.add(element);
         }
      }

      for (O element : a) {
         if (!bag.remove(element, 1)) {
            list.add(element);
         }
      }

      return list;
   }

   public static boolean containsAll(Collection<?> coll1, Collection<?> coll2) {
      if (coll2.isEmpty()) {
         return true;
      }

      Iterator<?> it = coll1.iterator();
      Set<Object> elementsAlreadySeen = new HashSet();

      for (Object nextElement : coll2) {
         if (!elementsAlreadySeen.contains(nextElement)) {
            boolean foundCurrentElement = false;

            while (it.hasNext()) {
               Object p = it.next();
               elementsAlreadySeen.add(p);
               if (nextElement == null ? p == null : nextElement.equals(p)) {
                  foundCurrentElement = true;
                  break;
               }
            }

            if (!foundCurrentElement) {
               return false;
            }
         }
      }

      return true;
   }

   public static <T> boolean containsAny(Collection<?> coll1, T... coll2) {
      if (coll1.size() < coll2.length) {
         for (Object aColl1 : coll1) {
            if (ArrayUtils.contains(coll2, aColl1)) {
               return true;
            }
         }
      } else {
         for (Object aColl2 : coll2) {
            if (coll1.contains(aColl2)) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean containsAny(Collection<?> coll1, Collection<?> coll2) {
      if (coll1.size() < coll2.size()) {
         for (Object aColl1 : coll1) {
            if (coll2.contains(aColl1)) {
               return true;
            }
         }
      } else {
         for (Object aColl2 : coll2) {
            if (coll1.contains(aColl2)) {
               return true;
            }
         }
      }

      return false;
   }

   public static <O> Map<O, Integer> getCardinalityMap(Iterable<? extends O> coll) {
      Map<O, Integer> count = new HashMap();

      for (O obj : coll) {
         Integer c = (Integer)count.get(obj);
         if (c == null) {
            count.put(obj, 1);
         } else {
            count.put(obj, c + 1);
         }
      }

      return count;
   }

   public static boolean isSubCollection(Collection<?> a, Collection<?> b) {
      CollectionUtils.CardinalityHelper<Object> helper = new CollectionUtils.CardinalityHelper<>((Iterable<? extends Object>)a, (Iterable<? extends Object>)b);

      for (Object obj : a) {
         if (helper.freqA(obj) > helper.freqB(obj)) {
            return false;
         }
      }

      return true;
   }

   public static boolean isProperSubCollection(Collection<?> a, Collection<?> b) {
      return a.size() < b.size() && isSubCollection(a, b);
   }

   public static boolean isEqualCollection(Collection<?> a, Collection<?> b) {
      if (a.size() != b.size()) {
         return false;
      }

      CollectionUtils.CardinalityHelper<Object> helper = new CollectionUtils.CardinalityHelper<>((Iterable<? extends Object>)a, (Iterable<? extends Object>)b);
      if (helper.cardinalityA.size() != helper.cardinalityB.size()) {
         return false;
      }

      for (Object obj : helper.cardinalityA.keySet()) {
         if (helper.freqA(obj) != helper.freqB(obj)) {
            return false;
         }
      }

      return true;
   }

   public static <E> boolean isEqualCollection(Collection<? extends E> a, Collection<? extends E> b, final Equator<? super E> equator) {
      if (equator == null) {
         throw new NullPointerException("Equator must not be null.");
      }

      if (a.size() != b.size()) {
         return false;
      }

      Transformer<E, ?> transformer = new Transformer() {
         public CollectionUtils.EquatorWrapper<?> transform(Object input) {
            return new CollectionUtils.EquatorWrapper<>(equator, input);
         }
      };
      return isEqualCollection(collect(a, transformer), collect(b, transformer));
   }

   @Deprecated
   public static <O> int cardinality(O obj, Iterable<? super O> coll) {
      if (coll == null) {
         throw new NullPointerException("coll must not be null.");
      } else {
         return IterableUtils.frequency(coll, obj);
      }
   }

   @Deprecated
   public static <T> T find(Iterable<T> collection, Predicate<? super T> predicate) {
      return (T)(predicate != null ? IterableUtils.find(collection, predicate) : null);
   }

   @Deprecated
   public static <T, C extends Closure<? super T>> C forAllDo(Iterable<T> collection, C closure) {
      if (closure != null) {
         IterableUtils.forEach(collection, closure);
      }

      return closure;
   }

   @Deprecated
   public static <T, C extends Closure<? super T>> C forAllDo(Iterator<T> iterator, C closure) {
      if (closure != null) {
         IteratorUtils.forEach(iterator, closure);
      }

      return closure;
   }

   @Deprecated
   public static <T, C extends Closure<? super T>> T forAllButLastDo(Iterable<T> collection, C closure) {
      return (T)(closure != null ? IterableUtils.forEachButLast(collection, closure) : null);
   }

   @Deprecated
   public static <T, C extends Closure<? super T>> T forAllButLastDo(Iterator<T> iterator, C closure) {
      return (T)(closure != null ? IteratorUtils.forEachButLast(iterator, closure) : null);
   }

   public static <T> boolean filter(Iterable<T> collection, Predicate<? super T> predicate) {
      boolean result = false;
      if (collection != null && predicate != null) {
         Iterator<T> it = collection.iterator();

         while (it.hasNext()) {
            if (!predicate.evaluate(it.next())) {
               it.remove();
               result = true;
            }
         }
      }

      return result;
   }

   public static <T> boolean filterInverse(Iterable<T> collection, Predicate<? super T> predicate) {
      return filter(collection, predicate == null ? null : PredicateUtils.notPredicate(predicate));
   }

   public static <C> void transform(Collection<C> collection, Transformer<? super C, ? extends C> transformer) {
      if (collection != null && transformer != null) {
         if (collection instanceof List) {
            List<C> list = (List<C>)collection;
            ListIterator<C> it = list.listIterator();

            while (it.hasNext()) {
               it.set(transformer.transform(it.next()));
            }
         } else {
            Collection<C> resultCollection = collect(collection, transformer);
            collection.clear();
            collection.addAll(resultCollection);
         }
      }
   }

   @Deprecated
   public static <C> int countMatches(Iterable<C> input, Predicate<? super C> predicate) {
      return predicate == null ? 0 : (int)IterableUtils.countMatches(input, predicate);
   }

   @Deprecated
   public static <C> boolean exists(Iterable<C> input, Predicate<? super C> predicate) {
      return predicate != null && IterableUtils.matchesAny(input, predicate);
   }

   @Deprecated
   public static <C> boolean matchesAll(Iterable<C> input, Predicate<? super C> predicate) {
      return predicate != null && IterableUtils.matchesAll(input, predicate);
   }

   public static <O> Collection<O> select(Iterable<? extends O> inputCollection, Predicate<? super O> predicate) {
      Collection<O> answer = inputCollection instanceof Collection ? new ArrayList(((Collection)inputCollection).size()) : new ArrayList();
      return select(inputCollection, predicate, answer);
   }

   public static <O, R extends Collection<? super O>> R select(Iterable<? extends O> inputCollection, Predicate<? super O> predicate, R outputCollection) {
      if (inputCollection != null && predicate != null) {
         for (O item : inputCollection) {
            if (predicate.evaluate(item)) {
               outputCollection.add(item);
            }
         }
      }

      return outputCollection;
   }

   public static <O, R extends Collection<? super O>> R select(
      Iterable<? extends O> inputCollection, Predicate<? super O> predicate, R outputCollection, R rejectedCollection
   ) {
      if (inputCollection != null && predicate != null) {
         for (O element : inputCollection) {
            if (predicate.evaluate(element)) {
               outputCollection.add(element);
            } else {
               rejectedCollection.add(element);
            }
         }
      }

      return outputCollection;
   }

   public static <O> Collection<O> selectRejected(Iterable<? extends O> inputCollection, Predicate<? super O> predicate) {
      Collection<O> answer = inputCollection instanceof Collection ? new ArrayList(((Collection)inputCollection).size()) : new ArrayList();
      return selectRejected(inputCollection, predicate, answer);
   }

   public static <O, R extends Collection<? super O>> R selectRejected(
      Iterable<? extends O> inputCollection, Predicate<? super O> predicate, R outputCollection
   ) {
      if (inputCollection != null && predicate != null) {
         for (O item : inputCollection) {
            if (!predicate.evaluate(item)) {
               outputCollection.add(item);
            }
         }
      }

      return outputCollection;
   }

   public static <I, O> Collection<O> collect(Iterable<I> inputCollection, Transformer<? super I, ? extends O> transformer) {
      Collection<O> answer = inputCollection instanceof Collection ? new ArrayList(((Collection)inputCollection).size()) : new ArrayList();
      return collect(inputCollection, transformer, answer);
   }

   public static <I, O> Collection<O> collect(Iterator<I> inputIterator, Transformer<? super I, ? extends O> transformer) {
      return collect(inputIterator, transformer, new ArrayList());
   }

   public static <I, O, R extends Collection<? super O>> R collect(
      Iterable<? extends I> inputCollection, Transformer<? super I, ? extends O> transformer, R outputCollection
   ) {
      return inputCollection != null ? collect(inputCollection.iterator(), transformer, outputCollection) : outputCollection;
   }

   public static <I, O, R extends Collection<? super O>> R collect(
      Iterator<? extends I> inputIterator, Transformer<? super I, ? extends O> transformer, R outputCollection
   ) {
      if (inputIterator != null && transformer != null) {
         while (inputIterator.hasNext()) {
            I item = (I)inputIterator.next();
            O value = (O)transformer.transform(item);
            outputCollection.add(value);
         }
      }

      return outputCollection;
   }

   public static <T> boolean addIgnoreNull(Collection<T> collection, T object) {
      if (collection == null) {
         throw new NullPointerException("The collection must not be null");
      } else {
         return object != null && collection.add(object);
      }
   }

   public static <C> boolean addAll(Collection<C> collection, Iterable<? extends C> iterable) {
      return iterable instanceof Collection ? collection.addAll((Collection)iterable) : addAll(collection, iterable.iterator());
   }

   public static <C> boolean addAll(Collection<C> collection, Iterator<? extends C> iterator) {
      boolean changed = false;

      while (iterator.hasNext()) {
         changed |= collection.add(iterator.next());
      }

      return changed;
   }

   public static <C> boolean addAll(Collection<C> collection, Enumeration<? extends C> enumeration) {
      boolean changed = false;

      while (enumeration.hasMoreElements()) {
         changed |= collection.add(enumeration.nextElement());
      }

      return changed;
   }

   public static <C> boolean addAll(Collection<C> collection, C... elements) {
      boolean changed = false;

      for (C element : elements) {
         changed |= collection.add(element);
      }

      return changed;
   }

   @Deprecated
   public static <T> T get(Iterator<T> iterator, int index) {
      return (T)IteratorUtils.get(iterator, index);
   }

   static void checkIndexBounds(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
      }
   }

   @Deprecated
   public static <T> T get(Iterable<T> iterable, int index) {
      return (T)IterableUtils.get(iterable, index);
   }

   public static Object get(Object object, int index) {
      int i = index;
      if (i < 0) {
         throw new IndexOutOfBoundsException("Index cannot be negative: " + i);
      }

      if (object instanceof Map) {
         Map<?, ?> map = (Map<?, ?>)object;
         Iterator<?> iterator = map.entrySet().iterator();
         return IteratorUtils.get(iterator, i);
      }

      if (object instanceof Object[]) {
         return ((Object[])object)[i];
      }

      if (object instanceof Iterator) {
         Iterator<?> it = (Iterator<?>)object;
         return IteratorUtils.get(it, i);
      }

      if (object instanceof Iterable) {
         Iterable<?> iterable = (Iterable<?>)object;
         return IterableUtils.get(iterable, i);
      }

      if (object instanceof Enumeration) {
         Enumeration<?> it = (Enumeration<?>)object;
         return EnumerationUtils.get(it, i);
      }

      if (object == null) {
         throw new IllegalArgumentException("Unsupported object type: null");
      }

      try {
         return Array.get(object, i);
      } catch (IllegalArgumentException ex) {
         throw new IllegalArgumentException("Unsupported object type: " + object.getClass().getName());
      }
   }

   public static <K, V> Entry<K, V> get(Map<K, V> map, int index) {
      checkIndexBounds(index);
      return get(map.entrySet(), index);
   }

   public static int size(Object object) {
      if (object == null) {
         return 0;
      }

      int total = 0;
      if (object instanceof Map) {
         total = ((Map)object).size();
      } else if (object instanceof Collection) {
         total = ((Collection)object).size();
      } else if (object instanceof Iterable) {
         total = IterableUtils.size((Iterable)object);
      } else if (object instanceof Object[]) {
         total = ((Object[])object).length;
      } else if (object instanceof Iterator) {
         total = IteratorUtils.size((Iterator)object);
      } else if (object instanceof Enumeration) {
         Enumeration<?> it = (Enumeration<?>)object;

         while (it.hasMoreElements()) {
            total++;
            it.nextElement();
         }
      } else {
         try {
            total = Array.getLength(object);
         } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported object type: " + object.getClass().getName());
         }
      }

      return total;
   }

   public static boolean sizeIsEmpty(Object object) {
      if (object == null) {
         return true;
      }

      if (object instanceof Collection) {
         return ((Collection)object).isEmpty();
      }

      if (object instanceof Iterable) {
         return IterableUtils.isEmpty((Iterable)object);
      }

      if (object instanceof Map) {
         return ((Map)object).isEmpty();
      }

      if (object instanceof Object[]) {
         return ((Object[])object).length == 0;
      }

      if (object instanceof Iterator) {
         return !((Iterator)object).hasNext();
      }

      if (object instanceof Enumeration) {
         return !((Enumeration)object).hasMoreElements();
      }

      try {
         return Array.getLength(object) == 0;
      } catch (IllegalArgumentException ex) {
         throw new IllegalArgumentException("Unsupported object type: " + object.getClass().getName());
      }
   }

   public static boolean isEmpty(Collection<?> coll) {
      return coll == null || coll.isEmpty();
   }

   public static boolean isNotEmpty(Collection<?> coll) {
      return !isEmpty(coll);
   }

   public static void reverseArray(Object[] array) {
      int i = 0;

      for (int j = array.length - 1; j > i; i++) {
         Object tmp = array[j];
         array[j] = array[i];
         array[i] = tmp;
         j--;
      }
   }

   public static boolean isFull(Collection<? extends Object> coll) {
      if (coll == null) {
         throw new NullPointerException("The collection must not be null");
      }

      if (coll instanceof BoundedCollection) {
         return ((BoundedCollection)coll).isFull();
      }

      try {
         BoundedCollection<?> bcoll = UnmodifiableBoundedCollection.unmodifiableBoundedCollection(coll);
         return bcoll.isFull();
      } catch (IllegalArgumentException ex) {
         return false;
      }
   }

   public static int maxSize(Collection<? extends Object> coll) {
      if (coll == null) {
         throw new NullPointerException("The collection must not be null");
      }

      if (coll instanceof BoundedCollection) {
         return ((BoundedCollection)coll).maxSize();
      }

      try {
         BoundedCollection<?> bcoll = UnmodifiableBoundedCollection.unmodifiableBoundedCollection(coll);
         return bcoll.maxSize();
      } catch (IllegalArgumentException ex) {
         return -1;
      }
   }

   public static <O extends Comparable<? super O>> List<O> collate(Iterable<? extends O> a, Iterable<? extends O> b) {
      return collate(a, b, ComparatorUtils.naturalComparator(), true);
   }

   public static <O extends Comparable<? super O>> List<O> collate(Iterable<? extends O> a, Iterable<? extends O> b, boolean includeDuplicates) {
      return collate(a, b, ComparatorUtils.naturalComparator(), includeDuplicates);
   }

   public static <O> List<O> collate(Iterable<? extends O> a, Iterable<? extends O> b, Comparator<? super O> c) {
      return collate(a, b, c, true);
   }

   public static <O> List<O> collate(Iterable<? extends O> a, Iterable<? extends O> b, Comparator<? super O> c, boolean includeDuplicates) {
      if (a != null && b != null) {
         if (c == null) {
            throw new NullPointerException("The comparator must not be null");
         }

         int totalSize = a instanceof Collection && b instanceof Collection ? Math.max(1, ((Collection)a).size() + ((Collection)b).size()) : 10;
         Iterator<O> iterator = new CollatingIterator(c, a.iterator(), b.iterator());
         if (includeDuplicates) {
            return IteratorUtils.toList(iterator, totalSize);
         }

         ArrayList<O> mergedList = new ArrayList(totalSize);
         O lastItem = null;

         while (iterator.hasNext()) {
            O item = (O)iterator.next();
            if (lastItem == null || !lastItem.equals(item)) {
               mergedList.add(item);
            }

            lastItem = item;
         }

         mergedList.trimToSize();
         return mergedList;
      } else {
         throw new NullPointerException("The collections must not be null");
      }
   }

   public static <E> Collection<List<E>> permutations(Collection<E> collection) {
      PermutationIterator<E> it = new PermutationIterator(collection);
      Collection<List<E>> result = new ArrayList();

      while (it.hasNext()) {
         result.add(it.next());
      }

      return result;
   }

   public static <C> Collection<C> retainAll(Collection<C> collection, Collection<?> retain) {
      return ListUtils.retainAll(collection, retain);
   }

   public static <E> Collection<E> retainAll(Iterable<E> collection, Iterable<? extends E> retain, final Equator<? super E> equator) {
      Transformer<E, CollectionUtils.EquatorWrapper<E>> transformer = new Transformer<E, CollectionUtils.EquatorWrapper<E>>() {
         public CollectionUtils.EquatorWrapper<E> transform(E input) {
            return new CollectionUtils.EquatorWrapper<>(equator, input);
         }
      };
      Set<CollectionUtils.EquatorWrapper<E>> retainSet = collect(retain, transformer, new HashSet());
      List<E> list = new ArrayList();

      for (E element : collection) {
         if (retainSet.contains(new CollectionUtils.EquatorWrapper<>(equator, element))) {
            list.add(element);
         }
      }

      return list;
   }

   public static <E> Collection<E> removeAll(Collection<E> collection, Collection<?> remove) {
      return ListUtils.removeAll(collection, remove);
   }

   public static <E> Collection<E> removeAll(Iterable<E> collection, Iterable<? extends E> remove, final Equator<? super E> equator) {
      Transformer<E, CollectionUtils.EquatorWrapper<E>> transformer = new Transformer<E, CollectionUtils.EquatorWrapper<E>>() {
         public CollectionUtils.EquatorWrapper<E> transform(E input) {
            return new CollectionUtils.EquatorWrapper<>(equator, input);
         }
      };
      Set<CollectionUtils.EquatorWrapper<E>> removeSet = collect(remove, transformer, new HashSet());
      List<E> list = new ArrayList();

      for (E element : collection) {
         if (!removeSet.contains(new CollectionUtils.EquatorWrapper<>(equator, element))) {
            list.add(element);
         }
      }

      return list;
   }

   @Deprecated
   public static <C> Collection<C> synchronizedCollection(Collection<C> collection) {
      return SynchronizedCollection.synchronizedCollection(collection);
   }

   @Deprecated
   public static <C> Collection<C> unmodifiableCollection(Collection<? extends C> collection) {
      return UnmodifiableCollection.unmodifiableCollection(collection);
   }

   public static <C> Collection<C> predicatedCollection(Collection<C> collection, Predicate<? super C> predicate) {
      return PredicatedCollection.predicatedCollection(collection, predicate);
   }

   public static <E> Collection<E> transformingCollection(Collection<E> collection, Transformer<? super E, ? extends E> transformer) {
      return TransformedCollection.transformingCollection(collection, transformer);
   }

   public static <E> E extractSingleton(Collection<E> collection) {
      if (collection == null) {
         throw new NullPointerException("Collection must not be null.");
      } else if (collection.size() != 1) {
         throw new IllegalArgumentException("Can extract singleton only when collection size == 1");
      } else {
         return (E)collection.iterator().next();
      }
   }

   private static class CardinalityHelper<O> {
      final Map<O, Integer> cardinalityA;
      final Map<O, Integer> cardinalityB;

      public CardinalityHelper(Iterable<? extends O> a, Iterable<? extends O> b) {
         this.cardinalityA = CollectionUtils.getCardinalityMap(a);
         this.cardinalityB = CollectionUtils.getCardinalityMap(b);
      }

      public final int max(Object obj) {
         return Math.max(this.freqA(obj), this.freqB(obj));
      }

      public final int min(Object obj) {
         return Math.min(this.freqA(obj), this.freqB(obj));
      }

      public int freqA(Object obj) {
         return this.getFreq(obj, this.cardinalityA);
      }

      public int freqB(Object obj) {
         return this.getFreq(obj, this.cardinalityB);
      }

      private int getFreq(Object obj, Map<?, Integer> freqMap) {
         Integer count = (Integer)freqMap.get(obj);
         return count != null ? count : 0;
      }
   }

   private static class EquatorWrapper<O> {
      private final Equator<? super O> equator;
      private final O object;

      public EquatorWrapper(Equator<? super O> equator, O object) {
         this.equator = equator;
         this.object = object;
      }

      public O getObject() {
         return this.object;
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof CollectionUtils.EquatorWrapper)) {
            return false;
         }

         CollectionUtils.EquatorWrapper<O> otherObj = (CollectionUtils.EquatorWrapper<O>)obj;
         return this.equator.equate(this.object, otherObj.getObject());
      }

      public int hashCode() {
         return this.equator.hash(this.object);
      }
   }

   private static class SetOperationCardinalityHelper<O> extends CollectionUtils.CardinalityHelper<O> implements Iterable<O> {
      private final Set<O> elements = new HashSet();
      private final List<O> newList;

      public SetOperationCardinalityHelper(Iterable<? extends O> a, Iterable<? extends O> b) {
         super(a, b);
         CollectionUtils.addAll(this.elements, a);
         CollectionUtils.addAll(this.elements, b);
         this.newList = new ArrayList(this.elements.size());
      }

      public Iterator<O> iterator() {
         return this.elements.iterator();
      }

      public void setCardinality(O obj, int count) {
         for (int i = 0; i < count; i++) {
            this.newList.add(obj);
         }
      }

      public Collection<O> list() {
         return this.newList;
      }
   }
}
