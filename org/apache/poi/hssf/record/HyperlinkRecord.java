package org.apache.poi.hssf.record;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.HexRead;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.RecordFormatException;
import org.apache.poi.util.StringUtil;

public final class HyperlinkRecord extends StandardRecord implements Cloneable {
   public static final short sid = 440;
   private static POILogger logger = POILogFactory.getLogger(HyperlinkRecord.class);
   private static final int MAX_RECORD_LENGTH = 100000;
   static final int HLINK_URL = 1;
   static final int HLINK_ABS = 2;
   static final int HLINK_LABEL = 20;
   static final int HLINK_PLACE = 8;
   private static final int HLINK_TARGET_FRAME = 128;
   private static final int HLINK_UNC_PATH = 256;
   static final HyperlinkRecord.GUID STD_MONIKER = HyperlinkRecord.GUID.parse("79EAC9D0-BAF9-11CE-8C82-00AA004BA90B");
   static final HyperlinkRecord.GUID URL_MONIKER = HyperlinkRecord.GUID.parse("79EAC9E0-BAF9-11CE-8C82-00AA004BA90B");
   static final HyperlinkRecord.GUID FILE_MONIKER = HyperlinkRecord.GUID.parse("00000303-0000-0000-C000-000000000046");
   private static final byte[] URL_TAIL = HexRead.readFromString("79 58 81 F4  3B 1D 7F 48   AF 2C 82 5D  C4 85 27 63   00 00 00 00  A5 AB 00 00");
   private static final byte[] FILE_TAIL = HexRead.readFromString("FF FF AD DE  00 00 00 00   00 00 00 00  00 00 00 00   00 00 00 00  00 00 00 00");
   private static final int TAIL_SIZE = FILE_TAIL.length;
   private CellRangeAddress _range;
   private HyperlinkRecord.GUID _guid;
   private int _fileOpts;
   private int _linkOpts;
   private String _label;
   private String _targetFrame;
   private HyperlinkRecord.GUID _moniker;
   private String _shortFilename;
   private String _address;
   private String _textMark;
   private byte[] _uninterpretedTail;

   public HyperlinkRecord() {
   }

   public int getFirstColumn() {
      return this._range.getFirstColumn();
   }

   public void setFirstColumn(int firstCol) {
      this._range.setFirstColumn(firstCol);
   }

   public int getLastColumn() {
      return this._range.getLastColumn();
   }

   public void setLastColumn(int lastCol) {
      this._range.setLastColumn(lastCol);
   }

   public int getFirstRow() {
      return this._range.getFirstRow();
   }

   public void setFirstRow(int firstRow) {
      this._range.setFirstRow(firstRow);
   }

   public int getLastRow() {
      return this._range.getLastRow();
   }

   public void setLastRow(int lastRow) {
      this._range.setLastRow(lastRow);
   }

   HyperlinkRecord.GUID getGuid() {
      return this._guid;
   }

   HyperlinkRecord.GUID getMoniker() {
      return this._moniker;
   }

   private static String cleanString(String s) {
      if (s == null) {
         return null;
      }

      int idx = s.indexOf(0);
      return idx < 0 ? s : s.substring(0, idx);
   }

   private static String appendNullTerm(String s) {
      return s == null ? null : s + '\u0000';
   }

   public String getLabel() {
      return cleanString(this._label);
   }

   public void setLabel(String label) {
      this._label = appendNullTerm(label);
   }

   public String getTargetFrame() {
      return cleanString(this._targetFrame);
   }

   public String getAddress() {
      if ((this._linkOpts & 1) != 0 && FILE_MONIKER.equals(this._moniker)) {
         return cleanString(this._address != null ? this._address : this._shortFilename);
      } else {
         return (this._linkOpts & 8) != 0 ? cleanString(this._textMark) : cleanString(this._address);
      }
   }

   public void setAddress(String address) {
      if ((this._linkOpts & 1) != 0 && FILE_MONIKER.equals(this._moniker)) {
         this._shortFilename = appendNullTerm(address);
      } else if ((this._linkOpts & 8) != 0) {
         this._textMark = appendNullTerm(address);
      } else {
         this._address = appendNullTerm(address);
      }
   }

   public String getShortFilename() {
      return cleanString(this._shortFilename);
   }

   public void setShortFilename(String shortFilename) {
      this._shortFilename = appendNullTerm(shortFilename);
   }

   public String getTextMark() {
      return cleanString(this._textMark);
   }

   public void setTextMark(String textMark) {
      this._textMark = appendNullTerm(textMark);
   }

