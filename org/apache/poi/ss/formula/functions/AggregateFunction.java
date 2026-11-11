package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

public abstract class AggregateFunction extends MultiOperandNumericFunction {
   public static final Function AVEDEV = new AggregateFunction() {
      protected double evaluate(double[] values) {
         return StatsLib.avedev(values);
      }
   };
   public static final Function AVERAGE = new AggregateFunction() {
      protected double evaluate(double[] values) throws EvaluationException {
         if (values.length < 1) {
            throw new EvaluationException(ErrorEval.DIV_ZERO);
         } else {
            return MathX.average(values);
         }
      }
   };
   public static final Function DEVSQ = new AggregateFunction() {
      protected double evaluate(double[] values) {
         return StatsLib.devsq(values);
      }
   };
   public static final Function LARGE = new AggregateFunction.LargeSmall(true);
   public static final Function MAX = new AggregateFunction() {
      protected double evaluate(double[] values) {
         return values.length > 0 ? MathX.max(values) : 0.0;
      }
   };
   public static final Function MEDIAN = new AggregateFunction() {
      protected double evaluate(double[] values) {
         return StatsLib.median(values);
      }
   };
   public static final Function MIN = new AggregateFunction() {
      protected double evaluate(double[] values) {
         return values.length > 0 ? MathX.min(values) : 0.0;
      }
   };
   public static final Function PERCENTILE = new AggregateFunction.Percentile();
   public static final Function PRODUCT = new AggregateFunction() {
      protected double evaluate(double[] values) {
         return MathX.product(values);
      }
   };
   public static final Function SMALL = new AggregateFunction.LargeSmall(false);
   public static final Function STDEV = new AggregateFunction() {
      protected double evaluate(double[] values) throws EvaluationException {
         if (values.length < 1) {
            throw new EvaluationException(ErrorEval.DIV_ZERO);
         } else {
            return StatsLib.stdev(values);
         }
      }
   };
   public static final Function SUM = new AggregateFunction() {
      protected double evaluate(double[] values) {
         return MathX.sum(values);
      }
   };
   public static final Function SUMSQ = new AggregateFunction() {
      protected double evaluate(double[] values) {
         return MathX.sumsq(values);
      }
   };
   public static final Function VAR = new AggregateFunction() {
      protected double evaluate(double[] values) throws EvaluationException {
         if (values.length < 1) {
            throw new EvaluationException(ErrorEval.DIV_ZERO);
         } else {
            return StatsLib.var(values);
         }
      }
   };
   public static final Function VARP = new AggregateFunction() {
      protected double evaluate(double[] values) throws EvaluationException {
         if (values.length < 1) {
            throw new EvaluationException(ErrorEval.DIV_ZERO);
         } else {
            return StatsLib.varp(values);
         }
      }
   };

   protected AggregateFunction() {
      super(false, false);
   }

   static Function subtotalInstance(Function func) {
      final AggregateFunction arg = (AggregateFunction)func;
      return new AggregateFunction() {
         protected double evaluate(double[] values) throws EvaluationException {
            return arg.evaluate(values);
         }

         public boolean isSubtotalCounted() {
            return false;
         }
      };
   }

   private static final class LargeSmall extends Fixed2ArgFunction {
      private final boolean _isLarge;

      protected LargeSmall(boolean isLarge) {
         this._isLarge = isLarge;
      }

      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
         double dn;
         try {
            ValueEval ve1 = OperandResolver.getSingleValue(arg1, srcRowIndex, srcColumnIndex);
            dn = OperandResolver.coerceValueToDouble(ve1);
         } catch (EvaluationException e1) {
            return ErrorEval.VALUE_INVALID;
         }

         if (dn < 1.0) {
            return ErrorEval.NUM_ERROR;
         }

         int k = (int)Math.ceil(dn);

         double result;
         try {
            double[] ds = AggregateFunction.ValueCollector.collectValues(arg0);
            if (k > ds.length) {
               return ErrorEval.NUM_ERROR;
            }

            result = this._isLarge ? StatsLib.kthLargest(ds, k) : StatsLib.kthSmallest(ds, k);
            NumericFunction.checkValue(result);
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }

         return new NumberEval(result);
      }
   }

   private static final class Percentile extends Fixed2ArgFunction {
      protected Percentile() {
      }

      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
         double dn;
         try {
            ValueEval ve1 = OperandResolver.getSingleValue(arg1, srcRowIndex, srcColumnIndex);
            dn = OperandResolver.coerceValueToDouble(ve1);
         } catch (EvaluationException e1) {
            return ErrorEval.VALUE_INVALID;
         }

         if (!(dn < 0.0) && !(dn > 1.0)) {
            double result;
            try {
               double[] ds = AggregateFunction.ValueCollector.collectValues(arg0);
               int N = ds.length;
               if (N == 0 || N > 8191) {
                  return ErrorEval.NUM_ERROR;
               }

               double n = (N - 1) * dn + 1.0;
               if (n == 1.0) {
                  result = StatsLib.kthSmallest(ds, 1);
               } else if (Double.compare(n, N) == 0) {
                  result = StatsLib.kthLargest(ds, 1);
               } else {
                  int k = (int)n;
                  double d = n - k;
                  result = StatsLib.kthSmallest(ds, k) + d * (StatsLib.kthSmallest(ds, k + 1) - StatsLib.kthSmallest(ds, k));
               }

               NumericFunction.checkValue(result);
            } catch (EvaluationException e) {
               return e.getErrorEval();
            }

            return new NumberEval(result);
         } else {
            return ErrorEval.NUM_ERROR;
         }
      }
   }

   static final class ValueCollector extends MultiOperandNumericFunction {
      private static final AggregateFunction.ValueCollector instance = new AggregateFunction.ValueCollector();

      public ValueCollector() {
         super(false, false);
      }

      public static double[] collectValues(ValueEval... operands) throws EvaluationException {
         return instance.getNumberArray(operands);
      }

      protected double evaluate(double[] values) {
         throw new IllegalStateException("should not be called");
      }
   }
}
