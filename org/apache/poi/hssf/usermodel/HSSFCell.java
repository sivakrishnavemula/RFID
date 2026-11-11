package org.apache.poi.hssf.usermodel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.HyperlinkRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordBase;
import org.apache.poi.hssf.record.aggregates.FormulaRecordAggregate;
import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.ptg.ExpPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.Removal;

public class HSSFCell implements Cell {
   private static final String FILE_FORMAT_NAME = "BIFF8";
   public static final int LAST_COLUMN_NUMBER = SpreadsheetVersion.EXCEL97.getLastColumnIndex();
   private static final String LAST_COLUMN_NAME = SpreadsheetVersion.EXCEL97.getLastColumnName();
   public static final short ENCODING_UNCHANGED = -1;
   public static final short ENCODING_COMPRESSED_UNICODE = 0;
   public static final short ENCODING_UTF_16 = 1;
   private final HSSFWorkbook _book;
   private final HSSFSheet _sheet;
   private CellType _cellType;
   private HSSFRichTextString _stringValue;
   private CellValueRecordInterface _record;
   private HSSFComment _comment;

   protected HSSFCell(HSSFWorkbook book, HSSFSheet sheet, int row, short col) {
      checkBounds(col);
      this._stringValue = null;
      this._book = book;
      this._sheet = sheet;
      short xfindex = sheet.getSheet().getXFIndexForColAt(col);
      this.setCellType(CellType.BLANK, false, row, col, xfindex);
   }

   public HSSFSheet getSheet() {
      return this._sheet;
   }

   public HSSFRow getRow() {
      int rowIndex = this.getRowIndex();
      return this._sheet.getRow(rowIndex);
   }

   protected HSSFCell(HSSFWorkbook book, HSSFSheet sheet, int row, short col, CellType type) {
      checkBounds(col);
      this._cellType = CellType._NONE;
      this._stringValue = null;
      this._book = book;
      this._sheet = sheet;
      short xfindex = sheet.getSheet().getXFIndexForColAt(col);
      this.setCellType(type, false, row, col, xfindex);
   }

   protected HSSFCell(HSSFWorkbook book, HSSFSheet sheet, CellValueRecordInterface cval) {
      this._record = cval;
      this._cellType = determineType(cval);
      this._stringValue = null;
      this._book = book;
      this._sheet = sheet;
      switch (this._cellType) {
         case STRING:
            this._stringValue = new HSSFRichTextString(book.getWorkbook(), (LabelSSTRecord)cval);
         case BLANK:
         default:
            break;
         case FORMULA:
            this._stringValue = new HSSFRichTextString(((FormulaRecordAggregate)cval).getStringValue());
      }
   }

   private static CellType determineType(CellValueRecordInterface cval) {
      if (cval instanceof FormulaRecordAggregate) {
         return CellType.FORMULA;
      }

      Record record = (Record)cval;
      switch (record.getSid()) {
         case 253:
            return CellType.STRING;
         case 513:
            return CellType.BLANK;
         case 515:
            return CellType.NUMERIC;
         case 517:
            BoolErrRecord boolErrRecord = (BoolErrRecord)record;
            return boolErrRecord.isBoolean() ? CellType.BOOLEAN : CellType.ERROR;
         default:
            throw new RuntimeException("Bad cell value rec (" + cval.getClass().getName() + ")");
      }
   }

   protected InternalWorkbook getBoundWorkbook() {
      return this._book.getWorkbook();
   }

   public int getRowIndex() {
      return this._record.getRow();
   }

   protected void updateCellNum(short num) {
      this._record.setColumn(num);
   }

   public int getColumnIndex() {
      return this._record.getColumn() & 65535;
   }

   public CellAddress getAddress() {
      return new CellAddress(this);
   }

   public void setCellType(CellType cellType) {
      this.notifyFormulaChanging();
      if (this.isPartOfArrayFormulaGroup()) {
         this.notifyArrayFormulaChanging();
      }

      int row = this._record.getRow();
      short col = this._record.getColumn();
      short styleIndex = this._record.getXFIndex();
      this.setCellType(cellType, true, row, col, styleIndex);
   }

