package org.apache.commons.collections4.bag;

import java.util.Set;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.collection.TransformedCollection;
import org.apache.commons.collections4.set.TransformedSet;

public class TransformedBag<E> extends TransformedCollection<E> implements Bag<E> {
   private static final long serialVersionUID = 5421170911299074185L;

   public static <E> Bag<E> transformingBag(Bag<E> bag, Transformer<? super E, ? extends E> transformer) {
      return new TransformedBag<>(bag, transformer);
   }

   public static <E> Bag<E> transformedBag(Bag<E> bag, Transformer<? super E, ? extends E> transformer) {
      TransformedBag<E> decorated = new TransformedBag<>(bag, transformer);
      if (bag.size() > 0) {
         E[] values = (E[])bag.toArray();
         bag.clear();

         for (E value : values) {
            decorated.decorated().add(transformer.transform(value));
         }
      }

      return decorated;
   }

   protected TransformedBag(Bag<E> bag, Transformer<? super E, ? extends E> transformer) {
      super(bag, transformer);
   }

   protected Bag<E> getBag() {
      return (Bag<E>)this.decorated();
   }

   public boolean equals(Object object) {
      return object == this || this.decorated().equals(object);
   }

   public int hashCode() {
      return this.decorated().hashCode();
   }

   public int getCount(Object object) {
      return this.getBag().getCount(object);
   }

   public boolean remove(Object object, int nCopies) {
      return this.getBag().remove(object, nCopies);
   }

   public boolean add(E object, int nCopies) {
      return this.getBag().add(this.transform(object), nCopies);
   }

   public Set<E> uniqueSet() {
      Set<E> set = this.getBag().uniqueSet();
      return TransformedSet.transformingSet(set, this.transformer);
   }
}
