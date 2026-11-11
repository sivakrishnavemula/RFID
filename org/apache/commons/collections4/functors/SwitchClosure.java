package org.apache.commons.collections4.functors;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.Predicate;

public class SwitchClosure<E> implements Closure<E>, Serializable {
   private static final long serialVersionUID = 3518477308466486130L;
   private final Predicate<? super E>[] iPredicates;
   private final Closure<? super E>[] iClosures;
   private final Closure<? super E> iDefault;

   public static <E> Closure<E> switchClosure(Predicate<? super E>[] predicates, Closure<? super E>[] closures, Closure<? super E> defaultClosure) {
      FunctorUtils.validate(predicates);
      FunctorUtils.validate(closures);
      if (predicates.length != closures.length) {
         throw new IllegalArgumentException("The predicate and closure arrays must be the same size");
      } else if (predicates.length == 0) {
         return defaultClosure == null ? NOPClosure.nopClosure() : defaultClosure;
      } else {
         return new SwitchClosure<>(predicates, closures, defaultClosure);
      }
   }

   public static <E> Closure<E> switchClosure(Map<Predicate<E>, Closure<E>> predicatesAndClosures) {
      if (predicatesAndClosures == null) {
         throw new NullPointerException("The predicate and closure map must not be null");
      }

      Closure<? super E> defaultClosure = (Closure<? super E>)predicatesAndClosures.remove(null);
      int size = predicatesAndClosures.size();
      if (size == 0) {
         return defaultClosure == null ? NOPClosure.nopClosure() : defaultClosure;
      }

      Closure<E>[] closures = new Closure[size];
      Predicate<E>[] preds = new Predicate[size];
      int i = 0;

      for (Entry<Predicate<E>, Closure<E>> entry : predicatesAndClosures.entrySet()) {
         preds[i] = (Predicate<E>)entry.getKey();
         closures[i] = (Closure<E>)entry.getValue();
         i++;
      }

      return new SwitchClosure<>(false, preds, closures, defaultClosure);
   }

   private SwitchClosure(boolean clone, Predicate<? super E>[] predicates, Closure<? super E>[] closures, Closure<? super E> defaultClosure) {
      this.iPredicates = clone ? FunctorUtils.copy(predicates) : predicates;
      this.iClosures = clone ? FunctorUtils.copy(closures) : closures;
      this.iDefault = defaultClosure == null ? NOPClosure.nopClosure() : defaultClosure;
   }

   public SwitchClosure(Predicate<? super E>[] predicates, Closure<? super E>[] closures, Closure<? super E> defaultClosure) {
      this(true, predicates, closures, defaultClosure);
   }

   public void execute(E input) {
      for (int i = 0; i < this.iPredicates.length; i++) {
         if (this.iPredicates[i].evaluate(input)) {
            this.iClosures[i].execute(input);
            return;
         }
      }

      this.iDefault.execute(input);
   }

   public Predicate<? super E>[] getPredicates() {
      return FunctorUtils.copy(this.iPredicates);
   }

   public Closure<? super E>[] getClosures() {
      return FunctorUtils.copy(this.iClosures);
   }

   public Closure<? super E> getDefaultClosure() {
      return this.iDefault;
   }
}
