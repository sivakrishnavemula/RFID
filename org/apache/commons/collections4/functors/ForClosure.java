package org.apache.commons.collections4.functors;

import org.apache.commons.collections4.Closure;

public class ForClosure<E> implements Closure<E> {
   private final int iCount;
   private final Closure<? super E> iClosure;

   public static <E> Closure<E> forClosure(int count, Closure<? super E> closure) {
      if (count <= 0 || closure == null) {
         return NOPClosure.nopClosure();
      } else {
         return (Closure<E>)(count == 1 ? closure : new ForClosure<>(count, closure));
      }
   }

   public ForClosure(int count, Closure<? super E> closure) {
      this.iCount = count;
      this.iClosure = closure;
   }

   public void execute(E input) {
      for (int i = 0; i < this.iCount; i++) {
         this.iClosure.execute(input);
      }
   }

   public Closure<? super E> getClosure() {
      return this.iClosure;
   }

   public int getCount() {
      return this.iCount;
   }
}
