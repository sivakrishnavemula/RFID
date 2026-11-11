package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public final class DSum implements IDStarAlgorithm {
   private double totalValue = 0.0;

   public boolean processMatch(ValueEval eval) {
      if (eval instanceof NumericValueEval) {
         double currentValue = ((NumericValueEval)eval).getNumberValue();
         this.totalValue += currentValue;
      }

      return true;
   }

   public ValueEval getResult() {
      return new NumberEval(this.totalValue);
   }
}
