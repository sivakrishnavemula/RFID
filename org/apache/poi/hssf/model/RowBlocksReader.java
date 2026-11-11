package org.apache.poi.hssf.model;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.record.ArrayRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.MergeCellsRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SharedFormulaRecord;
import org.apache.poi.hssf.record.TableRecord;
import org.apache.poi.hssf.record.aggregates.SharedValueManager;
import org.apache.poi.ss.util.CellReference;

public final class RowBlocksReader {
   private final List<Record> _plainRecords;
   private final SharedValueManager _sfm;
   private final MergeCellsRecord[] _mergedCellsRecords;

   public RowBlocksReader(RecordStream rs) {
      List<Record> plainRecords = new ArrayList();
      List<Record> shFrmRecords = new ArrayList();
      List<CellReference> firstCellRefs = new ArrayList();
      List<Record> arrayRecords = new ArrayList();
      List<Record> tableRecords = new ArrayList();
      List<Record> mergeCellRecords = new ArrayList();
      Record prevRec = null;

      while (!RecordOrderer.isEndOfRowBlock(rs.peekNextSid())) {
         if (!rs.hasNext()) {
            throw new RuntimeException("Failed to find end of row/cell records");
         }

         Record rec = rs.getNext();
         List<Record> dest;
         switch (rec.getSid()) {
            case 229:
               dest = mergeCellRecords;
               break;
            case 545:
               dest = arrayRecords;
               break;
            case 566:
               dest = tableRecords;
               break;
            case 1212:
               dest = shFrmRecords;
               if (!(prevRec instanceof FormulaRecord)) {
                  throw new RuntimeException("Shared formula record should follow a FormulaRecord");
               }

               FormulaRecord fr = (FormulaRecord)prevRec;
               firstCellRefs.add(new CellReference(fr.getRow(), fr.getColumn()));
               break;
            default:
               dest = plainRecords;
         }

         dest.add(rec);
         prevRec = rec;
      }

      SharedFormulaRecord[] sharedFormulaRecs = new SharedFormulaRecord[shFrmRecords.size()];
      CellReference[] firstCells = new CellReference[firstCellRefs.size()];
      ArrayRecord[] arrayRecs = new ArrayRecord[arrayRecords.size()];
      TableRecord[] tableRecs = new TableRecord[tableRecords.size()];
      shFrmRecords.toArray(sharedFormulaRecs);
      firstCellRefs.toArray(firstCells);
      arrayRecords.toArray(arrayRecs);
      tableRecords.toArray(tableRecs);
      this._plainRecords = plainRecords;
      this._sfm = SharedValueManager.create(sharedFormulaRecs, firstCells, arrayRecs, tableRecs);
      this._mergedCellsRecords = new MergeCellsRecord[mergeCellRecords.size()];
      mergeCellRecords.toArray(this._mergedCellsRecords);
   }

   public MergeCellsRecord[] getLooseMergedCells() {
      return this._mergedCellsRecords;
   }

   public SharedValueManager getSharedFormulaManager() {
      return this._sfm;
   }

   public RecordStream getPlainRecordStream() {
      return new RecordStream(this._plainRecords, 0);
   }
}