   private void setCellType(CellType cellType, boolean setValue, int row, short col, short styleIndex) {
      switch (cellType) {
         case STRING:
            LabelSSTRecord lrec;
            if (cellType == this._cellType) {
               lrec = (LabelSSTRecord)this._record;
            } else {
               lrec = new LabelSSTRecord();
               lrec.setColumn(col);
               lrec.setRow(row);
               lrec.setXFIndex(styleIndex);
            }

            if (setValue) {
               String str = this.convertCellValueToString();
               if (str == null) {
                  this.setCellType(CellType.BLANK, false, row, col, styleIndex);
                  return;
               }

               int sstIndex = this._book.getWorkbook().addSSTString(new UnicodeString(str));
               lrec.setSSTIndex(sstIndex);
               UnicodeString us = this._book.getWorkbook().getSSTString(sstIndex);
               this._stringValue = new HSSFRichTextString();
               this._stringValue.setUnicodeString(us);
            }

            this._record = lrec;
            break;
         case BLANK:
            BlankRecord brec;
            if (cellType != this._cellType) {
               brec = new BlankRecord();
            } else {
               brec = (BlankRecord)this._record;
            }

            brec.setColumn(col);
            brec.setXFIndex(styleIndex);
            brec.setRow(row);
            this._record = brec;
            break;
         case FORMULA:
            FormulaRecordAggregate frec;
            if (cellType != this._cellType) {
               frec = this._sheet.getSheet().getRowsAggregate().createFormula(row, col);
            } else {
               frec = (FormulaRecordAggregate)this._record;
               frec.setRow(row);
               frec.setColumn(col);
            }

            if (setValue) {
               frec.getFormulaRecord().setValue(this.getNumericCellValue());
            }

            frec.setXFIndex(styleIndex);
            this._record = frec;
            break;
         case NUMERIC:
            NumberRecord nrec;
            if (cellType != this._cellType) {
               nrec = new NumberRecord();
            } else {
               nrec = (NumberRecord)this._record;
            }

            nrec.setColumn(col);
            if (setValue) {
               nrec.setValue(this.getNumericCellValue());
            }

            nrec.setXFIndex(styleIndex);
            nrec.setRow(row);
            this._record = nrec;
            break;
         case BOOLEAN:
            BoolErrRecord boolRec;
            if (cellType != this._cellType) {
               boolRec = new BoolErrRecord();
            } else {
               boolRec = (BoolErrRecord)this._record;
            }

            boolRec.setColumn(col);
            if (setValue) {
               boolRec.setValue(this.convertCellValueToBoolean());
            }

            boolRec.setXFIndex(styleIndex);
            boolRec.setRow(row);
            this._record = boolRec;
            break;
         case ERROR:
            BoolErrRecord errRec;
            if (cellType != this._cellType) {
               errRec = new BoolErrRecord();
            } else {
               errRec = (BoolErrRecord)this._record;
            }

            errRec.setColumn(col);
            if (setValue) {
               errRec.setValue(FormulaError.VALUE.getCode());
            }

            errRec.setXFIndex(styleIndex);
            errRec.setRow(row);
            this._record = errRec;
            break;
         default:
            throw new IllegalStateException("Invalid cell type: " + cellType);
      }

      if (cellType != this._cellType && this._cellType != CellType._NONE) {
         this._sheet.getSheet().replaceValueRecord(this._record);
      }

      this._cellType = cellType;
   }

   public CellType getCellType() {
      return this._cellType;
   }

   @Deprecated
   @Removal(version = "4.2")
   public CellType getCellTypeEnum() {
      return this.getCellType();
   }

   public void setCellValue(double value) {
      if (Double.isInfinite(value)) {
         this.setCellErrorValue(FormulaError.DIV0.getCode());
      } else if (Double.isNaN(value)) {
         this.setCellErrorValue(FormulaError.NUM.getCode());
      } else {
         int row = this._record.getRow();
         short col = this._record.getColumn();
         short styleIndex = this._record.getXFIndex();
         switch (this._cellType) {
            case FORMULA:
               ((FormulaRecordAggregate)this._record).setCachedDoubleResult(value);
               break;
            default:
               this.setCellType(CellType.NUMERIC, false, row, col, styleIndex);
            case NUMERIC:
               ((NumberRecord)this._record).setValue(value);
         }
      }
   }

   public void setCellValue(Date value) {
      this.setCellValue(HSSFDateUtil.getExcelDate(value, this._book.getWorkbook().isUsing1904DateWindowing()));
   }

   public void setCellValue(Calendar value) {
      this.setCellValue(HSSFDateUtil.getExcelDate(value, this._book.getWorkbook().isUsing1904DateWindowing()));
   }

