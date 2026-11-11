package org.apache.poi.ss.format;

import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.DateFormat.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Matcher;
import org.apache.poi.ss.format.CellFormatPart.PartHandler;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.StringUtil;

public class CellDateFormatter extends CellFormatter {
   private boolean amPmUpper;
   private boolean showM;
   private boolean showAmPm;
   private final DateFormat dateFmt;
   private String sFmt;
   private final Calendar EXCEL_EPOCH_CAL = LocaleUtil.getLocaleCalendar(1904, 0, 1);
   private static CellDateFormatter SIMPLE_DATE;

   public CellDateFormatter(String format) {
      this(LocaleUtil.getUserLocale(), format);
   }

   public CellDateFormatter(Locale locale, String format) {
      super(format);
      CellDateFormatter.DatePartHandler partHandler = new CellDateFormatter.DatePartHandler();
      StringBuffer descBuf = CellFormatPart.parseFormat(format, CellFormatType.DATE, partHandler);
      partHandler.finish(descBuf);
      String ptrn = descBuf.toString().replaceAll("((y)(?!y))(?<!yy)", "yy");
      this.dateFmt = new SimpleDateFormat(ptrn, locale);
      this.dateFmt.setTimeZone(LocaleUtil.getUserTimeZone());
   }

   public void formatValue(StringBuffer toAppendTo, Object value) {
      if (value == null) {
         value = 0.0;
      }

      if (value instanceof Number) {
         Number num = (Number)value;
         long v = num.longValue();
         if (v == 0L) {
            value = this.EXCEL_EPOCH_CAL.getTime();
         } else {
            Calendar c = (Calendar)this.EXCEL_EPOCH_CAL.clone();
            c.add(13, (int)(v / 1000L));
            c.add(14, (int)(v % 1000L));
            value = c.getTime();
         }
      }

      AttributedCharacterIterator it = this.dateFmt.formatToCharacterIterator(value);
      boolean doneAm = false;
      boolean doneMillis = false;

      for (char ch = it.first(); ch != '\uffff'; ch = it.next()) {
         if (it.getAttribute(Field.MILLISECOND) != null) {
            if (!doneMillis) {
               Date dateObj = (Date)value;
               int pos = toAppendTo.length();
               Formatter formatter = new Formatter(toAppendTo, Locale.ROOT);
               Throwable var10 = null;

               try {
                  long msecs = dateObj.getTime() % 1000L;
                  formatter.format(this.locale, this.sFmt, new Object[]{msecs / 1000.0});
               } catch (Throwable var20) {
                  var10 = var20;
                  throw var20;
               } finally {
                  if (formatter != null) {
                     if (var10 != null) {
                        try {
                           formatter.close();
                        } catch (Throwable var19) {
                           var10.addSuppressed(var19);
                        }
                     } else {
                        formatter.close();
                     }
                  }
               }

               toAppendTo.delete(pos, pos + 2);
               doneMillis = true;
            }
         } else if (it.getAttribute(Field.AM_PM) != null) {
            if (!doneAm) {
               if (this.showAmPm) {
                  if (this.amPmUpper) {
                     toAppendTo.append(StringUtil.toUpperCase(ch));
                     if (this.showM) {
                        toAppendTo.append('M');
                     }
                  } else {
                     toAppendTo.append(StringUtil.toLowerCase(ch));
                     if (this.showM) {
                        toAppendTo.append('m');
                     }
                  }
               }

               doneAm = true;
            }
         } else {
            toAppendTo.append(ch);
         }
      }
   }

   public void simpleValue(StringBuffer toAppendTo, Object value) {
      synchronized (CellDateFormatter.class) {
         if (SIMPLE_DATE == null || !SIMPLE_DATE.EXCEL_EPOCH_CAL.equals(this.EXCEL_EPOCH_CAL)) {
            SIMPLE_DATE = new CellDateFormatter("mm/d/y");
         }
      }

      SIMPLE_DATE.formatValue(toAppendTo, value);
   }

   private class DatePartHandler implements PartHandler {
      private int mStart = -1;
      private int mLen;
      private int hStart = -1;
      private int hLen;

      private DatePartHandler() {
      }

      public String handlePart(Matcher m, String part, CellFormatType type, StringBuffer desc) {
         int pos = desc.length();
         char firstCh = part.charAt(0);
         switch (firstCh) {
            case '0':
               this.mStart = -1;
               int sLen = part.length();
               CellDateFormatter.this.sFmt = "%0" + (sLen + 2) + "." + sLen + "f";
               return part.replace('0', 'S');
            case 'A':
            case 'P':
            case 'a':
            case 'p':
               if (part.length() > 1) {
                  this.mStart = -1;
                  CellDateFormatter.this.showAmPm = true;
                  CellDateFormatter.this.showM = StringUtil.toLowerCase(part.charAt(1)).equals("m");
                  CellDateFormatter.this.amPmUpper = CellDateFormatter.this.showM || StringUtil.isUpperCase(part.charAt(0));
                  return "a";
               }
            default:
               return null;
            case 'D':
            case 'd':
               this.mStart = -1;
               if (part.length() <= 2) {
                  return part.toLowerCase(Locale.ROOT);
               }

               return part.toLowerCase(Locale.ROOT).replace('d', 'E');
            case 'H':
            case 'h':
               this.mStart = -1;
               this.hStart = pos;
               this.hLen = part.length();
               return part.toLowerCase(Locale.ROOT);
            case 'M':
            case 'm':
               this.mStart = pos;
               this.mLen = part.length();
               if (this.hStart >= 0) {
                  return part.toLowerCase(Locale.ROOT);
               }

               return part.toUpperCase(Locale.ROOT);
            case 'S':
            case 's':
               if (this.mStart >= 0) {
                  for (int i = 0; i < this.mLen; i++) {
                     desc.setCharAt(this.mStart + i, 'm');
                  }

                  this.mStart = -1;
               }

               return part.toLowerCase(Locale.ROOT);
            case 'Y':
            case 'y':
               this.mStart = -1;
               if (part.length() == 3) {
                  part = "yyyy";
               }

               return part.toLowerCase(Locale.ROOT);
         }
      }

      public void finish(StringBuffer toAppendTo) {
         if (this.hStart >= 0 && !CellDateFormatter.this.showAmPm) {
            for (int i = 0; i < this.hLen; i++) {
               toAppendTo.setCharAt(this.hStart + i, 'H');
            }
         }
      }
   }
}
