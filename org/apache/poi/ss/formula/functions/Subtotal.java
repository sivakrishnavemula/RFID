package org.apache.poi.ss.formula.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.formula.LazyRefEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.formula.eval.NotImplementedFunctionException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

public class Subtotal implements Function {
   private static Function findFunction(int functionCode) throws EvaluationException {
      switch (functionCode) {
         case 1:
            return AggregateFunction.subtotalInstance(AggregateFunction.AVERAGE);
         case 2:
            return Count.subtotalInstance();
         case 3:
            return Counta.subtotalInstance();
         case 4:
            return AggregateFunction.subtotalInstance(AggregateFunction.MAX);
         case 5:
            return AggregateFunction.subtotalInstance(AggregateFunction.MIN);
         case 6:
            return AggregateFunction.subtotalInstance(AggregateFunction.PRODUCT);
         case 7:
            return AggregateFunction.subtotalInstance(AggregateFunction.STDEV);
         case 8:
            throw new NotImplementedFunctionException("STDEVP");
         case 9:
            return AggregateFunction.subtotalInstance(AggregateFunction.SUM);
         case 10:
            throw new NotImplementedFunctionException("VAR");
         case 11:
            throw new NotImplementedFunctionException("VARP");
         default:
            if (functionCode > 100 && functionCode < 112) {
               throw new NotImplementedException("SUBTOTAL - with 'exclude hidden values' option");
            } else {
               throw EvaluationException.invalidValue();
            }
      }
   }

   public ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
      int nInnerArgs = args.length - 1;
      if (nInnerArgs < 1) {
         return ErrorEval.VALUE_INVALID;
      }

      Function innerFunc;
      try {
         ValueEval ve = OperandResolver.getSingleValue(args[0], srcRowIndex, srcColumnIndex);
         int functionCode = OperandResolver.coerceValueToInt(ve);
         innerFunc = findFunction(functionCode);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      List<ValueEval> list = new ArrayList(Arrays.asList(args).subList(1, args.length));
      Iterator<ValueEval> it = list.iterator();

      while (it.hasNext()) {
         ValueEval eval = (ValueEval)it.next();
         if (eval instanceof LazyRefEval) {
            LazyRefEval lazyRefEval = (LazyRefEval)eval;
            if (lazyRefEval.isSubTotal()) {
               it.remove();
            }
         }
      }

      return innerFunc.evaluate((ValueEval[])list.toArray(new ValueEval[list.size()]), srcRowIndex, srcColumnIndex);
   }
}
