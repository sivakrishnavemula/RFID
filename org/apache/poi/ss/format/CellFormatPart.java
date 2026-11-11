package org.apache.poi.ss.format;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.StringCodepointsIterable;

public class CellFormatPart {
   private final Color color;
   private CellFormatCondition condition;
   private final CellFormatter format;
   private final CellFormatType type;
   private static final Map<String, Color> NAMED_COLORS = new TreeMap(String.CASE_INSENSITIVE_ORDER);
   public static final Pattern COLOR_PAT;
   public static final Pattern CONDITION_PAT;
   public static final Pattern SPECIFICATION_PAT;
   public static final Pattern CURRENCY_PAT;
   public static final Pattern FORMAT_PAT;
   public static final int COLOR_GROUP;
   public static final int CONDITION_OPERATOR_GROUP;
   public static final int CONDITION_VALUE_GROUP;
   public static final int SPECIFICATION_GROUP;

   public CellFormatPart(String desc) {
      this(LocaleUtil.getUserLocale(), desc);
   }

   public CellFormatPart(Locale locale, String desc) {
      Matcher m = FORMAT_PAT.matcher(desc);
      if (!m.matches()) {
         throw new IllegalArgumentException("Unrecognized format: " + CellFormatter.quote(desc));
      }

      this.color = getColor(m);
      this.condition = this.getCondition(m);
      this.type = this.getCellFormatType(m);
      this.format = this.getFormatter(locale, m);
   }

   public boolean applies(Object valueObject) {
      if (this.condition != null && valueObject instanceof Number) {
         Number num = (Number)valueObject;
         return this.condition.pass(num.doubleValue());
      } else if (valueObject == null) {
         throw new NullPointerException("valueObject");
      } else {
         return true;
      }
   }

   private static int findGroup(Pattern pat, String str, String marker) {
      Matcher m = pat.matcher(str);
      if (!m.find()) {
         throw new IllegalArgumentException("Pattern \"" + pat.pattern() + "\" doesn't match \"" + str + "\"");
      }

      for (int i = 1; i <= m.groupCount(); i++) {
         String grp = m.group(i);
         if (grp != null && grp.equals(marker)) {
            return i;
         }
      }

      throw new IllegalArgumentException("\"" + marker + "\" not found in \"" + pat.pattern() + "\"");
   }

   private static Color getColor(Matcher m) {
      String cdesc = m.group(COLOR_GROUP);
      if (cdesc != null && cdesc.length() != 0) {
         Color c = (Color)NAMED_COLORS.get(cdesc);
         if (c == null) {
            CellFormatter.logger.warning("Unknown color: " + CellFormatter.quote(cdesc));
         }

         return c;
      } else {
         return null;
      }
   }

   private CellFormatCondition getCondition(Matcher m) {
      String mdesc = m.group(CONDITION_OPERATOR_GROUP);
      return mdesc != null && mdesc.length() != 0 ? CellFormatCondition.getInstance(m.group(CONDITION_OPERATOR_GROUP), m.group(CONDITION_VALUE_GROUP)) : null;
   }

   private CellFormatType getCellFormatType(Matcher matcher) {
      String fdesc = matcher.group(SPECIFICATION_GROUP);
      return this.formatType(fdesc);
   }

   private CellFormatter getFormatter(Locale locale, Matcher matcher) {
      String fdesc = matcher.group(SPECIFICATION_GROUP);
      Matcher currencyM = CURRENCY_PAT.matcher(fdesc);
      if (currencyM.find()) {
         String currencyPart = currencyM.group(1);
         String currencyRepl;
         if (currencyPart.startsWith("[$-")) {
            currencyRepl = "$";
         } else {
            currencyRepl = currencyPart.substring(2, currencyPart.lastIndexOf(45));
         }

         fdesc = fdesc.replace(currencyPart, currencyRepl);
      }

      return this.type.formatter(locale, fdesc);
   }

