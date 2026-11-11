package org.apache.poi.ss.format;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.format.CellFormatPart.PartHandler;

public class CellElapsedFormatter extends CellFormatter {
   private final List<CellElapsedFormatter.TimeSpec> specs = new ArrayList();
   private CellElapsedFormatter.TimeSpec topmost;
   private final String printfFmt;
   private static final Pattern PERCENTS = Pattern.compile("%");
   private static final double HOUR__FACTOR = 0.041666668F;
   private static final double MIN__FACTOR = 6.9444446E-4F;
   private static final double SEC__FACTOR = 1.1574074E-5F;

   public CellElapsedFormatter(String pattern) {
      super(pattern);
      StringBuffer desc = CellFormatPart.parseFormat(pattern, CellFormatType.ELAPSED, new CellElapsedFormatter.ElapsedPartHandler());
      ListIterator<CellElapsedFormatter.TimeSpec> it = this.specs.listIterator(this.specs.size());

      while (it.hasPrevious()) {
         CellElapsedFormatter.TimeSpec spec = (CellElapsedFormatter.TimeSpec)it.previous();
         desc.replace(spec.pos, spec.pos + spec.len, "%0" + spec.len + "d");
         if (spec.type != this.topmost.type) {
            spec.modBy = modFor(spec.type, spec.len);
         }
      }

      this.printfFmt = desc.toString();
   }

   private CellElapsedFormatter.TimeSpec assignSpec(char type, int pos, int len) {
      CellElapsedFormatter.TimeSpec spec = new CellElapsedFormatter.TimeSpec(type, pos, len, factorFor(type, len));
      this.specs.add(spec);
      return spec;
   }

   private static double factorFor(char type, int len) {
      switch (type) {
         case '0':
            return 1.1574074E-5F / Math.pow(10.0, len);
         case 'h':
            return 0.041666668F;
         case 'm':
            return 6.9444446E-4F;
         case 's':
            return 1.1574074E-5F;
         default:
            throw new IllegalArgumentException("Uknown elapsed time spec: " + type);
      }
   }

   private static double modFor(char type, int len) {
      switch (type) {
         case '0':
            return Math.pow(10.0, len);
         case 'h':
            return 24.0;
         case 'm':
            return 60.0;
         case 's':
            return 60.0;
         default:
            throw new IllegalArgumentException("Uknown elapsed time spec: " + type);
      }
   }

   public void formatValue(StringBuffer toAppendTo, Object value) {
      double elapsed = ((Number)value).doubleValue();
      if (elapsed < 0.0) {
         toAppendTo.append('-');
         elapsed = -elapsed;
      }

      Object[] parts = new Long[this.specs.size()];

      for (int i = 0; i < this.specs.size(); i++) {
         parts[i] = ((CellElapsedFormatter.TimeSpec)this.specs.get(i)).valueFor(elapsed);
      }

      Formatter formatter = new Formatter(toAppendTo, Locale.ROOT);

      try {
         formatter.format(this.printfFmt, parts);
      } finally {
         formatter.close();
      }
   }

   public void simpleValue(StringBuffer toAppendTo, Object value) {
      this.formatValue(toAppendTo, value);
   }

   private class ElapsedPartHandler implements PartHandler {
      private ElapsedPartHandler() {
      }

      public String handlePart(Matcher m, String part, CellFormatType type, StringBuffer desc) {
         int pos = desc.length();
         char firstCh = part.charAt(0);
         switch (firstCh) {
            case '\n':
               return "%n";
            case '"':
               part = part.substring(1, part.length() - 1);
               break;
            case '*':
               if (part.length() > 1) {
                  part = CellFormatPart.expandChar(part);
               }
               break;
            case '0':
            case 'h':
            case 'm':
            case 's':
               part = part.toLowerCase(Locale.ROOT);
               CellElapsedFormatter.this.assignSpec(part.charAt(0), pos, part.length());
               return part;
            case '[':
               if (part.length() >= 3) {
                  if (CellElapsedFormatter.this.topmost != null) {
                     throw new IllegalArgumentException("Duplicate '[' times in format");
                  }

                  part = part.toLowerCase(Locale.ROOT);
                  int specLen = part.length() - 2;
                  CellElapsedFormatter.this.topmost = CellElapsedFormatter.this.assignSpec(part.charAt(1), pos, specLen);
                  return part.substring(1, 1 + specLen);
               }
               break;
            case '\\':
               part = part.substring(1);
               break;
            case '_':
               return null;
         }

         return CellElapsedFormatter.PERCENTS.matcher(part).replaceAll("%%");
      }
   }

   private static class TimeSpec {
      final char type;
      final int pos;
      final int len;
      final double factor;
      double modBy;

      public TimeSpec(char type, int pos, int len, double factor) {
         this.type = type;
         this.pos = pos;
         this.len = len;
         this.factor = factor;
         this.modBy = 0.0;
      }

      public long valueFor(double elapsed) {
         double val;
         if (this.modBy == 0.0) {
            val = elapsed / this.factor;
         } else {
            val = elapsed / this.factor % this.modBy;
         }

         return this.type == '0' ? Math.round(val) : (long)val;
      }
   }
}
