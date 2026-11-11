package org.apache.poi.ss.formula;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment.WorkbookNotFoundException;
import org.apache.poi.ss.formula.EvaluationWorkbook.ExternalName;
import org.apache.poi.ss.formula.EvaluationWorkbook.ExternalSheet;
import org.apache.poi.ss.formula.EvaluationWorkbook.ExternalSheetRange;
import org.apache.poi.ss.formula.constant.ErrorConstant;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.ExternalNameEval;
import org.apache.poi.ss.formula.eval.FunctionNameEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.ptg.Area3DPtg;
import org.apache.poi.ss.formula.ptg.Area3DPxg;
import org.apache.poi.ss.formula.ptg.NameXPtg;
import org.apache.poi.ss.formula.ptg.NameXPxg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.Ref3DPtg;
import org.apache.poi.ss.formula.ptg.Ref3DPxg;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.CellReference.NameType;

public final class OperationEvaluationContext {
   public static final FreeRefFunction UDF = UserDefinedFunction.instance;
   private final EvaluationWorkbook _workbook;
   private final int _sheetIndex;
   private final int _rowIndex;
   private final int _columnIndex;
   private final EvaluationTracker _tracker;
   private final WorkbookEvaluator _bookEvaluator;
   private final boolean _isSingleValue;
   private boolean _isInArrayContext;

   public OperationEvaluationContext(
      WorkbookEvaluator bookEvaluator, EvaluationWorkbook workbook, int sheetIndex, int srcRowNum, int srcColNum, EvaluationTracker tracker
   ) {
      this(bookEvaluator, workbook, sheetIndex, srcRowNum, srcColNum, tracker, true);
   }

   public OperationEvaluationContext(
      WorkbookEvaluator bookEvaluator,
      EvaluationWorkbook workbook,
      int sheetIndex,
      int srcRowNum,
      int srcColNum,
      EvaluationTracker tracker,
      boolean isSingleValue
   ) {
      this._bookEvaluator = bookEvaluator;
      this._workbook = workbook;
      this._sheetIndex = sheetIndex;
      this._rowIndex = srcRowNum;
      this._columnIndex = srcColNum;
      this._tracker = tracker;
      this._isSingleValue = isSingleValue;
   }

   public boolean isArraymode() {
      return this._isInArrayContext;
   }

   public void setArrayMode(boolean value) {
      this._isInArrayContext = value;
   }

   public EvaluationWorkbook getWorkbook() {
      return this._workbook;
   }

   public int getRowIndex() {
      return this._rowIndex;
   }

   public int getColumnIndex() {
      return this._columnIndex;
   }

   SheetRangeEvaluator createExternSheetRefEvaluator(ExternSheetReferenceToken ptg) {
      return this.createExternSheetRefEvaluator(ptg.getExternSheetIndex());
   }

   SheetRangeEvaluator createExternSheetRefEvaluator(String firstSheetName, String lastSheetName, int externalWorkbookNumber) {
      ExternalSheet externalSheet = this._workbook.getExternalSheet(firstSheetName, lastSheetName, externalWorkbookNumber);
      return this.createExternSheetRefEvaluator(externalSheet);
   }

   SheetRangeEvaluator createExternSheetRefEvaluator(int externSheetIndex) {
      ExternalSheet externalSheet = this._workbook.getExternalSheet(externSheetIndex);
      return this.createExternSheetRefEvaluator(externalSheet);
   }

   SheetRangeEvaluator createExternSheetRefEvaluator(ExternalSheet externalSheet) {
      int otherLastSheetIndex = -1;
      WorkbookEvaluator targetEvaluator;
      int otherFirstSheetIndex;
      if (externalSheet != null && externalSheet.getWorkbookName() != null) {
         String workbookName = externalSheet.getWorkbookName();

         try {
            targetEvaluator = this._bookEvaluator.getOtherWorkbookEvaluator(workbookName);
         } catch (WorkbookNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
         }

         otherFirstSheetIndex = targetEvaluator.getSheetIndex(externalSheet.getSheetName());
         if (externalSheet instanceof ExternalSheetRange) {
            String lastSheetName = ((ExternalSheetRange)externalSheet).getLastSheetName();
            otherLastSheetIndex = targetEvaluator.getSheetIndex(lastSheetName);
         }

         if (otherFirstSheetIndex < 0) {
            throw new RuntimeException("Invalid sheet name '" + externalSheet.getSheetName() + "' in bool '" + workbookName + "'.");
         }
      } else {
         targetEvaluator = this._bookEvaluator;
         if (externalSheet == null) {
            otherFirstSheetIndex = 0;
         } else {
            otherFirstSheetIndex = this._workbook.getSheetIndex(externalSheet.getSheetName());
         }

         if (externalSheet instanceof ExternalSheetRange) {
            String lastSheetName = ((ExternalSheetRange)externalSheet).getLastSheetName();
            otherLastSheetIndex = this._workbook.getSheetIndex(lastSheetName);
         }
      }

      if (otherLastSheetIndex == -1) {
         otherLastSheetIndex = otherFirstSheetIndex;
      }

      SheetRefEvaluator[] evals = new SheetRefEvaluator[otherLastSheetIndex - otherFirstSheetIndex + 1];

      for (int i = 0; i < evals.length; i++) {
         int otherSheetIndex = i + otherFirstSheetIndex;
         evals[i] = new SheetRefEvaluator(targetEvaluator, this._tracker, otherSheetIndex);
      }

      return new SheetRangeEvaluator(otherFirstSheetIndex, otherLastSheetIndex, evals);
   }

