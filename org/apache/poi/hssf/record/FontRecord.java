package org.apache.poi.hssf.record;

import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.StringUtil;

public final class FontRecord extends StandardRecord {
   public static final short sid = 49;
   public static final short SS_NONE = 0;
   public static final short SS_SUPER = 1;
   public static final short SS_SUB = 2;
   public static final byte U_NONE = 0;
   public static final byte U_SINGLE = 1;
   public static final byte U_DOUBLE = 2;
   public static final byte U_SINGLE_ACCOUNTING = 33;
   public static final byte U_DOUBLE_ACCOUNTING = 34;
   private short field_1_font_height;
   private short field_2_attributes;
   private static final BitField italic = BitFieldFactory.getInstance(2);
   private static final BitField strikeout = BitFieldFactory.getInstance(8);
   private static final BitField macoutline = BitFieldFactory.getInstance(16);
   private static final BitField macshadow = BitFieldFactory.getInstance(32);
   private short field_3_color_palette_index;
   private short field_4_bold_weight;
   private short field_5_super_sub_script;
   private byte field_6_underline;
   private byte field_7_family;
   private byte field_8_charset;
   private byte field_9_zero;
   private String field_11_font_name;

   public FontRecord() {
   }

   public FontRecord(RecordInputStream in) {
      this.field_1_font_height = in.readShort();
      this.field_2_attributes = in.readShort();
      this.field_3_color_palette_index = in.readShort();
      this.field_4_bold_weight = in.readShort();
      this.field_5_super_sub_script = in.readShort();
      this.field_6_underline = in.readByte();
      this.field_7_family = in.readByte();
      this.field_8_charset = in.readByte();
      this.field_9_zero = in.readByte();
      int field_10_font_name_len = in.readUByte();
      int unicodeFlags = in.readUByte();
      if (field_10_font_name_len > 0) {
         if (unicodeFlags == 0) {
            this.field_11_font_name = in.readCompressedUnicode(field_10_font_name_len);
         } else {
            this.field_11_font_name = in.readUnicodeLEString(field_10_font_name_len);
         }
      } else {
         this.field_11_font_name = "";
      }
   }

   public void setFontHeight(short height) {
      this.field_1_font_height = height;
   }

   public void setAttributes(short attributes) {
      this.field_2_attributes = attributes;
   }

   public void setItalic(boolean italics) {
      this.field_2_attributes = italic.setShortBoolean(this.field_2_attributes, italics);
   }

   public void setStrikeout(boolean strike) {
      this.field_2_attributes = strikeout.setShortBoolean(this.field_2_attributes, strike);
   }

   public void setMacoutline(boolean mac) {
      this.field_2_attributes = macoutline.setShortBoolean(this.field_2_attributes, mac);
   }

   public void setMacshadow(boolean mac) {
      this.field_2_attributes = macshadow.setShortBoolean(this.field_2_attributes, mac);
   }

   public void setColorPaletteIndex(short cpi) {
      this.field_3_color_palette_index = cpi;
   }

   public void setBoldWeight(short bw) {
      this.field_4_bold_weight = bw;
   }

   public void setSuperSubScript(short sss) {
      this.field_5_super_sub_script = sss;
   }

   public void setUnderline(byte u) {
      this.field_6_underline = u;
   }

   public void setFamily(byte f) {
      this.field_7_family = f;
   }

   public void setCharset(byte charset) {
      this.field_8_charset = charset;
   }

   public void setFontName(String fn) {
      this.field_11_font_name = fn;
   }

   public short getFontHeight() {
      return this.field_1_font_height;
   }

   public short getAttributes() {
      return this.field_2_attributes;
   }

   public boolean isItalic() {
      return italic.isSet(this.field_2_attributes);
   }

   public boolean isStruckout() {
      return strikeout.isSet(this.field_2_attributes);
   }

   public boolean isMacoutlined() {
      return macoutline.isSet(this.field_2_attributes);
   }

   public boolean isMacshadowed() {
      return macshadow.isSet(this.field_2_attributes);
   }

   public short getColorPaletteIndex() {
      return this.field_3_color_palette_index;
   }

   public short getBoldWeight() {
      return this.field_4_bold_weight;
   }

   public short getSuperSubScript() {
      return this.field_5_super_sub_script;
   }

   public byte getUnderline() {
      return this.field_6_underline;
   }

   public byte getFamily() {
      return this.field_7_family;
   }

   public byte getCharset() {
      return this.field_8_charset;
   }

