package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.AreaEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.RefListEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public class Rank extends Var2or3ArgFunction {
   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
      try {
         ValueEval ve = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
         double result = OperandResolver.coerceValueToDouble(ve);
         if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new EvaluationException(ErrorEval.NUM_ERROR);
         }

         if (arg1 instanceof RefListEval) {
            return eval(result, (RefListEval)arg1, true);
         }

         AreaEval aeRange = convertRangeArg(arg1);
         return eval(result, aeRange, true);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }
   }

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
      try {
         ValueEval ve = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
         double result = OperandResolver.coerceValueToDouble(ve);
         if (!Double.isNaN(result) && !Double.isInfinite(result)) {
            ve = OperandResolver.getSingleValue(arg2, srcRowIndex, srcColumnIndex);
            int order_value = OperandResolver.coerceValueToInt(ve);
            boolean order;
            if (order_value == 0) {
               order = true;
            } else {
               if (order_value != 1) {
                  throw new EvaluationException(ErrorEval.NUM_ERROR);
               }

               order = false;
            }

            if (arg1 instanceof RefListEval) {
               return eval(result, (RefListEval)arg1, order);
            }

            AreaEval aeRange = convertRangeArg(arg1);
            return eval(result, aeRange, order);
         } else {
            throw new EvaluationException(ErrorEval.NUM_ERROR);
         }
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }
   }

   private static ValueEval eval(double arg0, AreaEval aeRange, boolean descending_order) {
      int rank = 1;
      int height = aeRange.getHeight();
      int width = aeRange.getWidth();

      for (int r = 0; r < height; r++) {
         for (int c = 0; c < width; c++) {
            Double value = getValue(aeRange, r, c);
            if (value != null && (descending_order && value > arg0 || !descending_order && value < arg0)) {
               rank++;
            }
         }
      }

      return new NumberEval(rank);
   }

   private static ValueEval eval(double arg0, RefListEval aeRange, boolean descending_order) {
      int rank = 1;

      for (ValueEval ve : aeRange.getList()) {
         if (ve instanceof RefEval) {
            ve = ((RefEval)ve).getInnerValueEval(((RefEval)ve).getFirstSheetIndex());
         }

         if (ve instanceof NumberEval) {
            Double value = ((NumberEval)ve).getNumberValue();
            if (descending_order && value > arg0 || !descending_order && value < arg0) {
               rank++;
            }
         }
      }

      return new NumberEval(rank);
   }

   private static Double getValue(AreaEval aeRange, int relRowIndex, int relColIndex) {
      ValueEval addend = aeRange.getRelativeValue(relRowIndex, relColIndex);
      return addend instanceof NumberEval ? ((NumberEval)addend).getNumberValue() : null;
   }

   private static AreaEval convertRangeArg(ValueEval eval) throws EvaluationException {
      if (eval instanceof AreaEval) {
         return (AreaEval)eval;
      } else if (eval instanceof RefEval) {
         return ((RefEval)eval).offset(0, 0, 0, 0);
      } else {
         throw new EvaluationException(ErrorEval.VALUE_INVALID);
      }
   }
}
