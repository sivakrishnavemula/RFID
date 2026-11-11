package org.apache.commons.collections4.queue;

import java.util.Queue;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.collection.TransformedCollection;

public class TransformedQueue<E> extends TransformedCollection<E> implements Queue<E> {
   private static final long serialVersionUID = -7901091318986132033L;

   public static <E> TransformedQueue<E> transformingQueue(Queue<E> queue, Transformer<? super E, ? extends E> transformer) {
      return new TransformedQueue<>(queue, transformer);
   }

   public static <E> TransformedQueue<E> transformedQueue(Queue<E> queue, Transformer<? super E, ? extends E> transformer) {
      TransformedQueue<E> decorated = new TransformedQueue<>(queue, transformer);
      if (queue.size() > 0) {
         E[] values = (E[])queue.toArray();
         queue.clear();

         for (E value : values) {
            decorated.decorated().add(transformer.transform(value));
         }
      }

      return decorated;
   }

   protected TransformedQueue(Queue<E> queue, Transformer<? super E, ? extends E> transformer) {
      super(queue, transformer);
   }

   protected Queue<E> getQueue() {
      return (Queue<E>)this.decorated();
   }

   public boolean offer(E obj) {
      return this.getQueue().offer(this.transform(obj));
   }

   public E poll() {
      return (E)this.getQueue().poll();
   }

   public E peek() {
      return (E)this.getQueue().peek();
   }

   public E element() {
      return (E)this.getQueue().element();
   }

   public E remove() {
      return (E)this.getQueue().remove();
   }
}
