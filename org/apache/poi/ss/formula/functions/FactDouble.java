package org.apache.poi.ss.formula.functions;

import java.math.BigInteger;
import java.util.HashMap;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

public class FactDouble extends Fixed1ArgFunction implements FreeRefFunction {
   public static final FreeRefFunction instance = new FactDouble();
   static HashMap<Integer, BigInteger> cache = new HashMap();

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval numberVE) {
      int number;
      try {
         number = OperandResolver.coerceValueToInt(numberVE);
      } catch (EvaluationException e) {
         return ErrorEval.VALUE_INVALID;
      }

      return (ValueEval)(number < 0 ? ErrorEval.NUM_ERROR : new NumberEval(factorial(number).longValue()));
   }

   public static BigInteger factorial(int n) {
      if (n != 0 && n >= 0) {
         if (cache.containsKey(n)) {
            return (BigInteger)cache.get(n);
         }

         BigInteger result = BigInteger.valueOf(n).multiply(factorial(n - 2));
         cache.put(n, result);
         return result;
      } else {
         return BigInteger.ONE;
      }
   }

   public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
      return (ValueEval)(args.length != 1 ? ErrorEval.VALUE_INVALID : this.evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0]));
   }
}
