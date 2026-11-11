package org.apache.poi.ss.formula.atp;

import java.util.Calendar;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.util.LocaleUtil;

final class YearFracCalculator {
   private static final int MS_PER_HOUR = 3600000;
   private static final int MS_PER_DAY = 86400000;
   private static final int DAYS_PER_NORMAL_YEAR = 365;
   private static final int DAYS_PER_LEAP_YEAR = 366;
   private static final int LONG_MONTH_LEN = 31;
   private static final int SHORT_MONTH_LEN = 30;
   private static final int SHORT_FEB_LEN = 28;
   private static final int LONG_FEB_LEN = 29;

   private YearFracCalculator() {
   }

   public static double calculate(double pStartDateVal, double pEndDateVal, int basis) throws EvaluationException {
      if (basis >= 0 && basis < 5) {
         int startDateVal = (int)Math.floor(pStartDateVal);
         int endDateVal = (int)Math.floor(pEndDateVal);
         if (startDateVal == endDateVal) {
            return 0.0;
         }

         if (startDateVal > endDateVal) {
            int temp = startDateVal;
            startDateVal = endDateVal;
            endDateVal = temp;
         }

         switch (basis) {
            case 0:
               return basis0(startDateVal, endDateVal);
            case 1:
               return basis1(startDateVal, endDateVal);
            case 2:
               return basis2(startDateVal, endDateVal);
            case 3:
               return basis3(startDateVal, endDateVal);
            case 4:
               return basis4(startDateVal, endDateVal);
            default:
               throw new IllegalStateException("cannot happen");
         }
      } else {
         throw new EvaluationException(ErrorEval.NUM_ERROR);
      }
   }

   public static double basis0(int startDateVal, int endDateVal) {
      YearFracCalculator.SimpleDate startDate = createDate(startDateVal);
      YearFracCalculator.SimpleDate endDate = createDate(endDateVal);
      int date1day = startDate.day;
      int date2day = endDate.day;
      if (date1day == 31 && date2day == 31) {
         date1day = 30;
         date2day = 30;
      } else if (date1day == 31) {
         date1day = 30;
      } else if (date1day == 30 && date2day == 31) {
         date2day = 30;
      } else if (startDate.month == 2 && isLastDayOfMonth(startDate)) {
         date1day = 30;
         if (endDate.month == 2 && isLastDayOfMonth(endDate)) {
            date2day = 30;
         }
      }

      return calculateAdjusted(startDate, endDate, date1day, date2day);
   }

   public static double basis1(int startDateVal, int endDateVal) {
      YearFracCalculator.SimpleDate startDate = createDate(startDateVal);
      YearFracCalculator.SimpleDate endDate = createDate(endDateVal);
      double yearLength;
      if (isGreaterThanOneYear(startDate, endDate)) {
         yearLength = averageYearLength(startDate.year, endDate.year);
      } else if (shouldCountFeb29(startDate, endDate)) {
         yearLength = 366.0;
      } else {
         yearLength = 365.0;
      }

      return dateDiff(startDate.tsMilliseconds, endDate.tsMilliseconds) / yearLength;
   }

   public static double basis2(int startDateVal, int endDateVal) {
      return (endDateVal - startDateVal) / 360.0;
   }

   public static double basis3(double startDateVal, double endDateVal) {
      return (endDateVal - startDateVal) / 365.0;
   }

   public static double basis4(int startDateVal, int endDateVal) {
      YearFracCalculator.SimpleDate startDate = createDate(startDateVal);
      YearFracCalculator.SimpleDate endDate = createDate(endDateVal);
      int date1day = startDate.day;
      int date2day = endDate.day;
      if (date1day == 31) {
         date1day = 30;
      }

      if (date2day == 31) {
         date2day = 30;
      }

      return calculateAdjusted(startDate, endDate, date1day, date2day);
   }

   private static double calculateAdjusted(YearFracCalculator.SimpleDate startDate, YearFracCalculator.SimpleDate endDate, int date1day, int date2day) {
      double dayCount = (endDate.year - startDate.year) * 360 + (endDate.month - startDate.month) * 30 + (date2day - date1day) * 1;
      return dayCount / 360.0;
   }

   private static boolean isLastDayOfMonth(YearFracCalculator.SimpleDate date) {
      return date.day < 28 ? false : date.day == getLastDayOfMonth(date);
   }

   private static int getLastDayOfMonth(YearFracCalculator.SimpleDate date) {
      switch (date.month) {
         case 1:
         case 3:
         case 5:
         case 7:
         case 8:
         case 10:
         case 12:
            return 31;
         case 2:
         default:
            if (isLeapYear(date.year)) {
               return 29;
            }

            return 28;
         case 4:
         case 6:
         case 9:
         case 11:
            return 30;
      }
   }

   private static boolean shouldCountFeb29(YearFracCalculator.SimpleDate start, YearFracCalculator.SimpleDate end) {
      if (isLeapYear(start.year)) {
         if (start.year == end.year) {
            return true;
         }

         switch (start.month) {
            case 1:
            case 2:
               return true;
            default:
               return false;
         }
      } else if (isLeapYear(end.year)) {
         switch (end.month) {
            case 1:
               return false;
            case 2:
               return end.day == 29;
            default:
               return true;
         }
      } else {
         return false;
      }
   }

   private static int dateDiff(long startDateMS, long endDateMS) {
      long msDiff = endDateMS - startDateMS;
      int remainderHours = (int)(msDiff % 86400000L / 3600000L);
      switch (remainderHours) {
         case 0:
            return (int)(0.5 + msDiff / 8.64E7);
         case 1:
         case 23:
         default:
            throw new RuntimeException("Unexpected date diff between " + startDateMS + " and " + endDateMS);
      }
   }

   private static double averageYearLength(int startYear, int endYear) {
      int dayCount = 0;

      for (int i = startYear; i <= endYear; i++) {
         dayCount += 365;
         if (isLeapYear(i)) {
            dayCount++;
         }
      }

      double numberOfYears = endYear - startYear + 1;
      return dayCount / numberOfYears;
   }

   private static boolean isLeapYear(int i) {
      if (i % 4 != 0) {
         return false;
      } else {
         return i % 400 == 0 ? true : i % 100 != 0;
      }
   }

   private static boolean isGreaterThanOneYear(YearFracCalculator.SimpleDate start, YearFracCalculator.SimpleDate end) {
      if (start.year == end.year) {
         return false;
      } else if (start.year + 1 != end.year) {
         return true;
      } else if (start.month > end.month) {
         return false;
      } else {
         return start.month < end.month ? true : start.day < end.day;
      }
   }

   private static YearFracCalculator.SimpleDate createDate(int dayCount) {
      Calendar cal = LocaleUtil.getLocaleCalendar(LocaleUtil.TIMEZONE_UTC);
      DateUtil.setCalendar(cal, dayCount, 0, false, false);
      return new YearFracCalculator.SimpleDate(cal);
   }

   private static final class SimpleDate {
      public static final int JANUARY = 1;
      public static final int FEBRUARY = 2;
      public final int year;
      public final int month;
      public final int day;
      public long tsMilliseconds;

      public SimpleDate(Calendar cal) {
         this.year = cal.get(1);
         this.month = cal.get(2) + 1;
         this.day = cal.get(5);
         this.tsMilliseconds = cal.getTimeInMillis();
      }
   }
}
