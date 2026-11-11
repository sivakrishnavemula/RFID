package org.apache.poi.hssf.record;

import java.util.Arrays;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.RecordFormatException;
import org.apache.poi.util.StringUtil;

public class DConRefRecord extends StandardRecord {
   private static final int MAX_RECORD_LENGTH = 100000;
   public static final short sid = 81;
   private int firstRow;
   private int lastRow;
   private int firstCol;
   private int lastCol;
   private int charCount;
   private int charType;
   private byte[] path;
   private byte[] _unused;

   public DConRefRecord(byte[] data) {
      int offset = 0;
      if (LittleEndian.getShort(data, offset) != 81) {
         throw new RecordFormatException("incompatible sid.");
      }

      offset += 2;
      offset += 2;
      this.firstRow = LittleEndian.getUShort(data, offset);
      offset += 2;
      this.lastRow = LittleEndian.getUShort(data, offset);
      offset += 2;
      this.firstCol = LittleEndian.getUByte(data, offset);
      this.lastCol = LittleEndian.getUByte(data, ++offset);
      this.charCount = LittleEndian.getUShort(data, ++offset);
      offset += 2;
      if (this.charCount < 2) {
         throw new RecordFormatException("Character count must be >= 2");
      }

      this.charType = LittleEndian.getUByte(data, offset);
      offset++;
      int byteLength = this.charCount * ((this.charType & 1) + 1);
      this.path = LittleEndian.getByteArray(data, offset, byteLength, 100000);
      offset += byteLength;
      if (this.path[0] == 2) {
         this._unused = LittleEndian.getByteArray(data, offset, this.charType + 1, 100000);
      }
   }

   public DConRefRecord(RecordInputStream inStream) {
      if (inStream.getSid() != 81) {
         throw new RecordFormatException("Wrong sid: " + inStream.getSid());
      }

      this.firstRow = inStream.readUShort();
      this.lastRow = inStream.readUShort();
      this.firstCol = inStream.readUByte();
      this.lastCol = inStream.readUByte();
      this.charCount = inStream.readUShort();
      this.charType = inStream.readUByte() & 1;
      int byteLength = this.charCount * (this.charType + 1);
      this.path = IOUtils.safelyAllocate(byteLength, 100000);
      inStream.readFully(this.path);
      if (this.path[0] == 2) {
         this._unused = inStream.readRemainder();
      }
   }

   protected int getDataSize() {
      int sz = 9 + this.path.length;
      if (this.path[0] == 2) {
         sz += this._unused.length;
      }

      return sz;
   }

   protected void serialize(LittleEndianOutput out) {
      out.writeShort(this.firstRow);
      out.writeShort(this.lastRow);
      out.writeByte(this.firstCol);
      out.writeByte(this.lastCol);
      out.writeShort(this.charCount);
      out.writeByte(this.charType);
      out.write(this.path);
      if (this.path[0] == 2) {
         out.write(this._unused);
      }
   }

   public short getSid() {
      return 81;
   }

   public int getFirstColumn() {
      return this.firstCol;
   }

   public int getFirstRow() {
      return this.firstRow;
   }

   public int getLastColumn() {
      return this.lastCol;
   }

   public int getLastRow() {
      return this.lastRow;
   }

   public String toString() {
      StringBuilder b = new StringBuilder();
      b.append("[DCONREF]\n");
      b.append("    .ref\n");
      b.append("        .firstrow   = ").append(this.firstRow).append("\n");
      b.append("        .lastrow    = ").append(this.lastRow).append("\n");
      b.append("        .firstcol   = ").append(this.firstCol).append("\n");
      b.append("        .lastcol    = ").append(this.lastCol).append("\n");
      b.append("    .cch            = ").append(this.charCount).append("\n");
      b.append("    .stFile\n");
      b.append("        .h          = ").append(this.charType).append("\n");
      b.append("        .rgb        = ").append(this.getReadablePath()).append("\n");
      b.append("[/DCONREF]\n");
      return b.toString();
   }

   public byte[] getPath() {
      return Arrays.copyOf(this.path, this.path.length);
   }

   public String getReadablePath() {
      if (this.path == null) {
         return null;
      }

      int offset = 1;

      while (this.path[offset] < 32 && offset < this.path.length) {
         offset++;
      }

      String out = new String(Arrays.copyOfRange(this.path, offset, this.path.length), StringUtil.UTF8);
      return out.replaceAll("\u0003", "/");
   }

   public boolean isExternalRef() {
      return this.path[0] == 1;
   }
}