   private SheetRefEvaluator createExternSheetRefEvaluator(String workbookName, String sheetName) {
      WorkbookEvaluator targetEvaluator;
      if (workbookName == null) {
         targetEvaluator = this._bookEvaluator;
      } else {
         if (sheetName == null) {
            throw new IllegalArgumentException("sheetName must not be null if workbookName is provided");
         }

         try {
            targetEvaluator = this._bookEvaluator.getOtherWorkbookEvaluator(workbookName);
         } catch (WorkbookNotFoundException e) {
            return null;
         }
      }

      int otherSheetIndex = sheetName == null ? this._sheetIndex : targetEvaluator.getSheetIndex(sheetName);
      return otherSheetIndex < 0 ? null : new SheetRefEvaluator(targetEvaluator, this._tracker, otherSheetIndex);
   }

   public SheetRangeEvaluator getRefEvaluatorForCurrentSheet() {
      SheetRefEvaluator sre = new SheetRefEvaluator(this._bookEvaluator, this._tracker, this._sheetIndex);
      return new SheetRangeEvaluator(this._sheetIndex, sre);
   }

   public ValueEval getDynamicReference(String workbookName, String sheetName, String refStrPart1, String refStrPart2, boolean isA1Style) {
      if (!isA1Style) {
         throw new RuntimeException("R1C1 style not supported yet");
      }

      SheetRefEvaluator se = this.createExternSheetRefEvaluator(workbookName, sheetName);
      if (se == null) {
         return ErrorEval.REF_INVALID;
      }

      SheetRangeEvaluator sre = new SheetRangeEvaluator(this._sheetIndex, se);
      SpreadsheetVersion ssVersion = this._workbook.getSpreadsheetVersion();
      NameType part1refType = classifyCellReference(refStrPart1, ssVersion);
      switch (part1refType) {
         case BAD_CELL_OR_NAMED_RANGE:
            return ErrorEval.REF_INVALID;
         case NAMED_RANGE:
            EvaluationName nm = this._workbook.getName(refStrPart1, this._sheetIndex);
            if (!nm.isRange()) {
               throw new RuntimeException("Specified name '" + refStrPart1 + "' is not a range as expected.");
            }

            return this._bookEvaluator.evaluateNameFormula(nm.getNameDefinition(), this);
         default:
            if (refStrPart2 == null) {
               switch (part1refType) {
                  case COLUMN:
                  case ROW:
                     return ErrorEval.REF_INVALID;
                  case CELL:
                     CellReference cr = new CellReference(refStrPart1);
                     return new LazyRefEval(cr.getRow(), cr.getCol(), sre);
                  default:
                     throw new IllegalStateException("Unexpected reference classification of '" + refStrPart1 + "'.");
               }
            } else {
               NameType part2refType = classifyCellReference(refStrPart1, ssVersion);
               switch (part2refType) {
                  case BAD_CELL_OR_NAMED_RANGE:
                     return ErrorEval.REF_INVALID;
                  case NAMED_RANGE:
                     throw new RuntimeException("Cannot evaluate '" + refStrPart1 + "'. Indirect evaluation of defined names not supported yet");
                  default:
                     if (part2refType != part1refType) {
                        return ErrorEval.REF_INVALID;
                     } else {
                        int firstRow;
                        int firstCol;
                        int lastRow;
                        int lastCol;
                        switch (part1refType) {
                           case COLUMN:
                              firstRow = 0;
                              if (part2refType.equals(NameType.COLUMN)) {
                                 lastRow = ssVersion.getLastRowIndex();
                                 firstCol = parseRowRef(refStrPart1);
                                 lastCol = parseRowRef(refStrPart2);
                              } else {
                                 lastRow = ssVersion.getLastRowIndex();
                                 firstCol = parseColRef(refStrPart1);
                                 lastCol = parseColRef(refStrPart2);
                              }
                              break;
                           case ROW:
                              firstCol = 0;
                              if (part2refType.equals(NameType.ROW)) {
                                 firstRow = parseColRef(refStrPart1);
                                 lastRow = parseColRef(refStrPart2);
                                 lastCol = ssVersion.getLastColumnIndex();
                              } else {
                                 lastCol = ssVersion.getLastColumnIndex();
                                 firstRow = parseRowRef(refStrPart1);
                                 lastRow = parseRowRef(refStrPart2);
                              }
                              break;
                           case CELL:
                              CellReference cr = new CellReference(refStrPart1);
                              firstRow = cr.getRow();
                              firstCol = cr.getCol();
                              cr = new CellReference(refStrPart2);
                              lastRow = cr.getRow();
                              lastCol = cr.getCol();
                              break;
                           default:
                              throw new IllegalStateException("Unexpected reference classification of '" + refStrPart1 + "'.");
                        }

                        return new LazyAreaEval(firstRow, firstCol, lastRow, lastCol, sre);
                     }
               }
            }
      }
   }

