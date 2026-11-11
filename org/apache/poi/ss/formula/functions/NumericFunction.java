package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

public abstract class NumericFunction implements Function {
   static final double ZERO = 0.0;
   static final double TEN = 10.0;
   static final double LOG_10_TO_BASE_e = Math.log(10.0);
   public static final Function ABS = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.abs(d);
      }
   };
   public static final Function ACOS = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.acos(d);
      }
   };
   public static final Function ACOSH = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return MathX.acosh(d);
      }
   };
   public static final Function ASIN = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.asin(d);
      }
   };
   public static final Function ASINH = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return MathX.asinh(d);
      }
   };
   public static final Function ATAN = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.atan(d);
      }
   };
   public static final Function ATANH = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return MathX.atanh(d);
      }
   };
   public static final Function COS = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.cos(d);
      }
   };
   public static final Function COSH = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return MathX.cosh(d);
      }
   };
   public static final Function DEGREES = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.toDegrees(d);
      }
   };
   static final NumberEval DOLLAR_ARG2_DEFAULT = new NumberEval(2.0);
   public static final Function DOLLAR = new Var1or2ArgFunction() {
      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
         return this.evaluate(srcRowIndex, srcColumnIndex, arg0, NumericFunction.DOLLAR_ARG2_DEFAULT);
      }

      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
         double val;
         double d1;
         try {
            val = NumericFunction.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
            d1 = NumericFunction.singleOperandEvaluate(arg1, srcRowIndex, srcColumnIndex);
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }

         int nPlaces = (int)d1;
         return (ValueEval)(nPlaces > 127 ? ErrorEval.VALUE_INVALID : new NumberEval(val));
      }
   };
   public static final Function EXP = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.pow(Math.E, d);
      }
   };
   public static final Function FACT = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return MathX.factorial((int)d);
      }
   };
   public static final Function INT = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.round(d - 0.5);
      }
   };
   public static final Function LN = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.log(d);
      }
   };
   public static final Function LOG10 = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.log(d) / NumericFunction.LOG_10_TO_BASE_e;
      }
   };
   public static final Function RADIANS = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.toRadians(d);
      }
   };
   public static final Function SIGN = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return MathX.sign(d);
      }
   };
   public static final Function SIN = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.sin(d);
      }
   };
   public static final Function SINH = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return MathX.sinh(d);
      }
   };
   public static final Function SQRT = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.sqrt(d);
      }
   };
   public static final Function TAN = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return Math.tan(d);
      }
   };
   public static final Function TANH = new NumericFunction.OneArg() {
      @Override
      protected double evaluate(double d) {
         return MathX.tanh(d);
      }
   };
   public static final Function ATAN2 = new NumericFunction.TwoArg() {
      @Override
      protected double evaluate(double d0, double d1) throws EvaluationException {
         if (d0 == 0.0 && d1 == 0.0) {
            throw new EvaluationException(ErrorEval.DIV_ZERO);
         } else {
            return Math.atan2(d1, d0);
         }
      }
   };
   public static final Function CEILING = new NumericFunction.TwoArg() {
      @Override
      protected double evaluate(double d0, double d1) {
         return MathX.ceiling(d0, d1);
      }
   };
   public static final Function COMBIN = new NumericFunction.TwoArg() {
      @Override
      protected double evaluate(double d0, double d1) throws EvaluationException {
         if (!(d0 > 2.1474836E9F) && !(d1 > 2.1474836E9F)) {
            return MathX.nChooseK((int)d0, (int)d1);
         } else {
            throw new EvaluationException(ErrorEval.NUM_ERROR);
         }
      }
   };
   public static final Function FLOOR = new NumericFunction.TwoArg() {
      @Override
      protected double evaluate(double d0, double d1) throws EvaluationException {
         if (d1 == 0.0) {
            if (d0 == 0.0) {
               return 0.0;
            } else {
               throw new EvaluationException(ErrorEval.DIV_ZERO);
            }
         } else {
            return MathX.floor(d0, d1);
         }
      }
   };
   public static final Function MOD = new NumericFunction.TwoArg() {
      @Override
      protected double evaluate(double d0, double d1) throws EvaluationException {
         if (d1 == 0.0) {
            throw new EvaluationException(ErrorEval.DIV_ZERO);
         } else {
            return MathX.mod(d0, d1);
         }
      }
   };
   public static final Function POWER = new NumericFunction.TwoArg() {
      @Override
      protected double evaluate(double d0, double d1) {
         return Math.pow(d0, d1);
      }
   };
   public static final Function ROUND = new NumericFunction.TwoArg() {
      @Override
      protected double evaluate(double d0, double d1) {
         return MathX.round(d0, (int)d1);
      }
   };
   public static final Function ROUNDDOWN = new NumericFunction.TwoArg() {
      @Override
      protected double evaluate(double d0, double d1) {
         return MathX.roundDown(d0, (int)d1);
      }
   };
   public static final Function ROUNDUP = new NumericFunction.TwoArg() {
      @Override
      protected double evaluate(double d0, double d1) {
         return MathX.roundUp(d0, (int)d1);
      }
   };
   static final NumberEval TRUNC_ARG2_DEFAULT = new NumberEval(0.0);
   public static final Function TRUNC = new Var1or2ArgFunction() {
      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
         return this.evaluate(srcRowIndex, srcColumnIndex, arg0, NumericFunction.TRUNC_ARG2_DEFAULT);
      }

      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
         double result;
         try {
            double d0 = NumericFunction.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
            double d1 = NumericFunction.singleOperandEvaluate(arg1, srcRowIndex, srcColumnIndex);
            double multi = Math.pow(10.0, d1);
            if (d0 < 0.0) {
               result = -Math.floor(-d0 * multi) / multi;
            } else {
               result = Math.floor(d0 * multi) / multi;
            }

            NumericFunction.checkValue(result);
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }

         return new NumberEval(result);
      }
   };
   public static final Function LOG = new NumericFunction.Log();
   static final NumberEval PI_EVAL = new NumberEval(Math.PI);
   public static final Function PI = new Fixed0ArgFunction() {
      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex) {
         return NumericFunction.PI_EVAL;
      }
   };
   public static final Function RAND = new Fixed0ArgFunction() {
      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex) {
         return new NumberEval(Math.random());
      }
   };
   public static final Function POISSON = new Fixed3ArgFunction() {
      private static final double DEFAULT_RETURN_RESULT = 1.0;
      private final long[] FACTORIALS = new long[]{
         1L,
         1L,
         2L,
         6L,
         24L,
         120L,
         720L,
         5040L,
         40320L,
         362880L,
         3628800L,
         39916800L,
         479001600L,
         6227020800L,
         87178291200L,
         1307674368000L,
         20922789888000L,
         355687428096000L,
         6402373705728000L,
         121645100408832000L,
         2432902008176640000L
      };

      private boolean isDefaultResult(double x, double mean) {
         return x == 0.0 && mean == 0.0;
      }

      private boolean checkArgument(double aDouble) throws EvaluationException {
         NumericFunction.checkValue(aDouble);
         if (aDouble < 0.0) {
            throw new EvaluationException(ErrorEval.NUM_ERROR);
         } else {
            return true;
         }
      }

      private double probability(int k, double lambda) {
         return Math.pow(lambda, k) * Math.exp(-lambda) / this.factorial(k);
      }

      private double cumulativeProbability(int x, double lambda) {
         double result = 0.0;

         for (int k = 0; k <= x; k++) {
            result += this.probability(k, lambda);
         }

         return result;
      }

      public long factorial(int n) {
         if (n >= 0 && n <= 20) {
            return this.FACTORIALS[n];
         } else {
            throw new IllegalArgumentException("Valid argument should be in the range [0..20]");
         }
      }

      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
         double mean = 0.0;
         double x = 0.0;
         boolean cumulative = ((BoolEval)arg2).getBooleanValue();
         double result = 0.0;

         try {
            x = NumericFunction.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
            mean = NumericFunction.singleOperandEvaluate(arg1, srcRowIndex, srcColumnIndex);
            if (this.isDefaultResult(x, mean)) {
               return new NumberEval(1.0);
            }

            this.checkArgument(x);
            this.checkArgument(mean);
            if (cumulative) {
               result = this.cumulativeProbability((int)x, mean);
            } else {
               result = this.probability((int)x, mean);
            }

            NumericFunction.checkValue(result);
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }

         return new NumberEval(result);
      }
   };

   protected static double singleOperandEvaluate(ValueEval arg, int srcRowIndex, int srcColumnIndex) throws EvaluationException {
      if (arg == null) {
         throw new IllegalArgumentException("arg must not be null");
      }

      ValueEval ve = OperandResolver.getSingleValue(arg, srcRowIndex, srcColumnIndex);
      double result = OperandResolver.coerceValueToDouble(ve);
      checkValue(result);
      return result;
   }

   public static void checkValue(double result) throws EvaluationException {
      if (Double.isNaN(result) || Double.isInfinite(result)) {
         throw new EvaluationException(ErrorEval.NUM_ERROR);
      }
   }

   public final ValueEval evaluate(ValueEval[] args, int srcCellRow, int srcCellCol) {
      double result;
      try {
         result = this.eval(args, srcCellRow, srcCellCol);
         checkValue(result);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      return new NumberEval(result);
   }

   protected abstract double eval(ValueEval[] var1, int var2, int var3) throws EvaluationException;

   private static final class Log extends Var1or2ArgFunction {
      public Log() {
      }

      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
         double result;
         try {
            double d0 = NumericFunction.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
            result = Math.log(d0) / NumericFunction.LOG_10_TO_BASE_e;
            NumericFunction.checkValue(result);
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }

         return new NumberEval(result);
      }

      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
         double result;
         try {
            double d0 = NumericFunction.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
            double d1 = NumericFunction.singleOperandEvaluate(arg1, srcRowIndex, srcColumnIndex);
            double logE = Math.log(d0);
            if (Double.compare(d1, Math.E) == 0) {
               result = logE;
            } else {
               result = logE / Math.log(d1);
            }

            NumericFunction.checkValue(result);
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }

         return new NumberEval(result);
      }
   }

   public abstract static class OneArg extends Fixed1ArgFunction {
      protected OneArg() {
      }

      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
         double result;
         try {
            double d = NumericFunction.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
            result = this.evaluate(d);
            NumericFunction.checkValue(result);
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }

         return new NumberEval(result);
      }

      protected final double eval(ValueEval[] args, int srcCellRow, int srcCellCol) throws EvaluationException {
         if (args.length != 1) {
            throw new EvaluationException(ErrorEval.VALUE_INVALID);
         }

         double d = NumericFunction.singleOperandEvaluate(args[0], srcCellRow, srcCellCol);
         return this.evaluate(d);
      }

      protected abstract double evaluate(double var1) throws EvaluationException;
   }

   public abstract static class TwoArg extends Fixed2ArgFunction {
      protected TwoArg() {
      }

      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
         double result;
         try {
            double d0 = NumericFunction.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
            double d1 = NumericFunction.singleOperandEvaluate(arg1, srcRowIndex, srcColumnIndex);
            result = this.evaluate(d0, d1);
            NumericFunction.checkValue(result);
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }

         return new NumberEval(result);
      }

      protected abstract double evaluate(double var1, double var3) throws EvaluationException;
   }
}
