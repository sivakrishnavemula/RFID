package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.AggregateFunction.ValueCollector;

public final class Irr implements Function {
   public ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
      if (args.length != 0 && args.length <= 2) {
         try {
            double[] values = ValueCollector.collectValues(new ValueEval[]{args[0]});
            double guess;
            if (args.length == 2) {
               guess = NumericFunction.singleOperandEvaluate(args[1], srcRowIndex, srcColumnIndex);
            } else {
               guess = 0.1;
            }

            double result = irr(values, guess);
            NumericFunction.checkValue(result);
            return new NumberEval(result);
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }
      } else {
         return ErrorEval.VALUE_INVALID;
      }
   }

   public static double irr(double[] income) {
      return irr(income, 0.1);
   }

   public static double irr(double[] values, double guess) {
      int maxIterationCount = 20;
      double absoluteAccuracy = 1.0E-7;
      double x0 = guess;

      for (int i = 0; i < 20; i++) {
         double factor = 1.0 + x0;
         int k = 0;
         double fValue = values[k];
         double fDerivative = 0.0;
         double denominator = factor;

         while (++k < values.length) {
            double value = values[k];
            fValue += value / denominator;
            denominator *= factor;
            fDerivative -= k * value / denominator;
         }

         double x1 = x0 - fValue / fDerivative;
         if (Math.abs(x1 - x0) <= 1.0E-7) {
            return x1;
         }

         x0 = x1;
      }

      return Double.NaN;
   }
}
