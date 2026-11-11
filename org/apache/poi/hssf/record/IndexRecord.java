package org.apache.poi.hssf.record;

import org.apache.poi.util.IntList;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.RecordFormatException;

public final class IndexRecord extends StandardRecord implements Cloneable {
   public static final short sid = 523;
   private int field_2_first_row;
   private int field_3_last_row_add1;
   private int field_4_zero;
   private IntList field_5_dbcells;

   public IndexRecord() {
   }

   public IndexRecord(RecordInputStream in) {
      int field_1_zero = in.readInt();
      if (field_1_zero != 0) {
         throw new RecordFormatException("Expected zero for field 1 but got " + field_1_zero);
      }

      this.field_2_first_row = in.readInt();
      this.field_3_last_row_add1 = in.readInt();
      this.field_4_zero = in.readInt();
      int nCells = in.remaining() / 4;
      this.field_5_dbcells = new IntList(nCells);

      for (int i = 0; i < nCells; i++) {
         this.field_5_dbcells.add(in.readInt());
      }
   }

   public void setFirstRow(int row) {
      this.field_2_first_row = row;
   }

   public void setLastRowAdd1(int row) {
      this.field_3_last_row_add1 = row;
   }

   public void addDbcell(int cell) {
      if (this.field_5_dbcells == null) {
         this.field_5_dbcells = new IntList();
      }

      this.field_5_dbcells.add(cell);
   }

   public void setDbcell(int cell, int value) {
      this.field_5_dbcells.set(cell, value);
   }

   public int getFirstRow() {
      return this.field_2_first_row;
   }

   public int getLastRowAdd1() {
      return this.field_3_last_row_add1;
   }

   public int getNumDbcells() {
      return this.field_5_dbcells == null ? 0 : this.field_5_dbcells.size();
   }

   public int getDbcellAt(int cellnum) {
      return this.field_5_dbcells.get(cellnum);
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[INDEX]\n");
      buffer.append("    .firstrow       = ").append(Integer.toHexString(this.getFirstRow())).append("\n");
      buffer.append("    .lastrowadd1    = ").append(Integer.toHexString(this.getLastRowAdd1())).append("\n");

      for (int k = 0; k < this.getNumDbcells(); k++) {
         buffer.append("    .dbcell_").append(k).append(" = ").append(Integer.toHexString(this.getDbcellAt(k))).append("\n");
      }

      buffer.append("[/INDEX]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeInt(0);
      out.writeInt(this.getFirstRow());
      out.writeInt(this.getLastRowAdd1());
      out.writeInt(this.field_4_zero);

      for (int k = 0; k < this.getNumDbcells(); k++) {
         out.writeInt(this.getDbcellAt(k));
      }
   }

   protected int getDataSize() {
      return 16 + this.getNumDbcells() * 4;
   }

   public static int getRecordSizeForBlockCount(int blockCount) {
      return 20 + 4 * blockCount;
   }

   public short getSid() {
      return 523;
   }

   public IndexRecord clone() {
      IndexRecord rec = new IndexRecord();
      rec.field_2_first_row = this.field_2_first_row;
      rec.field_3_last_row_add1 = this.field_3_last_row_add1;
      rec.field_4_zero = this.field_4_zero;
      rec.field_5_dbcells = new IntList();
      rec.field_5_dbcells.addAll(this.field_5_dbcells);
      return rec;
   }
}
