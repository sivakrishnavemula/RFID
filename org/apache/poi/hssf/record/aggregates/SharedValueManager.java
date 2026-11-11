package org.apache.poi.hssf.record.aggregates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.hssf.record.ArrayRecord;
import org.apache.poi.hssf.record.SharedFormulaRecord;
import org.apache.poi.hssf.record.SharedValueRecordBase;
import org.apache.poi.hssf.record.TableRecord;
import org.apache.poi.hssf.util.CellRangeAddress8Bit;
import org.apache.poi.ss.util.CellReference;

public final class SharedValueManager {
   private final List<ArrayRecord> _arrayRecords;
   private final TableRecord[] _tableRecords;
   private final Map<SharedFormulaRecord, SharedValueManager.SharedFormulaGroup> _groupsBySharedFormulaRecord;
   private Map<Integer, SharedValueManager.SharedFormulaGroup> _groupsCache;

   public static SharedValueManager createEmpty() {
      return new SharedValueManager(new SharedFormulaRecord[0], new CellReference[0], new ArrayRecord[0], new TableRecord[0]);
   }

   private SharedValueManager(SharedFormulaRecord[] sharedFormulaRecords, CellReference[] firstCells, ArrayRecord[] arrayRecords, TableRecord[] tableRecords) {
      int nShF = sharedFormulaRecords.length;
      if (nShF != firstCells.length) {
         throw new IllegalArgumentException("array sizes don't match: " + nShF + "!=" + firstCells.length + ".");
      }

      this._arrayRecords = toList(arrayRecords);
      this._tableRecords = tableRecords;
      Map<SharedFormulaRecord, SharedValueManager.SharedFormulaGroup> m = new HashMap(nShF * 3 / 2);

      for (int i = 0; i < nShF; i++) {
         SharedFormulaRecord sfr = sharedFormulaRecords[i];
         m.put(sfr, new SharedValueManager.SharedFormulaGroup(sfr, firstCells[i]));
      }

      this._groupsBySharedFormulaRecord = m;
   }

   private static <Z> List<Z> toList(Z[] zz) {
      List<Z> result = new ArrayList(zz.length);

      for (int i = 0; i < zz.length; i++) {
         result.add(zz[i]);
      }

      return result;
   }

   public static SharedValueManager create(
      SharedFormulaRecord[] sharedFormulaRecords, CellReference[] firstCells, ArrayRecord[] arrayRecords, TableRecord[] tableRecords
   ) {
      return sharedFormulaRecords.length + firstCells.length + arrayRecords.length + tableRecords.length < 1
         ? createEmpty()
         : new SharedValueManager(sharedFormulaRecords, firstCells, arrayRecords, tableRecords);
   }

   public SharedFormulaRecord linkSharedFormulaRecord(CellReference firstCell, FormulaRecordAggregate agg) {
      SharedValueManager.SharedFormulaGroup result = this.findFormulaGroupForCell(firstCell);
      if (null == result) {
         throw new RuntimeException("Failed to find a matching shared formula record");
      }

      result.add(agg);
      return result.getSFR();
   }

   private SharedValueManager.SharedFormulaGroup findFormulaGroupForCell(CellReference cellRef) {
      if (null == this._groupsCache) {
         this._groupsCache = new HashMap(this._groupsBySharedFormulaRecord.size());

         for (SharedValueManager.SharedFormulaGroup group : this._groupsBySharedFormulaRecord.values()) {
            this._groupsCache.put(this.getKeyForCache(group._firstCell), group);
         }
      }

      return (SharedValueManager.SharedFormulaGroup)this._groupsCache.get(this.getKeyForCache(cellRef));
   }

   private Integer getKeyForCache(CellReference cellRef) {
      return cellRef.getCol() + 1 << 16 | cellRef.getRow();
   }

