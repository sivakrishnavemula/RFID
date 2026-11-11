package org.apache.poi.hssf.record;

import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.RecordFormatException;

public final class NoteStructureSubRecord extends SubRecord implements Cloneable {
   public static final short sid = 13;
   private static final int ENCODED_SIZE = 22;
   private byte[] reserved;

   public NoteStructureSubRecord() {
      this.reserved = new byte[22];
   }

   public NoteStructureSubRecord(LittleEndianInput in, int size) {
      if (size != 22) {
         throw new RecordFormatException("Unexpected size (" + size + ")");
      }

      byte[] buf = IOUtils.safelyAllocate(size, 22);
      in.readFully(buf);
      this.reserved = buf;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[ftNts ]").append("\n");
      buffer.append("  size     = ").append(this.getDataSize()).append("\n");
      buffer.append("  reserved = ").append(HexDump.toHex(this.reserved)).append("\n");
      buffer.append("[/ftNts ]").append("\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(13);
      out.writeShort(this.reserved.length);
      out.write(this.reserved);
   }

   protected int getDataSize() {
      return this.reserved.length;
   }

   public short getSid() {
      return 13;
   }

   public NoteStructureSubRecord clone() {
      NoteStructureSubRecord rec = new NoteStructureSubRecord();
      byte[] recdata = new byte[this.reserved.length];
      System.arraycopy(this.reserved, 0, recdata, 0, recdata.length);
      rec.reserved = recdata;
      return rec;
   }
}
