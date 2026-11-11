package org.apache.poi.hssf.record.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.cont.ContinuableRecordInput;
import org.apache.poi.hssf.record.cont.ContinuableRecordOutput;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringUtil;

public class UnicodeString implements Comparable<UnicodeString> {
   private static POILogger _logger = POILogFactory.getLogger(UnicodeString.class);
   private static final int MAX_RECORD_LENGTH = 100000;
   private short field_1_charCount;
   private byte field_2_optionflags;
   private String field_3_string;
   private List<UnicodeString.FormatRun> field_4_format_runs;
   private UnicodeString.ExtRst field_5_ext_rst;
   private static final BitField highByte = BitFieldFactory.getInstance(1);
   private static final BitField extBit = BitFieldFactory.getInstance(4);
   private static final BitField richText = BitFieldFactory.getInstance(8);

   private UnicodeString() {
   }

   public UnicodeString(String str) {
      this.setString(str);
   }

   public int hashCode() {
      int stringHash = 0;
      if (this.field_3_string != null) {
         stringHash = this.field_3_string.hashCode();
      }

      return this.field_1_charCount + stringHash;
   }

   public boolean equals(Object o) {
      if (!(o instanceof UnicodeString)) {
         return false;
      }

      UnicodeString other = (UnicodeString)o;
      if (this.field_1_charCount == other.field_1_charCount
         && this.field_2_optionflags == other.field_2_optionflags
         && this.field_3_string.equals(other.field_3_string)) {
         if (this.field_4_format_runs == null) {
            return other.field_4_format_runs == null;
         }

         if (other.field_4_format_runs == null) {
            return false;
         }

         int size = this.field_4_format_runs.size();
         if (size != other.field_4_format_runs.size()) {
            return false;
         }

         for (int i = 0; i < size; i++) {
            UnicodeString.FormatRun run1 = (UnicodeString.FormatRun)this.field_4_format_runs.get(i);
            UnicodeString.FormatRun run2 = (UnicodeString.FormatRun)other.field_4_format_runs.get(i);
            if (!run1.equals(run2)) {
               return false;
            }
         }

         if (this.field_5_ext_rst == null) {
            return other.field_5_ext_rst == null;
         } else {
            return other.field_5_ext_rst == null ? false : this.field_5_ext_rst.equals(other.field_5_ext_rst);
         }
      } else {
         return false;
      }
   }

   public UnicodeString(RecordInputStream in) {
      this.field_1_charCount = in.readShort();
      this.field_2_optionflags = in.readByte();
      int runCount = 0;
      int extensionLength = 0;
      if (this.isRichText()) {
         runCount = in.readShort();
      }

      if (this.isExtendedText()) {
         extensionLength = in.readInt();
      }

      boolean isCompressed = (this.field_2_optionflags & 1) == 0;
      int cc = this.getCharCount();
      this.field_3_string = isCompressed ? in.readCompressedUnicode(cc) : in.readUnicodeLEString(cc);
      if (this.isRichText() && runCount > 0) {
         this.field_4_format_runs = new ArrayList(runCount);

         for (int i = 0; i < runCount; i++) {
            this.field_4_format_runs.add(new UnicodeString.FormatRun(in));
         }
      }

      if (this.isExtendedText() && extensionLength > 0) {
         this.field_5_ext_rst = new UnicodeString.ExtRst(new ContinuableRecordInput(in), extensionLength);
         if (this.field_5_ext_rst.getDataSize() + 4 != extensionLength) {
            _logger.log(
               5,
               new Object[]{
                  "ExtRst was supposed to be " + extensionLength + " bytes long, but seems to actually be " + (this.field_5_ext_rst.getDataSize() + 4)
               }
            );
         }
      }
   }

   public int getCharCount() {
      return this.field_1_charCount < 0 ? this.field_1_charCount + 65536 : this.field_1_charCount;
   }

   public short getCharCountShort() {
      return this.field_1_charCount;
   }

   public void setCharCount(short cc) {
      this.field_1_charCount = cc;
   }

   public byte getOptionFlags() {
      return this.field_2_optionflags;
   }

