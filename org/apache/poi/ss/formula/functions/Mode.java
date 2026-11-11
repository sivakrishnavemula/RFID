package org.apache.poi.ss.formula.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public final class Mode implements Function {
   public static double evaluate(double[] v) throws EvaluationException {
      if (v.length < 2) {
         throw new EvaluationException(ErrorEval.NA);
      }

      int[] counts = new int[v.length];
      Arrays.fill(counts, 1);
      int i = 0;

      for (int iSize = v.length; i < iSize; i++) {
         int j = i + 1;

         for (int jSize = v.length; j < jSize; j++) {
            if (v[i] == v[j]) {
               counts[i]++;
            }
         }
      }

      double maxv = 0.0;
      int maxc = 0;
      int ix = 0;

      for (int iSize = counts.length; ix < iSize; ix++) {
         if (counts[ix] > maxc) {
            maxv = v[ix];
            maxc = counts[ix];
         }
      }

      if (maxc > 1) {
         return maxv;
      } else {
         throw new EvaluationException(ErrorEval.NA);
      }
   }

   public ValueEval evaluate(ValueEval[] args, int srcCellRow, int srcCellCol) {
      double result;
      try {
         List<Double> temp = new ArrayList();

         for (ValueEval arg : args) {
            collectValues(arg, temp);
         }

         double[] values = new double[temp.size()];

         for (int i = 0; i < values.length; i++) {
            values[i] = (Double)temp.get(i);
         }

         result = evaluate(values);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      return new NumberEval(result);
   }

   private static void collectValues(ValueEval arg, List<Double> temp) throws EvaluationException {
      if (arg instanceof TwoDEval) {
         TwoDEval ae = (TwoDEval)arg;
         int width = ae.getWidth();
         int height = ae.getHeight();

         for (int rrIx = 0; rrIx < height; rrIx++) {
            for (int rcIx = 0; rcIx < width; rcIx++) {
               ValueEval ve1 = ae.getValue(rrIx, rcIx);
               collectValue(ve1, temp, false);
            }
         }
      } else if (!(arg instanceof RefEval)) {
         collectValue(arg, temp, true);
      } else {
         RefEval re = (RefEval)arg;
         int firstSheetIndex = re.getFirstSheetIndex();
         int lastSheetIndex = re.getLastSheetIndex();

         for (int sIx = firstSheetIndex; sIx <= lastSheetIndex; sIx++) {
            collectValue(re.getInnerValueEval(sIx), temp, true);
         }
      }
   }

   private static void collectValue(ValueEval arg, List<Double> temp, boolean mustBeNumber) throws EvaluationException {
      if (arg instanceof ErrorEval) {
         throw new EvaluationException((ErrorEval)arg);
      }

      if (arg != BlankEval.instance && !(arg instanceof BoolEval) && !(arg instanceof StringEval)) {
         if (arg instanceof NumberEval) {
            temp.add(((NumberEval)arg).getNumberValue());
         } else {
            throw new RuntimeException("Unexpected value type (" + arg.getClass().getName() + ")");
         }
      } else {
         if (mustBeNumber) {
            throw EvaluationException.invalidValue();
         }
      }
   }
}
