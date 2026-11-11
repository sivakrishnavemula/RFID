package org.apache.poi.ss.formula.atp;

import java.util.Calendar;
import java.util.regex.Pattern;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.util.LocaleUtil;

public class DateParser {
   private DateParser() {
   }

   public static Calendar parseDate(String strVal) throws EvaluationException {
      String[] parts = Pattern.compile("/").split(strVal);
      if (parts.length != 3) {
         throw new EvaluationException(ErrorEval.VALUE_INVALID);
      }

      String part2 = parts[2];
      int spacePos = part2.indexOf(32);
      if (spacePos > 0) {
         part2 = part2.substring(0, spacePos);
      }

      int f0;
      int f1;
      int f2;
      try {
         f0 = Integer.parseInt(parts[0]);
         f1 = Integer.parseInt(parts[1]);
         f2 = Integer.parseInt(part2);
      } catch (NumberFormatException e) {
         throw new EvaluationException(ErrorEval.VALUE_INVALID);
      }

      if (f0 >= 0 && f1 >= 0 && f2 >= 0 && (f0 <= 12 || f1 <= 12 || f2 <= 12)) {
         if (f0 >= 1900 && f0 < 9999) {
            return makeDate(f0, f1, f2);
         } else {
            throw new RuntimeException("Unable to determine date format for text '" + strVal + "'");
         }
      } else {
         throw new EvaluationException(ErrorEval.VALUE_INVALID);
      }
   }

   private static Calendar makeDate(int year, int month, int day) throws EvaluationException {
      if (month >= 1 && month <= 12) {
         Calendar cal = LocaleUtil.getLocaleCalendar(year, month - 1, 1, 0, 0, 0);
         if (day >= 1 && day <= cal.getActualMaximum(5)) {
            cal.set(5, day);
            return cal;
         } else {
            throw new EvaluationException(ErrorEval.VALUE_INVALID);
         }
      } else {
         throw new EvaluationException(ErrorEval.VALUE_INVALID);
      }
   }
}
