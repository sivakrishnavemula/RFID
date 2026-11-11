package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public abstract class Fixed3ArgFunction implements Function3Arg {
   public final ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
      return (ValueEval)(args.length != 3 ? ErrorEval.VALUE_INVALID : this.evaluate(srcRowIndex, srcColumnIndex, args[0], args[1], args[2]));
   }
}
