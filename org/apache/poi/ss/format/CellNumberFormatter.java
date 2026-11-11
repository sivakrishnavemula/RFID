package org.apache.poi.ss.format;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public class CellNumberFormatter extends CellFormatter {
   private static final POILogger LOG = POILogFactory.getLogger(CellNumberFormatter.class);
   private final String desc;
   private final String printfFmt;
   private final double scale;
   private final CellNumberFormatter.Special decimalPoint;
   private final CellNumberFormatter.Special slash;
   private final CellNumberFormatter.Special exponent;
   private final CellNumberFormatter.Special numerator;
   private final CellNumberFormatter.Special afterInteger;
   private final CellNumberFormatter.Special afterFractional;
   private final boolean showGroupingSeparator;
   private final List<CellNumberFormatter.Special> specials = new ArrayList();
   private final List<CellNumberFormatter.Special> integerSpecials = new ArrayList();
   private final List<CellNumberFormatter.Special> fractionalSpecials = new ArrayList();
   private final List<CellNumberFormatter.Special> numeratorSpecials = new ArrayList();
   private final List<CellNumberFormatter.Special> denominatorSpecials = new ArrayList();
   private final List<CellNumberFormatter.Special> exponentSpecials = new ArrayList();
   private final List<CellNumberFormatter.Special> exponentDigitSpecials = new ArrayList();
   private final int maxDenominator;
   private final String numeratorFmt;
   private final String denominatorFmt;
   private final boolean improperFraction;
   private final DecimalFormat decimalFmt;
   private final CellFormatter SIMPLE_NUMBER = new CellNumberFormatter.GeneralNumberFormatter(this.locale);

   public CellNumberFormatter(String format) {
      this(LocaleUtil.getUserLocale(), format);
   }

   public CellNumberFormatter(Locale locale, String format) {
      super(locale, format);
      CellNumberPartHandler ph = new CellNumberPartHandler();
      StringBuffer descBuf = CellFormatPart.parseFormat(format, CellFormatType.NUMBER, ph);
      this.exponent = ph.getExponent();
      this.specials.addAll(ph.getSpecials());
      this.improperFraction = ph.isImproperFraction();
      if ((ph.getDecimalPoint() != null || ph.getExponent() != null) && ph.getSlash() != null) {
         this.slash = null;
         this.numerator = null;
      } else {
         this.slash = ph.getSlash();
         this.numerator = ph.getNumerator();
      }

      int precision = interpretPrecision(ph.getDecimalPoint(), this.specials);
      int fractionPartWidth = 0;
      if (ph.getDecimalPoint() != null) {
         fractionPartWidth = 1 + precision;
         if (precision == 0) {
            this.specials.remove(ph.getDecimalPoint());
            this.decimalPoint = null;
         } else {
            this.decimalPoint = ph.getDecimalPoint();
         }
      } else {
         this.decimalPoint = null;
      }

      if (this.decimalPoint != null) {
         this.afterInteger = this.decimalPoint;
      } else if (this.exponent != null) {
         this.afterInteger = this.exponent;
      } else if (this.numerator != null) {
         this.afterInteger = this.numerator;
      } else {
         this.afterInteger = null;
      }

      if (this.exponent != null) {
         this.afterFractional = this.exponent;
      } else if (this.numerator != null) {
         this.afterFractional = this.numerator;
      } else {
         this.afterFractional = null;
      }

      double[] scaleByRef = new double[]{ph.getScale()};
      this.showGroupingSeparator = interpretIntegerCommas(descBuf, this.specials, this.decimalPoint, this.integerEnd(), this.fractionalEnd(), scaleByRef);
      if (this.exponent == null) {
         this.scale = scaleByRef[0];
      } else {
         this.scale = 1.0;
      }

      if (precision != 0) {
         this.fractionalSpecials.addAll(this.specials.subList(this.specials.indexOf(this.decimalPoint) + 1, this.fractionalEnd()));
      }

      if (this.exponent != null) {
         int exponentPos = this.specials.indexOf(this.exponent);
         this.exponentSpecials.addAll(this.specialsFor(exponentPos, 2));
         this.exponentDigitSpecials.addAll(this.specialsFor(exponentPos + 2));
      }

      if (this.slash != null) {
         if (this.numerator != null) {
            this.numeratorSpecials.addAll(this.specialsFor(this.specials.indexOf(this.numerator)));
         }

         this.denominatorSpecials.addAll(this.specialsFor(this.specials.indexOf(this.slash) + 1));
         if (this.denominatorSpecials.isEmpty()) {
            this.numeratorSpecials.clear();
            this.maxDenominator = 1;
            this.numeratorFmt = null;
            this.denominatorFmt = null;
         } else {
            this.maxDenominator = maxValue(this.denominatorSpecials);
            this.numeratorFmt = singleNumberFormat(this.numeratorSpecials);
            this.denominatorFmt = singleNumberFormat(this.denominatorSpecials);
         }
      } else {
         this.maxDenominator = 1;
         this.numeratorFmt = null;
         this.denominatorFmt = null;
      }

      this.integerSpecials.addAll(this.specials.subList(0, this.integerEnd()));
      if (this.exponent == null) {
         StringBuffer fmtBuf = new StringBuffer("%");
         int integerPartWidth = this.calculateIntegerPartWidth();
         int totalWidth = integerPartWidth + fractionPartWidth;
         fmtBuf.append('0').append(totalWidth).append('.').append(precision);
         fmtBuf.append("f");
         this.printfFmt = fmtBuf.toString();
         this.decimalFmt = null;
      } else {
         StringBuffer fmtBuf = new StringBuffer();
         boolean first = true;
         if (this.integerSpecials.size() == 1) {
            fmtBuf.append("0");
            first = false;
         } else {
            for (CellNumberFormatter.Special s : this.integerSpecials) {
               if (isDigitFmt(s)) {
                  fmtBuf.append((char)(first ? '#' : '0'));
                  first = false;
               }
            }
         }

         if (this.fractionalSpecials.size() > 0) {
            fmtBuf.append('.');

            for (CellNumberFormatter.Special s : this.fractionalSpecials) {
               if (isDigitFmt(s)) {
                  if (!first) {
                     fmtBuf.append('0');
                  }

                  first = false;
               }
            }
         }

         fmtBuf.append('E');
         placeZeros(fmtBuf, this.exponentSpecials.subList(2, this.exponentSpecials.size()));
         this.decimalFmt = new DecimalFormat(fmtBuf.toString(), this.getDecimalFormatSymbols());
         this.printfFmt = null;
      }

      this.desc = descBuf.toString();
   }

   private DecimalFormatSymbols getDecimalFormatSymbols() {
      return DecimalFormatSymbols.getInstance(this.locale);
   }

   private static void placeZeros(StringBuffer sb, List<CellNumberFormatter.Special> specials) {
      for (CellNumberFormatter.Special s : specials) {
         if (isDigitFmt(s)) {
            sb.append('0');
         }
      }
   }

   private static CellNumberStringMod insertMod(CellNumberFormatter.Special special, CharSequence toAdd, int where) {
      return new CellNumberStringMod(special, toAdd, where);
   }

   private static CellNumberStringMod deleteMod(
      CellNumberFormatter.Special start, boolean startInclusive, CellNumberFormatter.Special end, boolean endInclusive
   ) {
      return new CellNumberStringMod(start, startInclusive, end, endInclusive);
   }

   private static CellNumberStringMod replaceMod(
      CellNumberFormatter.Special start, boolean startInclusive, CellNumberFormatter.Special end, boolean endInclusive, char withChar
   ) {
      return new CellNumberStringMod(start, startInclusive, end, endInclusive, withChar);
   }

   private static String singleNumberFormat(List<CellNumberFormatter.Special> numSpecials) {
      return "%0" + numSpecials.size() + "d";
   }

   private static int maxValue(List<CellNumberFormatter.Special> s) {
      return (int)Math.round(Math.pow(10.0, s.size()) - 1.0);
   }

   private List<CellNumberFormatter.Special> specialsFor(int pos, int takeFirst) {
      if (pos >= this.specials.size()) {
         return Collections.emptyList();
      }

      ListIterator<CellNumberFormatter.Special> it = this.specials.listIterator(pos + takeFirst);
      CellNumberFormatter.Special last = (CellNumberFormatter.Special)it.next();
      int end = pos + takeFirst;

      while (it.hasNext()) {
         CellNumberFormatter.Special s = (CellNumberFormatter.Special)it.next();
         if (!isDigitFmt(s) || s.pos - last.pos > 1) {
            break;
         }

         end++;
         last = s;
      }

      return this.specials.subList(pos, end + 1);
   }

   private List<CellNumberFormatter.Special> specialsFor(int pos) {
      return this.specialsFor(pos, 0);
   }

   private static boolean isDigitFmt(CellNumberFormatter.Special s) {
      return s.ch == '0' || s.ch == '?' || s.ch == '#';
   }

   private int calculateIntegerPartWidth() {
      int digitCount = 0;

      for (CellNumberFormatter.Special s : this.specials) {
         if (s == this.afterInteger) {
            break;
         }

         if (isDigitFmt(s)) {
            digitCount++;
         }
      }

      return digitCount;
   }

   private static int interpretPrecision(CellNumberFormatter.Special decimalPoint, List<CellNumberFormatter.Special> specials) {
      int idx = specials.indexOf(decimalPoint);
      int precision = 0;
      if (idx != -1) {
         for (ListIterator<CellNumberFormatter.Special> it = specials.listIterator(idx + 1); it.hasNext(); precision++) {
            CellNumberFormatter.Special s = (CellNumberFormatter.Special)it.next();
            if (!isDigitFmt(s)) {
               break;
            }
         }
      }

      return precision;
   }

   private static boolean interpretIntegerCommas(
      StringBuffer sb, List<CellNumberFormatter.Special> specials, CellNumberFormatter.Special decimalPoint, int integerEnd, int fractionalEnd, double[] scale
   ) {
      ListIterator<CellNumberFormatter.Special> it = specials.listIterator(integerEnd);
      boolean stillScaling = true;
      boolean integerCommas = false;

      while (it.hasPrevious()) {
         CellNumberFormatter.Special s = (CellNumberFormatter.Special)it.previous();
         if (s.ch != ',') {
            stillScaling = false;
         } else if (stillScaling) {
            scale[0] /= 1000.0;
         } else {
            integerCommas = true;
         }
      }

      if (decimalPoint != null) {
         for (ListIterator<CellNumberFormatter.Special> var11 = specials.listIterator(fractionalEnd); var11.hasPrevious(); scale[0] /= 1000.0) {
            CellNumberFormatter.Special s = (CellNumberFormatter.Special)var11.previous();
            if (s.ch != ',') {
               break;
            }
         }
      }

      it = specials.listIterator();
      int removed = 0;

      while (it.hasNext()) {
         CellNumberFormatter.Special s = (CellNumberFormatter.Special)it.next();
         s.pos -= removed;
         if (s.ch == ',') {
            removed++;
            it.remove();
            sb.deleteCharAt(s.pos);
         }
      }

      return integerCommas;
   }

   private int integerEnd() {
      return this.afterInteger == null ? this.specials.size() : this.specials.indexOf(this.afterInteger);
   }

   private int fractionalEnd() {
      return this.afterFractional == null ? this.specials.size() : this.specials.indexOf(this.afterFractional);
   }

   public void formatValue(StringBuffer toAppendTo, Object valueObject) {
      double value = ((Number)valueObject).doubleValue();
      value *= this.scale;
      boolean negative = value < 0.0;
      if (negative) {
         value = -value;
      }

      double fractional = 0.0;
      if (this.slash != null) {
         if (this.improperFraction) {
            fractional = value;
            value = 0.0;
         } else {
            fractional = value % 1.0;
            value = (long)value;
         }
      }

      Set<CellNumberStringMod> mods = new TreeSet();
      StringBuffer output = new StringBuffer(this.localiseFormat(this.desc));
      if (this.exponent != null) {
         this.writeScientific(value, output, mods);
      } else if (this.improperFraction) {
         this.writeFraction(value, null, fractional, output, mods);
      } else {
         StringBuffer result = new StringBuffer();
         Formatter f = new Formatter(result, this.locale);

         try {
            f.format(this.locale, this.printfFmt, new Object[]{value});
         } finally {
            f.close();
         }

         if (this.numerator == null) {
            this.writeFractional(result, output);
            this.writeInteger(result, output, this.integerSpecials, mods, this.showGroupingSeparator);
         } else {
            this.writeFraction(value, result, fractional, output, mods);
         }
      }

      DecimalFormatSymbols dfs = this.getDecimalFormatSymbols();
      String groupingSeparator = Character.toString(dfs.getGroupingSeparator());
      Iterator<CellNumberStringMod> changes = mods.iterator();
      CellNumberStringMod nextChange = changes.hasNext() ? (CellNumberStringMod)changes.next() : null;
      BitSet deletedChars = new BitSet();
      int adjust = 0;

      for (CellNumberFormatter.Special s : this.specials) {
         int adjustedPos = s.pos + adjust;
         if (!deletedChars.get(s.pos) && output.charAt(adjustedPos) == '#') {
            output.deleteCharAt(adjustedPos);
            adjust--;
            deletedChars.set(s.pos);
         }

         while (nextChange != null && s == nextChange.getSpecial()) {
            int lenBefore;
            label195: {
               lenBefore = output.length();
               int modPos = s.pos + adjust;
               int delPos;
               switch (nextChange.getOp()) {
                  case 1:
                     output.insert(modPos, nextChange.getToAdd());
                     break label195;
                  case 2:
                     if (!nextChange.getToAdd().equals(groupingSeparator) || !deletedChars.get(s.pos)) {
                        output.insert(modPos + 1, nextChange.getToAdd());
                     }
                     break label195;
                  case 3:
                     delPos = s.pos;
                     if (!nextChange.isStartInclusive()) {
                        delPos++;
                        modPos++;
                     }
                     break;
                  default:
                     throw new IllegalStateException("Unknown op: " + nextChange.getOp());
               }

               while (deletedChars.get(delPos)) {
                  delPos++;
                  modPos++;
               }

               int delEndPos = nextChange.getEnd().pos;
               if (nextChange.isEndInclusive()) {
                  delEndPos++;
               }

               int modEndPos = delEndPos + adjust;
               if (modPos < modEndPos) {
                  if ("".equals(nextChange.getToAdd())) {
                     output.delete(modPos, modEndPos);
                  } else {
                     char fillCh = nextChange.getToAdd().charAt(0);

                     for (int i = modPos; i < modEndPos; i++) {
                        output.setCharAt(i, fillCh);
                     }
                  }

                  deletedChars.set(delPos, delEndPos);
               }
            }

            adjust += output.length() - lenBefore;
            nextChange = changes.hasNext() ? (CellNumberStringMod)changes.next() : null;
         }
      }

      if (negative) {
         toAppendTo.append('-');
      }

      toAppendTo.append(output);
   }

   private void writeScientific(double value, StringBuffer output, Set<CellNumberStringMod> mods) {
      StringBuffer result = new StringBuffer();
      FieldPosition fractionPos = new FieldPosition(1);
      this.decimalFmt.format(value, result, fractionPos);
      this.writeInteger(result, output, this.integerSpecials, mods, this.showGroupingSeparator);
      this.writeFractional(result, output);
      int ePos = fractionPos.getEndIndex();
      int signPos = ePos + 1;
      char expSignRes = result.charAt(signPos);
      if (expSignRes != '-') {
         expSignRes = '+';
         result.insert(signPos, '+');
      }

      ListIterator<CellNumberFormatter.Special> it = this.exponentSpecials.listIterator(1);
      CellNumberFormatter.Special expSign = (CellNumberFormatter.Special)it.next();
      char expSignFmt = expSign.ch;
      if (expSignRes != '-' && expSignFmt != '+') {
         mods.add(deleteMod(expSign, true, expSign, true));
      } else {
         mods.add(replaceMod(expSign, true, expSign, true, expSignRes));
      }

      StringBuffer exponentNum = new StringBuffer(result.substring(signPos + 1));
      this.writeInteger(exponentNum, output, this.exponentDigitSpecials, mods, false);
   }

   private void writeFraction(double value, StringBuffer result, double fractional, StringBuffer output, Set<CellNumberStringMod> mods) {
      if (!this.improperFraction) {
         if (fractional == 0.0 && !hasChar('0', this.numeratorSpecials)) {
            this.writeInteger(result, output, this.integerSpecials, mods, false);
            CellNumberFormatter.Special start = lastSpecial(this.integerSpecials);
            CellNumberFormatter.Special end = lastSpecial(this.denominatorSpecials);
            if (hasChar('?', this.integerSpecials, this.numeratorSpecials, this.denominatorSpecials)) {
               mods.add(replaceMod(start, false, end, true, ' '));
            } else {
               mods.add(deleteMod(start, false, end, true));
            }

            return;
         }

         boolean numNoZero = !hasChar('0', this.numeratorSpecials);
         boolean intNoZero = !hasChar('0', this.integerSpecials);
         boolean intOnlyHash = this.integerSpecials.isEmpty() || this.integerSpecials.size() == 1 && hasChar('#', this.integerSpecials);
         boolean removeBecauseZero = fractional == 0.0 && (intOnlyHash || numNoZero);
         boolean removeBecauseFraction = fractional != 0.0 && intNoZero;
         if (value == 0.0 && (removeBecauseZero || removeBecauseFraction)) {
            CellNumberFormatter.Special start = lastSpecial(this.integerSpecials);
            boolean hasPlaceHolder = hasChar('?', this.integerSpecials, this.numeratorSpecials);
            CellNumberStringMod sm = hasPlaceHolder ? replaceMod(start, true, this.numerator, false, ' ') : deleteMod(start, true, this.numerator, false);
            mods.add(sm);
         } else {
            this.writeInteger(result, output, this.integerSpecials, mods, false);
         }
      }

      try {
         int n;
         int d;
         if (fractional != 0.0 && (!this.improperFraction || fractional % 1.0 != 0.0)) {
            SimpleFraction frac = SimpleFraction.buildFractionMaxDenominator(fractional, this.maxDenominator);
            n = frac.getNumerator();
            d = frac.getDenominator();
         } else {
            n = (int)Math.round(fractional);
            d = 1;
         }

         if (this.improperFraction) {
            n = (int)(n + Math.round(value * d));
         }

         this.writeSingleInteger(this.numeratorFmt, n, output, this.numeratorSpecials, mods);
         this.writeSingleInteger(this.denominatorFmt, d, output, this.denominatorSpecials, mods);
      } catch (RuntimeException ignored) {
         LOG.log(7, new Object[]{"error while fraction evaluation", ignored});
      }
   }

   private String localiseFormat(String format) {
      DecimalFormatSymbols dfs = this.getDecimalFormatSymbols();
      if (format.contains(",") && dfs.getGroupingSeparator() != ',') {
         if (format.contains(".") && dfs.getDecimalSeparator() != '.') {
            format = replaceLast(format, "\\.", "[DECIMAL_SEPARATOR]");
            format = format.replace(',', dfs.getGroupingSeparator()).replace("[DECIMAL_SEPARATOR]", Character.toString(dfs.getDecimalSeparator()));
         } else {
            format = format.replace(',', dfs.getGroupingSeparator());
         }
      } else if (format.contains(".") && dfs.getDecimalSeparator() != '.') {
         format = format.replace('.', dfs.getDecimalSeparator());
      }

      return format;
   }

   private static String replaceLast(String text, String regex, String replacement) {
      return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
   }

   private static boolean hasChar(char ch, List<CellNumberFormatter.Special>... numSpecials) {
      List[] var2 = numSpecials;
      int var3 = var2.length;

      for (int var4 = 0; var4 < var3; var4++) {
         for (CellNumberFormatter.Special s : var2[var4]) {
            if (s.ch == ch) {
               return true;
            }
         }
      }

      return false;
   }

   private void writeSingleInteger(String fmt, int num, StringBuffer output, List<CellNumberFormatter.Special> numSpecials, Set<CellNumberStringMod> mods) {
      StringBuffer sb = new StringBuffer();
      Formatter formatter = new Formatter(sb, this.locale);

      try {
         formatter.format(this.locale, fmt, new Object[]{num});
      } finally {
         formatter.close();
      }

      this.writeInteger(sb, output, numSpecials, mods, false);
   }

   private void writeInteger(
      StringBuffer result, StringBuffer output, List<CellNumberFormatter.Special> numSpecials, Set<CellNumberStringMod> mods, boolean showGroupingSeparator
   ) {
      DecimalFormatSymbols dfs = this.getDecimalFormatSymbols();
      String decimalSeparator = Character.toString(dfs.getDecimalSeparator());
      String groupingSeparator = Character.toString(dfs.getGroupingSeparator());
      int pos = result.indexOf(decimalSeparator) - 1;
      if (pos < 0) {
         if (this.exponent != null && numSpecials == this.integerSpecials) {
            pos = result.indexOf("E") - 1;
         } else {
            pos = result.length() - 1;
         }
      }

      int strip;
      for (strip = 0; strip < pos; strip++) {
         char resultCh = result.charAt(strip);
         if (resultCh != '0' && resultCh != dfs.getGroupingSeparator()) {
            break;
         }
      }

      ListIterator<CellNumberFormatter.Special> it = numSpecials.listIterator(numSpecials.size());
      boolean followWithGroupingSeparator = false;
      CellNumberFormatter.Special lastOutputIntegerDigit = null;
      int digit = 0;

      while (it.hasPrevious()) {
         char resultCh;
         if (pos >= 0) {
            resultCh = result.charAt(pos);
         } else {
            resultCh = '0';
         }

         CellNumberFormatter.Special s = (CellNumberFormatter.Special)it.previous();
         followWithGroupingSeparator = showGroupingSeparator && digit > 0 && digit % 3 == 0;
         boolean zeroStrip = false;
         if (resultCh != '0' || s.ch == '0' || s.ch == '?' || pos >= strip) {
            zeroStrip = s.ch == '?' && pos < strip;
            output.setCharAt(s.pos, zeroStrip ? ' ' : resultCh);
            lastOutputIntegerDigit = s;
         }

         if (followWithGroupingSeparator) {
            mods.add(insertMod(s, zeroStrip ? " " : groupingSeparator, 2));
            followWithGroupingSeparator = false;
         }

         digit++;
         pos--;
      }

      new StringBuffer();
      if (pos >= 0) {
         StringBuffer var22 = new StringBuffer(result.substring(0, ++pos));
         if (showGroupingSeparator) {
            while (pos > 0) {
               if (digit > 0 && digit % 3 == 0) {
                  var22.insert(pos, groupingSeparator);
               }

               digit++;
               pos--;
            }
         }

         mods.add(insertMod(lastOutputIntegerDigit, var22, 1));
      }
   }

   private void writeFractional(StringBuffer result, StringBuffer output) {
      if (this.fractionalSpecials.size() > 0) {
         String decimalSeparator = Character.toString(this.getDecimalFormatSymbols().getDecimalSeparator());
         int digit = result.indexOf(decimalSeparator) + 1;
         int strip;
         if (this.exponent != null) {
            strip = result.indexOf("e") - 1;
         } else {
            strip = result.length() - 1;
         }

         while (strip > digit && result.charAt(strip) == '0') {
            strip--;
         }

         for (CellNumberFormatter.Special s : this.fractionalSpecials) {
            char resultCh = result.charAt(digit);
            if (resultCh != '0' || s.ch == '0' || digit < strip) {
               output.setCharAt(s.pos, resultCh);
            } else if (s.ch == '?') {
               output.setCharAt(s.pos, ' ');
            }

            digit++;
         }
      }
   }

   public void simpleValue(StringBuffer toAppendTo, Object value) {
      this.SIMPLE_NUMBER.formatValue(toAppendTo, value);
   }

   private static CellNumberFormatter.Special lastSpecial(List<CellNumberFormatter.Special> s) {
      return (CellNumberFormatter.Special)s.get(s.size() - 1);
   }

   private static class GeneralNumberFormatter extends CellFormatter {
      private GeneralNumberFormatter(Locale locale) {
         super(locale, "General");
      }

      public void formatValue(StringBuffer toAppendTo, Object value) {
         if (value != null) {
            CellFormatter cf;
            if (value instanceof Number) {
               Number num = (Number)value;
               cf = num.doubleValue() % 1.0 == 0.0 ? new CellNumberFormatter(this.locale, "#") : new CellNumberFormatter(this.locale, "#.#");
            } else {
               cf = CellTextFormatter.SIMPLE_TEXT;
            }

            cf.formatValue(toAppendTo, value);
         }
      }

      public void simpleValue(StringBuffer toAppendTo, Object value) {
         this.formatValue(toAppendTo, value);
      }
   }

   static class Special {
      final char ch;
      int pos;

      Special(char ch, int pos) {
         this.ch = ch;
         this.pos = pos;
      }

      public String toString() {
         return "'" + this.ch + "' @ " + this.pos;
      }
   }
}
