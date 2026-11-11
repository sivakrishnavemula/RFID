package org.apache.poi.ss.format;

import java.util.Formatter;
import java.util.Locale;
import org.apache.poi.util.LocaleUtil;

public class CellGeneralFormatter extends CellFormatter {
   public CellGeneralFormatter() {
      this(LocaleUtil.getUserLocale());
   }

   public CellGeneralFormatter(Locale locale) {
      super(locale, "General");
   }

   public void formatValue(StringBuffer toAppendTo, Object value) {
      if (value instanceof Number) {
         double val = ((Number)value).doubleValue();
         if (val == 0.0) {
            toAppendTo.append('0');
            return;
         }

         double exp = Math.log10(Math.abs(val));
         boolean stripZeros = true;
         String fmt;
         if (!(exp > 10.0) && !(exp < -9.0)) {
            if ((long)val != val) {
               fmt = "%1.9f";
            } else {
               fmt = "%1.0f";
               stripZeros = false;
            }
         } else {
            fmt = "%1.5E";
         }

         Formatter formatter = new Formatter(toAppendTo, this.locale);

         try {
            formatter.format(this.locale, fmt, new Object[]{value});
         } finally {
            formatter.close();
         }

         if (stripZeros) {
            int removeFrom;
            if (fmt.endsWith("E")) {
               removeFrom = toAppendTo.lastIndexOf("E") - 1;
            } else {
               removeFrom = toAppendTo.length() - 1;
            }

            while (toAppendTo.charAt(removeFrom) == '0') {
               toAppendTo.deleteCharAt(removeFrom--);
            }

            if (toAppendTo.charAt(removeFrom) == '.') {
               toAppendTo.deleteCharAt(removeFrom--);
            }
         }
      } else if (value instanceof Boolean) {
         toAppendTo.append(value.toString().toUpperCase(Locale.ROOT));
      } else {
         toAppendTo.append(value);
      }
   }

   public void simpleValue(StringBuffer toAppendTo, Object value) {
      this.formatValue(toAppendTo, value);
   }
}
