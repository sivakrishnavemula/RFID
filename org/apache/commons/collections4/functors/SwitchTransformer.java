package org.apache.commons.collections4.functors;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;

public class SwitchTransformer<I, O> implements Transformer<I, O>, Serializable {
   private static final long serialVersionUID = -6404460890903469332L;
   private final Predicate<? super I>[] iPredicates;
   private final Transformer<? super I, ? extends O>[] iTransformers;
   private final Transformer<? super I, ? extends O> iDefault;

   public static <I, O> Transformer<I, O> switchTransformer(
      Predicate<? super I>[] predicates, Transformer<? super I, ? extends O>[] transformers, Transformer<? super I, ? extends O> defaultTransformer
   ) {
      FunctorUtils.validate(predicates);
      FunctorUtils.validate(transformers);
      if (predicates.length != transformers.length) {
         throw new IllegalArgumentException("The predicate and transformer arrays must be the same size");
      } else if (predicates.length == 0) {
         return defaultTransformer == null ? ConstantTransformer.nullTransformer() : defaultTransformer;
      } else {
         return new SwitchTransformer<>(predicates, transformers, defaultTransformer);
      }
   }

   public static <I, O> Transformer<I, O> switchTransformer(Map<? extends Predicate<? super I>, ? extends Transformer<? super I, ? extends O>> map) {
      if (map == null) {
         throw new NullPointerException("The predicate and transformer map must not be null");
      }

      if (map.size() == 0) {
         return ConstantTransformer.nullTransformer();
      }

      Transformer<? super I, ? extends O> defaultTransformer = (Transformer<? super I, ? extends O>)map.remove(null);
      int size = map.size();
      if (size == 0) {
         return defaultTransformer == null ? ConstantTransformer.nullTransformer() : defaultTransformer;
      }

      Transformer<? super I, ? extends O>[] transformers = new Transformer[size];
      Predicate<? super I>[] preds = new Predicate[size];
      int i = 0;

      for (Entry<? extends Predicate<? super I>, ? extends Transformer<? super I, ? extends O>> entry : map.entrySet()) {
         preds[i] = (Predicate<? super I>)entry.getKey();
         transformers[i] = (Transformer<? super I, ? extends O>)entry.getValue();
         i++;
      }

      return new SwitchTransformer<>(false, preds, transformers, defaultTransformer);
   }

   private SwitchTransformer(
      boolean clone,
      Predicate<? super I>[] predicates,
      Transformer<? super I, ? extends O>[] transformers,
      Transformer<? super I, ? extends O> defaultTransformer
   ) {
      this.iPredicates = clone ? FunctorUtils.copy(predicates) : predicates;
      this.iTransformers = clone ? FunctorUtils.copy(transformers) : transformers;
      this.iDefault = defaultTransformer == null ? ConstantTransformer.nullTransformer() : defaultTransformer;
   }

   public SwitchTransformer(
      Predicate<? super I>[] predicates, Transformer<? super I, ? extends O>[] transformers, Transformer<? super I, ? extends O> defaultTransformer
   ) {
      this(true, predicates, transformers, defaultTransformer);
   }

   public O transform(I input) {
      for (int i = 0; i < this.iPredicates.length; i++) {
         if (this.iPredicates[i].evaluate(input)) {
            return (O)this.iTransformers[i].transform(input);
         }
      }

      return (O)this.iDefault.transform(input);
   }

   public Predicate<? super I>[] getPredicates() {
      return FunctorUtils.copy(this.iPredicates);
   }

   public Transformer<? super I, ? extends O>[] getTransformers() {
      return FunctorUtils.copy(this.iTransformers);
   }

   public Transformer<? super I, ? extends O> getDefaultTransformer() {
      return this.iDefault;
   }
}
