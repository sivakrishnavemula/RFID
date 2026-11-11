package org.apache.poi.hssf.record;

import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.RecordFormatException;

public final class FtCfSubRecord extends SubRecord implements Cloneable {
   public static final short sid = 7;
   public static final short length = 2;
   public static final short METAFILE_BIT = 2;
   public static final short BITMAP_BIT = 9;
   public static final short UNSPECIFIED_BIT = -1;
   private short flags;

   public FtCfSubRecord() {
   }

   public FtCfSubRecord(LittleEndianInput in, int size) {
      if (size != 2) {
         throw new RecordFormatException("Unexpected size (" + size + ")");
      }

      this.flags = in.readShort();
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[FtCf ]\n");
      buffer.append("  size     = ").append(2).append("\n");
      buffer.append("  flags    = ").append(HexDump.toHex(this.flags)).append("\n");
      buffer.append("[/FtCf ]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(7);
      out.writeShort(2);
      out.writeShort(this.flags);
   }

   protected int getDataSize() {
      return 2;
   }

   public short getSid() {
      return 7;
   }

   public FtCfSubRecord clone() {
      FtCfSubRecord rec = new FtCfSubRecord();
      rec.flags = this.flags;
      return rec;
   }

   public short getFlags() {
      return this.flags;
   }

   public void setFlags(short flags) {
      this.flags = flags;
   }
}
