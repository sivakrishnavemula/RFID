package org.apache.poi.hssf.record;

import org.apache.poi.ss.util.CellRangeAddress;

public final class CFHeaderRecord extends CFHeaderBase implements Cloneable {
   public static final short sid = 432;

   public CFHeaderRecord() {
      this.createEmpty();
   }

   public CFHeaderRecord(CellRangeAddress[] regions, int nRules) {
      super(regions, nRules);
   }

   public CFHeaderRecord(RecordInputStream in) {
      this.read(in);
   }

   protected String getRecordName() {
      return "CFHEADER";
   }

   public short getSid() {
      return 432;
   }

   public CFHeaderRecord clone() {
      CFHeaderRecord result = new CFHeaderRecord();
      super.copyTo(result);
      return result;
   }
}
