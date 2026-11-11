package org.apache.poi.hssf.record;

import org.apache.poi.hssf.record.cont.ContinuableRecord;
import org.apache.poi.hssf.record.cont.ContinuableRecordOutput;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.ss.formula.ptg.OperandPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.RecordFormatException;

public final class TextObjectRecord extends ContinuableRecord {
   public static final short sid = 438;
   private static final int FORMAT_RUN_ENCODED_SIZE = 8;
   private static final BitField HorizontalTextAlignment = BitFieldFactory.getInstance(14);
   private static final BitField VerticalTextAlignment = BitFieldFactory.getInstance(112);
   private static final BitField textLocked = BitFieldFactory.getInstance(512);
   public static final short HORIZONTAL_TEXT_ALIGNMENT_LEFT_ALIGNED = 1;
   public static final short HORIZONTAL_TEXT_ALIGNMENT_CENTERED = 2;
   public static final short HORIZONTAL_TEXT_ALIGNMENT_RIGHT_ALIGNED = 3;
   public static final short HORIZONTAL_TEXT_ALIGNMENT_JUSTIFIED = 4;
   public static final short VERTICAL_TEXT_ALIGNMENT_TOP = 1;
   public static final short VERTICAL_TEXT_ALIGNMENT_CENTER = 2;
   public static final short VERTICAL_TEXT_ALIGNMENT_BOTTOM = 3;
   public static final short VERTICAL_TEXT_ALIGNMENT_JUSTIFY = 4;
   public static final short TEXT_ORIENTATION_NONE = 0;
   public static final short TEXT_ORIENTATION_TOP_TO_BOTTOM = 1;
   public static final short TEXT_ORIENTATION_ROT_RIGHT = 2;
   public static final short TEXT_ORIENTATION_ROT_LEFT = 3;
   private int field_1_options;
   private int field_2_textOrientation;
   private int field_3_reserved4;
   private int field_4_reserved5;
   private int field_5_reserved6;
   private int field_8_reserved7;
   private HSSFRichTextString _text;
   private int _unknownPreFormulaInt;
   private OperandPtg _linkRefPtg;
   private Byte _unknownPostFormulaByte;

   public TextObjectRecord() {
   }

   public TextObjectRecord(RecordInputStream in) {
      this.field_1_options = in.readUShort();
      this.field_2_textOrientation = in.readUShort();
      this.field_3_reserved4 = in.readUShort();
      this.field_4_reserved5 = in.readUShort();
      this.field_5_reserved6 = in.readUShort();
      int field_6_textLength = in.readUShort();
      int field_7_formattingDataLength = in.readUShort();
      this.field_8_reserved7 = in.readInt();
      if (in.remaining() > 0) {
         if (in.remaining() < 11) {
            throw new RecordFormatException("Not enough remaining data for a link formula");
         }

         int formulaSize = in.readUShort();
         this._unknownPreFormulaInt = in.readInt();
         Ptg[] ptgs = Ptg.readTokens(formulaSize, in);
         if (ptgs.length != 1) {
            throw new RecordFormatException("Read " + ptgs.length + " tokens but expected exactly 1");
         }

         this._linkRefPtg = (OperandPtg)ptgs[0];
         if (in.remaining() > 0) {
            this._unknownPostFormulaByte = in.readByte();
         } else {
            this._unknownPostFormulaByte = null;
         }
      } else {
         this._linkRefPtg = null;
      }

      if (in.remaining() > 0) {
         throw new RecordFormatException("Unused " + in.remaining() + " bytes at end of record");
      }

      String text;
      if (field_6_textLength > 0) {
         text = readRawString(in, field_6_textLength);
      } else {
         text = "";
      }

      this._text = new HSSFRichTextString(text);
      if (field_7_formattingDataLength > 0) {
         processFontRuns(in, this._text, field_7_formattingDataLength);
      }
   }

   private static String readRawString(RecordInputStream in, int textLength) {
      byte compressByte = in.readByte();
      boolean isCompressed = (compressByte & 1) == 0;
      return isCompressed ? in.readCompressedUnicode(textLength) : in.readUnicodeLEString(textLength);
   }

   private static void processFontRuns(RecordInputStream in, HSSFRichTextString str, int formattingRunDataLength) {
      if (formattingRunDataLength % 8 != 0) {
         throw new RecordFormatException("Bad format run data length " + formattingRunDataLength + ")");
      }

      int nRuns = formattingRunDataLength / 8;

      for (int i = 0; i < nRuns; i++) {
         short index = in.readShort();
         short iFont = in.readShort();
         in.readInt();
         str.applyFont(index, str.length(), iFont);
      }
   }

   public short getSid() {
      return 438;
   }

