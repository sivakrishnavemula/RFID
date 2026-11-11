package org.apache.poi.ss.usermodel;

import org.apache.poi.util.Removal;

@Removal(version = "4.0")
@Deprecated
public enum FontCharset {
   ANSI(0),
   DEFAULT(1),
   SYMBOL(2),
   MAC(77),
   SHIFTJIS(128),
   HANGEUL(129),
   JOHAB(130),
   GB2312(134),
   CHINESEBIG5(136),
   GREEK(161),
   TURKISH(162),
   VIETNAMESE(163),
   HEBREW(177),
   ARABIC(178),
   BALTIC(186),
   RUSSIAN(204),
   THAI(222),
   EASTEUROPE(238),
   OEM(255);

   private int charset;
   private static FontCharset[] _table = new FontCharset[256];

   FontCharset(int value) {
      this.charset = value;
   }

   public int getValue() {
      return this.charset;
   }

   public static FontCharset valueOf(int value) {
      return value >= _table.length ? null : _table[value];
   }

   static {
      for (FontCharset c : values()) {
         _table[c.getValue()] = c;
      }
   }
}