   public void setCellValue(String value) {
      HSSFRichTextString str = value == null ? null : new HSSFRichTextString(value);
      this.setCellValue(str);
   }

   public void setCellValue(RichTextString value) {
      int row = this._record.getRow();
      short col = this._record.getColumn();
      short styleIndex = this._record.getXFIndex();
      if (value == null) {
         this.notifyFormulaChanging();
         this.setCellType(CellType.BLANK, false, row, col, styleIndex);
      } else {
         if (value.length() > SpreadsheetVersion.EXCEL97.getMaxTextLength()) {
            throw new IllegalArgumentException("The maximum length of cell contents (text) is 32,767 characters");
         }

         if (this._cellType == CellType.FORMULA) {
            FormulaRecordAggregate fr = (FormulaRecordAggregate)this._record;
            fr.setCachedStringResult(value.getString());
            this._stringValue = new HSSFRichTextString(value.getString());
         } else {
            if (this._cellType != CellType.STRING) {
               this.setCellType(CellType.STRING, false, row, col, styleIndex);
            }

            HSSFRichTextString hvalue = (HSSFRichTextString)value;
            UnicodeString str = hvalue.getUnicodeString();
            int index = this._book.getWorkbook().addSSTString(str);
            ((LabelSSTRecord)this._record).setSSTIndex(index);
            this._stringValue = hvalue;
            this._stringValue.setWorkbookReferences(this._book.getWorkbook(), (LabelSSTRecord)this._record);
            this._stringValue.setUnicodeString(this._book.getWorkbook().getSSTString(index));
         }
      }
   }

   public void setCellFormula(String formula) {
      if (this.isPartOfArrayFormulaGroup()) {
         this.notifyArrayFormulaChanging();
      }

      int row = this._record.getRow();
      short col = this._record.getColumn();
      short styleIndex = this._record.getXFIndex();
      if (formula == null) {
         this.notifyFormulaChanging();
         this.setCellType(CellType.BLANK, false, row, col, styleIndex);
      } else {
         int sheetIndex = this._book.getSheetIndex(this._sheet);
         Ptg[] ptgs = HSSFFormulaParser.parse(formula, this._book, FormulaType.CELL, sheetIndex);
         this.setCellType(CellType.FORMULA, false, row, col, styleIndex);
         FormulaRecordAggregate agg = (FormulaRecordAggregate)this._record;
         FormulaRecord frec = agg.getFormulaRecord();
         frec.setOptions((short)2);
         frec.setValue(0.0);
         if (agg.getXFIndex() == 0) {
            agg.setXFIndex((short)15);
         }

         agg.setParsedExpression(ptgs);
      }
   }

   private void notifyFormulaChanging() {
      if (this._record instanceof FormulaRecordAggregate) {
         ((FormulaRecordAggregate)this._record).notifyFormulaChanging();
      }
   }

   public String getCellFormula() {
      if (!(this._record instanceof FormulaRecordAggregate)) {
         throw typeMismatch(CellType.FORMULA, this._cellType, true);
      } else {
         return HSSFFormulaParser.toFormulaString(this._book, ((FormulaRecordAggregate)this._record).getFormulaTokens());
      }
   }

   private static RuntimeException typeMismatch(CellType expectedTypeCode, CellType actualTypeCode, boolean isFormulaCell) {
      String msg = "Cannot get a " + expectedTypeCode + " value from a " + actualTypeCode + " " + (isFormulaCell ? "formula " : "") + "cell";
      return new IllegalStateException(msg);
   }

   private static void checkFormulaCachedValueType(CellType expectedTypeCode, FormulaRecord fr) {
      CellType cachedValueType = CellType.forInt(fr.getCachedResultType());
      if (cachedValueType != expectedTypeCode) {
         throw typeMismatch(expectedTypeCode, cachedValueType, true);
      }
   }

   public double getNumericCellValue() {
      switch (this._cellType) {
         case BLANK:
            return 0.0;
         case FORMULA:
            FormulaRecord fr = ((FormulaRecordAggregate)this._record).getFormulaRecord();
            checkFormulaCachedValueType(CellType.NUMERIC, fr);
            return fr.getValue();
         case NUMERIC:
            return ((NumberRecord)this._record).getValue();
         default:
            throw typeMismatch(CellType.NUMERIC, this._cellType, false);
      }
   }

