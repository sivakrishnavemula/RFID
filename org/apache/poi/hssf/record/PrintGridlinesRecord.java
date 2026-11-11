package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndianOutput;

public final class PrintGridlinesRecord extends StandardRecord {
   public static final short sid = 43;
   private short field_1_print_gridlines;

   public PrintGridlinesRecord() {
   }

   public PrintGridlinesRecord(RecordInputStream in) {
      this.field_1_print_gridlines = in.readShort();
   }

   public void setPrintGridlines(boolean pg) {
      if (pg) {
         this.field_1_print_gridlines = 1;
      } else {
         this.field_1_print_gridlines = 0;
      }
   }

   public boolean getPrintGridlines() {
      return this.field_1_print_gridlines == 1;
   }

   public String toString() {
      return "[PRINTGRIDLINES]\n    .printgridlines = " + this.getPrintGridlines() + "\n[/PRINTGRIDLINES]\n";
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(this.field_1_print_gridlines);
   }

   protected int getDataSize() {
      return 2;
   }

   public short getSid() {
      return 43;
   }

   public Object clone() {
      PrintGridlinesRecord rec = new PrintGridlinesRecord();
      rec.field_1_print_gridlines = this.field_1_print_gridlines;
      return rec;
   }
}