   public void setOptionFlags(byte of) {
      this.field_2_optionflags = of;
   }

   public String getString() {
      return this.field_3_string;
   }

   public void setString(String string) {
      this.field_3_string = string;
      this.setCharCount((short)this.field_3_string.length());
      boolean useUTF16 = false;
      int strlen = string.length();

      for (int j = 0; j < strlen; j++) {
         if (string.charAt(j) > 255) {
            useUTF16 = true;
            break;
         }
      }

      if (useUTF16) {
         this.field_2_optionflags = highByte.setByte(this.field_2_optionflags);
      } else {
         this.field_2_optionflags = highByte.clearByte(this.field_2_optionflags);
      }
   }

   public int getFormatRunCount() {
      return this.field_4_format_runs == null ? 0 : this.field_4_format_runs.size();
   }

   public UnicodeString.FormatRun getFormatRun(int index) {
      if (this.field_4_format_runs == null) {
         return null;
      } else {
         return index >= 0 && index < this.field_4_format_runs.size() ? (UnicodeString.FormatRun)this.field_4_format_runs.get(index) : null;
      }
   }

   private int findFormatRunAt(int characterPos) {
      int size = this.field_4_format_runs.size();

      for (int i = 0; i < size; i++) {
         UnicodeString.FormatRun r = (UnicodeString.FormatRun)this.field_4_format_runs.get(i);
         if (r._character == characterPos) {
            return i;
         }

         if (r._character > characterPos) {
            return -1;
         }
      }

      return -1;
   }

   public void addFormatRun(UnicodeString.FormatRun r) {
      if (this.field_4_format_runs == null) {
         this.field_4_format_runs = new ArrayList();
      }

      int index = this.findFormatRunAt(r._character);
      if (index != -1) {
         this.field_4_format_runs.remove(index);
      }

      this.field_4_format_runs.add(r);
      Collections.sort(this.field_4_format_runs);
      this.field_2_optionflags = richText.setByte(this.field_2_optionflags);
   }

   public Iterator<UnicodeString.FormatRun> formatIterator() {
      return this.field_4_format_runs != null ? this.field_4_format_runs.iterator() : null;
   }

   public void removeFormatRun(UnicodeString.FormatRun r) {
      this.field_4_format_runs.remove(r);
      if (this.field_4_format_runs.size() == 0) {
         this.field_4_format_runs = null;
         this.field_2_optionflags = richText.clearByte(this.field_2_optionflags);
      }
   }

   public void clearFormatting() {
      this.field_4_format_runs = null;
      this.field_2_optionflags = richText.clearByte(this.field_2_optionflags);
   }

   public UnicodeString.ExtRst getExtendedRst() {
      return this.field_5_ext_rst;
   }

   void setExtendedRst(UnicodeString.ExtRst ext_rst) {
      if (ext_rst != null) {
         this.field_2_optionflags = extBit.setByte(this.field_2_optionflags);
      } else {
         this.field_2_optionflags = extBit.clearByte(this.field_2_optionflags);
      }

      this.field_5_ext_rst = ext_rst;
   }

   public void swapFontUse(short oldFontIndex, short newFontIndex) {
      for (UnicodeString.FormatRun run : this.field_4_format_runs) {
         if (run._fontIndex == oldFontIndex) {
            run._fontIndex = newFontIndex;
         }
      }
   }

   public String toString() {
      return this.getString();
   }

   public String getDebugInfo() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[UNICODESTRING]\n");
      buffer.append("    .charcount       = ").append(Integer.toHexString(this.getCharCount())).append("\n");
      buffer.append("    .optionflags     = ").append(Integer.toHexString(this.getOptionFlags())).append("\n");
      buffer.append("    .string          = ").append(this.getString()).append("\n");
      if (this.field_4_format_runs != null) {
         for (int i = 0; i < this.field_4_format_runs.size(); i++) {
            UnicodeString.FormatRun r = (UnicodeString.FormatRun)this.field_4_format_runs.get(i);
            buffer.append("      .format_run" + i + "          = ").append(r).append("\n");
         }
      }