   public Date getDateCellValue() {
      if (this._cellType == CellType.BLANK) {
         return null;
      }

      double value = this.getNumericCellValue();
      return this._book.getWorkbook().isUsing1904DateWindowing() ? HSSFDateUtil.getJavaDate(value, true) : HSSFDateUtil.getJavaDate(value, false);
   }

   public String getStringCellValue() {
      HSSFRichTextString str = this.getRichStringCellValue();
      return str.getString();
   }

   public HSSFRichTextString getRichStringCellValue() {
      switch (this._cellType) {
         case STRING:
            return this._stringValue;
         case BLANK:
            return new HSSFRichTextString("");
         case FORMULA:
            FormulaRecordAggregate fra = (FormulaRecordAggregate)this._record;
            checkFormulaCachedValueType(CellType.STRING, fra.getFormulaRecord());
            String strVal = fra.getStringValue();
            return new HSSFRichTextString(strVal == null ? "" : strVal);
         default:
            throw typeMismatch(CellType.STRING, this._cellType, false);
      }
   }

   public void setCellValue(boolean value) {
      int row = this._record.getRow();
      short col = this._record.getColumn();
      short styleIndex = this._record.getXFIndex();
      switch (this._cellType) {
         case FORMULA:
            ((FormulaRecordAggregate)this._record).setCachedBooleanResult(value);
            break;
         default:
            this.setCellType(CellType.BOOLEAN, false, row, col, styleIndex);
         case BOOLEAN:
            ((BoolErrRecord)this._record).setValue(value);
      }
   }

   /** @deprecated */
   public void setCellErrorValue(byte errorCode) {
      FormulaError error = FormulaError.forInt(errorCode);
      this.setCellErrorValue(error);
   }

   public void setCellErrorValue(FormulaError error) {
      int row = this._record.getRow();
      short col = this._record.getColumn();
      short styleIndex = this._record.getXFIndex();
      switch (this._cellType) {
         case FORMULA:
            ((FormulaRecordAggregate)this._record).setCachedErrorResult(error.getCode());
            break;
         default:
            this.setCellType(CellType.ERROR, false, row, col, styleIndex);
         case ERROR:
            ((BoolErrRecord)this._record).setValue(error);
      }
   }

   private boolean convertCellValueToBoolean() {
      switch (this._cellType) {
         case STRING:
            int sstIndex = ((LabelSSTRecord)this._record).getSSTIndex();
            String text = this._book.getWorkbook().getSSTString(sstIndex).getString();
            return Boolean.valueOf(text);
         case BLANK:
         case ERROR:
            return false;
         case FORMULA:
            FormulaRecord fr = ((FormulaRecordAggregate)this._record).getFormulaRecord();
            checkFormulaCachedValueType(CellType.BOOLEAN, fr);
            return fr.getCachedBooleanValue();
         case NUMERIC:
            return ((NumberRecord)this._record).getValue() != 0.0;
         case BOOLEAN:
            return ((BoolErrRecord)this._record).getBooleanValue();
         default:
            throw new RuntimeException("Unexpected cell type (" + this._cellType + ")");
      }
   }

   private String convertCellValueToString() {
      switch (this._cellType) {
         case STRING:
            int sstIndex = ((LabelSSTRecord)this._record).getSSTIndex();
            return this._book.getWorkbook().getSSTString(sstIndex).getString();
         case BLANK:
            return "";
         case FORMULA:
            FormulaRecordAggregate fra = (FormulaRecordAggregate)this._record;
            FormulaRecord fr = fra.getFormulaRecord();
            switch (CellType.forInt(fr.getCachedResultType())) {
               case STRING:
                  return fra.getStringValue();
               case BLANK:
               case FORMULA:
               default:
                  throw new IllegalStateException("Unexpected formula result type (" + this._cellType + ")");
               case NUMERIC:
                  return NumberToTextConverter.toText(fr.getValue());
               case BOOLEAN:
                  return fr.getCachedBooleanValue() ? "TRUE" : "FALSE";
               case ERROR:
                  return FormulaError.forInt(fr.getCachedErrorValue()).getString();
            }
         case NUMERIC:
            return NumberToTextConverter.toText(((NumberRecord)this._record).getValue());
         case BOOLEAN:
            return ((BoolErrRecord)this._record).getBooleanValue() ? "TRUE" : "FALSE";
         case ERROR:
            return FormulaError.forInt(((BoolErrRecord)this._record).getErrorValue()).getString();
         default:
            throw new IllegalStateException("Unexpected cell type (" + this._cellType + ")");
      }
   }

