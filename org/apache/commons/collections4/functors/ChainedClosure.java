package org.apache.commons.collections4.functors;

import java.io.Serializable;
import java.util.Collection;
import org.apache.commons.collections4.Closure;

public class ChainedClosure<E> implements Closure<E>, Serializable {
   private static final long serialVersionUID = -3520677225766901240L;
   private final Closure<? super E>[] iClosures;

   public static <E> Closure<E> chainedClosure(Closure<? super E>... closures) {
      FunctorUtils.validate(closures);
      return closures.length == 0 ? NOPClosure.nopClosure() : new ChainedClosure<>(closures);
   }

   public static <E> Closure<E> chainedClosure(Collection<? extends Closure<? super E>> closures) {
      if (closures == null) {
         throw new NullPointerException("Closure collection must not be null");
      }

      if (closures.size() == 0) {
         return NOPClosure.nopClosure();
      }

      Closure<? super E>[] cmds = new Closure[closures.size()];
      int i = 0;

      for (Closure<? super E> closure : closures) {
         cmds[i++] = closure;
      }

      FunctorUtils.validate(cmds);
      return new ChainedClosure<>(false, cmds);
   }

   private ChainedClosure(boolean clone, Closure<? super E>... closures) {
      this.iClosures = clone ? FunctorUtils.copy(closures) : closures;
   }

   public ChainedClosure(Closure<? super E>... closures) {
      this(true, closures);
   }

   public void execute(E input) {
      for (Closure<? super E> iClosure : this.iClosures) {
         iClosure.execute(input);
      }
   }

   public Closure<? super E>[] getClosures() {
      return FunctorUtils.copy(this.iClosures);
   }
}
