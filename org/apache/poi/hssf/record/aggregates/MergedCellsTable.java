package org.apache.poi.hssf.record.aggregates;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.model.RecordStream;
import org.apache.poi.hssf.record.MergeCellsRecord;
import org.apache.poi.hssf.record.aggregates.RecordAggregate.RecordVisitor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;

public final class MergedCellsTable extends RecordAggregate {
   private static int MAX_MERGED_REGIONS = 1027;
   private final List<CellRangeAddress> _mergedRegions = new ArrayList();

   public void read(RecordStream rs) {
      while (rs.peekNextClass() == MergeCellsRecord.class) {
         MergeCellsRecord mcr = (MergeCellsRecord)rs.getNext();
         int nRegions = mcr.getNumAreas();

         for (int i = 0; i < nRegions; i++) {
            CellRangeAddress cra = mcr.getAreaAt(i);
            this._mergedRegions.add(cra);
         }
      }
   }

   public int getRecordSize() {
      int nRegions = this._mergedRegions.size();
      if (nRegions < 1) {
         return 0;
      }

      int nMergedCellsRecords = nRegions / MAX_MERGED_REGIONS;
      int nLeftoverMergedRegions = nRegions % MAX_MERGED_REGIONS;
      return nMergedCellsRecords * (4 + CellRangeAddressList.getEncodedSize(MAX_MERGED_REGIONS))
         + 4
         + CellRangeAddressList.getEncodedSize(nLeftoverMergedRegions);
   }

   public void visitContainedRecords(RecordVisitor rv) {
      int nRegions = this._mergedRegions.size();
      if (nRegions >= 1) {
         int nFullMergedCellsRecords = nRegions / MAX_MERGED_REGIONS;
         int nLeftoverMergedRegions = nRegions % MAX_MERGED_REGIONS;
         CellRangeAddress[] cras = new CellRangeAddress[nRegions];
         this._mergedRegions.toArray(cras);

         for (int i = 0; i < nFullMergedCellsRecords; i++) {
            int startIx = i * MAX_MERGED_REGIONS;
            rv.visitRecord(new MergeCellsRecord(cras, startIx, MAX_MERGED_REGIONS));
         }

         if (nLeftoverMergedRegions > 0) {
            int startIx = nFullMergedCellsRecords * MAX_MERGED_REGIONS;
            rv.visitRecord(new MergeCellsRecord(cras, startIx, nLeftoverMergedRegions));
         }
      }
   }

   public void addRecords(MergeCellsRecord[] mcrs) {
      for (int i = 0; i < mcrs.length; i++) {
         this.addMergeCellsRecord(mcrs[i]);
      }
   }

   private void addMergeCellsRecord(MergeCellsRecord mcr) {
      int nRegions = mcr.getNumAreas();

      for (int i = 0; i < nRegions; i++) {
         CellRangeAddress cra = mcr.getAreaAt(i);
         this._mergedRegions.add(cra);
      }
   }

   public CellRangeAddress get(int index) {
      this.checkIndex(index);
      return (CellRangeAddress)this._mergedRegions.get(index);
   }

   public void remove(int index) {
      this.checkIndex(index);
      this._mergedRegions.remove(index);
   }

   private void checkIndex(int index) {
      if (index < 0 || index >= this._mergedRegions.size()) {
         throw new IllegalArgumentException("Specified CF index " + index + " is outside the allowable range (0.." + (this._mergedRegions.size() - 1) + ")");
      }
   }

   public void addArea(int rowFrom, int colFrom, int rowTo, int colTo) {
      this._mergedRegions.add(new CellRangeAddress(rowFrom, rowTo, colFrom, colTo));
   }

   public int getNumberOfMergedRegions() {
      return this._mergedRegions.size();
   }
}