   int getLinkOptions() {
      return this._linkOpts;
   }

   public int getLabelOptions() {
      return 2;
   }

   public int getFileOptions() {
      return this._fileOpts;
   }

   public HyperlinkRecord(RecordInputStream in) {
      this._range = new CellRangeAddress(in);
      this._guid = new HyperlinkRecord.GUID(in);
      int streamVersion = in.readInt();
      if (streamVersion != 2) {
         throw new RecordFormatException("Stream Version must be 0x2 but found " + streamVersion);
      }

      this._linkOpts = in.readInt();
      if ((this._linkOpts & 20) != 0) {
         int label_len = in.readInt();
         this._label = in.readUnicodeLEString(label_len);
      }

      if ((this._linkOpts & 128) != 0) {
         int len = in.readInt();
         this._targetFrame = in.readUnicodeLEString(len);
      }

      if ((this._linkOpts & 1) != 0 && (this._linkOpts & 256) != 0) {
         this._moniker = null;
         int nChars = in.readInt();
         this._address = in.readUnicodeLEString(nChars);
      }

      if ((this._linkOpts & 1) != 0 && (this._linkOpts & 256) == 0) {
         this._moniker = new HyperlinkRecord.GUID(in);
         if (URL_MONIKER.equals(this._moniker)) {
            int length = in.readInt();
            int remaining = in.remaining();
            if (length == remaining) {
               int nChars = length / 2;
               this._address = in.readUnicodeLEString(nChars);
            } else {
               int nChars = (length - TAIL_SIZE) / 2;
               this._address = in.readUnicodeLEString(nChars);
               this._uninterpretedTail = readTail(URL_TAIL, in);
            }
         } else if (FILE_MONIKER.equals(this._moniker)) {
            this._fileOpts = in.readShort();
            int len = in.readInt();
            this._shortFilename = StringUtil.readCompressedUnicode(in, len);
            this._uninterpretedTail = readTail(FILE_TAIL, in);
            int size = in.readInt();
            if (size > 0) {
               int charDataSize = in.readInt();
               in.readUShort();
               this._address = StringUtil.readUnicodeLE(in, charDataSize / 2);
            } else {
               this._address = null;
            }
         } else if (STD_MONIKER.equals(this._moniker)) {
            this._fileOpts = in.readShort();
            int len = in.readInt();
            byte[] path_bytes = IOUtils.safelyAllocate(len, 100000);
            in.readFully(path_bytes);
            this._address = new String(path_bytes, StringUtil.UTF8);
         }
      }

      if ((this._linkOpts & 8) != 0) {
         int len = in.readInt();
         this._textMark = in.readUnicodeLEString(len);
      }

      if (in.remaining() > 0) {
         logger.log(5, new Object[]{"Hyperlink data remains: " + in.remaining() + " : " + HexDump.toHex(in.readRemainder())});
      }
   }

   public void serialize(LittleEndianOutput out) {
      this._range.serialize(out);
      this._guid.serialize(out);
      out.writeInt(2);
      out.writeInt(this._linkOpts);
      if ((this._linkOpts & 20) != 0) {
         out.writeInt(this._label.length());
         StringUtil.putUnicodeLE(this._label, out);
      }

      if ((this._linkOpts & 128) != 0) {
         out.writeInt(this._targetFrame.length());
         StringUtil.putUnicodeLE(this._targetFrame, out);
      }

      if ((this._linkOpts & 1) != 0 && (this._linkOpts & 256) != 0) {
         out.writeInt(this._address.length());
         StringUtil.putUnicodeLE(this._address, out);
      }

      if ((this._linkOpts & 1) != 0 && (this._linkOpts & 256) == 0) {
         this._moniker.serialize(out);
         if (URL_MONIKER.equals(this._moniker)) {
            if (this._uninterpretedTail == null) {
               out.writeInt(this._address.length() * 2);
               StringUtil.putUnicodeLE(this._address, out);
            } else {
               out.writeInt(this._address.length() * 2 + TAIL_SIZE);
               StringUtil.putUnicodeLE(this._address, out);
               writeTail(this._uninterpretedTail, out);
            }
         } else if (FILE_MONIKER.equals(this._moniker)) {
            out.writeShort(this._fileOpts);
            out.writeInt(this._shortFilename.length());
            StringUtil.putCompressedUnicode(this._shortFilename, out);
            writeTail(this._uninterpretedTail, out);
            if (this._address == null) {
               out.writeInt(0);
            } else {
               int addrLen = this._address.length() * 2;
               out.writeInt(addrLen + 6);
               out.writeInt(addrLen);
               out.writeShort(3);
               StringUtil.putUnicodeLE(this._address, out);
            }
         }
      }

      if ((this._linkOpts & 8) != 0) {
         out.writeInt(this._textMark.length());
         StringUtil.putUnicodeLE(this._textMark, out);
      }
   }