   private CellFormatType formatType(String fdesc) {
      fdesc = fdesc.trim();
      if (!fdesc.isEmpty() && !fdesc.equalsIgnoreCase("General")) {
         Matcher m = SPECIFICATION_PAT.matcher(fdesc);
         boolean couldBeDate = false;
         boolean seenZero = false;

         while (m.find()) {
            String repl = m.group(0);
            Iterator<String> codePoints = new StringCodepointsIterable(repl).iterator();
            if (codePoints.hasNext()) {
               String c1 = (String)codePoints.next();
               String c2 = null;
               if (codePoints.hasNext()) {
                  c2 = ((String)codePoints.next()).toLowerCase(Locale.ROOT);
               }

               switch (c1) {
                  case "@":
                     return CellFormatType.TEXT;
                  case "d":
                  case "D":
                  case "y":
                  case "Y":
                     return CellFormatType.DATE;
                  case "h":
                  case "H":
                  case "m":
                  case "M":
                  case "s":
                  case "S":
                     couldBeDate = true;
                     break;
                  case "0":
                     seenZero = true;
                     break;
                  case "[":
                     if (!"h".equals(c2) && !"m".equals(c2) && !"s".equals(c2)) {
                        if ("$".equals(c2)) {
                           return CellFormatType.NUMBER;
                        }

                        throw new IllegalArgumentException("Unsupported [] format block '" + repl + "' in '" + fdesc + "' with c2: " + c2);
                     }

                     return CellFormatType.ELAPSED;
                  case "#":
                  case "?":
                     return CellFormatType.NUMBER;
               }
            }
         }

         if (couldBeDate) {
            return CellFormatType.DATE;
         } else {
            return seenZero ? CellFormatType.NUMBER : CellFormatType.TEXT;
         }
      } else {
         return CellFormatType.GENERAL;
      }
   }

   static String quoteSpecial(String repl, CellFormatType type) {
      StringBuilder sb = new StringBuilder();

      for (String ch : new StringCodepointsIterable(repl)) {
         if ("'".equals(ch) && type.isSpecial('\'')) {
            sb.append('\u0000');
         } else {
            boolean special = type.isSpecial(ch.charAt(0));
            if (special) {
               sb.append("'");
            }

            sb.append(ch);
            if (special) {
               sb.append("'");
            }
         }
      }

      return sb.toString();
   }

   public CellFormatResult apply(Object value) {
      boolean applies = this.applies(value);
      String text;
      Color textColor;
      if (applies) {
         text = this.format.format(value);
         textColor = this.color;
      } else {
         text = this.format.simpleFormat(value);
         textColor = null;
      }

      return new CellFormatResult(applies, text, textColor);
   }

   public CellFormatResult apply(JLabel label, Object value) {
      CellFormatResult result = this.apply(value);
      label.setText(result.text);
      if (result.textColor != null) {
         label.setForeground(result.textColor);
      }

      return result;
   }

   CellFormatType getCellFormatType() {
      return this.type;
   }

   boolean hasCondition() {
      return this.condition != null;
   }

   public static StringBuffer parseFormat(String fdesc, CellFormatType type, CellFormatPart.PartHandler partHandler) {
      Matcher m = SPECIFICATION_PAT.matcher(fdesc);
      StringBuffer fmt = new StringBuffer();

      while (m.find()) {
         String part = group(m, 0);
         if (part.length() > 0) {
            String repl = partHandler.handlePart(m, part, type, fmt);
            if (repl == null) {
               switch (part.charAt(0)) {
                  case '"':
                     repl = quoteSpecial(part.substring(1, part.length() - 1), type);
                     break;
                  case '*':
                     repl = expandChar(part);
                     break;
                  case '\\':
                     repl = quoteSpecial(part.substring(1), type);
                     break;
                  case '_':
                     repl = " ";
                     break;
                  default:
                     repl = part;
               }
            }

            m.appendReplacement(fmt, Matcher.quoteReplacement(repl));
         }
      }

      m.appendTail(fmt);
      if (type.isSpecial('\'')) {
         int pos = 0;

         while ((pos = fmt.indexOf("''", pos)) >= 0) {
            fmt.delete(pos, pos + 2);
         }

         pos = 0;

         while ((pos = fmt.indexOf("\u0000", pos)) >= 0) {
            fmt.replace(pos, pos + 1, "''");
         }
      }

      return fmt;
   }

