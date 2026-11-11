package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.MissingArgEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

public final class TimeFunc extends Fixed3ArgFunction {
   private static final int SECONDS_PER_MINUTE = 60;
   private static final int SECONDS_PER_HOUR = 3600;
   private static final int HOURS_PER_DAY = 24;
   private static final int SECONDS_PER_DAY = 86400;

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
      double result;
      try {
         result = evaluate(evalArg(arg0, srcRowIndex, srcColumnIndex), evalArg(arg1, srcRowIndex, srcColumnIndex), evalArg(arg2, srcRowIndex, srcColumnIndex));
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      return new NumberEval(result);
   }

   private static int evalArg(ValueEval arg, int srcRowIndex, int srcColumnIndex) throws EvaluationException {
      if (arg == MissingArgEval.instance) {
         return 0;
      }

      ValueEval ev = OperandResolver.getSingleValue(arg, srcRowIndex, srcColumnIndex);
      return OperandResolver.coerceValueToInt(ev);
   }

   private static double evaluate(int hours, int minutes, int seconds) throws EvaluationException {
      if (hours <= 32767 && minutes <= 32767 && seconds <= 32767) {
         int totalSeconds = hours * 3600 + minutes * 60 + seconds;
         if (totalSeconds < 0) {
            throw new EvaluationException(ErrorEval.VALUE_INVALID);
         } else {
            return totalSeconds % 86400 / 86400.0;
         }
      } else {
         throw new EvaluationException(ErrorEval.VALUE_INVALID);
      }
   }
}
