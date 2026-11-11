package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.ValueEval;

public final class DMin implements IDStarAlgorithm {
   private ValueEval minimumValue;

   public boolean processMatch(ValueEval eval) {
      if (eval instanceof NumericValueEval) {
         if (this.minimumValue == null) {
            this.minimumValue = eval;
         } else {
            double currentValue = ((NumericValueEval)eval).getNumberValue();
            double oldValue = ((NumericValueEval)this.minimumValue).getNumberValue();
            if (currentValue < oldValue) {
               this.minimumValue = eval;
            }
         }
      }

      return true;
   }

   public ValueEval getResult() {
      return (ValueEval)(this.minimumValue == null ? NumberEval.ZERO : this.minimumValue);
   }
}