      if (this.field_5_ext_rst != null) {
         buffer.append("    .field_5_ext_rst          = ").append("\n");
         buffer.append(this.field_5_ext_rst).append("\n");
      }

      buffer.append("[/UNICODESTRING]\n");
      return buffer.toString();
   }

   public void serialize(ContinuableRecordOutput out) {
      int numberOfRichTextRuns = 0;
      int extendedDataSize = 0;
      if (this.isRichText() && this.field_4_format_runs != null) {
         numberOfRichTextRuns = this.field_4_format_runs.size();
      }

      if (this.isExtendedText() && this.field_5_ext_rst != null) {
         extendedDataSize = 4 + this.field_5_ext_rst.getDataSize();
      }

      out.writeString(this.field_3_string, numberOfRichTextRuns, extendedDataSize);
      if (numberOfRichTextRuns > 0) {
         for (int i = 0; i < numberOfRichTextRuns; i++) {
            if (out.getAvailableSpace() < 4) {
               out.writeContinue();
            }

            UnicodeString.FormatRun r = (UnicodeString.FormatRun)this.field_4_format_runs.get(i);
            r.serialize(out);
         }
      }

      if (extendedDataSize > 0 && this.field_5_ext_rst != null) {
         this.field_5_ext_rst.serialize(out);
      }
   }

   public int compareTo(UnicodeString str) {
      int result = this.getString().compareTo(str.getString());
      if (result != 0) {
         return result;
      }

      if (this.field_4_format_runs == null) {
         return str.field_4_format_runs == null ? 0 : 1;
      }

      if (str.field_4_format_runs == null) {
         return -1;
      }

      int size = this.field_4_format_runs.size();
      if (size != str.field_4_format_runs.size()) {
         return size - str.field_4_format_runs.size();
      }

      for (int i = 0; i < size; i++) {
         UnicodeString.FormatRun run1 = (UnicodeString.FormatRun)this.field_4_format_runs.get(i);
         UnicodeString.FormatRun run2 = (UnicodeString.FormatRun)str.field_4_format_runs.get(i);
         result = run1.compareTo(run2);
         if (result != 0) {
            return result;
         }
      }

      if (this.field_5_ext_rst == null) {
         return str.field_5_ext_rst == null ? 0 : 1;
      } else {
         return str.field_5_ext_rst == null ? -1 : this.field_5_ext_rst.compareTo(str.field_5_ext_rst);
      }
   }

   private boolean isRichText() {
      return richText.isSet(this.getOptionFlags());
   }

   private boolean isExtendedText() {
      return extBit.isSet(this.getOptionFlags());
   }

   public Object clone() {
      UnicodeString str = new UnicodeString();
      str.field_1_charCount = this.field_1_charCount;
      str.field_2_optionflags = this.field_2_optionflags;
      str.field_3_string = this.field_3_string;
      if (this.field_4_format_runs != null) {
         str.field_4_format_runs = new ArrayList();

         for (UnicodeString.FormatRun r : this.field_4_format_runs) {
            str.field_4_format_runs.add(new UnicodeString.FormatRun(r._character, r._fontIndex));
         }
      }

      if (this.field_5_ext_rst != null) {
         str.field_5_ext_rst = this.field_5_ext_rst.clone();
      }

      return str;
   }

   public static class ExtRst implements Comparable<UnicodeString.ExtRst> {
      private short reserved;
      private short formattingFontIndex;
      private short formattingOptions;
      private int numberOfRuns;
      private String phoneticText;
      private UnicodeString.PhRun[] phRuns;
      private byte[] extraData;

      private void populateEmpty() {
         this.reserved = 1;
         this.phoneticText = "";
         this.phRuns = new UnicodeString.PhRun[0];
         this.extraData = new byte[0];
      }

      protected ExtRst() {
         this.populateEmpty();
      }

      protected ExtRst(LittleEndianInput in, int expectedLength) {
         this.reserved = in.readShort();
         if (this.reserved == -1) {
            this.populateEmpty();
         } else if (this.reserved != 1) {
            UnicodeString._logger.log(5, new Object[]{"Warning - ExtRst has wrong magic marker, expecting 1 but found " + this.reserved + " - ignoring"});

            for (int i = 0; i < expectedLength - 2; i++) {
               in.readByte();
            }

            this.populateEmpty();
         } else {
            short stringDataSize = in.readShort();
            this.formattingFontIndex = in.readShort();
            this.formattingOptions = in.readShort();
            this.numberOfRuns = in.readUShort();
            short length1 = in.readShort();
            short length2 = in.readShort();
            if (length1 == 0 && length2 > 0) {
               length2 = 0;
            }

            if (length1 != length2) {
               throw new IllegalStateException("The two length fields of the Phonetic Text don't agree! " + length1 + " vs " + length2);
            }

            this.phoneticText = StringUtil.readUnicodeLE(in, length1);
            int runData = stringDataSize - 4 - 6 - 2 * this.phoneticText.length();
            int numRuns = runData / 6;
            this.phRuns = new UnicodeString.PhRun[numRuns];

            for (int i = 0; i < this.phRuns.length; i++) {
               this.phRuns[i] = new UnicodeString.PhRun(in);
            }

            int extraDataLength = runData - numRuns * 6;
            if (extraDataLength < 0) {
               UnicodeString._logger.log(5, new Object[]{"Warning - ExtRst overran by " + (0 - extraDataLength) + " bytes"});
               extraDataLength = 0;
            }

            this.extraData = IOUtils.safelyAllocate(extraDataLength, 100000);

            for (int i = 0; i < this.extraData.length; i++) {
               this.extraData[i] = in.readByte();
            }
         }
      }

      protected int getDataSize() {
         return 10 + 2 * this.phoneticText.length() + 6 * this.phRuns.length + this.extraData.length;
      }

      protected void serialize(ContinuableRecordOutput out) {
         int dataSize = this.getDataSize();
         out.writeContinueIfRequired(8);
         out.writeShort(this.reserved);
         out.writeShort(dataSize);
         out.writeShort(this.formattingFontIndex);
         out.writeShort(this.formattingOptions);
         out.writeContinueIfRequired(6);
         out.writeShort(this.numberOfRuns);
         out.writeShort(this.phoneticText.length());
         out.writeShort(this.phoneticText.length());
         out.writeContinueIfRequired(this.phoneticText.length() * 2);
         StringUtil.putUnicodeLE(this.phoneticText, out);

         for (int i = 0; i < this.phRuns.length; i++) {
            this.phRuns[i].serialize(out);
         }

         out.write(this.extraData);
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof UnicodeString.ExtRst)) {
            return false;
         }

         UnicodeString.ExtRst other = (UnicodeString.ExtRst)obj;
         return this.compareTo(other) == 0;
      }

      public int compareTo(UnicodeString.ExtRst o) {
         int result = this.reserved - o.reserved;
         if (result != 0) {
            return result;
         }

         result = this.formattingFontIndex - o.formattingFontIndex;
         if (result != 0) {
            return result;
         }

         result = this.formattingOptions - o.formattingOptions;
         if (result != 0) {
            return result;
         }

         result = this.numberOfRuns - o.numberOfRuns;
         if (result != 0) {
            return result;
         }

         result = this.phoneticText.compareTo(o.phoneticText);
         if (result != 0) {
            return result;
         }

         result = this.phRuns.length - o.phRuns.length;
         if (result != 0) {
            return result;
         }

         for (int i = 0; i < this.phRuns.length; i++) {
            result = this.phRuns[i].phoneticTextFirstCharacterOffset - o.phRuns[i].phoneticTextFirstCharacterOffset;
            if (result != 0) {
               return result;
            }

            result = this.phRuns[i].realTextFirstCharacterOffset - o.phRuns[i].realTextFirstCharacterOffset;
            if (result != 0) {
               return result;
            }

            result = this.phRuns[i].realTextLength - o.phRuns[i].realTextLength;
            if (result != 0) {
               return result;
            }
         }

         return Arrays.hashCode(this.extraData) - Arrays.hashCode(o.extraData);
      }

      public int hashCode() {
         int hash = this.reserved;
         hash = 31 * hash + this.formattingFontIndex;
         hash = 31 * hash + this.formattingOptions;
         hash = 31 * hash + this.numberOfRuns;
         hash = 31 * hash + this.phoneticText.hashCode();
         if (this.phRuns != null) {
            for (UnicodeString.PhRun ph : this.phRuns) {
               hash = 31 * hash + ph.phoneticTextFirstCharacterOffset;
               hash = 31 * hash + ph.realTextFirstCharacterOffset;
               hash = 31 * hash + ph.realTextLength;
            }
         }

         return hash;
      }

      protected UnicodeString.ExtRst clone() {
         UnicodeString.ExtRst ext = new UnicodeString.ExtRst();
         ext.reserved = this.reserved;
         ext.formattingFontIndex = this.formattingFontIndex;
         ext.formattingOptions = this.formattingOptions;
         ext.numberOfRuns = this.numberOfRuns;
         ext.phoneticText = this.phoneticText;
         ext.phRuns = new UnicodeString.PhRun[this.phRuns.length];

         for (int i = 0; i < ext.phRuns.length; i++) {
            ext.phRuns[i] = new UnicodeString.PhRun(
               this.phRuns[i].phoneticTextFirstCharacterOffset, this.phRuns[i].realTextFirstCharacterOffset, this.phRuns[i].realTextLength
            );
         }

         return ext;
      }

      public short getFormattingFontIndex() {
         return this.formattingFontIndex;
      }

      public short getFormattingOptions() {
         return this.formattingOptions;
      }

      public int getNumberOfRuns() {
         return this.numberOfRuns;
      }

      public String getPhoneticText() {
         return this.phoneticText;
      }

      public UnicodeString.PhRun[] getPhRuns() {
         return this.phRuns;
      }
   }

   public static class FormatRun implements Comparable<UnicodeString.FormatRun> {
      final short _character;
      short _fontIndex;

      public FormatRun(short character, short fontIndex) {
         this._character = character;
         this._fontIndex = fontIndex;
      }

      public FormatRun(LittleEndianInput in) {
         this(in.readShort(), in.readShort());
      }

      public short getCharacterPos() {
         return this._character;
      }

      public short getFontIndex() {
         return this._fontIndex;
      }

      public boolean equals(Object o) {
         if (!(o instanceof UnicodeString.FormatRun)) {
            return false;
         }

         UnicodeString.FormatRun other = (UnicodeString.FormatRun)o;
         return this._character == other._character && this._fontIndex == other._fontIndex;
      }

      public int compareTo(UnicodeString.FormatRun r) {
         if (this._character == r._character && this._fontIndex == r._fontIndex) {
            return 0;
         } else {
            return this._character == r._character ? this._fontIndex - r._fontIndex : this._character - r._character;
         }
      }

      public int hashCode() {
         assert false : "hashCode not designed";
         return 42;
      }

      public String toString() {
         return "character=" + this._character + ",fontIndex=" + this._fontIndex;
      }

      public void serialize(LittleEndianOutput out) {
         out.writeShort(this._character);
         out.writeShort(this._fontIndex);
      }
   }

   public static class PhRun {
      private int phoneticTextFirstCharacterOffset;
      private int realTextFirstCharacterOffset;
      private int realTextLength;

      public PhRun(int phoneticTextFirstCharacterOffset, int realTextFirstCharacterOffset, int realTextLength) {
         this.phoneticTextFirstCharacterOffset = phoneticTextFirstCharacterOffset;
         this.realTextFirstCharacterOffset = realTextFirstCharacterOffset;
         this.realTextLength = realTextLength;
      }

      private PhRun(LittleEndianInput in) {
         this.phoneticTextFirstCharacterOffset = in.readUShort();
         this.realTextFirstCharacterOffset = in.readUShort();
         this.realTextLength = in.readUShort();
      }

      private void serialize(ContinuableRecordOutput out) {
         out.writeContinueIfRequired(6);
         out.writeShort(this.phoneticTextFirstCharacterOffset);
         out.writeShort(this.realTextFirstCharacterOffset);
         out.writeShort(this.realTextLength);
      }
   }
}