   protected int getDataSize() {
      int size = 0;
      size += 8;
      size += 16;
      size += 4;
      size += 4;
      if ((this._linkOpts & 20) != 0) {
         size += 4;
         size += this._label.length() * 2;
      }

      if ((this._linkOpts & 128) != 0) {
         size += 4;
         size += this._targetFrame.length() * 2;
      }

      if ((this._linkOpts & 1) != 0 && (this._linkOpts & 256) != 0) {
         size += 4;
         size += this._address.length() * 2;
      }

      if ((this._linkOpts & 1) != 0 && (this._linkOpts & 256) == 0) {
         size += 16;
         if (URL_MONIKER.equals(this._moniker)) {
            size += 4;
            size += this._address.length() * 2;
            if (this._uninterpretedTail != null) {
               size += TAIL_SIZE;
            }
         } else if (FILE_MONIKER.equals(this._moniker)) {
            size += 2;
            size += 4;
            size += this._shortFilename.length();
            size += TAIL_SIZE;
            size += 4;
            if (this._address != null) {
               size += 6;
               size += this._address.length() * 2;
            }
         }
      }

      if ((this._linkOpts & 8) != 0) {
         size += 4;
         size += this._textMark.length() * 2;
      }

      return size;
   }

   private static byte[] readTail(byte[] expectedTail, LittleEndianInput in) {
      byte[] result = new byte[TAIL_SIZE];
      in.readFully(result);
      return result;
   }

   private static void writeTail(byte[] tail, LittleEndianOutput out) {
      out.write(tail);
   }

   public short getSid() {
      return 440;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[HYPERLINK RECORD]\n");
      buffer.append("    .range   = ").append(this._range.formatAsString()).append("\n");
      buffer.append("    .guid    = ").append(this._guid.formatAsString()).append("\n");
      buffer.append("    .linkOpts= ").append(HexDump.intToHex(this._linkOpts)).append("\n");
      buffer.append("    .label   = ").append(this.getLabel()).append("\n");
      if ((this._linkOpts & 128) != 0) {
         buffer.append("    .targetFrame= ").append(this.getTargetFrame()).append("\n");
      }

      if ((this._linkOpts & 1) != 0 && this._moniker != null) {
         buffer.append("    .moniker   = ").append(this._moniker.formatAsString()).append("\n");
      }

      if ((this._linkOpts & 8) != 0) {
         buffer.append("    .textMark= ").append(this.getTextMark()).append("\n");
      }

      buffer.append("    .address   = ").append(this.getAddress()).append("\n");
      buffer.append("[/HYPERLINK RECORD]\n");
      return buffer.toString();
   }

   public boolean isUrlLink() {
      return (this._linkOpts & 1) > 0 && (this._linkOpts & 2) > 0;
   }

   public boolean isFileLink() {
      return (this._linkOpts & 1) > 0 && (this._linkOpts & 2) == 0;
   }

   public boolean isDocumentLink() {
      return (this._linkOpts & 8) > 0;
   }

   public void newUrlLink() {
      this._range = new CellRangeAddress(0, 0, 0, 0);
      this._guid = STD_MONIKER;
      this._linkOpts = 23;
      this.setLabel("");
      this._moniker = URL_MONIKER;
      this.setAddress("");
      this._uninterpretedTail = URL_TAIL;
   }

   public void newFileLink() {
      this._range = new CellRangeAddress(0, 0, 0, 0);
      this._guid = STD_MONIKER;
      this._linkOpts = 21;
      this._fileOpts = 0;
      this.setLabel("");
      this._moniker = FILE_MONIKER;
      this.setAddress(null);
      this.setShortFilename("");
      this._uninterpretedTail = FILE_TAIL;
   }

   public void newDocumentLink() {
      this._range = new CellRangeAddress(0, 0, 0, 0);
      this._guid = STD_MONIKER;
      this._linkOpts = 28;
      this.setLabel("");
      this._moniker = FILE_MONIKER;
      this.setAddress("");
      this.setTextMark("");
   }

