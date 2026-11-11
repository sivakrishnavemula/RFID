package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;

public class Mirr extends MultiOperandNumericFunction {
   public Mirr() {
      super(false, false);
   }

   protected int getMaxNumOperands() {
      return 3;
   }

   protected double evaluate(double[] values) throws EvaluationException {
      double financeRate = values[values.length - 1];
      double reinvestRate = values[values.length - 2];
      double[] mirrValues = new double[values.length - 2];
      System.arraycopy(values, 0, mirrValues, 0, mirrValues.length);
      boolean mirrValuesAreAllNegatives = true;

      for (double mirrValue : mirrValues) {
         mirrValuesAreAllNegatives &= mirrValue < 0.0;
      }

      if (mirrValuesAreAllNegatives) {
         return -1.0;
      }

      boolean mirrValuesAreAllPositives = true;

      for (double mirrValue : mirrValues) {
         mirrValuesAreAllPositives &= mirrValue > 0.0;
      }

      if (mirrValuesAreAllPositives) {
         throw new EvaluationException(ErrorEval.DIV_ZERO);
      } else {
         return mirr(mirrValues, financeRate, reinvestRate);
      }
   }

   private static double mirr(double[] in, double financeRate, double reinvestRate) {
      double value = 0.0;
      int numOfYears = in.length - 1;
      double pv = 0.0;
      double fv = 0.0;
      int indexN = 0;

      for (double anIn : in) {
         if (anIn < 0.0) {
            pv += anIn / Math.pow(1.0 + financeRate + reinvestRate, indexN++);
         }
      }

      for (double anIn : in) {
         if (anIn > 0.0) {
            fv += anIn * Math.pow(1.0 + financeRate, numOfYears - indexN++);
         }
      }

      if (fv != 0.0 && pv != 0.0) {
         value = Math.pow(-fv / pv, 1.0 / numOfYears) - 1.0;
      }

      return value;
   }
}
