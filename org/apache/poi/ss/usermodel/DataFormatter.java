package org.apache.poi.ss.usermodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.format.CellFormat;
import org.apache.poi.ss.format.CellFormatResult;
import org.apache.poi.ss.formula.ConditionalFormattingEvaluator;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public class DataFormatter implements Observer {
   private static final String defaultFractionWholePartFormat = "#";
   private static final String defaultFractionFractionPartFormat = "#/##";
   private static final Pattern numPattern = Pattern.compile("[0#]+");
   private static final Pattern daysAsText = Pattern.compile("([d]{3,})", 2);
   private static final Pattern amPmPattern = Pattern.compile("(([AP])[M/P]*)", 2);
   private static final Pattern rangeConditionalPattern = Pattern.compile(".*\\[\\s*(>|>=|<|<=|=)\\s*[0-9]*\\.*[0-9].*");
   private static final Pattern localePatternGroup = Pattern.compile("(\\[\\$[^-\\]]*-[0-9A-Z]+])");
   private static final Pattern colorPattern = Pattern.compile(
      "(\\[BLACK\\])|(\\[BLUE\\])|(\\[CYAN\\])|(\\[GREEN\\])|(\\[MAGENTA\\])|(\\[RED\\])|(\\[WHITE\\])|(\\[YELLOW\\])|(\\[COLOR\\s*\\d\\])|(\\[COLOR\\s*[0-5]\\d\\])",
      2
   );
   private static final Pattern fractionPattern = Pattern.compile("(?:([#\\d]+)\\s+)?(#+)\\s*\\/\\s*([#\\d]+)");
   private static final Pattern fractionStripper = Pattern.compile("(\"[^\"]*\")|([^ \\?#\\d\\/]+)");
   private static final Pattern alternateGrouping = Pattern.compile("([#0]([^.#0])[#0]{3})");
   private static final String invalidDateTimeString;
   private DecimalFormatSymbols decimalSymbols;
   private DateFormatSymbols dateSymbols;
   private DateFormat defaultDateformat;
   private Format generalNumberFormat;
   private Format defaultNumFormat;
   private final Map<String, Format> formats = new HashMap();
   private final boolean emulateCSV;
   private Locale locale;
   private boolean localeIsAdapting;
   private final DataFormatter.LocaleChangeObservable localeChangedObservable = new DataFormatter.LocaleChangeObservable();
   private static POILogger logger;

   public DataFormatter() {
      this(false);
   }

   public DataFormatter(boolean emulateCSV) {
      this(LocaleUtil.getUserLocale(), true, emulateCSV);
   }

   public DataFormatter(Locale locale) {
      this(locale, false);
   }

   public DataFormatter(Locale locale, boolean emulateCSV) {
      this(locale, false, emulateCSV);
   }

   public DataFormatter(Locale locale, boolean localeIsAdapting, boolean emulateCSV) {
      this.localeIsAdapting = true;
      this.localeChangedObservable.addObserver(this);
      this.localeChangedObservable.checkForLocaleChange(locale);
      this.localeIsAdapting = localeIsAdapting;
      this.emulateCSV = emulateCSV;
   }

   private Format getFormat(Cell cell, ConditionalFormattingEvaluator cfEvaluator) {
      if (cell == null) {
         return null;
      }

      ExcelNumberFormat numFmt = ExcelNumberFormat.from(cell, cfEvaluator);
      if (numFmt == null) {
         return null;
      }

      int formatIndex = numFmt.getIdx();
      String formatStr = numFmt.getFormat();
      return formatStr != null && formatStr.trim().length() != 0 ? this.getFormat(cell.getNumericCellValue(), formatIndex, formatStr) : null;
   }

   private Format getFormat(double cellValue, int formatIndex, String formatStrIn) {
      this.localeChangedObservable.checkForLocaleChange();
      String formatStr = formatStrIn;
      if (formatStr.contains(";") && (formatStr.indexOf(59) != formatStr.lastIndexOf(59) || rangeConditionalPattern.matcher(formatStr).matches())) {
         try {
            CellFormat cfmt = CellFormat.getInstance(this.locale, formatStr);
            Object cellValueO = cellValue;
            if (DateUtil.isADateFormat(formatIndex, formatStr) && (Double)cellValueO != 0.0) {
               cellValueO = DateUtil.getJavaDate(cellValue);
            }

            return new DataFormatter.CellFormatResultWrapper(cfmt.apply(cellValueO));
         } catch (Exception e) {
            logger.log(5, new Object[]{"Formatting failed for format " + formatStr + ", falling back", e});
         }
      }

      if (this.emulateCSV && cellValue == 0.0 && formatStr.contains("#") && !formatStr.contains("0")) {
         formatStr = formatStr.replaceAll("#", "");
      }

      Format format = (Format)this.formats.get(formatStr);
      if (format != null) {
         return format;
      } else if (!"General".equalsIgnoreCase(formatStr) && !"@".equals(formatStr)) {
         format = this.createFormat(cellValue, formatIndex, formatStr);
         this.formats.put(formatStr, format);
         return format;
      } else {
         return this.generalNumberFormat;
      }
   }

   public Format createFormat(Cell cell) {
      int formatIndex = cell.getCellStyle().getDataFormat();
      String formatStr = cell.getCellStyle().getDataFormatString();
      return this.createFormat(cell.getNumericCellValue(), formatIndex, formatStr);
   }

   private Format createFormat(double cellValue, int formatIndex, String sFormat) {
      this.localeChangedObservable.checkForLocaleChange();
      String formatStr = sFormat;

      for (Matcher colourM = colorPattern.matcher(formatStr); colourM.find(); colourM = colorPattern.matcher(formatStr)) {
         String colour = colourM.group();
         int at = formatStr.indexOf(colour);
         if (at == -1) {
            break;
         }

         String nFormatStr = formatStr.substring(0, at) + formatStr.substring(at + colour.length());
         if (nFormatStr.equals(formatStr)) {
            break;
         }

         formatStr = nFormatStr;
      }

      for (Matcher m = localePatternGroup.matcher(formatStr); m.find(); m = localePatternGroup.matcher(formatStr)) {
         String match = m.group();
         String symbol = match.substring(match.indexOf(36) + 1, match.indexOf(45));
         if (symbol.indexOf(36) > -1) {
            symbol = symbol.substring(0, symbol.indexOf(36)) + '\\' + symbol.substring(symbol.indexOf(36), symbol.length());
         }

         formatStr = m.replaceAll(symbol);
      }

      if (formatStr == null || formatStr.trim().length() == 0) {
         return this.getDefaultFormat(cellValue);
      }

      if ("General".equalsIgnoreCase(formatStr) || "@".equals(formatStr)) {
         return this.generalNumberFormat;
      }

      if (DateUtil.isADateFormat(formatIndex, formatStr) && DateUtil.isValidExcelDate(cellValue)) {
         return this.createDateFormat(formatStr, cellValue);
      }

      if (formatStr.contains("#/") || formatStr.contains("?/")) {
         String[] chunks = formatStr.split(";");

         for (String chunk1 : chunks) {
            String chunk = chunk1.replaceAll("\\?", "#");
            Matcher matcher = fractionStripper.matcher(chunk);
            chunk = matcher.replaceAll(" ");
            chunk = chunk.replaceAll(" +", " ");
            Matcher fractionMatcher = fractionPattern.matcher(chunk);
            if (fractionMatcher.find()) {
               String wholePart = fractionMatcher.group(1) == null ? "" : "#";
               return new FractionFormat(wholePart, fractionMatcher.group(3));
            }
         }

         return new FractionFormat("#", "#/##");
      } else if (numPattern.matcher(formatStr).find()) {
         return this.createNumberFormat(formatStr, cellValue);
      } else {
         return this.emulateCSV ? new DataFormatter.ConstantStringFormat(this.cleanFormatForNumber(formatStr)) : null;
      }
   }

   private Format createDateFormat(String pFormatStr, double cellValue) {
      String formatStr = pFormatStr;
      formatStr = formatStr.replaceAll("\\\\-", "-");
      formatStr = formatStr.replaceAll("\\\\,", ",");
      formatStr = formatStr.replaceAll("\\\\\\.", ".");
      formatStr = formatStr.replaceAll("\\\\ ", " ");
      formatStr = formatStr.replaceAll("\\\\/", "/");
      formatStr = formatStr.replaceAll(";@", "");
      formatStr = formatStr.replaceAll("\"/\"", "/");
      formatStr = formatStr.replace("\"\"", "'");
      formatStr = formatStr.replaceAll("\\\\T", "'T'");
      boolean hasAmPm = false;

      for (Matcher amPmMatcher = amPmPattern.matcher(formatStr); amPmMatcher.find(); amPmMatcher = amPmPattern.matcher(formatStr)) {
         formatStr = amPmMatcher.replaceAll("@");
         hasAmPm = true;
      }

      formatStr = formatStr.replaceAll("@", "a");
      Matcher dateMatcher = daysAsText.matcher(formatStr);
      if (dateMatcher.find()) {
         String match = dateMatcher.group(0).toUpperCase(Locale.ROOT).replaceAll("D", "E");
         formatStr = dateMatcher.replaceAll(match);
      }

      StringBuilder sb = new StringBuilder();
      char[] chars = formatStr.toCharArray();
      boolean mIsMonth = true;
      List<Integer> ms = new ArrayList();
      boolean isElapsed = false;

      for (int j = 0; j < chars.length; j++) {
         char c = chars[j];
         if (c == '\'') {
            sb.append(c);
            j++;

            while (j < chars.length) {
               c = chars[j];
               sb.append(c);
               if (c == '\'') {
                  break;
               }

               j++;
            }
         } else if (c == '[' && !isElapsed) {
            isElapsed = true;
            mIsMonth = false;
            sb.append(c);
         } else if (c == ']' && isElapsed) {
            isElapsed = false;
            sb.append(c);
         } else if (isElapsed) {
            if (c == 'h' || c == 'H') {
               sb.append('H');
            } else if (c == 'm' || c == 'M') {
               sb.append('m');
            } else if (c != 's' && c != 'S') {
               sb.append(c);
            } else {
               sb.append('s');
            }
         } else if (c == 'h' || c == 'H') {
            mIsMonth = false;
            if (hasAmPm) {
               sb.append('h');
            } else {
               sb.append('H');
            }
         } else if (c != 'm' && c != 'M') {
            if (c == 's' || c == 'S') {
               sb.append('s');

               for (int index : ms) {
                  if (sb.charAt(index) == 'M') {
                     sb.replace(index, index + 1, "m");
                  }
               }

               mIsMonth = true;
               ms.clear();
            } else if (Character.isLetter(c)) {
               mIsMonth = true;
               ms.clear();
               if (c == 'y' || c == 'Y') {
                  sb.append('y');
               } else if (c != 'd' && c != 'D') {
                  sb.append(c);
               } else {
                  sb.append('d');
               }
            } else {
               if (Character.isWhitespace(c)) {
                  ms.clear();
               }

               sb.append(c);
            }
         } else if (mIsMonth) {
            sb.append('M');
            ms.add(sb.length() - 1);
         } else {
            sb.append('m');
         }
      }

      formatStr = sb.toString();

      try {
         return new ExcelStyleDateFormatter(formatStr, this.dateSymbols);
      } catch (IllegalArgumentException iae) {
         logger.log(1, new Object[]{"Formatting failed for format " + formatStr + ", falling back", iae});
         return this.getDefaultFormat(cellValue);
      }
   }

   private String cleanFormatForNumber(String formatStr) {
      StringBuilder sb = new StringBuilder(formatStr);
      if (this.emulateCSV) {
         for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if ((c == '_' || c == '*' || c == '?') && (i <= 0 || sb.charAt(i - 1) != '\\')) {
               if (c == '?') {
                  sb.setCharAt(i, ' ');
               } else if (i < sb.length() - 1) {
                  if (c == '_') {
                     sb.setCharAt(i + 1, ' ');
                  } else {
                     sb.deleteCharAt(i + 1);
                  }

                  sb.deleteCharAt(i);
                  i--;
               }
            }
         }
      } else {
         for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if ((c == '_' || c == '*') && (i <= 0 || sb.charAt(i - 1) != '\\')) {
               if (i < sb.length() - 1) {
                  sb.deleteCharAt(i + 1);
               }

               sb.deleteCharAt(i);
               i--;
            }
         }
      }

      for (int i = 0; i < sb.length(); i++) {
         char c = sb.charAt(i);
         if (c == '\\' || c == '"') {
            sb.deleteCharAt(i);
            i--;
         } else if (c == '+' && i > 0 && sb.charAt(i - 1) == 'E') {
            sb.deleteCharAt(i);
            i--;
         }
      }

      return sb.toString();
   }

   private Format createNumberFormat(String formatStr, double cellValue) {
      String format = this.cleanFormatForNumber(formatStr);
      DecimalFormatSymbols symbols = this.decimalSymbols;
      Matcher agm = alternateGrouping.matcher(format);
      if (agm.find()) {
         char grouping = agm.group(2).charAt(0);
         if (grouping != ',') {
            symbols = DecimalFormatSymbols.getInstance(this.locale);
            symbols.setGroupingSeparator(grouping);
            String oldPart = agm.group(1);
            String newPart = oldPart.replace(grouping, ',');
            format = format.replace(oldPart, newPart);
         }
      }

      try {
         return new DataFormatter.InternalDecimalFormatWithScale(format, symbols);
      } catch (IllegalArgumentException iae) {
         logger.log(1, new Object[]{"Formatting failed for format " + formatStr + ", falling back", iae});
         return this.getDefaultFormat(cellValue);
      }
   }

   public Format getDefaultFormat(Cell cell) {
      return this.getDefaultFormat(cell.getNumericCellValue());
   }

   private Format getDefaultFormat(double cellValue) {
      this.localeChangedObservable.checkForLocaleChange();
      return this.defaultNumFormat != null ? this.defaultNumFormat : this.generalNumberFormat;
   }

   private String performDateFormatting(Date d, Format dateFormat) {
      return (dateFormat != null ? dateFormat : this.defaultDateformat).format(d);
   }

   private String getFormattedDateString(Cell cell, ConditionalFormattingEvaluator cfEvaluator) {
      if (cell == null) {
         return null;
      }

      Format dateFormat = this.getFormat(cell, cfEvaluator);
      if (dateFormat instanceof ExcelStyleDateFormatter) {
         ((ExcelStyleDateFormatter)dateFormat).setDateToBeFormatted(cell.getNumericCellValue());
      }

      Date d = cell.getDateCellValue();
      return this.performDateFormatting(d, dateFormat);
   }

   private String getFormattedNumberString(Cell cell, ConditionalFormattingEvaluator cfEvaluator) {
      if (cell == null) {
         return null;
      }

      Format numberFormat = this.getFormat(cell, cfEvaluator);
      double d = cell.getNumericCellValue();
      if (numberFormat == null) {
         return String.valueOf(d);
      }

      String formatted = numberFormat.format(d);
      return formatted.replaceFirst("E(\\d)", "E+$1");
   }

   public String formatRawCellContents(double value, int formatIndex, String formatString) {
      return this.formatRawCellContents(value, formatIndex, formatString, false);
   }

   public String formatRawCellContents(double value, int formatIndex, String formatString, boolean use1904Windowing) {
      this.localeChangedObservable.checkForLocaleChange();
      if (DateUtil.isADateFormat(formatIndex, formatString)) {
         if (DateUtil.isValidExcelDate(value)) {
            Format dateFormat = this.getFormat(value, formatIndex, formatString);
            if (dateFormat instanceof ExcelStyleDateFormatter) {
               ((ExcelStyleDateFormatter)dateFormat).setDateToBeFormatted(value);
            }

            Date d = DateUtil.getJavaDate(value, use1904Windowing);
            return this.performDateFormatting(d, dateFormat);
         }

         if (this.emulateCSV) {
            return invalidDateTimeString;
         }
      }

      Format numberFormat = this.getFormat(value, formatIndex, formatString);
      if (numberFormat == null) {
         return String.valueOf(value);
      }

      String textValue = NumberToTextConverter.toText(value);
      String result;
      if (textValue.indexOf(69) > -1) {
         result = numberFormat.format(value);
      } else {
         result = numberFormat.format(new BigDecimal(textValue));
      }

      if (result.indexOf(69) > -1 && !result.contains("E-")) {
         result = result.replaceFirst("E", "E+");
      }

      return result;
   }

   public String formatCellValue(Cell cell) {
      return this.formatCellValue(cell, null);
   }

   public String formatCellValue(Cell cell, FormulaEvaluator evaluator) {
      return this.formatCellValue(cell, evaluator, null);
   }

   public String formatCellValue(Cell cell, FormulaEvaluator evaluator, ConditionalFormattingEvaluator cfEvaluator) {
      this.localeChangedObservable.checkForLocaleChange();
      if (cell == null) {
         return "";
      }

      CellType cellType = cell.getCellType();
      if (cellType == CellType.FORMULA) {
         if (evaluator == null) {
            return cell.getCellFormula();
         }

         cellType = evaluator.evaluateFormulaCell(cell);
      }

      switch (cellType) {
         case NUMERIC:
            if (DateUtil.isCellDateFormatted(cell, cfEvaluator)) {
               return this.getFormattedDateString(cell, cfEvaluator);
            }

            return this.getFormattedNumberString(cell, cfEvaluator);
         case STRING:
            return cell.getRichStringCellValue().getString();
         case BOOLEAN:
            return cell.getBooleanCellValue() ? "TRUE" : "FALSE";
         case BLANK:
            return "";
         case ERROR:
            return FormulaError.forInt(cell.getErrorCellValue()).getString();
         default:
            throw new RuntimeException("Unexpected celltype (" + cellType + ")");
      }
   }

   public void setDefaultNumberFormat(Format format) {
      for (Entry<String, Format> entry : this.formats.entrySet()) {
         if (entry.getValue() == this.generalNumberFormat) {
            entry.setValue(format);
         }
      }

      this.defaultNumFormat = format;
   }

   public void addFormat(String excelFormatStr, Format format) {
      this.formats.put(excelFormatStr, format);
   }

   private static DecimalFormat createIntegerOnlyFormat(String fmt) {
      DecimalFormatSymbols dsf = DecimalFormatSymbols.getInstance(Locale.ROOT);
      DecimalFormat result = new DecimalFormat(fmt, dsf);
      result.setParseIntegerOnly(true);
      return result;
   }

   public static void setExcelStyleRoundingMode(DecimalFormat format) {
      setExcelStyleRoundingMode(format, RoundingMode.HALF_UP);
   }

   public static void setExcelStyleRoundingMode(DecimalFormat format, RoundingMode roundingMode) {
      format.setRoundingMode(roundingMode);
   }

   public Observable getLocaleChangedObservable() {
      return this.localeChangedObservable;
   }

   public void update(Observable observable, Object localeObj) {
      if (localeObj instanceof Locale) {
         Locale newLocale = (Locale)localeObj;
         if (this.localeIsAdapting && !newLocale.equals(this.locale)) {
            this.locale = newLocale;
            this.dateSymbols = DateFormatSymbols.getInstance(this.locale);
            this.decimalSymbols = DecimalFormatSymbols.getInstance(this.locale);
            this.generalNumberFormat = new ExcelGeneralNumberFormat(this.locale);
            this.defaultDateformat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", this.dateSymbols);
            this.defaultDateformat.setTimeZone(LocaleUtil.getUserTimeZone());
            this.formats.clear();
            Format zipFormat = DataFormatter.ZipPlusFourFormat.instance;
            this.addFormat("00000\\-0000", zipFormat);
            this.addFormat("00000-0000", zipFormat);
            Format phoneFormat = DataFormatter.PhoneFormat.instance;
            this.addFormat("[<=9999999]###\\-####;\\(###\\)\\ ###\\-####", phoneFormat);
            this.addFormat("[<=9999999]###-####;(###) ###-####", phoneFormat);
            this.addFormat("###\\-####;\\(###\\)\\ ###\\-####", phoneFormat);
            this.addFormat("###-####;(###) ###-####", phoneFormat);
            Format ssnFormat = DataFormatter.SSNFormat.instance;
            this.addFormat("000\\-00\\-0000", ssnFormat);
            this.addFormat("000-00-0000", ssnFormat);
         }
      }
   }

   static {
      StringBuilder buf = new StringBuilder();

      for (int i = 0; i < 255; i++) {
         buf.append('#');
      }

      invalidDateTimeString = buf.toString();
      logger = POILogFactory.getLogger(DataFormatter.class);
   }

   private final class CellFormatResultWrapper extends Format {
      private final CellFormatResult result;

      private CellFormatResultWrapper(CellFormatResult result) {
         this.result = result;
      }

      public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
         return DataFormatter.this.emulateCSV ? toAppendTo.append(this.result.text) : toAppendTo.append(this.result.text.trim());
      }

      public Object parseObject(String source, ParsePosition pos) {
         return null;
      }
   }

   private static final class ConstantStringFormat extends Format {
      private static final DecimalFormat df = DataFormatter.createIntegerOnlyFormat("##########");
      private final String str;

      public ConstantStringFormat(String s) {
         this.str = s;
      }

      public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
         return toAppendTo.append(this.str);
      }

      public Object parseObject(String source, ParsePosition pos) {
         return df.parseObject(source, pos);
      }
   }

   private static class InternalDecimalFormatWithScale extends Format {
      private static final Pattern endsWithCommas = Pattern.compile("(,+)$");
      private BigDecimal divider;
      private static final BigDecimal ONE_THOUSAND = new BigDecimal(1000);
      private final DecimalFormat df;

      private static String trimTrailingCommas(String s) {
         return s.replaceAll(",+$", "");
      }

      public InternalDecimalFormatWithScale(String pattern, DecimalFormatSymbols symbols) {
         this.df = new DecimalFormat(trimTrailingCommas(pattern), symbols);
         DataFormatter.setExcelStyleRoundingMode(this.df);
         Matcher endsWithCommasMatcher = endsWithCommas.matcher(pattern);
         if (endsWithCommasMatcher.find()) {
            String commas = endsWithCommasMatcher.group(1);
            BigDecimal temp = BigDecimal.ONE;

            for (int i = 0; i < commas.length(); i++) {
               temp = temp.multiply(ONE_THOUSAND);
            }

            this.divider = temp;
         } else {
            this.divider = null;
         }
      }

      private Object scaleInput(Object obj) {
         if (this.divider != null) {
            if (obj instanceof BigDecimal) {
               obj = ((BigDecimal)obj).divide(this.divider, RoundingMode.HALF_UP);
            } else {
               if (!(obj instanceof Double)) {
                  throw new UnsupportedOperationException();
               }

               obj = (Double)obj / this.divider.doubleValue();
            }
         }

         return obj;
      }

      public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
         obj = this.scaleInput(obj);
         return this.df.format(obj, toAppendTo, pos);
      }

      public Object parseObject(String source, ParsePosition pos) {
         throw new UnsupportedOperationException();
      }
   }

   private class LocaleChangeObservable extends Observable {
      private LocaleChangeObservable() {
      }

      void checkForLocaleChange() {
         this.checkForLocaleChange(LocaleUtil.getUserLocale());
      }

      void checkForLocaleChange(Locale newLocale) {
         if (DataFormatter.this.localeIsAdapting) {
            if (!newLocale.equals(DataFormatter.this.locale)) {
               super.setChanged();
               this.notifyObservers(newLocale);
            }
         }
      }
   }

   private static final class PhoneFormat extends Format {
      public static final Format instance = new DataFormatter.PhoneFormat();
      private static final DecimalFormat df = DataFormatter.createIntegerOnlyFormat("##########");

      public static String format(Number num) {
         String result = df.format(num);
         StringBuilder sb = new StringBuilder();
         int len = result.length();
         if (len <= 4) {
            return result;
         }

         String seg3 = result.substring(len - 4, len);
         String seg2 = result.substring(Math.max(0, len - 7), len - 4);
         String seg1 = result.substring(Math.max(0, len - 10), Math.max(0, len - 7));
         if (seg1.trim().length() > 0) {
            sb.append('(').append(seg1).append(") ");
         }

         if (seg2.trim().length() > 0) {
            sb.append(seg2).append('-');
         }

         sb.append(seg3);
         return sb.toString();
      }

      public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
         return toAppendTo.append(format((Number)obj));
      }

      public Object parseObject(String source, ParsePosition pos) {
         return df.parseObject(source, pos);
      }
   }

   private static final class SSNFormat extends Format {
      public static final Format instance = new DataFormatter.SSNFormat();
      private static final DecimalFormat df = DataFormatter.createIntegerOnlyFormat("000000000");

      public static String format(Number num) {
         String result = df.format(num);
         return result.substring(0, 3) + '-' + result.substring(3, 5) + '-' + result.substring(5, 9);
      }

      public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
         return toAppendTo.append(format((Number)obj));
      }

      public Object parseObject(String source, ParsePosition pos) {
         return df.parseObject(source, pos);
      }
   }

   private static final class ZipPlusFourFormat extends Format {
      public static final Format instance = new DataFormatter.ZipPlusFourFormat();
      private static final DecimalFormat df = DataFormatter.createIntegerOnlyFormat("000000000");

      public static String format(Number num) {
         String result = df.format(num);
         return result.substring(0, 5) + '-' + result.substring(5, 9);
      }

      public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
         return toAppendTo.append(format((Number)obj));
      }

      public Object parseObject(String source, ParsePosition pos) {
         return df.parseObject(source, pos);
      }
   }
}
