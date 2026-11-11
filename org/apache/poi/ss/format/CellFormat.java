package org.apache.poi.ss.format;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.Removal;

public class CellFormat {
   private final Locale locale;
   private final String format;
   private final CellFormatPart posNumFmt;
   private final CellFormatPart zeroNumFmt;
   private final CellFormatPart negNumFmt;
   private final CellFormatPart textFmt;
   private final int formatPartCount;
   private static final Pattern ONE_PART = Pattern.compile(CellFormatPart.FORMAT_PAT.pattern() + "(;|$)", 6);
   private static final String INVALID_VALUE_FOR_FORMAT = "###############################################################################################################################################################################################################################################################";
   private static String QUOTE = "\"";
   private static final Map<Locale, Map<String, CellFormat>> formatCache = new WeakHashMap();

   private static CellFormat createGeneralFormat(final Locale locale) {
      return new CellFormat(locale, "General") {
         @Override
         public CellFormatResult apply(Object value) {
            String text = new CellGeneralFormatter(locale).format(value);
            return new CellFormatResult(true, text, null);
         }
      };
   }

   public static CellFormat getInstance(String format) {
      return getInstance(LocaleUtil.getUserLocale(), format);
   }

   public static synchronized CellFormat getInstance(Locale locale, String format) {
      Map<String, CellFormat> formatMap = (Map<String, CellFormat>)formatCache.get(locale);
      if (formatMap == null) {
         formatMap = new WeakHashMap();
         formatCache.put(locale, formatMap);
      }

      CellFormat fmt = (CellFormat)formatMap.get(format);
      if (fmt == null) {
         if (!format.equals("General") && !format.equals("@")) {
            fmt = new CellFormat(locale, format);
         } else {
            fmt = createGeneralFormat(locale);
         }

         formatMap.put(format, fmt);
      }

      return fmt;
   }

   private CellFormat(Locale locale, String format) {
      this.locale = locale;
      this.format = format;
      CellFormatPart defaultTextFormat = new CellFormatPart(locale, "@");
      Matcher m = ONE_PART.matcher(format);
      List<CellFormatPart> parts = new ArrayList();

      while (m.find()) {
         try {
            String valueDesc = m.group();
            if (valueDesc.endsWith(";")) {
               valueDesc = valueDesc.substring(0, valueDesc.length() - 1);
            }

            parts.add(new CellFormatPart(locale, valueDesc));
         } catch (RuntimeException e) {
            CellFormatter.logger.log(Level.WARNING, "Invalid format: " + CellFormatter.quote(m.group()), e);
            parts.add(null);
         }
      }

      this.formatPartCount = parts.size();
      switch (this.formatPartCount) {
         case 1:
            this.posNumFmt = (CellFormatPart)parts.get(0);
            this.negNumFmt = null;
            this.zeroNumFmt = null;
            this.textFmt = defaultTextFormat;
            break;
         case 2:
            this.posNumFmt = (CellFormatPart)parts.get(0);
            this.negNumFmt = (CellFormatPart)parts.get(1);
            this.zeroNumFmt = null;
            this.textFmt = defaultTextFormat;
            break;
         case 3:
            this.posNumFmt = (CellFormatPart)parts.get(0);
            this.negNumFmt = (CellFormatPart)parts.get(1);
            this.zeroNumFmt = (CellFormatPart)parts.get(2);
            this.textFmt = defaultTextFormat;
            break;
         case 4:
         default:
            this.posNumFmt = (CellFormatPart)parts.get(0);
            this.negNumFmt = (CellFormatPart)parts.get(1);
            this.zeroNumFmt = (CellFormatPart)parts.get(2);
            this.textFmt = (CellFormatPart)parts.get(3);
      }
   }

   public CellFormatResult apply(Object value) {
      if (!(value instanceof Number)) {
         if (value instanceof Date) {
            Double numericValue = DateUtil.getExcelDate((Date)value);
            if (DateUtil.isValidExcelDate(numericValue)) {
               return this.getApplicableFormatPart(numericValue).apply(value);
            } else {
               throw new IllegalArgumentException("value " + numericValue + " of date " + value + " is not a valid Excel date");
            }
         } else {
            return this.textFmt.apply(value);
         }
      } else {
         Number num = (Number)value;
         double val = num.doubleValue();
         return !(val < 0.0)
               || (this.formatPartCount != 2 || this.posNumFmt.hasCondition() || this.negNumFmt.hasCondition())
                  && (this.formatPartCount != 3 || this.negNumFmt.hasCondition())
                  && (this.formatPartCount != 4 || this.negNumFmt.hasCondition())
            ? this.getApplicableFormatPart(val).apply(val)
            : this.negNumFmt.apply(-val);
      }
   }