   private static int parseRowRef(String refStrPart) {
      return CellReference.convertColStringToIndex(refStrPart);
   }

   private static int parseColRef(String refStrPart) {
      return Integer.parseInt(refStrPart) - 1;
   }

   private static NameType classifyCellReference(String str, SpreadsheetVersion ssVersion) {
      int len = str.length();
      return len < 1 ? NameType.BAD_CELL_OR_NAMED_RANGE : CellReference.classifyCellReference(str, ssVersion);
   }

   public FreeRefFunction findUserDefinedFunction(String functionName) {
      return this._bookEvaluator.findUserDefinedFunction(functionName);
   }

   public ValueEval getRefEval(int rowIndex, int columnIndex) {
      SheetRangeEvaluator sre = this.getRefEvaluatorForCurrentSheet();
      return new LazyRefEval(rowIndex, columnIndex, sre);
   }

   public ValueEval getRef3DEval(Ref3DPtg rptg) {
      SheetRangeEvaluator sre = this.createExternSheetRefEvaluator(rptg.getExternSheetIndex());
      return new LazyRefEval(rptg.getRow(), rptg.getColumn(), sre);
   }

   public ValueEval getRef3DEval(Ref3DPxg rptg) {
      SheetRangeEvaluator sre = this.createExternSheetRefEvaluator(rptg.getSheetName(), rptg.getLastSheetName(), rptg.getExternalWorkbookNumber());
      return new LazyRefEval(rptg.getRow(), rptg.getColumn(), sre);
   }

   public ValueEval getAreaEval(int firstRowIndex, int firstColumnIndex, int lastRowIndex, int lastColumnIndex) {
      SheetRangeEvaluator sre = this.getRefEvaluatorForCurrentSheet();
      return new LazyAreaEval(firstRowIndex, firstColumnIndex, lastRowIndex, lastColumnIndex, sre);
   }

   public ValueEval getArea3DEval(Area3DPtg aptg) {
      SheetRangeEvaluator sre = this.createExternSheetRefEvaluator(aptg.getExternSheetIndex());
      return new LazyAreaEval(aptg.getFirstRow(), aptg.getFirstColumn(), aptg.getLastRow(), aptg.getLastColumn(), sre);
   }

   public ValueEval getArea3DEval(Area3DPxg aptg) {
      SheetRangeEvaluator sre = this.createExternSheetRefEvaluator(aptg.getSheetName(), aptg.getLastSheetName(), aptg.getExternalWorkbookNumber());
      return new LazyAreaEval(aptg.getFirstRow(), aptg.getFirstColumn(), aptg.getLastRow(), aptg.getLastColumn(), sre);
   }

   public ValueEval getAreaValueEval(int firstRowIndex, int firstColumnIndex, int lastRowIndex, int lastColumnIndex, Object[][] tokens) {
      ValueEval[] values = new ValueEval[tokens.length * tokens[0].length];
      int index = 0;

      for (int jdx = 0; jdx < tokens.length; jdx++) {
         for (int idx = 0; idx < tokens[0].length; idx++) {
            values[index++] = this.convertObjectEval(tokens[jdx][idx]);
         }
      }

      return new CacheAreaEval(firstRowIndex, firstColumnIndex, lastRowIndex, lastColumnIndex, values);
   }

