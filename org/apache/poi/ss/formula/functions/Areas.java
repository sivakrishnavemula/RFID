package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.RefListEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.ptg.NumberPtg;

public final class Areas implements Function {
   public ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
      if (args.length == 0) {
         return ErrorEval.VALUE_INVALID;
      }

      try {
         ValueEval valueEval = args[0];
         int result = 1;
         if (valueEval instanceof RefListEval) {
            RefListEval refListEval = (RefListEval)valueEval;
            result = refListEval.getList().size();
         }

         return new NumberEval(new NumberPtg(result));
      } catch (Exception e) {
         return ErrorEval.VALUE_INVALID;
      }
   }
}
