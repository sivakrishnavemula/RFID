package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public class Dec2Bin extends Var1or2ArgFunction implements FreeRefFunction {
   public static final FreeRefFunction instance = new Dec2Bin();
   private static final long MIN_VALUE = -512L;
   private static final long MAX_VALUE = 511L;
   private static final int DEFAULT_PLACES_VALUE = 10;

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval numberVE, ValueEval placesVE) {
      ValueEval veText1;
      try {
         veText1 = OperandResolver.getSingleValue(numberVE, srcRowIndex, srcColumnIndex);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      String strText1 = OperandResolver.coerceValueToString(veText1);
      Double number = OperandResolver.parseDouble(strText1);
      if (number == null) {
         return ErrorEval.VALUE_INVALID;
      }

      if (number.longValue() >= -512L && number.longValue() <= 511L) {
         int placesNumber;
         if (!(number < 0.0) && placesVE != null) {
            ValueEval placesValueEval;
            try {
               placesValueEval = OperandResolver.getSingleValue(placesVE, srcRowIndex, srcColumnIndex);
            } catch (EvaluationException e) {
               return e.getErrorEval();
            }

            String placesStr = OperandResolver.coerceValueToString(placesValueEval);
            Double placesNumberDouble = OperandResolver.parseDouble(placesStr);
            if (placesNumberDouble == null) {
               return ErrorEval.VALUE_INVALID;
            }

            placesNumber = placesNumberDouble.intValue();
            if (placesNumber < 0 || placesNumber == 0) {
               return ErrorEval.NUM_ERROR;
            }
         } else {
            placesNumber = 10;
         }

         String binary = Integer.toBinaryString(number.intValue());
         if (binary.length() > 10) {
            binary = binary.substring(binary.length() - 10, binary.length());
         }

         return (ValueEval)(binary.length() > placesNumber ? ErrorEval.NUM_ERROR : new StringEval(binary));
      } else {
         return ErrorEval.NUM_ERROR;
      }
   }

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval numberVE) {
      return this.evaluate(srcRowIndex, srcColumnIndex, numberVE, null);
   }

   public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
      if (args.length == 1) {
         return this.evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0]);
      } else {
         return (ValueEval)(args.length == 2 ? this.evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0], args[1]) : ErrorEval.VALUE_INVALID);
      }
   }
}