   private void serializeTXORecord(ContinuableRecordOutput out) {
      out.writeShort(this.field_1_options);
      out.writeShort(this.field_2_textOrientation);
      out.writeShort(this.field_3_reserved4);
      out.writeShort(this.field_4_reserved5);
      out.writeShort(this.field_5_reserved6);
      out.writeShort(this._text.length());
      out.writeShort(this.getFormattingDataLength());
      out.writeInt(this.field_8_reserved7);
      if (this._linkRefPtg != null) {
         int formulaSize = this._linkRefPtg.getSize();
         out.writeShort(formulaSize);
         out.writeInt(this._unknownPreFormulaInt);
         this._linkRefPtg.write(out);
         if (this._unknownPostFormulaByte != null) {
            out.writeByte(this._unknownPostFormulaByte);
         }
      }
   }

   private void serializeTrailingRecords(ContinuableRecordOutput out) {
      out.writeContinue();
      out.writeStringData(this._text.getString());
      out.writeContinue();
      writeFormatData(out, this._text);
   }

   protected void serialize(ContinuableRecordOutput out) {
      this.serializeTXORecord(out);
      if (this._text.getString().length() > 0) {
         this.serializeTrailingRecords(out);
      }
   }

   private int getFormattingDataLength() {
      return this._text.length() < 1 ? 0 : (this._text.numFormattingRuns() + 1) * 8;
   }

   private static void writeFormatData(ContinuableRecordOutput out, HSSFRichTextString str) {
      int nRuns = str.numFormattingRuns();

      for (int i = 0; i < nRuns; i++) {
         out.writeShort(str.getIndexOfFormattingRun(i));
         int fontIndex = str.getFontOfFormattingRun(i);
         out.writeShort(fontIndex == 0 ? 0 : fontIndex);
         out.writeInt(0);
      }

      out.writeShort(str.length());
      out.writeShort(0);
      out.writeInt(0);
   }

   public void setHorizontalTextAlignment(int value) {
      this.field_1_options = HorizontalTextAlignment.setValue(this.field_1_options, value);
   }

   public int getHorizontalTextAlignment() {
      return HorizontalTextAlignment.getValue(this.field_1_options);
   }

   public void setVerticalTextAlignment(int value) {
      this.field_1_options = VerticalTextAlignment.setValue(this.field_1_options, value);
   }

   public int getVerticalTextAlignment() {
      return VerticalTextAlignment.getValue(this.field_1_options);
   }

   public void setTextLocked(boolean value) {
      this.field_1_options = textLocked.setBoolean(this.field_1_options, value);
   }

   public boolean isTextLocked() {
      return textLocked.isSet(this.field_1_options);
   }

   public int getTextOrientation() {
      return this.field_2_textOrientation;
   }

   public void setTextOrientation(int textOrientation) {
      this.field_2_textOrientation = textOrientation;
   }

   public HSSFRichTextString getStr() {
      return this._text;
   }

   public void setStr(HSSFRichTextString str) {
      this._text = str;
   }

   public Ptg getLinkRefPtg() {
      return this._linkRefPtg;
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[TXO]\n");
      sb.append("    .options        = ").append(HexDump.shortToHex(this.field_1_options)).append("\n");
      sb.append("         .isHorizontal = ").append(this.getHorizontalTextAlignment()).append('\n');
      sb.append("         .isVertical   = ").append(this.getVerticalTextAlignment()).append('\n');
      sb.append("         .textLocked   = ").append(this.isTextLocked()).append('\n');
      sb.append("    .textOrientation= ").append(HexDump.shortToHex(this.getTextOrientation())).append("\n");
      sb.append("    .reserved4      = ").append(HexDump.shortToHex(this.field_3_reserved4)).append("\n");
      sb.append("    .reserved5      = ").append(HexDump.shortToHex(this.field_4_reserved5)).append("\n");
      sb.append("    .reserved6      = ").append(HexDump.shortToHex(this.field_5_reserved6)).append("\n");
      sb.append("    .textLength     = ").append(HexDump.shortToHex(this._text.length())).append("\n");
      sb.append("    .reserved7      = ").append(HexDump.intToHex(this.field_8_reserved7)).append("\n");
      sb.append("    .string = ").append(this._text).append('\n');

      for (int i = 0; i < this._text.numFormattingRuns(); i++) {
         sb.append("    .textrun = ").append(this._text.getFontOfFormattingRun(i)).append('\n');
      }

      sb.append("[/TXO]\n");
      return sb.toString();
   }

   public Object clone() {
      TextObjectRecord rec = new TextObjectRecord();
      rec._text = this._text;
      rec.field_1_options = this.field_1_options;
      rec.field_2_textOrientation = this.field_2_textOrientation;
      rec.field_3_reserved4 = this.field_3_reserved4;
      rec.field_4_reserved5 = this.field_4_reserved5;
      rec.field_5_reserved6 = this.field_5_reserved6;
      rec.field_8_reserved7 = this.field_8_reserved7;
      rec._text = this._text;
      if (this._linkRefPtg != null) {
         rec._unknownPreFormulaInt = this._unknownPreFormulaInt;
         rec._linkRefPtg = this._linkRefPtg.copy();
         rec._unknownPostFormulaByte = this._unknownPostFormulaByte;
      }

      return rec;
   }
}