   public boolean getBooleanCellValue() {
      switch (this._cellType) {
         case BLANK:
            return false;
         case FORMULA:
            FormulaRecord fr = ((FormulaRecordAggregate)this._record).getFormulaRecord();
            checkFormulaCachedValueType(CellType.BOOLEAN, fr);
            return fr.getCachedBooleanValue();
         case NUMERIC:
         default:
            throw typeMismatch(CellType.BOOLEAN, this._cellType, false);
         case BOOLEAN:
            return ((BoolErrRecord)this._record).getBooleanValue();
      }
   }

   public byte getErrorCellValue() {
      switch (this._cellType) {
         case FORMULA:
            FormulaRecord fr = ((FormulaRecordAggregate)this._record).getFormulaRecord();
            checkFormulaCachedValueType(CellType.ERROR, fr);
            return (byte)fr.getCachedErrorValue();
         case ERROR:
            return ((BoolErrRecord)this._record).getErrorValue();
         default:
            throw typeMismatch(CellType.ERROR, this._cellType, false);
      }
   }

   public void setCellStyle(CellStyle style) {
      this.setCellStyle((HSSFCellStyle)style);
   }

   public void setCellStyle(HSSFCellStyle style) {
      if (style == null) {
         this._record.setXFIndex((short)15);
      } else {
         style.verifyBelongsToWorkbook(this._book);
         short styleIndex;
         if (style.getUserStyleName() != null) {
            styleIndex = this.applyUserCellStyle(style);
         } else {
            styleIndex = style.getIndex();
         }

         this._record.setXFIndex(styleIndex);
      }
   }

   public HSSFCellStyle getCellStyle() {
      short styleIndex = this._record.getXFIndex();
      ExtendedFormatRecord xf = this._book.getWorkbook().getExFormatAt(styleIndex);
      return new HSSFCellStyle(styleIndex, xf, this._book);
   }

   protected CellValueRecordInterface getCellValueRecord() {
      return this._record;
   }

   private static void checkBounds(int cellIndex) {
      if (cellIndex < 0 || cellIndex > LAST_COLUMN_NUMBER) {
         throw new IllegalArgumentException(
            "Invalid column index ("
               + cellIndex
               + ").  Allowable column range for "
               + "BIFF8"
               + " is (0.."
               + LAST_COLUMN_NUMBER
               + ") or ('A'..'"
               + LAST_COLUMN_NAME
               + "')"
         );
      }
   }

   public void setAsActiveCell() {
      int row = this._record.getRow();
      short col = this._record.getColumn();
      this._sheet.getSheet().setActiveCellRow(row);
      this._sheet.getSheet().setActiveCellCol(col);
   }

   public String toString() {
      switch (this.getCellTypeEnum()) {
         case STRING:
            return this.getStringCellValue();
         case BLANK:
            return "";
         case FORMULA:
            return this.getCellFormula();
         case NUMERIC:
            if (HSSFDateUtil.isCellDateFormatted(this)) {
               SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", LocaleUtil.getUserLocale());
               sdf.setTimeZone(LocaleUtil.getUserTimeZone());
               return sdf.format(this.getDateCellValue());
            }

            return String.valueOf(this.getNumericCellValue());
         case BOOLEAN:
            return this.getBooleanCellValue() ? "TRUE" : "FALSE";
         case ERROR:
            return ErrorEval.getText(((BoolErrRecord)this._record).getErrorValue());
         default:
            return "Unknown Cell Type: " + this.getCellType();
      }
   }

   public void setCellComment(Comment comment) {
      if (comment == null) {
         this.removeCellComment();
      } else {
         comment.setRow(this._record.getRow());
         comment.setColumn(this._record.getColumn());
         this._comment = (HSSFComment)comment;
      }
   }

   public HSSFComment getCellComment() {
      if (this._comment == null) {
         this._comment = this._sheet.findCellComment(this._record.getRow(), this._record.getColumn());
      }

      return this._comment;
   }

   public void removeCellComment() {
      HSSFComment comment = this._sheet.findCellComment(this._record.getRow(), this._record.getColumn());
      this._comment = null;
      if (null != comment) {
         this._sheet.getDrawingPatriarch().removeShape(comment);
      }
   }

   public HSSFHyperlink getHyperlink() {
      return this._sheet.getHyperlink(this._record.getRow(), this._record.getColumn());
   }

