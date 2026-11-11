package org.apache.poi.ss.formula.eval;

import org.apache.poi.ss.formula.functions.ArrayFunction;
import org.apache.poi.ss.formula.functions.Fixed2ArgFunction;
import org.apache.poi.ss.formula.functions.Function;
import org.apache.poi.ss.formula.functions.MatrixFunction.MutableValueCollector;
import org.apache.poi.ss.formula.functions.MatrixFunction.TwoArrayArg;

public abstract class TwoOperandNumericOperation extends Fixed2ArgFunction implements ArrayFunction {
   public static final Function AddEval = new TwoOperandNumericOperation() {
      @Override
      protected double evaluate(double d0, double d1) {
         return d0 + d1;
      }
   };
   public static final Function DivideEval = new TwoOperandNumericOperation() {
      @Override
      protected double evaluate(double d0, double d1) throws EvaluationException {
         if (d1 == 0.0) {
            throw new EvaluationException(ErrorEval.DIV_ZERO);
         } else {
            return d0 / d1;
         }
      }
   };
   public static final Function MultiplyEval = new TwoOperandNumericOperation() {
      @Override
      protected double evaluate(double d0, double d1) {
         return d0 * d1;
      }
   };
   public static final Function PowerEval = new TwoOperandNumericOperation() {
      @Override
      protected double evaluate(double d0, double d1) {
         return d0 < 0.0 && Math.abs(d1) > 0.0 && Math.abs(d1) < 1.0 ? -1.0 * Math.pow(d0 * -1.0, d1) : Math.pow(d0, d1);
      }
   };
   public static final Function SubtractEval = new TwoOperandNumericOperation.SubtractEvalClass();

   protected final double singleOperandEvaluate(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
      ValueEval ve = OperandResolver.getSingleValue(arg, srcCellRow, srcCellCol);
      return OperandResolver.coerceValueToDouble(ve);
   }

   public ValueEval evaluateArray(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
      return (ValueEval)(args.length != 2
         ? ErrorEval.VALUE_INVALID
         : new TwoOperandNumericOperation.ArrayEval().evaluate(srcRowIndex, srcColumnIndex, args[0], args[1]));
   }

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
      double result;
      try {
         double d0 = this.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
         double d1 = this.singleOperandEvaluate(arg1, srcRowIndex, srcColumnIndex);
         result = this.evaluate(d0, d1);
         if (result == 0.0 && !(this instanceof TwoOperandNumericOperation.SubtractEvalClass)) {
            return NumberEval.ZERO;
         }

         if (Double.isNaN(result) || Double.isInfinite(result)) {
            return ErrorEval.NUM_ERROR;
         }
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      return new NumberEval(result);
   }

   protected abstract double evaluate(double var1, double var3) throws EvaluationException;

   private final class ArrayEval extends TwoArrayArg {
      private final MutableValueCollector instance = new MutableValueCollector(true, true);

      private ArrayEval() {
      }

      protected double[] collectValues(ValueEval arg) throws EvaluationException {
         return this.instance.collectValues(new ValueEval[]{arg});
      }

      protected double[][] evaluate(double[][] d1, double[][] d2) throws IllegalArgumentException, EvaluationException {
         int width = d1[0].length < d2[0].length ? d1[0].length : d2[0].length;
         int height = d1.length < d2.length ? d1.length : d2.length;
         double[][] result = new double[height][width];

         for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
               result[j][i] = TwoOperandNumericOperation.this.evaluate(d1[j][i], d2[j][i]);
            }
         }

         return result;
      }
   }

   private static final class SubtractEvalClass extends TwoOperandNumericOperation {
      public SubtractEvalClass() {
      }

      @Override
      protected double evaluate(double d0, double d1) {
         return d0 - d1;
      }
   }
}
