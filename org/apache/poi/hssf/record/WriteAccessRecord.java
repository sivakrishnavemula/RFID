package org.apache.poi.hssf.record;

import java.util.Arrays;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.RecordFormatException;
import org.apache.poi.util.StringUtil;

public final class WriteAccessRecord extends StandardRecord {
   public static final short sid = 92;
   private static final byte PAD_CHAR = 32;
   private static final int DATA_SIZE = 112;
   private String field_1_username;
   private static final byte[] PADDING = new byte[112];

   public WriteAccessRecord() {
      this.setUsername("");
   }

   public WriteAccessRecord(RecordInputStream in) {
      if (in.remaining() > 112) {
         throw new RecordFormatException("Expected data size (112) but got (" + in.remaining() + ")");
      }

      int nChars = in.readUShort();
      int is16BitFlag = in.readUByte();
      if (nChars <= 112 && (is16BitFlag & 254) == 0) {
         String rawText;
         if ((is16BitFlag & 1) == 0) {
            rawText = StringUtil.readCompressedUnicode(in, nChars);
         } else {
            rawText = StringUtil.readUnicodeLE(in, nChars);
         }

         this.field_1_username = rawText.trim();

         for (int padSize = in.remaining(); padSize > 0; padSize--) {
            in.readUByte();
         }
      } else {
         byte[] data = new byte[3 + in.remaining()];
         LittleEndian.putUShort(data, 0, nChars);
         LittleEndian.putByte(data, 2, is16BitFlag);
         in.readFully(data, 3, data.length - 3);
         String rawValue = new String(data, StringUtil.UTF8);
         this.setUsername(rawValue.trim());
      }
   }

   public void setUsername(String username) {
      boolean is16bit = StringUtil.hasMultibyte(username);
      int encodedByteCount = 3 + username.length() * (is16bit ? 2 : 1);
      int paddingSize = 112 - encodedByteCount;
      if (paddingSize < 0) {
         throw new IllegalArgumentException("Name is too long: " + username);
      }

      this.field_1_username = username;
   }

   public String getUsername() {
      return this.field_1_username;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[WRITEACCESS]\n");
      buffer.append("    .name = ").append(this.field_1_username).append("\n");
      buffer.append("[/WRITEACCESS]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      String username = this.getUsername();
      boolean is16bit = StringUtil.hasMultibyte(username);
      out.writeShort(username.length());
      out.writeByte(is16bit ? 1 : 0);
      if (is16bit) {
         StringUtil.putUnicodeLE(username, out);
      } else {
         StringUtil.putCompressedUnicode(username, out);
      }

      int encodedByteCount = 3 + username.length() * (is16bit ? 2 : 1);
      int paddingSize = 112 - encodedByteCount;
      out.write(PADDING, 0, paddingSize);
   }

   protected int getDataSize() {
      return 112;
   }

   public short getSid() {
      return 92;
   }

   static {
      Arrays.fill(PADDING, (byte)32);
   }
}
