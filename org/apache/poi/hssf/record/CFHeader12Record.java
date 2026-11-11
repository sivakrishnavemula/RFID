package org.apache.poi.hssf.record;

import org.apache.poi.hssf.record.common.FtrHeader;
import org.apache.poi.hssf.record.common.FutureRecord;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.LittleEndianOutput;

public final class CFHeader12Record extends CFHeaderBase implements FutureRecord, Cloneable {
   public static final short sid = 2169;
   private FtrHeader futureHeader;

   public CFHeader12Record() {
      this.createEmpty();
      this.futureHeader = new FtrHeader();
      this.futureHeader.setRecordType((short)2169);
   }

   public CFHeader12Record(CellRangeAddress[] regions, int nRules) {
      super(regions, nRules);
      this.futureHeader = new FtrHeader();
      this.futureHeader.setRecordType((short)2169);
   }

   public CFHeader12Record(RecordInputStream in) {
      this.futureHeader = new FtrHeader(in);
      this.read(in);
   }

   protected String getRecordName() {
      return "CFHEADER12";
   }

   protected int getDataSize() {
      return FtrHeader.getDataSize() + super.getDataSize();
   }

   public void serialize(LittleEndianOutput out) {
      this.futureHeader.setAssociatedRange(this.getEnclosingCellRange());
      this.futureHeader.serialize(out);
      super.serialize(out);
   }

   public short getSid() {
      return 2169;
   }

   public short getFutureRecordType() {
      return this.futureHeader.getRecordType();
   }

   public FtrHeader getFutureHeader() {
      return this.futureHeader;
   }

   public CellRangeAddress getAssociatedRange() {
      return this.futureHeader.getAssociatedRange();
   }

   public CFHeader12Record clone() {
      CFHeader12Record result = new CFHeader12Record();
      result.futureHeader = (FtrHeader)this.futureHeader.clone();
      super.copyTo(result);
      return result;
   }
}
