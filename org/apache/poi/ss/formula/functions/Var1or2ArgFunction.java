package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.ValueEval;

abstract class Var1or2ArgFunction implements Function1Arg, Function2Arg {
   public final ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
      switch (args.length) {
         case 1:
            return this.evaluate(srcRowIndex, srcColumnIndex, args[0]);
         case 2:
            return this.evaluate(srcRowIndex, srcColumnIndex, args[0], args[1]);
         default:
            return ErrorEval.VALUE_INVALID;
      }
   }
}
