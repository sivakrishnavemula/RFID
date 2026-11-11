package org.apache.poi.ss.formula.eval;

import org.apache.poi.ss.formula.functions.Fixed1ArgFunction;
import org.apache.poi.ss.formula.functions.Function;

public final class UnaryPlusEval extends Fixed1ArgFunction {
   public static final Function instance = new UnaryPlusEval();

   private UnaryPlusEval() {
   }

   public ValueEval evaluate(int srcCellRow, int srcCellCol, ValueEval arg0) {
      double d;
      try {
         ValueEval ve = OperandResolver.getSingleValue(arg0, srcCellRow, srcCellCol);
         if (ve instanceof StringEval) {
            return ve;
         }

         d = OperandResolver.coerceValueToDouble(ve);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      return new NumberEval(d);
   }
}
