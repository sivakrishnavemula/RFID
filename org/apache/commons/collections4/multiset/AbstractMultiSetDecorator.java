package org.apache.commons.collections4.multiset;

import java.util.Set;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiSet.Entry;
import org.apache.commons.collections4.collection.AbstractCollectionDecorator;

public abstract class AbstractMultiSetDecorator<E> extends AbstractCollectionDecorator<E> implements MultiSet<E> {
   private static final long serialVersionUID = 20150610L;

   protected AbstractMultiSetDecorator() {
   }

   protected AbstractMultiSetDecorator(MultiSet<E> multiset) {
      super(multiset);
   }

   protected MultiSet<E> decorated() {
      return (MultiSet<E>)super.decorated();
   }

   public boolean equals(Object object) {
      return object == this || this.decorated().equals(object);
   }

   public int hashCode() {
      return this.decorated().hashCode();
   }

   public int getCount(Object object) {
      return this.decorated().getCount(object);
   }

   public int setCount(E object, int count) {
      return this.decorated().setCount(object, count);
   }

   public int add(E object, int count) {
      return this.decorated().add(object, count);
   }

   public int remove(Object object, int count) {
      return this.decorated().remove(object, count);
   }

   public Set<E> uniqueSet() {
      return this.decorated().uniqueSet();
   }

   public Set<Entry<E>> entrySet() {
      return this.decorated().entrySet();
   }
}
