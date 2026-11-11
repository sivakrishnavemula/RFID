package org.apache.poi.hssf.record.aggregates;

import java.util.Iterator;
import org.apache.poi.hssf.model.RecordStream;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.MulBlankRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordBase;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.record.aggregates.RecordAggregate.RecordVisitor;
import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.formula.ptg.Ptg;

public final class ValueRecordsAggregate implements Iterable<CellValueRecordInterface> {
   private static final int MAX_ROW_INDEX = 65535;
   private static final int INDEX_NOT_SET = -1;
   private int firstcell = -1;
   private int lastcell = -1;
   private CellValueRecordInterface[][] records;

   public ValueRecordsAggregate() {
      this(-1, -1, new CellValueRecordInterface[30][]);
   }

   private ValueRecordsAggregate(int firstCellIx, int lastCellIx, CellValueRecordInterface[][] pRecords) {
      this.firstcell = firstCellIx;
      this.lastcell = lastCellIx;
      this.records = pRecords;
   }

   public void insertCell(CellValueRecordInterface cell) {
      short column = cell.getColumn();
      int row = cell.getRow();
      if (row >= this.records.length) {
         CellValueRecordInterface[][] oldRecords = this.records;
         int newSize = oldRecords.length * 2;
         if (newSize < row + 1) {
            newSize = row + 1;
         }

         this.records = new CellValueRecordInterface[newSize][];
         System.arraycopy(oldRecords, 0, this.records, 0, oldRecords.length);
      }

      CellValueRecordInterface[] rowCells = this.records[row];
      if (rowCells == null) {
         int newSize = column + 1;
         if (newSize < 10) {
            newSize = 10;
         }

         rowCells = new CellValueRecordInterface[newSize];
         this.records[row] = rowCells;
      }

      if (column >= rowCells.length) {
         CellValueRecordInterface[] oldRowCells = rowCells;
         int newSize = oldRowCells.length * 2;
         if (newSize < column + 1) {
            newSize = column + 1;
         }

         rowCells = new CellValueRecordInterface[newSize];
         System.arraycopy(oldRowCells, 0, rowCells, 0, oldRowCells.length);
         this.records[row] = rowCells;
      }

      rowCells[column] = cell;
      if (column < this.firstcell || this.firstcell == -1) {
         this.firstcell = column;
      }

      if (column > this.lastcell || this.lastcell == -1) {
         this.lastcell = column;
      }
   }

   public void removeCell(CellValueRecordInterface cell) {
      if (cell == null) {
         throw new IllegalArgumentException("cell must not be null");
      }

      int row = cell.getRow();
      if (row >= this.records.length) {
         throw new RuntimeException("cell row is out of range");
      }

      CellValueRecordInterface[] rowCells = this.records[row];
      if (rowCells == null) {
         throw new RuntimeException("cell row is already empty");
      }

      short column = cell.getColumn();
      if (column >= rowCells.length) {
         throw new RuntimeException("cell column is out of range");
      }

      rowCells[column] = null;
   }

   public void removeAllCellsValuesForRow(int rowIndex) {
      if (rowIndex < 0 || rowIndex > 65535) {
         throw new IllegalArgumentException("Specified rowIndex " + rowIndex + " is outside the allowable range (0.." + 65535 + ")");
      }

      if (rowIndex < this.records.length) {
         this.records[rowIndex] = null;
      }
   }

   public int getPhysicalNumberOfCells() {
      int count = 0;

      for (int r = 0; r < this.records.length; r++) {
         CellValueRecordInterface[] rowCells = this.records[r];
         if (rowCells != null) {
            for (int c = 0; c < rowCells.length; c++) {
               if (rowCells[c] != null) {
                  count++;
               }
            }
         }
      }

      return count;
   }

   public int getFirstCellNum() {
      return this.firstcell;
   }

   public int getLastCellNum() {
      return this.lastcell;
   }

   public void addMultipleBlanks(MulBlankRecord mbr) {
      for (int j = 0; j < mbr.getNumColumns(); j++) {
         BlankRecord br = new BlankRecord();
         br.setColumn((short)(j + mbr.getFirstColumn()));
         br.setRow(mbr.getRow());
         br.setXFIndex(mbr.getXFAt(j));
         this.insertCell(br);
      }
   }

   public void construct(CellValueRecordInterface rec, RecordStream rs, SharedValueManager sfh) {
      if (rec instanceof FormulaRecord) {
         FormulaRecord formulaRec = (FormulaRecord)rec;
         Class<? extends Record> nextClass = rs.peekNextClass();
         StringRecord cachedText;
         if (nextClass == StringRecord.class) {
            cachedText = (StringRecord)rs.getNext();
         } else {
            cachedText = null;
         }

         this.insertCell(new FormulaRecordAggregate(formulaRec, cachedText, sfh));
      } else {
         this.insertCell(rec);
      }
   }

   public int getRowCellBlockSize(int startRow, int endRow) {
      int result = 0;

      for (int rowIx = startRow; rowIx <= endRow && rowIx < this.records.length; rowIx++) {
         result += getRowSerializedSize(this.records[rowIx]);
      }

      return result;
   }

