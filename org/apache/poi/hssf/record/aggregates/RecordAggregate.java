package org.apache.poi.hssf.record.aggregates;

import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordBase;

public abstract class RecordAggregate extends RecordBase {
   public abstract void visitContainedRecords(RecordAggregate.RecordVisitor var1);

   public final int serialize(int offset, byte[] data) {
      RecordAggregate.SerializingRecordVisitor srv = new RecordAggregate.SerializingRecordVisitor(data, offset);
      this.visitContainedRecords(srv);
      return srv.countBytesWritten();
   }

   public int getRecordSize() {
      RecordAggregate.RecordSizingVisitor rsv = new RecordAggregate.RecordSizingVisitor();
      this.visitContainedRecords(rsv);
      return rsv.getTotalSize();
   }

   public static final class PositionTrackingVisitor implements RecordAggregate.RecordVisitor {
      private final RecordAggregate.RecordVisitor _rv;
      private int _position;

      public PositionTrackingVisitor(RecordAggregate.RecordVisitor rv, int initialPosition) {
         this._rv = rv;
         this._position = initialPosition;
      }

      @Override
      public void visitRecord(Record r) {
         this._position = this._position + r.getRecordSize();
         this._rv.visitRecord(r);
      }

      public void setPosition(int position) {
         this._position = position;
      }

      public int getPosition() {
         return this._position;
      }
   }

   private static final class RecordSizingVisitor implements RecordAggregate.RecordVisitor {
      private int _totalSize = 0;

      public RecordSizingVisitor() {
      }

      public int getTotalSize() {
         return this._totalSize;
      }

      @Override
      public void visitRecord(Record r) {
         this._totalSize = this._totalSize + r.getRecordSize();
      }
   }

   public interface RecordVisitor {
      void visitRecord(Record var1);
   }

   private static final class SerializingRecordVisitor implements RecordAggregate.RecordVisitor {
      private final byte[] _data;
      private final int _startOffset;
      private int _countBytesWritten;

      public SerializingRecordVisitor(byte[] data, int startOffset) {
         this._data = data;
         this._startOffset = startOffset;
         this._countBytesWritten = 0;
      }

      public int countBytesWritten() {
         return this._countBytesWritten;
      }

      @Override
      public void visitRecord(Record r) {
         int currentOffset = this._startOffset + this._countBytesWritten;
         this._countBytesWritten = this._countBytesWritten + r.serialize(currentOffset, this._data);
      }
   }
}
