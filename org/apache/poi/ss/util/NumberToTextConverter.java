package org.apache.poi.ss.util;

public final class NumberToTextConverter {
   private static final long EXCEL_NAN_BITS = -276939487313920L;
   private static final int MAX_TEXT_LEN = 20;

   private NumberToTextConverter() {
   }

   public static String toText(double value) {
      return rawDoubleBitsToText(Double.doubleToLongBits(value));
   }

   static String rawDoubleBitsToText(long pRawBits) {
      long rawBits = pRawBits;
      boolean isNegative = rawBits < 0L;
      if (isNegative) {
         rawBits &= Long.MAX_VALUE;
      }

      if (rawBits == 0L) {
         return isNegative ? "-0" : "0";
      }

      ExpandedDouble ed = new ExpandedDouble(rawBits);
      if (ed.getBinaryExponent() < -1022) {
         return isNegative ? "-0" : "0";
      }

      if (ed.getBinaryExponent() == 1024) {
         if (rawBits == -276939487313920L) {
            return "3.484840871308E+308";
         }

         isNegative = false;
      }

      NormalisedDecimal nd = ed.normaliseBaseTen();
      StringBuilder sb = new StringBuilder(21);
      if (isNegative) {
         sb.append('-');
      }

      convertToText(sb, nd);
      return sb.toString();
   }

   private static void convertToText(StringBuilder sb, NormalisedDecimal pnd) {
      NormalisedDecimal rnd = pnd.roundUnits();
      int decExponent = rnd.getDecimalExponent();
      String decimalDigits;
      if (Math.abs(decExponent) > 98) {
         decimalDigits = rnd.getSignificantDecimalDigitsLastDigitRounded();
         if (decimalDigits.length() == 16) {
            decExponent++;
         }
      } else {
         decimalDigits = rnd.getSignificantDecimalDigits();
      }

      int countSigDigits = countSignifantDigits(decimalDigits);
      if (decExponent < 0) {
         formatLessThanOne(sb, decimalDigits, decExponent, countSigDigits);
      } else {
         formatGreaterThanOne(sb, decimalDigits, decExponent, countSigDigits);
      }
   }

   private static void formatLessThanOne(StringBuilder sb, String decimalDigits, int decExponent, int countSigDigits) {
      int nLeadingZeros = -decExponent - 1;
      int normalLength = 2 + nLeadingZeros + countSigDigits;
      if (needsScientificNotation(normalLength)) {
         sb.append(decimalDigits.charAt(0));
         if (countSigDigits > 1) {
            sb.append('.');
            sb.append(decimalDigits.subSequence(1, countSigDigits));
         }

         sb.append("E-");
         appendExp(sb, -decExponent);
      } else {
         sb.append("0.");

         for (int i = nLeadingZeros; i > 0; i--) {
            sb.append('0');
         }

         sb.append(decimalDigits.subSequence(0, countSigDigits));
      }
   }

   private static void formatGreaterThanOne(StringBuilder sb, String decimalDigits, int decExponent, int countSigDigits) {
      if (decExponent > 19) {
         sb.append(decimalDigits.charAt(0));
         if (countSigDigits > 1) {
            sb.append('.');
            sb.append(decimalDigits.subSequence(1, countSigDigits));
         }

         sb.append("E+");
         appendExp(sb, decExponent);
      } else {
         int nFractionalDigits = countSigDigits - decExponent - 1;
         if (nFractionalDigits > 0) {
            sb.append(decimalDigits.subSequence(0, decExponent + 1));
            sb.append('.');
            sb.append(decimalDigits.subSequence(decExponent + 1, countSigDigits));
         } else {
            sb.append(decimalDigits.subSequence(0, countSigDigits));

            for (int i = -nFractionalDigits; i > 0; i--) {
               sb.append('0');
            }
         }
      }
   }

   private static boolean needsScientificNotation(int nDigits) {
      return nDigits > 20;
   }

   private static int countSignifantDigits(String sb) {
      int result = sb.length() - 1;

      while (sb.charAt(result) == '0') {
         if (--result < 0) {
            throw new RuntimeException("No non-zero digits found");
         }
      }

      return result + 1;
   }

   private static void appendExp(StringBuilder sb, int val) {
      if (val < 10) {
         sb.append('0');
         sb.append((char)(48 + val));
      } else {
         sb.append(val);
      }
   }
}