   private ValueEval convertObjectEval(Object token) {
      if (token == null) {
         throw new RuntimeException("Array item cannot be null");
      } else if (token instanceof String) {
         return new StringEval((String)token);
      } else if (token instanceof Double) {
         return new NumberEval((Double)token);
      } else if (token instanceof Boolean) {
         return BoolEval.valueOf((Boolean)token);
      } else if (token instanceof ErrorConstant) {
         return ErrorEval.valueOf(((ErrorConstant)token).getErrorCode());
      } else {
         throw new IllegalArgumentException("Unexpected constant class (" + token.getClass().getName() + ")");
      }
   }

   public ValueEval getNameXEval(NameXPtg nameXPtg) {
      ExternalSheet externSheet = this._workbook.getExternalSheet(nameXPtg.getSheetRefIndex());
      if (externSheet != null && externSheet.getWorkbookName() != null) {
         String workbookName = externSheet.getWorkbookName();
         ExternalName externName = this._workbook.getExternalName(nameXPtg.getSheetRefIndex(), nameXPtg.getNameIndex());
         return this.getExternalNameXEval(externName, workbookName);
      } else {
         return this.getLocalNameXEval(nameXPtg);
      }
   }

   public ValueEval getNameXEval(NameXPxg nameXPxg) {
      ExternalSheet externSheet = this._workbook.getExternalSheet(nameXPxg.getSheetName(), null, nameXPxg.getExternalWorkbookNumber());
      if (externSheet != null && externSheet.getWorkbookName() != null) {
         String workbookName = externSheet.getWorkbookName();
         ExternalName externName = this._workbook.getExternalName(nameXPxg.getNameName(), nameXPxg.getSheetName(), nameXPxg.getExternalWorkbookNumber());
         return this.getExternalNameXEval(externName, workbookName);
      } else {
         return this.getLocalNameXEval(nameXPxg);
      }
   }

   private ValueEval getLocalNameXEval(NameXPxg nameXPxg) {
      int sIdx = -1;
      if (nameXPxg.getSheetName() != null) {
         sIdx = this._workbook.getSheetIndex(nameXPxg.getSheetName());
      }

      String name = nameXPxg.getNameName();
      EvaluationName evalName = this._workbook.getName(name, sIdx);
      return (ValueEval)(evalName != null ? new ExternalNameEval(evalName) : new FunctionNameEval(name));
   }

   private ValueEval getLocalNameXEval(NameXPtg nameXPtg) {
      String name = this._workbook.resolveNameXText(nameXPtg);
      int sheetNameAt = name.indexOf(33);
      EvaluationName evalName = null;
      if (sheetNameAt > -1) {
         String sheetName = name.substring(0, sheetNameAt);
         String nameName = name.substring(sheetNameAt + 1);
         evalName = this._workbook.getName(nameName, this._workbook.getSheetIndex(sheetName));
      } else {
         evalName = this._workbook.getName(name, -1);
      }

      return (ValueEval)(evalName != null ? new ExternalNameEval(evalName) : new FunctionNameEval(name));
   }

   public int getSheetIndex() {
      return this._sheetIndex;
   }

   public boolean isSingleValue() {
      return this._isSingleValue;
   }

   private ValueEval getExternalNameXEval(ExternalName externName, String workbookName) {
      try {
         WorkbookEvaluator refWorkbookEvaluator = this._bookEvaluator.getOtherWorkbookEvaluator(workbookName);
         EvaluationName evaluationName = refWorkbookEvaluator.getName(externName.getName(), externName.getIx() - 1);
         if (evaluationName != null && evaluationName.hasFormula()) {
            if (evaluationName.getNameDefinition().length > 1) {
               throw new RuntimeException("Complex name formulas not supported yet");
            }

            OperationEvaluationContext refWorkbookContext = new OperationEvaluationContext(
               refWorkbookEvaluator, refWorkbookEvaluator.getWorkbook(), -1, -1, -1, this._tracker
            );
            Ptg ptg = evaluationName.getNameDefinition()[0];
            if (ptg instanceof Ref3DPtg) {
               Ref3DPtg ref3D = (Ref3DPtg)ptg;
               return refWorkbookContext.getRef3DEval(ref3D);
            }

            if (ptg instanceof Ref3DPxg) {
               Ref3DPxg ref3D = (Ref3DPxg)ptg;
               return refWorkbookContext.getRef3DEval(ref3D);
            }

            if (ptg instanceof Area3DPtg) {
               Area3DPtg area3D = (Area3DPtg)ptg;
               return refWorkbookContext.getArea3DEval(area3D);
            }

            if (ptg instanceof Area3DPxg) {
               Area3DPxg area3D = (Area3DPxg)ptg;
               return refWorkbookContext.getArea3DEval(area3D);
            }
         }

         return ErrorEval.REF_INVALID;
      } catch (WorkbookNotFoundException wnfe) {
         return ErrorEval.REF_INVALID;
      }
   }
}