   public SharedValueRecordBase getRecordForFirstCell(FormulaRecordAggregate agg) {
      CellReference firstCell = agg.getFormulaRecord().getFormula().getExpReference();
      if (firstCell == null) {
         return null;
      }

      int row = firstCell.getRow();
      int column = firstCell.getCol();
      if (agg.getRow() == row && agg.getColumn() == column) {
         if (!this._groupsBySharedFormulaRecord.isEmpty()) {
            SharedValueManager.SharedFormulaGroup sfg = this.findFormulaGroupForCell(firstCell);
            if (null != sfg) {
               return sfg.getSFR();
            }
         }

         for (TableRecord tr : this._tableRecords) {
            if (tr.isFirstCell(row, column)) {
               return tr;
            }
         }

         for (ArrayRecord ar : this._arrayRecords) {
            if (ar.isFirstCell(row, column)) {
               return ar;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public void unlink(SharedFormulaRecord sharedFormulaRecord) {
      SharedValueManager.SharedFormulaGroup svg = (SharedValueManager.SharedFormulaGroup)this._groupsBySharedFormulaRecord.remove(sharedFormulaRecord);
      if (svg == null) {
         throw new IllegalStateException("Failed to find formulas for shared formula");
      }

      this._groupsCache = null;
      svg.unlinkSharedFormulas();
   }

   public void addArrayRecord(ArrayRecord ar) {
      this._arrayRecords.add(ar);
   }

   public CellRangeAddress8Bit removeArrayFormula(int rowIndex, int columnIndex) {
      for (ArrayRecord ar : this._arrayRecords) {
         if (ar.isInRange(rowIndex, columnIndex)) {
            this._arrayRecords.remove(ar);
            return ar.getRange();
         }
      }

      String ref = new CellReference(rowIndex, columnIndex, false, false).formatAsString();
      throw new IllegalArgumentException("Specified cell " + ref + " is not part of an array formula.");
   }

   public ArrayRecord getArrayRecord(int firstRow, int firstColumn) {
      for (ArrayRecord ar : this._arrayRecords) {
         if (ar.isFirstCell(firstRow, firstColumn)) {
            return ar;
         }
      }

      return null;
   }

   private static final class SharedFormulaGroup {
      private final SharedFormulaRecord _sfr;
      private final FormulaRecordAggregate[] _frAggs;
      private int _numberOfFormulas;
      private final CellReference _firstCell;

      public SharedFormulaGroup(SharedFormulaRecord sfr, CellReference firstCell) {
         if (!sfr.isInRange(firstCell.getRow(), firstCell.getCol())) {
            throw new IllegalArgumentException("First formula cell " + firstCell.formatAsString() + " is not shared formula range " + sfr.getRange() + ".");
         }

         this._sfr = sfr;
         this._firstCell = firstCell;
         int width = sfr.getLastColumn() - sfr.getFirstColumn() + 1;
         int height = sfr.getLastRow() - sfr.getFirstRow() + 1;
         this._frAggs = new FormulaRecordAggregate[width * height];
         this._numberOfFormulas = 0;
      }

      public void add(FormulaRecordAggregate agg) {
         if (this._numberOfFormulas != 0 || this._firstCell.getRow() == agg.getRow() && this._firstCell.getCol() == agg.getColumn()) {
            if (this._numberOfFormulas >= this._frAggs.length) {
               throw new RuntimeException("Too many formula records for shared formula group");
            }

            this._frAggs[this._numberOfFormulas++] = agg;
         } else {
            throw new IllegalStateException(
               "shared formula coding error: " + this._firstCell.getCol() + '/' + this._firstCell.getRow() + " != " + agg.getColumn() + '/' + agg.getRow()
            );
         }
      }

      public void unlinkSharedFormulas() {
         for (int i = 0; i < this._numberOfFormulas; i++) {
            this._frAggs[i].unlinkSharedFormula();
         }
      }

      public SharedFormulaRecord getSFR() {
         return this._sfr;
      }

      public final String toString() {
         StringBuffer sb = new StringBuffer(64);
         sb.append(this.getClass().getName()).append(" [");
         sb.append(this._sfr.getRange());
         sb.append("]");
         return sb.toString();
      }
   }
}
