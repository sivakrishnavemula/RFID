package org.apache.poi.hssf.record;

import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringUtil;

public final class FormatRecord extends StandardRecord implements Cloneable {
   private static final POILogger logger = POILogFactory.getLogger(FormatRecord.class);
   public static final short sid = 1054;
   private final int field_1_index_code;
   private final boolean field_3_hasMultibyte;
   private final String field_4_formatstring;

   private FormatRecord(FormatRecord other) {
      this.field_1_index_code = other.field_1_index_code;
      this.field_3_hasMultibyte = other.field_3_hasMultibyte;
      this.field_4_formatstring = other.field_4_formatstring;
   }

   public FormatRecord(int indexCode, String fs) {
      this.field_1_index_code = indexCode;
      this.field_4_formatstring = fs;
      this.field_3_hasMultibyte = StringUtil.hasMultibyte(fs);
   }

   public FormatRecord(RecordInputStream in) {
      this.field_1_index_code = in.readShort();
      int field_3_unicode_len = in.readUShort();
      this.field_3_hasMultibyte = (in.readByte() & 1) != 0;
      if (this.field_3_hasMultibyte) {
         this.field_4_formatstring = readStringCommon(in, field_3_unicode_len, false);
      } else {
         this.field_4_formatstring = readStringCommon(in, field_3_unicode_len, true);
      }
   }

   public int getIndexCode() {
      return this.field_1_index_code;
   }

   public String getFormatString() {
      return this.field_4_formatstring;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[FORMAT]\n");
      buffer.append("    .indexcode       = ").append(HexDump.shortToHex(this.getIndexCode())).append("\n");
      buffer.append("    .isUnicode       = ").append(this.field_3_hasMultibyte).append("\n");
      buffer.append("    .formatstring    = ").append(this.getFormatString()).append("\n");
      buffer.append("[/FORMAT]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      String formatString = this.getFormatString();
      out.writeShort(this.getIndexCode());
      out.writeShort(formatString.length());
      out.writeByte(this.field_3_hasMultibyte ? 1 : 0);
      if (this.field_3_hasMultibyte) {
         StringUtil.putUnicodeLE(formatString, out);
      } else {
         StringUtil.putCompressedUnicode(formatString, out);
      }
   }

   protected int getDataSize() {
      return 5 + this.getFormatString().length() * (this.field_3_hasMultibyte ? 2 : 1);
   }

   public short getSid() {
      return 1054;
   }

   public FormatRecord clone() {
      return new FormatRecord(this);
   }

   private static String readStringCommon(RecordInputStream ris, int requestedLength, boolean pIsCompressedEncoding) {
      if (requestedLength >= 0 && requestedLength <= 1048576) {
         char[] buf = null;
         int availableChars = pIsCompressedEncoding ? ris.remaining() : ris.remaining() / 2;
         int remaining = ris.remaining();
         if (requestedLength == availableChars) {
            buf = new char[requestedLength];
         } else {
            buf = new char[availableChars];
         }

         for (int i = 0; i < buf.length; i++) {
            char ch;
            if (pIsCompressedEncoding) {
               ch = (char)ris.readUByte();
            } else {
               ch = (char)ris.readShort();
            }

            buf[i] = ch;
         }

         if (ris.available() == 1) {
            char[] tmp = new char[buf.length + 1];
            System.arraycopy(buf, 0, tmp, 0, buf.length);
            tmp[buf.length] = (char)ris.readUByte();
            buf = tmp;
         }

         if (ris.available() > 0) {
            logger.log(3, new Object[]{"FormatRecord has " + ris.available() + " unexplained bytes. Silently skipping"});

            while (ris.available() > 0) {
               ris.readByte();
            }
         }

         return new String(buf);
      } else {
         throw new IllegalArgumentException("Bad requested string length (" + requestedLength + ")");
      }
   }
}