   private CellFormatResult apply(Date date, double numericValue) {
      return this.getApplicableFormatPart(numericValue).apply(date);
   }

   public CellFormatResult apply(Cell c) {
      switch (ultimateType(c)) {
         case BLANK:
            return this.apply("");
         case BOOLEAN:
            return this.apply(c.getBooleanCellValue());
         case NUMERIC:
            Double value = c.getNumericCellValue();
            if (this.getApplicableFormatPart(value).getCellFormatType() == CellFormatType.DATE) {
               if (DateUtil.isValidExcelDate(value)) {
                  return this.apply(c.getDateCellValue(), value.doubleValue());
               }

               return this.apply(
                  "###############################################################################################################################################################################################################################################################"
               );
            }

            return this.apply(value);
         case STRING:
            return this.apply(c.getStringCellValue());
         default:
            return this.apply("?");
      }
   }

   public CellFormatResult apply(JLabel label, Object value) {
      CellFormatResult result = this.apply(value);
      label.setText(result.text);
      if (result.textColor != null) {
         label.setForeground(result.textColor);
      }

      return result;
   }

   private CellFormatResult apply(JLabel label, Date date, double numericValue) {
      CellFormatResult result = this.apply(date, numericValue);
      label.setText(result.text);
      if (result.textColor != null) {
         label.setForeground(result.textColor);
      }

      return result;
   }

   public CellFormatResult apply(JLabel label, Cell c) {
      switch (ultimateType(c)) {
         case BLANK:
            return this.apply(label, "");
         case BOOLEAN:
            return this.apply(label, c.getBooleanCellValue());
         case NUMERIC:
            Double value = c.getNumericCellValue();
            if (this.getApplicableFormatPart(value).getCellFormatType() == CellFormatType.DATE) {
               if (DateUtil.isValidExcelDate(value)) {
                  return this.apply(label, c.getDateCellValue(), value);
               }

               return this.apply(
                  label,
                  "###############################################################################################################################################################################################################################################################"
               );
            }

            return this.apply(label, value);
         case STRING:
            return this.apply(label, c.getStringCellValue());
         default:
            return this.apply(label, "?");
      }
   }

   private CellFormatPart getApplicableFormatPart(Object value) {
      if (!(value instanceof Number)) {
         throw new IllegalArgumentException("value must be a Number");
      }

      double val = ((Number)value).doubleValue();
      if (this.formatPartCount == 1) {
         return this.posNumFmt.hasCondition() && (!this.posNumFmt.hasCondition() || !this.posNumFmt.applies(val))
            ? new CellFormatPart(this.locale, "General")
            : this.posNumFmt;
      }

      if (this.formatPartCount == 2) {
         if ((this.posNumFmt.hasCondition() || !(val >= 0.0)) && (!this.posNumFmt.hasCondition() || !this.posNumFmt.applies(val))) {
            return this.negNumFmt.hasCondition() && (!this.negNumFmt.hasCondition() || !this.negNumFmt.applies(val))
               ? new CellFormatPart(
                  QUOTE
                     + "###############################################################################################################################################################################################################################################################"
                     + QUOTE
               )
               : this.negNumFmt;
         } else {
            return this.posNumFmt;
         }
      } else if ((this.posNumFmt.hasCondition() || !(val > 0.0)) && (!this.posNumFmt.hasCondition() || !this.posNumFmt.applies(val))) {
         return (this.negNumFmt.hasCondition() || !(val < 0.0)) && (!this.negNumFmt.hasCondition() || !this.negNumFmt.applies(val))
            ? this.zeroNumFmt
            : this.negNumFmt;
      } else {
         return this.posNumFmt;
      }
   }

   public static CellType ultimateType(Cell cell) {
      CellType type = cell.getCellType();
      return type == CellType.FORMULA ? cell.getCachedFormulaResultType() : type;
   }

   @Deprecated
   @Removal(version = "4.2")
   public static CellType ultimateTypeEnum(Cell cell) {
      return ultimateType(cell);
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj instanceof CellFormat) {
         CellFormat that = (CellFormat)obj;
         return this.format.equals(that.format);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.format.hashCode();
   }
}
