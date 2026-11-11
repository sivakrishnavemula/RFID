package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.LookupUtils.ValueVector;

public final class Lookup extends Var2or3ArgFunction {
   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
      try {
         ValueEval lookupValue = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
         TwoDEval lookupArray = LookupUtils.resolveTableArrayArg(arg1);
         ValueVector lookupVector;
         ValueVector resultVector;
         if (lookupArray.getWidth() > lookupArray.getHeight()) {
            lookupVector = createVector(lookupArray.getRow(0));
            resultVector = createVector(lookupArray.getRow(lookupArray.getHeight() - 1));
         } else {
            lookupVector = createVector(lookupArray.getColumn(0));
            resultVector = createVector(lookupArray.getColumn(lookupArray.getWidth() - 1));
         }

         assert lookupVector.getSize() == resultVector.getSize();
         int index = LookupUtils.lookupIndexOfValue(lookupValue, lookupVector, true);
         return resultVector.getItem(index);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }
   }

   public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
      try {
         ValueEval lookupValue = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
         TwoDEval aeLookupVector = LookupUtils.resolveTableArrayArg(arg1);
         TwoDEval aeResultVector = LookupUtils.resolveTableArrayArg(arg2);
         ValueVector lookupVector = createVector(aeLookupVector);
         ValueVector resultVector = createVector(aeResultVector);
         if (lookupVector.getSize() > resultVector.getSize()) {
            throw new RuntimeException("Lookup vector and result vector of differing sizes not supported yet");
         }

         int index = LookupUtils.lookupIndexOfValue(lookupValue, lookupVector, true);
         return resultVector.getItem(index);
      } catch (EvaluationException e) {
         return e.getErrorEval();
      }
   }

   private static ValueVector createVector(TwoDEval ae) {
      ValueVector result = LookupUtils.createVector(ae);
      if (result != null) {
         return result;
      } else {
         throw new RuntimeException("non-vector lookup or result areas not supported yet");
      }
   }
}
