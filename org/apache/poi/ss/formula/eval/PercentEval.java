package org.apache.poi.ss.formula.eval;

import org.apache.poi.ss.formula.functions.Fixed1ArgFunction;
import org.apache.poi.ss.formula.functions.Function;

public final class PercentEval extends Fixed1ArgFunction {
   public static final Function instance = new PercentEval();

   private PercentEval() {
   }

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
      double d;
      try {
         ValueEval ve = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
         d = OperandResolver.coerceValueToDouble(ve);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      return d == 0.0 ? NumberEval.ZERO : new NumberEval(d / 100.0);
   }
}
