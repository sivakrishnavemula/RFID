package org.apache.poi.ss.formula.functions;

import java.math.BigDecimal;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

public final class Delta extends Fixed2ArgFunction implements FreeRefFunction {
   public static final FreeRefFunction instance = new Delta();
   private static final NumberEval ONE = new NumberEval(1.0);
   private static final NumberEval ZERO = new NumberEval(0.0);

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg1, ValueEval arg2) {
      ValueEval veText1;
      try {
         veText1 = OperandResolver.getSingleValue(arg1, srcRowIndex, srcColumnIndex);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      String strText1 = OperandResolver.coerceValueToString(veText1);
      Double number1 = OperandResolver.parseDouble(strText1);
      if (number1 == null) {
         return ErrorEval.VALUE_INVALID;
      }

      ValueEval veText2;
      try {
         veText2 = OperandResolver.getSingleValue(arg2, srcRowIndex, srcColumnIndex);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      String strText2 = OperandResolver.coerceValueToString(veText2);
      Double number2 = OperandResolver.parseDouble(strText2);
      if (number2 == null) {
         return ErrorEval.VALUE_INVALID;
      }

      int result = new BigDecimal(number1).compareTo(new BigDecimal(number2));
      return result == 0 ? ONE : ZERO;
   }

   public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
      return (ValueEval)(args.length == 2 ? this.evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0], args[1]) : ErrorEval.VALUE_INVALID);
   }
}
