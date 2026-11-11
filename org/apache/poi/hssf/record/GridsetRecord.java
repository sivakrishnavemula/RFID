package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndianOutput;

public final class GridsetRecord extends StandardRecord implements Cloneable {
   public static final short sid = 130;
   public short field_1_gridset_flag;

   public GridsetRecord() {
   }

   public GridsetRecord(RecordInputStream in) {
      this.field_1_gridset_flag = in.readShort();
   }

   public void setGridset(boolean gridset) {
      if (gridset) {
         this.field_1_gridset_flag = 1;
      } else {
         this.field_1_gridset_flag = 0;
      }
   }

   public boolean getGridset() {
      return this.field_1_gridset_flag == 1;
   }

   public String toString() {
      return "[GRIDSET]\n    .gridset        = " + this.getGridset() + "\n[/GRIDSET]\n";
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(this.field_1_gridset_flag);
   }

   protected int getDataSize() {
      return 2;
   }

   public short getSid() {
      return 130;
   }

   public GridsetRecord clone() {
      GridsetRecord rec = new GridsetRecord();
      rec.field_1_gridset_flag = this.field_1_gridset_flag;
      return rec;
   }
}