   public boolean rowHasCells(int row) {
      if (row >= this.records.length) {
         return false;
      }

      CellValueRecordInterface[] rowCells = this.records[row];
      if (rowCells == null) {
         return false;
      }

      for (int col = 0; col < rowCells.length; col++) {
         if (rowCells[col] != null) {
            return true;
         }
      }

      return false;
   }

   private static int getRowSerializedSize(CellValueRecordInterface[] rowCells) {
      if (rowCells == null) {
         return 0;
      }

      int result = 0;

      for (int i = 0; i < rowCells.length; i++) {
         RecordBase cvr = (RecordBase)rowCells[i];
         if (cvr != null) {
            int nBlank = countBlanks(rowCells, i);
            if (nBlank > 1) {
               result += 10 + 2 * nBlank;
               i += nBlank - 1;
            } else {
               result += cvr.getRecordSize();
            }
         }
      }

      return result;
   }

   public void visitCellsForRow(int rowIndex, RecordVisitor rv) {
      CellValueRecordInterface[] rowCells = this.records[rowIndex];
      if (rowCells == null) {
         throw new IllegalArgumentException("Row [" + rowIndex + "] is empty");
      }

      for (int i = 0; i < rowCells.length; i++) {
         RecordBase cvr = (RecordBase)rowCells[i];
         if (cvr != null) {
            int nBlank = countBlanks(rowCells, i);
            if (nBlank > 1) {
               rv.visitRecord(this.createMBR(rowCells, i, nBlank));
               i += nBlank - 1;
            } else if (cvr instanceof RecordAggregate) {
               RecordAggregate agg = (RecordAggregate)cvr;
               agg.visitContainedRecords(rv);
            } else {
               rv.visitRecord((Record)cvr);
            }
         }
      }
   }

   private static int countBlanks(CellValueRecordInterface[] rowCellValues, int startIx) {
      int i;
      for (i = startIx; i < rowCellValues.length; i++) {
         CellValueRecordInterface cvr = rowCellValues[i];
         if (!(cvr instanceof BlankRecord)) {
            break;
         }
      }

      return i - startIx;
   }

   private MulBlankRecord createMBR(CellValueRecordInterface[] cellValues, int startIx, int nBlank) {
      short[] xfs = new short[nBlank];

      for (int i = 0; i < xfs.length; i++) {
         xfs[i] = cellValues[startIx + i].getXFIndex();
      }

      int rowIx = cellValues[startIx].getRow();
      return new MulBlankRecord(rowIx, startIx, xfs);
   }

   public void updateFormulasAfterRowShift(FormulaShifter shifter, int currentExternSheetIndex) {
      for (int i = 0; i < this.records.length; i++) {
         CellValueRecordInterface[] rowCells = this.records[i];
         if (rowCells != null) {
            for (int j = 0; j < rowCells.length; j++) {
               CellValueRecordInterface cell = rowCells[j];
               if (cell instanceof FormulaRecordAggregate) {
                  FormulaRecordAggregate fra = (FormulaRecordAggregate)cell;
                  Ptg[] ptgs = fra.getFormulaTokens();
                  Ptg[] ptgs2 = ((FormulaRecordAggregate)cell).getFormulaRecord().getParsedExpression();
                  if (shifter.adjustFormula(ptgs, currentExternSheetIndex)) {
                     fra.setParsedExpression(ptgs);
                  }
               }
            }
         }
      }
   }

   public Iterator<CellValueRecordInterface> iterator() {
      return new ValueRecordsAggregate.ValueIterator();
   }

   public Object clone() {
      throw new RuntimeException("clone() should not be called.  ValueRecordsAggregate should be copied via Sheet.cloneSheet()");
   }

   class ValueIterator implements Iterator<CellValueRecordInterface> {
      int curRowIndex;
      int curColIndex = -1;
      int nextRowIndex;
      int nextColIndex = -1;

      public ValueIterator() {
         this.getNextPos();
      }

      void getNextPos() {
         if (this.nextRowIndex < ValueRecordsAggregate.this.records.length) {
            while (this.nextRowIndex < ValueRecordsAggregate.this.records.length) {
               this.nextColIndex++;
               if (ValueRecordsAggregate.this.records[this.nextRowIndex] != null
                  && this.nextColIndex < ValueRecordsAggregate.this.records[this.nextRowIndex].length) {
                  if (ValueRecordsAggregate.this.records[this.nextRowIndex][this.nextColIndex] != null) {
                     return;
                  }
               } else {
                  this.nextRowIndex++;
                  this.nextColIndex = -1;
               }
            }
         }
      }

      public boolean hasNext() {
         return this.nextRowIndex < ValueRecordsAggregate.this.records.length;
      }

      public CellValueRecordInterface next() {
         if (!this.hasNext()) {
            throw new IndexOutOfBoundsException("iterator has no next");
         }

         this.curRowIndex = this.nextRowIndex;
         this.curColIndex = this.nextColIndex;
         CellValueRecordInterface ret = ValueRecordsAggregate.this.records[this.curRowIndex][this.curColIndex];
         this.getNextPos();
         return ret;
      }

      public void remove() {
         ValueRecordsAggregate.this.records[this.curRowIndex][this.curColIndex] = null;
      }
   }
}
