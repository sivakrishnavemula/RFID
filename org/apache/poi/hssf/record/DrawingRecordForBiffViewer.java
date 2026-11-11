package org.apache.poi.hssf.record;

import java.io.ByteArrayInputStream;

public final class DrawingRecordForBiffViewer extends AbstractEscherHolderRecord {
   public static final short sid = 236;

   public DrawingRecordForBiffViewer() {
   }

   public DrawingRecordForBiffViewer(RecordInputStream in) {
      super(in);
   }

   public DrawingRecordForBiffViewer(DrawingRecord r) {
      super(convertToInputStream(r));
      this.convertRawBytesToEscherRecords();
   }

   private static RecordInputStream convertToInputStream(DrawingRecord r) {
      byte[] data = r.serialize();
      RecordInputStream rinp = new RecordInputStream(new ByteArrayInputStream(data));
      rinp.nextRecord();
      return rinp;
   }

   protected String getRecordName() {
      return "MSODRAWING";
   }

   public short getSid() {
      return 236;
   }
}
