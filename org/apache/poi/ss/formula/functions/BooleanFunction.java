package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.MissingArgEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public abstract class BooleanFunction implements Function {
   public static final Function AND = new BooleanFunction() {
      @Override
      protected boolean getInitialResultValue() {
         return true;
      }

      @Override
      protected boolean partialEvaluate(boolean cumulativeResult, boolean currentValue) {
         return cumulativeResult && currentValue;
      }
   };
   public static final Function OR = new BooleanFunction() {
      @Override
      protected boolean getInitialResultValue() {
         return false;
      }

      @Override
      protected boolean partialEvaluate(boolean cumulativeResult, boolean currentValue) {
         return cumulativeResult || currentValue;
      }
   };
   public static final Function FALSE = new Fixed0ArgFunction() {
      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex) {
         return BoolEval.FALSE;
      }
   };
   public static final Function TRUE = new Fixed0ArgFunction() {
      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex) {
         return BoolEval.TRUE;
      }
   };
   public static final Function NOT = new Fixed1ArgFunction() {
      public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
         boolean boolArgVal;
         try {
            ValueEval ve = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
            Boolean b = OperandResolver.coerceValueToBoolean(ve, false);
            boolArgVal = b == null ? false : b;
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }

         return BoolEval.valueOf(!boolArgVal);
      }
   };

   public final ValueEval evaluate(ValueEval[] args, int srcRow, int srcCol) {
      if (args.length < 1) {
         return ErrorEval.VALUE_INVALID;
      }

      boolean boolResult;
      try {
         boolResult = this.calculate(args);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      return BoolEval.valueOf(boolResult);
   }

   private boolean calculate(ValueEval[] args) throws EvaluationException {
      boolean result = this.getInitialResultValue();
      boolean atleastOneNonBlank = false;

      for (ValueEval arg : args) {
         if (!(arg instanceof TwoDEval)) {
            if (arg instanceof RefEval) {
               RefEval re = (RefEval)arg;
               int firstSheetIndex = re.getFirstSheetIndex();
               int lastSheetIndex = re.getLastSheetIndex();

               for (int sIx = firstSheetIndex; sIx <= lastSheetIndex; sIx++) {
                  ValueEval ve = re.getInnerValueEval(sIx);
                  Boolean tempVe = OperandResolver.coerceValueToBoolean(ve, true);
                  if (tempVe != null) {
                     result = this.partialEvaluate(result, tempVe);
                     atleastOneNonBlank = true;
                  }
               }
            } else {
               Boolean tempVe;
               if (arg == MissingArgEval.instance) {
                  tempVe = null;
               } else {
                  tempVe = OperandResolver.coerceValueToBoolean(arg, false);
               }

               if (tempVe != null) {
                  result = this.partialEvaluate(result, tempVe);
                  atleastOneNonBlank = true;
               }
            }
         } else {
            TwoDEval ae = (TwoDEval)arg;
            int height = ae.getHeight();
            int width = ae.getWidth();

            for (int rrIx = 0; rrIx < height; rrIx++) {
               for (int rcIx = 0; rcIx < width; rcIx++) {
                  ValueEval ve = ae.getValue(rrIx, rcIx);
                  Boolean tempVe = OperandResolver.coerceValueToBoolean(ve, true);
                  if (tempVe != null) {
                     result = this.partialEvaluate(result, tempVe);
                     atleastOneNonBlank = true;
                  }
               }
            }
         }
      }

      if (!atleastOneNonBlank) {
         throw new EvaluationException(ErrorEval.VALUE_INVALID);
      } else {
         return result;
      }
   }

   protected abstract boolean getInitialResultValue();

   protected abstract boolean partialEvaluate(boolean var1, boolean var2);
}
