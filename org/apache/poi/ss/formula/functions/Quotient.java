package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

public class Quotient extends Fixed2ArgFunction implements FreeRefFunction {
   public static final FreeRefFunction instance = new Quotient();

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval venumerator, ValueEval vedenominator) {
      double enumerator = 0.0;

      try {
         enumerator = OperandResolver.coerceValueToDouble(venumerator);
      } catch (EvaluationException e) {
         return ErrorEval.VALUE_INVALID;
      }

      double denominator = 0.0;

      try {
         denominator = OperandResolver.coerceValueToDouble(vedenominator);
      } catch (EvaluationException e) {
         return ErrorEval.VALUE_INVALID;
      }

      return (ValueEval)(denominator == 0.0 ? ErrorEval.DIV_ZERO : new NumberEval((int)(enumerator / denominator)));
   }

   public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
      return (ValueEval)(args.length != 2 ? ErrorEval.VALUE_INVALID : this.evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0], args[1]));
   }
}