   public void setHyperlink(Hyperlink hyperlink) {
      if (hyperlink == null) {
         this.removeHyperlink();
      } else {
         HSSFHyperlink link = (HSSFHyperlink)hyperlink;
         link.setFirstRow(this._record.getRow());
         link.setLastRow(this._record.getRow());
         link.setFirstColumn(this._record.getColumn());
         link.setLastColumn(this._record.getColumn());
         switch (link.getTypeEnum()) {
            case EMAIL:
            case URL:
               link.setLabel("url");
               break;
            case FILE:
               link.setLabel("file");
               break;
            case DOCUMENT:
               link.setLabel("place");
         }

         List<RecordBase> records = this._sheet.getSheet().getRecords();
         int eofLoc = records.size() - 1;
         records.add(eofLoc, link.record);
      }
   }

   public void removeHyperlink() {
      Iterator<RecordBase> it = this._sheet.getSheet().getRecords().iterator();

      while (it.hasNext()) {
         RecordBase rec = (RecordBase)it.next();
         if (rec instanceof HyperlinkRecord) {
            HyperlinkRecord link = (HyperlinkRecord)rec;
            if (link.getFirstColumn() == this._record.getColumn() && link.getFirstRow() == this._record.getRow()) {
               it.remove();
               return;
            }
         }
      }
   }

   public CellType getCachedFormulaResultType() {
      if (this._cellType != CellType.FORMULA) {
         throw new IllegalStateException("Only formula cells have cached results");
      }

      int code = ((FormulaRecordAggregate)this._record).getFormulaRecord().getCachedResultType();
      return CellType.forInt(code);
   }

   @Deprecated
   @Removal(version = "4.2")
   public CellType getCachedFormulaResultTypeEnum() {
      return this.getCachedFormulaResultType();
   }

   void setCellArrayFormula(CellRangeAddress range) {
      int row = this._record.getRow();
      short col = this._record.getColumn();
      short styleIndex = this._record.getXFIndex();
      this.setCellType(CellType.FORMULA, false, row, col, styleIndex);
      Ptg[] ptgsForCell = new Ptg[]{new ExpPtg(range.getFirstRow(), range.getFirstColumn())};
      FormulaRecordAggregate agg = (FormulaRecordAggregate)this._record;
      agg.setParsedExpression(ptgsForCell);
   }

   public CellRangeAddress getArrayFormulaRange() {
      if (this._cellType != CellType.FORMULA) {
         String ref = new CellReference(this).formatAsString();
         throw new IllegalStateException("Cell " + ref + " is not part of an array formula.");
      } else {
         return ((FormulaRecordAggregate)this._record).getArrayFormulaRange();
      }
   }

   public boolean isPartOfArrayFormulaGroup() {
      return this._cellType == CellType.FORMULA && ((FormulaRecordAggregate)this._record).isPartOfArrayFormula();
   }

   void notifyArrayFormulaChanging(String msg) {
      CellRangeAddress cra = this.getArrayFormulaRange();
      if (cra.getNumberOfCells() > 1) {
         throw new IllegalStateException(msg);
      }

      this.getRow().getSheet().removeArrayFormula(this);
   }

   void notifyArrayFormulaChanging() {
      CellReference ref = new CellReference(this);
      String msg = "Cell " + ref.formatAsString() + " is part of a multi-cell array formula. You cannot change part of an array.";
      this.notifyArrayFormulaChanging(msg);
   }

   private short applyUserCellStyle(HSSFCellStyle style) {
      if (style.getUserStyleName() == null) {
         throw new IllegalArgumentException("Expected user-defined style");
      }

      InternalWorkbook iwb = this._book.getWorkbook();
      short userXf = -1;
      int numfmt = iwb.getNumExFormats();

      for (short i = 0; i < numfmt; i++) {
         ExtendedFormatRecord xf = iwb.getExFormatAt(i);
         if (xf.getXFType() == 0 && xf.getParentIndex() == style.getIndex()) {
            userXf = i;
            break;
         }
      }

      short styleIndex;
      if (userXf == -1) {
         ExtendedFormatRecord xfr = iwb.createCellXF();
         xfr.cloneStyleFrom(iwb.getExFormatAt(style.getIndex()));
         xfr.setIndentionOptions((short)0);
         xfr.setXFType((short)0);
         xfr.setParentIndex(style.getIndex());
         styleIndex = (short)numfmt;
      } else {
         styleIndex = userXf;
      }

      return styleIndex;
   }
}
