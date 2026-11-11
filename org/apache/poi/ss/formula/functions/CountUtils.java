package org.apache.poi.ss.formula.functions;

import org.apache.poi.ss.formula.ThreeDEval;
import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.ValueEval;

final class CountUtils {
   private CountUtils() {
   }

   public static int countMatchingCellsInArea(ThreeDEval areaEval, CountUtils.I_MatchPredicate criteriaPredicate) {
      int result = 0;
      int firstSheetIndex = areaEval.getFirstSheetIndex();
      int lastSheetIndex = areaEval.getLastSheetIndex();

      for (int sIx = firstSheetIndex; sIx <= lastSheetIndex; sIx++) {
         int height = areaEval.getHeight();
         int width = areaEval.getWidth();

         for (int rrIx = 0; rrIx < height; rrIx++) {
            for (int rcIx = 0; rcIx < width; rcIx++) {
               ValueEval ve = areaEval.getValue(sIx, rrIx, rcIx);
               if (criteriaPredicate instanceof CountUtils.I_MatchAreaPredicate) {
                  CountUtils.I_MatchAreaPredicate areaPredicate = (CountUtils.I_MatchAreaPredicate)criteriaPredicate;
                  if (!areaPredicate.matches(areaEval, rrIx, rcIx)) {
                     continue;
                  }
               }

               if (criteriaPredicate.matches(ve)) {
                  result++;
               }
            }
         }
      }

      return result;
   }

   public static int countMatchingCellsInRef(RefEval refEval, CountUtils.I_MatchPredicate criteriaPredicate) {
      int result = 0;
      int firstSheetIndex = refEval.getFirstSheetIndex();
      int lastSheetIndex = refEval.getLastSheetIndex();

      for (int sIx = firstSheetIndex; sIx <= lastSheetIndex; sIx++) {
         ValueEval ve = refEval.getInnerValueEval(sIx);
         if (criteriaPredicate.matches(ve)) {
            result++;
         }
      }

      return result;
   }

   public static int countArg(ValueEval eval, CountUtils.I_MatchPredicate criteriaPredicate) {
      if (eval == null) {
         throw new IllegalArgumentException("eval must not be null");
      } else if (eval instanceof ThreeDEval) {
         return countMatchingCellsInArea((ThreeDEval)eval, criteriaPredicate);
      } else if (eval instanceof TwoDEval) {
         throw new IllegalArgumentException("Count requires 3D Evals, 2D ones aren't supported");
      } else if (eval instanceof RefEval) {
         return countMatchingCellsInRef((RefEval)eval, criteriaPredicate);
      } else {
         return criteriaPredicate.matches(eval) ? 1 : 0;
      }
   }

   public interface I_MatchAreaPredicate extends CountUtils.I_MatchPredicate {
      boolean matches(TwoDEval var1, int var2, int var3);
   }

   public interface I_MatchPredicate {
      boolean matches(ValueEval var1);
   }
}
