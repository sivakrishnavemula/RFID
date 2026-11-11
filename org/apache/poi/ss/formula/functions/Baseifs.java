package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.AreaEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.CountUtils.I_MatchPredicate;
import org.apache.poi.ss.formula.functions.Countif.ErrorMatcher;

abstract class Baseifs implements FreeRefFunction {
   protected abstract boolean hasInitialRange();

   public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
      boolean hasInitialRange = this.hasInitialRange();
      int firstCriteria = hasInitialRange ? 1 : 0;
      if (args.length >= 2 + firstCriteria && args.length % 2 == firstCriteria) {
         try {
            AreaEval sumRange = null;
            if (hasInitialRange) {
               sumRange = convertRangeArg(args[0]);
            }

            AreaEval[] ae = new AreaEval[(args.length - firstCriteria) / 2];
            I_MatchPredicate[] mp = new I_MatchPredicate[ae.length];
            int i = firstCriteria;

            for (int k = 0; i < args.length; k++) {
               ae[k] = convertRangeArg(args[i]);
               mp[k] = Countif.createCriteriaPredicate(args[i + 1], ec.getRowIndex(), ec.getColumnIndex());
               i += 2;
            }

            validateCriteriaRanges(sumRange, ae);
            validateCriteria(mp);
            double result = aggregateMatchingCells(sumRange, ae, mp);
            return new NumberEval(result);
         } catch (EvaluationException e) {
            return e.getErrorEval();
         }
      } else {
         return ErrorEval.VALUE_INVALID;
      }
   }

   private static void validateCriteriaRanges(AreaEval sumRange, AreaEval[] criteriaRanges) throws EvaluationException {
      int h = criteriaRanges[0].getHeight();
      int w = criteriaRanges[0].getWidth();
      if (sumRange == null || sumRange.getHeight() == h && sumRange.getWidth() == w) {
         for (AreaEval r : criteriaRanges) {
            if (r.getHeight() != h || r.getWidth() != w) {
               throw EvaluationException.invalidValue();
            }
         }
      } else {
         throw EvaluationException.invalidValue();
      }
   }

   private static void validateCriteria(I_MatchPredicate[] criteria) throws EvaluationException {
      for (I_MatchPredicate predicate : criteria) {
         if (predicate instanceof ErrorMatcher) {
            throw new EvaluationException(ErrorEval.valueOf(((ErrorMatcher)predicate).getValue()));
         }
      }
   }

   private static double aggregateMatchingCells(AreaEval sumRange, AreaEval[] ranges, I_MatchPredicate[] predicates) {
      int height = ranges[0].getHeight();
      int width = ranges[0].getWidth();
      double result = 0.0;

      for (int r = 0; r < height; r++) {
         for (int c = 0; c < width; c++) {
            boolean matches = true;

            for (int i = 0; i < ranges.length; i++) {
               AreaEval aeRange = ranges[i];
               I_MatchPredicate mp = predicates[i];
               if (mp == null || !mp.matches(aeRange.getRelativeValue(r, c))) {
                  matches = false;
                  break;
               }
            }

            if (matches) {
               result += accumulate(sumRange, r, c);
            }
         }
      }

      return result;
   }

   private static double accumulate(AreaEval sumRange, int relRowIndex, int relColIndex) {
      if (sumRange == null) {
         return 1.0;
      }

      ValueEval addend = sumRange.getRelativeValue(relRowIndex, relColIndex);
      return addend instanceof NumberEval ? ((NumberEval)addend).getNumberValue() : 0.0;
   }

   protected static AreaEval convertRangeArg(ValueEval eval) throws EvaluationException {
      if (eval instanceof AreaEval) {
         return (AreaEval)eval;
      } else if (eval instanceof RefEval) {
         return ((RefEval)eval).offset(0, 0, 0, 0);
      } else {
         throw new EvaluationException(ErrorEval.VALUE_INVALID);
      }
   }
}
