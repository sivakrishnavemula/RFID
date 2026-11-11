package org.apache.poi.ss.format;

import java.util.Locale;
import java.util.regex.Matcher;
import org.apache.poi.ss.format.CellFormatPart.PartHandler;

public class CellTextFormatter extends CellFormatter {
   private final int[] textPos;
   private final String desc;
   static final CellFormatter SIMPLE_TEXT = new CellTextFormatter("@");

   public CellTextFormatter(String format) {
      super(format);
      final int[] numPlaces = new int[1];
      this.desc = CellFormatPart.parseFormat(format, CellFormatType.TEXT, new PartHandler() {
         public String handlePart(Matcher m, String part, CellFormatType type, StringBuffer desc) {
            if (part.equals("@")) {
               numPlaces[0]++;
               return "\u0000";
            } else {
               return null;
            }
         }
      }).toString();
      this.textPos = new int[numPlaces[0]];
      int pos = this.desc.length() - 1;

      for (int i = 0; i < this.textPos.length; i++) {
         this.textPos[i] = this.desc.lastIndexOf("\u0000", pos);
         pos = this.textPos[i] - 1;
      }
   }

   public void formatValue(StringBuffer toAppendTo, Object obj) {
      int start = toAppendTo.length();
      String text = obj.toString();
      if (obj instanceof Boolean) {
         text = text.toUpperCase(Locale.ROOT);
      }

      toAppendTo.append(this.desc);

      for (int i = 0; i < this.textPos.length; i++) {
         int pos = start + this.textPos[i];
         toAppendTo.replace(pos, pos + 1, text);
      }
   }

   public void simpleValue(StringBuffer toAppendTo, Object value) {
      SIMPLE_TEXT.formatValue(toAppendTo, value);
   }
}
