package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

public class Oct2Dec extends Fixed1ArgFunction implements FreeRefFunction {
   public static final FreeRefFunction instance = new Oct2Dec();
   static final int MAX_NUMBER_OF_PLACES = 10;
   static final int OCTAL_BASE = 8;

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval numberVE) {
      String octal = OperandResolver.coerceValueToString(numberVE);

      try {
         return new NumberEval(BaseNumberUtils.convertToDecimal(octal, 8, 10));
      } catch (IllegalArgumentException e) {
         return ErrorEval.NUM_ERROR;
      }
   }

   public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
      return (ValueEval)(args.length != 1 ? ErrorEval.VALUE_INVALID : this.evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0]));
   }
}