   static String expandChar(String part) {
      List<String> codePoints = new ArrayList();
      new StringCodepointsIterable(part).iterator().forEachRemaining(codePoints::add);
      if (codePoints.size() < 2) {
         throw new IllegalArgumentException("Expected part string to have at least 2 chars");
      }

      String ch = (String)codePoints.get(1);
      return ch + ch + ch;
   }

   public static String group(Matcher m, int g) {
      String str = m.group(g);
      return str == null ? "" : str;
   }

   public String toString() {
      return this.format.format;
   }

   static {
      Map<Integer, HSSFColor> colors = HSSFColor.getIndexHash();

      for (HSSFColor color : colors.values()) {
         Class<? extends HSSFColor> type = color.getClass();
         String name = type.getSimpleName();
         if (name.equals(name.toUpperCase(Locale.ROOT))) {
            short[] rgb = color.getTriplet();
            Color c = new Color(rgb[0], rgb[1], rgb[2]);
            NAMED_COLORS.put(name, c);
            if (name.indexOf(95) > 0) {
               NAMED_COLORS.put(name.replace('_', ' '), c);
            }

            if (name.indexOf("_PERCENT") > 0) {
               NAMED_COLORS.put(name.replace("_PERCENT", "%").replace('_', ' '), c);
            }
         }
      }

      String condition = "([<>=]=?|!=|<>)    # The operator\n  \\s*([0-9]+(?:\\.[0-9]*)?)\\s*  # The constant to test against\n";
      String currency = "(\\[\\$.{0,3}-[0-9a-f]{3}\\])";
      String color = "\\[(black|blue|cyan|green|magenta|red|white|yellow|color [0-9]+)\\]";
      String part = "\\\\.                 # Quoted single character\n|\"([^\\\\\"]|\\\\.)*\"         # Quoted string of characters (handles escaped quotes like \\\") \n|"
         + currency
         + "                   # Currency symbol in a given locale\n|_.                             # Space as wide as a given character\n|\\*.                           # Repeating fill character\n|@                              # Text: cell text\n|([0?\\#](?:[0?\\#,]*))         # Number: digit + other digits and commas\n|e[-+]                          # Number: Scientific: Exponent\n|m{1,5}                         # Date: month or minute spec\n|d{1,4}                         # Date: day/date spec\n|y{2,4}                         # Date: year spec\n|h{1,2}                         # Date: hour spec\n|s{1,2}                         # Date: second spec\n|am?/pm?                        # Date: am/pm spec\n|\\[h{1,2}\\]                   # Elapsed time: hour spec\n|\\[m{1,2}\\]                   # Elapsed time: minute spec\n|\\[s{1,2}\\]                   # Elapsed time: second spec\n|[^;]                           # A character\n";
      String format = "(?:"
         + color
         + ")?                 # Text color\n(?:\\["
         + condition
         + "\\])?               # Condition\n(?:\\[\\$-[0-9a-fA-F]+\\])?                # Optional locale id, ignored currently\n((?:"
         + part
         + ")+)                        # Format spec\n";
      int flags = 6;
      COLOR_PAT = Pattern.compile(color, flags);
      CONDITION_PAT = Pattern.compile(condition, flags);
      SPECIFICATION_PAT = Pattern.compile(part, flags);
      CURRENCY_PAT = Pattern.compile(currency, flags);
      FORMAT_PAT = Pattern.compile(format, flags);
      COLOR_GROUP = findGroup(FORMAT_PAT, "[Blue]@", "Blue");
      CONDITION_OPERATOR_GROUP = findGroup(FORMAT_PAT, "[>=1]@", ">=");
      CONDITION_VALUE_GROUP = findGroup(FORMAT_PAT, "[>=1]@", "1");
      SPECIFICATION_GROUP = findGroup(FORMAT_PAT, "[Blue][>1]\\a ?", "\\a ?");
   }

   interface PartHandler {
      String handlePart(Matcher var1, String var2, CellFormatType var3, StringBuffer var4);
   }
}