   public HyperlinkRecord clone() {
      HyperlinkRecord rec = new HyperlinkRecord();
      rec._range = this._range.copy();
      rec._guid = this._guid;
      rec._linkOpts = this._linkOpts;
      rec._fileOpts = this._fileOpts;
      rec._label = this._label;
      rec._address = this._address;
      rec._moniker = this._moniker;
      rec._shortFilename = this._shortFilename;
      rec._targetFrame = this._targetFrame;
      rec._textMark = this._textMark;
      rec._uninterpretedTail = this._uninterpretedTail;
      return rec;
   }

   static final class GUID {
      private static final int TEXT_FORMAT_LENGTH = 36;
      public static final int ENCODED_SIZE = 16;
      private final int _d1;
      private final int _d2;
      private final int _d3;
      private final long _d4;

      public GUID(LittleEndianInput in) {
         this(in.readInt(), in.readUShort(), in.readUShort(), in.readLong());
      }

      public GUID(int d1, int d2, int d3, long d4) {
         this._d1 = d1;
         this._d2 = d2;
         this._d3 = d3;
         this._d4 = d4;
      }

      public void serialize(LittleEndianOutput out) {
         out.writeInt(this._d1);
         out.writeShort(this._d2);
         out.writeShort(this._d3);
         out.writeLong(this._d4);
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof HyperlinkRecord.GUID)) {
            return false;
         }

         HyperlinkRecord.GUID other = (HyperlinkRecord.GUID)obj;
         return this._d1 == other._d1 && this._d2 == other._d2 && this._d3 == other._d3 && this._d4 == other._d4;
      }

      public int hashCode() {
         assert false : "hashCode not designed";
         return 42;
      }

      public int getD1() {
         return this._d1;
      }

      public int getD2() {
         return this._d2;
      }

      public int getD3() {
         return this._d3;
      }

      public long getD4() {
         byte[] result = new byte[8];
         long l = this._d4;

         for (int i = result.length - 1; i >= 0; i--) {
            result[i] = (byte)(l & 255L);
            l >>= 8;
         }

         return LittleEndian.getLong(result, 0);
      }

      public String formatAsString() {
         StringBuilder sb = new StringBuilder(36);
         int PREFIX_LEN = "0x".length();
         sb.append(HexDump.intToHex(this._d1).substring(PREFIX_LEN));
         sb.append("-");
         sb.append(HexDump.shortToHex(this._d2).substring(PREFIX_LEN));
         sb.append("-");
         sb.append(HexDump.shortToHex(this._d3).substring(PREFIX_LEN));
         sb.append("-");
         String d4Chars = HexDump.longToHex(this.getD4());
         sb.append(d4Chars.substring(PREFIX_LEN, PREFIX_LEN + 4));
         sb.append("-");
         sb.append(d4Chars.substring(PREFIX_LEN + 4));
         return sb.toString();
      }

      public String toString() {
         StringBuilder sb = new StringBuilder(64);
         sb.append(this.getClass().getName()).append(" [");
         sb.append(this.formatAsString());
         sb.append("]");
         return sb.toString();
      }

      public static HyperlinkRecord.GUID parse(String rep) {
         char[] cc = rep.toCharArray();
         if (cc.length != 36) {
            throw new RecordFormatException("supplied text is the wrong length for a GUID");
         }

         int d0 = (parseShort(cc, 0) << 16) + (parseShort(cc, 4) << 0);
         int d1 = parseShort(cc, 9);
         int d2 = parseShort(cc, 14);

         for (int i = 23; i > 19; i--) {
            cc[i] = cc[i - 1];
         }

         long d3 = parseLELong(cc, 20);
         return new HyperlinkRecord.GUID(d0, d1, d2, d3);
      }

      private static long parseLELong(char[] cc, int startIndex) {
         long acc = 0L;

         for (int i = startIndex + 14; i >= startIndex; i -= 2) {
            acc <<= 4;
            acc += parseHexChar(cc[i + 0]);
            acc <<= 4;
            acc += parseHexChar(cc[i + 1]);
         }

         return acc;
      }

      private static int parseShort(char[] cc, int startIndex) {
         int acc = 0;

         for (int i = 0; i < 4; i++) {
            acc <<= 4;
            acc += parseHexChar(cc[startIndex + i]);
         }

         return acc;
      }

      private static int parseHexChar(char c) {
         if (c >= '0' && c <= '9') {
            return c - 48;
         } else if (c >= 'A' && c <= 'F') {
            return c - 65 + 10;
         } else if (c >= 'a' && c <= 'f') {
            return c - 97 + 10;
         } else {
            throw new RecordFormatException("Bad hex char '" + c + "'");
         }
      }
   }
}
