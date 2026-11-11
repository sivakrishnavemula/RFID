package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.MissingArgEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

public final class IfFunc extends Var2or3ArgFunction {
   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
      boolean b;
      try {
         b = evaluateFirstArg(arg0, srcRowIndex, srcColumnIndex);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      if (b) {
         return (ValueEval)(arg1 == MissingArgEval.instance ? BlankEval.instance : arg1);
      } else {
         return BoolEval.FALSE;
      }
   }

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
      boolean b;
      try {
         b = evaluateFirstArg(arg0, srcRowIndex, srcColumnIndex);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }

      if (b) {
         return (ValueEval)(arg1 == MissingArgEval.instance ? BlankEval.instance : arg1);
      } else {
         return (ValueEval)(arg2 == MissingArgEval.instance ? BlankEval.instance : arg2);
      }
   }

   public static boolean evaluateFirstArg(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
      ValueEval ve = OperandResolver.getSingleValue(arg, srcCellRow, srcCellCol);
      Boolean b = OperandResolver.coerceValueToBoolean(ve, false);
      return b == null ? false : b;
   }
}
