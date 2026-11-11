package org.apache.poi.ss.usermodel;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.format.SimpleFraction;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public class FractionFormat extends Format {
   private static final POILogger LOGGER = POILogFactory.getLogger(FractionFormat.class);
   private static final Pattern DENOM_FORMAT_PATTERN = Pattern.compile("(?:(#+)|(\\d+))");
   private static final int MAX_DENOM_POW = 4;
   private final int exactDenom;
   private final int maxDenom;
   private final String wholePartFormatString;

   public FractionFormat(String wholePartFormatString, String denomFormatString) {
      this.wholePartFormatString = wholePartFormatString;
      Matcher m = DENOM_FORMAT_PATTERN.matcher(denomFormatString);
      int tmpExact = -1;
      int tmpMax = -1;
      if (m.find()) {
         if (m.group(2) != null) {
            try {
               tmpExact = Integer.parseInt(m.group(2));
               if (tmpExact == 0) {
                  tmpExact = -1;
               }
            } catch (NumberFormatException var7) {
            }
         } else if (m.group(1) != null) {
            int len = m.group(1).length();
            len = len > 4 ? 4 : len;
            tmpMax = (int)Math.pow(10.0, len);
         } else {
            tmpExact = 100;
         }
      }

      if (tmpExact <= 0 && tmpMax <= 0) {
         tmpExact = 100;
      }

      this.exactDenom = tmpExact;
      this.maxDenom = tmpMax;
   }

   public String format(Number num) {
      double doubleValue = num.doubleValue();
      boolean isNeg = doubleValue < 0.0;
      double absDoubleValue = Math.abs(doubleValue);
      double wholePart = Math.floor(absDoubleValue);
      double decPart = absDoubleValue - wholePart;
      if (wholePart + decPart == 0.0) {
         return "0";
      }

      if (Double.compare(decPart, 0.0) == 0) {
         StringBuilder sb = new StringBuilder();
         if (isNeg) {
            sb.append("-");
         }

         sb.append((int)wholePart);
         return sb.toString();
      } else {
         SimpleFraction fract = null;

         try {
            if (this.exactDenom > 0) {
               fract = SimpleFraction.buildFractionExactDenominator(decPart, this.exactDenom);
            } else {
               fract = SimpleFraction.buildFractionMaxDenominator(decPart, this.maxDenom);
            }
         } catch (RuntimeException e) {
            LOGGER.log(5, new Object[]{"Can't format fraction", e});
            return Double.toString(doubleValue);
         }

         StringBuilder sb = new StringBuilder();
         if (isNeg) {
            sb.append("-");
         }

         if (this.wholePartFormatString == null || this.wholePartFormatString.isEmpty()) {
            int trueNum = fract.getDenominator() * (int)wholePart + fract.getNumerator();
            sb.append(trueNum).append("/").append(fract.getDenominator());
            return sb.toString();
         }

         if (fract.getNumerator() == 0) {
            sb.append(Integer.toString((int)wholePart));
            return sb.toString();
         }

         if (fract.getNumerator() == fract.getDenominator()) {
            sb.append(Integer.toString((int)wholePart + 1));
            return sb.toString();
         }

         if (wholePart > 0.0) {
            sb.append(Integer.toString((int)wholePart)).append(" ");
         }

         sb.append(fract.getNumerator()).append("/").append(fract.getDenominator());
         return sb.toString();
      }
   }

   public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
      return toAppendTo.append(this.format((Number)obj));
   }

   public Object parseObject(String source, ParsePosition pos) {
      throw new NotImplementedException("Reverse parsing not supported");
   }
}