   public String getFontName() {
      return this.field_11_font_name;
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[FONT]\n");
      sb.append("    .fontheight    = ").append(HexDump.shortToHex(this.getFontHeight())).append("\n");
      sb.append("    .attributes    = ").append(HexDump.shortToHex(this.getAttributes())).append("\n");
      sb.append("       .italic     = ").append(this.isItalic()).append("\n");
      sb.append("       .strikout   = ").append(this.isStruckout()).append("\n");
      sb.append("       .macoutlined= ").append(this.isMacoutlined()).append("\n");
      sb.append("       .macshadowed= ").append(this.isMacshadowed()).append("\n");
      sb.append("    .colorpalette  = ").append(HexDump.shortToHex(this.getColorPaletteIndex())).append("\n");
      sb.append("    .boldweight    = ").append(HexDump.shortToHex(this.getBoldWeight())).append("\n");
      sb.append("    .supersubscript= ").append(HexDump.shortToHex(this.getSuperSubScript())).append("\n");
      sb.append("    .underline     = ").append(HexDump.byteToHex(this.getUnderline())).append("\n");
      sb.append("    .family        = ").append(HexDump.byteToHex(this.getFamily())).append("\n");
      sb.append("    .charset       = ").append(HexDump.byteToHex(this.getCharset())).append("\n");
      sb.append("    .fontname      = ").append(this.getFontName()).append("\n");
      sb.append("[/FONT]\n");
      return sb.toString();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(this.getFontHeight());
      out.writeShort(this.getAttributes());
      out.writeShort(this.getColorPaletteIndex());
      out.writeShort(this.getBoldWeight());
      out.writeShort(this.getSuperSubScript());
      out.writeByte(this.getUnderline());
      out.writeByte(this.getFamily());
      out.writeByte(this.getCharset());
      out.writeByte(this.field_9_zero);
      int fontNameLen = this.field_11_font_name.length();
      out.writeByte(fontNameLen);
      boolean hasMultibyte = StringUtil.hasMultibyte(this.field_11_font_name);
      out.writeByte(hasMultibyte ? 1 : 0);
      if (fontNameLen > 0) {
         if (hasMultibyte) {
            StringUtil.putUnicodeLE(this.field_11_font_name, out);
         } else {
            StringUtil.putCompressedUnicode(this.field_11_font_name, out);
         }
      }
   }

   protected int getDataSize() {
      int size = 16;
      int fontNameLen = this.field_11_font_name.length();
      if (fontNameLen < 1) {
         return size;
      }

      boolean hasMultibyte = StringUtil.hasMultibyte(this.field_11_font_name);
      return size + fontNameLen * (hasMultibyte ? 2 : 1);
   }

   public short getSid() {
      return 49;
   }

   public void cloneStyleFrom(FontRecord source) {
      this.field_1_font_height = source.field_1_font_height;
      this.field_2_attributes = source.field_2_attributes;
      this.field_3_color_palette_index = source.field_3_color_palette_index;
      this.field_4_bold_weight = source.field_4_bold_weight;
      this.field_5_super_sub_script = source.field_5_super_sub_script;
      this.field_6_underline = source.field_6_underline;
      this.field_7_family = source.field_7_family;
      this.field_8_charset = source.field_8_charset;
      this.field_9_zero = source.field_9_zero;
      this.field_11_font_name = source.field_11_font_name;
   }

   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.field_11_font_name == null ? 0 : this.field_11_font_name.hashCode());
      result = 31 * result + this.field_1_font_height;
      result = 31 * result + this.field_2_attributes;
      result = 31 * result + this.field_3_color_palette_index;
      result = 31 * result + this.field_4_bold_weight;
      result = 31 * result + this.field_5_super_sub_script;
      result = 31 * result + this.field_6_underline;
      result = 31 * result + this.field_7_family;
      result = 31 * result + this.field_8_charset;
      return 31 * result + this.field_9_zero;
   }

   public boolean sameProperties(FontRecord other) {
      return this.field_1_font_height == other.field_1_font_height
         && this.field_2_attributes == other.field_2_attributes
         && this.field_3_color_palette_index == other.field_3_color_palette_index
         && this.field_4_bold_weight == other.field_4_bold_weight
         && this.field_5_super_sub_script == other.field_5_super_sub_script
         && this.field_6_underline == other.field_6_underline
         && this.field_7_family == other.field_7_family
         && this.field_8_charset == other.field_8_charset
         && this.field_9_zero == other.field_9_zero
         && stringEquals(this.field_11_font_name, other.field_11_font_name);
   }

   public boolean equals(Object o) {
      return o instanceof FontRecord ? this.sameProperties((FontRecord)o) : false;
   }

   private static boolean stringEquals(String s1, String s2) {
      return s1 == s2 || s1 != null && s1.equals(s2);
   }
}
