package org.apache.poi.ss.formula;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.constant.ErrorConstant;
import org.apache.poi.ss.formula.function.FunctionMetadata;
import org.apache.poi.ss.formula.function.FunctionMetadataRegistry;
import org.apache.poi.ss.formula.ptg.AbstractFunctionPtg;
import org.apache.poi.ss.formula.ptg.AddPtg;
import org.apache.poi.ss.formula.ptg.Area3DPxg;
import org.apache.poi.ss.formula.ptg.AreaPtg;
import org.apache.poi.ss.formula.ptg.ArrayPtg;
import org.apache.poi.ss.formula.ptg.AttrPtg;
import org.apache.poi.ss.formula.ptg.BoolPtg;
import org.apache.poi.ss.formula.ptg.ConcatPtg;
import org.apache.poi.ss.formula.ptg.DividePtg;
import org.apache.poi.ss.formula.ptg.EqualPtg;
import org.apache.poi.ss.formula.ptg.ErrPtg;
import org.apache.poi.ss.formula.ptg.FuncPtg;
import org.apache.poi.ss.formula.ptg.FuncVarPtg;
import org.apache.poi.ss.formula.ptg.GreaterEqualPtg;
import org.apache.poi.ss.formula.ptg.GreaterThanPtg;
import org.apache.poi.ss.formula.ptg.IntPtg;
import org.apache.poi.ss.formula.ptg.IntersectionPtg;
import org.apache.poi.ss.formula.ptg.LessEqualPtg;
import org.apache.poi.ss.formula.ptg.LessThanPtg;
import org.apache.poi.ss.formula.ptg.MemAreaPtg;
import org.apache.poi.ss.formula.ptg.MemFuncPtg;
import org.apache.poi.ss.formula.ptg.MissingArgPtg;
import org.apache.poi.ss.formula.ptg.MultiplyPtg;
import org.apache.poi.ss.formula.ptg.NamePtg;
import org.apache.poi.ss.formula.ptg.NameXPtg;
import org.apache.poi.ss.formula.ptg.NameXPxg;
import org.apache.poi.ss.formula.ptg.NotEqualPtg;
import org.apache.poi.ss.formula.ptg.NumberPtg;
import org.apache.poi.ss.formula.ptg.OperandPtg;
import org.apache.poi.ss.formula.ptg.OperationPtg;
import org.apache.poi.ss.formula.ptg.ParenthesisPtg;
import org.apache.poi.ss.formula.ptg.PercentPtg;
import org.apache.poi.ss.formula.ptg.PowerPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RangePtg;
import org.apache.poi.ss.formula.ptg.RefPtg;
import org.apache.poi.ss.formula.ptg.StringPtg;
import org.apache.poi.ss.formula.ptg.SubtractPtg;
import org.apache.poi.ss.formula.ptg.UnaryMinusPtg;
import org.apache.poi.ss.formula.ptg.UnaryPlusPtg;
import org.apache.poi.ss.formula.ptg.UnionPtg;
import org.apache.poi.ss.formula.ptg.ValueOperatorPtg;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Table;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.CellReference.NameType;
import org.apache.poi.util.Internal;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

@Internal
public final class FormulaParser {
   private static final POILogger log = POILogFactory.getLogger(FormulaParser.class);
   private final String _formulaString;
   private final int _formulaLength;
   private int _pointer;
   private ParseNode _rootNode;
   private static final char TAB = '\t';
   private static final char CR = '\r';
   private static final char LF = '\n';
   private int look;
   private boolean _inIntersection;
   private final FormulaParsingWorkbook _book;
   private final SpreadsheetVersion _ssVersion;
   private final int _sheetIndex;
   private final int _rowIndex;
   private static final String specHeaders = "Headers";
   private static final String specAll = "All";
   private static final String specData = "Data";
   private static final String specTotals = "Totals";
   private static final String specThisRow = "This Row";
   private static final Pattern CELL_REF_PATTERN = Pattern.compile("(\\$?[A-Za-z]+)?(\\$?[0-9]+)?");

   private FormulaParser(String formula, FormulaParsingWorkbook book, int sheetIndex, int rowIndex) {
      this._formulaString = formula;
      this._pointer = 0;
      this._book = book;
      this._ssVersion = book == null ? SpreadsheetVersion.EXCEL97 : book.getSpreadsheetVersion();
      this._formulaLength = this._formulaString.length();
      this._sheetIndex = sheetIndex;
      this._rowIndex = rowIndex;
   }

   public static Ptg[] parse(String formula, FormulaParsingWorkbook workbook, FormulaType formulaType, int sheetIndex, int rowIndex) {
      FormulaParser fp = new FormulaParser(formula, workbook, sheetIndex, rowIndex);
      fp.parse();
      return fp.getRPNPtg(formulaType);
   }

   public static Ptg[] parse(String formula, FormulaParsingWorkbook workbook, FormulaType formulaType, int sheetIndex) {
      return parse(formula, workbook, formulaType, sheetIndex, -1);
   }

   public static Area3DPxg parseStructuredReference(String tableText, FormulaParsingWorkbook workbook, int rowIndex) {
      int sheetIndex = -1;
      Ptg[] arr = parse(tableText, workbook, FormulaType.CELL, -1, rowIndex);
      if (arr.length == 1 && arr[0] instanceof Area3DPxg) {
         return (Area3DPxg)arr[0];
      } else {
         throw new IllegalStateException("Illegal structured reference");
      }
   }

   private void GetChar() {
      if (IsWhite(this.look)) {
         if (this.look == 32) {
            this._inIntersection = true;
         }
      } else {
         this._inIntersection = false;
      }

      if (this._pointer > this._formulaLength) {
         throw new RuntimeException("too far");
      }

      if (this._pointer < this._formulaLength) {
         this.look = this._formulaString.codePointAt(this._pointer);
      } else {
         this.look = 0;
         this._inIntersection = false;
      }

      this._pointer = this._pointer + Character.charCount(this.look);
   }

   private void resetPointer(int ptr) {
      this._pointer = ptr;
      if (this._pointer <= this._formulaLength) {
         this.look = this._formulaString.codePointAt(this._pointer - Character.charCount(this.look));
      } else {
         this.look = 0;
      }
   }

   private RuntimeException expected(String s) {
      String msg;
      if (this.look == 61 && this._formulaString.substring(0, this._pointer - 1).trim().length() < 1) {
         msg = "The specified formula '" + this._formulaString + "' starts with an equals sign which is not allowed.";
      } else {
         msg = new StringBuilder("Parse error near char ")
            .append(this._pointer - 1)
            .append(" '")
            .appendCodePoint(this.look)
            .append("'")
            .append(" in specified formula '")
            .append(this._formulaString)
            .append("'. Expected ")
            .append(s)
            .toString();
      }

      return new FormulaParseException(msg);
   }

   private static boolean IsAlpha(int c) {
      return Character.isLetter(c) || c == 36 || c == 95;
   }

   private static boolean IsDigit(int c) {
      return Character.isDigit(c);
   }

   private static boolean IsWhite(int c) {
      return c == 32 || c == 9 || c == 13 || c == 10;
   }

   private void SkipWhite() {
      while (IsWhite(this.look)) {
         this.GetChar();
      }
   }

   private void Match(int x) {
      if (this.look != x) {
         throw this.expected(new StringBuilder().append("'").appendCodePoint(x).append("'").toString());
      }

      this.GetChar();
   }

   private String GetNum() {
      StringBuilder value = new StringBuilder();

      while (IsDigit(this.look)) {
         value.appendCodePoint(this.look);
         this.GetChar();
      }

      return value.length() == 0 ? null : value.toString();
   }

   private ParseNode parseRangeExpression() {
      ParseNode result = this.parseRangeable();

      boolean hasRange;
      for (hasRange = false; this.look == 58; hasRange = true) {
         int pos = this._pointer;
         this.GetChar();
         ParseNode nextPart = this.parseRangeable();
         checkValidRangeOperand("LHS", pos, result);
         checkValidRangeOperand("RHS", pos, nextPart);
         ParseNode[] children = new ParseNode[]{result, nextPart};
         result = new ParseNode(RangePtg.instance, children);
      }

      return hasRange ? augmentWithMemPtg(result) : result;
   }

   private static ParseNode augmentWithMemPtg(ParseNode root) {
      Ptg memPtg;
      if (needsMemFunc(root)) {
         memPtg = new MemFuncPtg(root.getEncodedSize());
      } else {
         memPtg = new MemAreaPtg(root.getEncodedSize());
      }

      return new ParseNode(memPtg, root);
   }

   private static boolean needsMemFunc(ParseNode root) {
      Ptg token = root.getToken();
      if (token instanceof AbstractFunctionPtg) {
         return true;
      }

      if (token instanceof ExternSheetReferenceToken) {
         return true;
      }

      if (!(token instanceof NamePtg) && !(token instanceof NameXPtg)) {
         if (!(token instanceof OperationPtg) && !(token instanceof ParenthesisPtg)) {
            return token instanceof OperandPtg ? false : token instanceof OperationPtg;
         }

         for (ParseNode child : root.getChildren()) {
            if (needsMemFunc(child)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static void checkValidRangeOperand(String sideName, int currentParsePosition, ParseNode pn) {
      if (!isValidRangeOperand(pn)) {
         throw new FormulaParseException("The " + sideName + " of the range operator ':' at position " + currentParsePosition + " is not a proper reference.");
      }
   }

   private static boolean isValidRangeOperand(ParseNode a) {
      Ptg tkn = a.getToken();
      if (tkn instanceof OperandPtg) {
         return true;
      } else if (tkn instanceof AbstractFunctionPtg) {
         AbstractFunctionPtg afp = (AbstractFunctionPtg)tkn;
         byte returnClass = afp.getDefaultOperandClass();
         return 0 == returnClass;
      } else if (tkn instanceof ValueOperatorPtg) {
         return false;
      } else if (tkn instanceof OperationPtg) {
         return true;
      } else {
         return tkn instanceof ParenthesisPtg ? isValidRangeOperand(a.getChildren()[0]) : tkn == ErrPtg.REF_INVALID;
      }
   }

   private ParseNode parseRangeable() {
      this.SkipWhite();
      int savePointer = this._pointer;
      SheetIdentifier sheetIden = this.parseSheetName();
      if (sheetIden == null) {
         this.resetPointer(savePointer);
      } else {
         this.SkipWhite();
         savePointer = this._pointer;
      }

      FormulaParser.SimpleRangePart part1 = this.parseSimpleRangePart();
      if (part1 == null) {
         if (sheetIden != null) {
            if (this.look == 35) {
               return new ParseNode(ErrPtg.valueOf(this.parseErrorLiteral()));
            } else {
               String name = this.parseAsName();
               if (name.length() == 0) {
                  throw new FormulaParseException("Cell reference or Named Range expected after sheet name at index " + this._pointer + ".");
               } else {
                  Ptg nameXPtg = this._book.getNameXPtg(name, sheetIden);
                  if (nameXPtg == null) {
                     throw new FormulaParseException("Specified name '" + name + "' for sheet " + sheetIden.asFormulaString() + " not found");
                  } else {
                     return new ParseNode(nameXPtg);
                  }
               }
            }
         } else {
            return this.parseNonRange(savePointer);
         }
      } else {
         boolean whiteAfterPart1 = IsWhite(this.look);
         if (whiteAfterPart1) {
            this.SkipWhite();
         }

         if (this.look == 58) {
            int colonPos = this._pointer;
            this.GetChar();
            this.SkipWhite();
            FormulaParser.SimpleRangePart part2 = this.parseSimpleRangePart();
            if (part2 != null && !part1.isCompatibleForArea(part2)) {
               part2 = null;
            }

            if (part2 == null) {
               this.resetPointer(colonPos);
               if (!part1.isCell()) {
                  String prefix = "";
                  if (sheetIden != null) {
                     prefix = "'" + sheetIden.getSheetIdentifier().getName() + '!';
                  }

                  throw new FormulaParseException(prefix + part1.getRep() + "' is not a proper reference.");
               }
            }

            return this.createAreaRefParseNode(sheetIden, part1, part2);
         } else if (this.look != 46) {
            if (part1.isCell() && this.isValidCellReference(part1.getRep())) {
               return this.createAreaRefParseNode(sheetIden, part1, null);
            } else if (sheetIden != null) {
               throw new FormulaParseException("Second part of cell reference expected after sheet name at index " + this._pointer + ".");
            } else {
               return this.parseNonRange(savePointer);
            }
         } else {
            this.GetChar();
            int dotCount = 1;

            while (this.look == 46) {
               dotCount++;
               this.GetChar();
            }

            boolean whiteBeforePart2 = IsWhite(this.look);
            this.SkipWhite();
            FormulaParser.SimpleRangePart part2 = this.parseSimpleRangePart();
            String part1And2 = this._formulaString.substring(savePointer - 1, this._pointer - 1);
            if (part2 == null) {
               if (sheetIden != null) {
                  throw new FormulaParseException("Complete area reference expected after sheet name at index " + this._pointer + ".");
               } else {
                  return this.parseNonRange(savePointer);
               }
            } else if (!whiteAfterPart1 && !whiteBeforePart2) {
               if (dotCount == 1 && part1.isRow() && part2.isRow()) {
                  return this.parseNonRange(savePointer);
               } else if ((part1.isRowOrColumn() || part2.isRowOrColumn()) && dotCount != 2) {
                  throw new FormulaParseException("Dotted range (full row or column) expression '" + part1And2 + "' must have exactly 2 dots.");
               } else {
                  return this.createAreaRefParseNode(sheetIden, part1, part2);
               }
            } else if (!part1.isRowOrColumn() && !part2.isRowOrColumn()) {
               return this.createAreaRefParseNode(sheetIden, part1, part2);
            } else {
               throw new FormulaParseException("Dotted range (full row or column) expression '" + part1And2 + "' must not contain whitespace.");
            }
         }
      }
   }

   private ParseNode parseStructuredReference(String tableName) {
      if (!this._ssVersion.equals(SpreadsheetVersion.EXCEL2007)) {
         throw new FormulaParseException("Structured references work only on XSSF (Excel 2007+)!");
      }

      Table tbl = this._book.getTable(tableName);
      if (tbl == null) {
         throw new FormulaParseException("Illegal table name: '" + tableName + "'");
      }

      String sheetName = tbl.getSheetName();
      int startCol = tbl.getStartColIndex();
      int endCol = tbl.getEndColIndex();
      int startRow = tbl.getStartRowIndex();
      int endRow = tbl.getEndRowIndex();
      int savePtr0 = this._pointer;
      this.GetChar();
      boolean isTotalsSpec = false;
      boolean isThisRowSpec = false;
      boolean isDataSpec = false;
      boolean isHeadersSpec = false;
      boolean isAllSpec = false;
      int nSpecQuantifiers = 0;

      while (true) {
         int savePtr1 = this._pointer;
         String specName = this.parseAsSpecialQuantifier();
         if (specName == null) {
            this.resetPointer(savePtr1);
            break;
         }

         if (specName.equals("All")) {
            isAllSpec = true;
         } else if (specName.equals("Data")) {
            isDataSpec = true;
         } else if (specName.equals("Headers")) {
            isHeadersSpec = true;
         } else if (specName.equals("This Row")) {
            isThisRowSpec = true;
         } else {
            if (!specName.equals("Totals")) {
               throw new FormulaParseException("Unknown special quantifier " + specName);
            }

            isTotalsSpec = true;
         }

         nSpecQuantifiers++;
         if (this.look != 44) {
            break;
         }

         this.GetChar();
      }

      boolean isThisRow = false;
      this.SkipWhite();
      if (this.look == 64) {
         isThisRow = true;
         this.GetChar();
      }

      String endColumnName = null;
      int nColQuantifiers = 0;
      int savePtr1 = this._pointer;
      String startColumnName = this.parseAsColumnQuantifier();
      if (startColumnName == null) {
         this.resetPointer(savePtr1);
      } else {
         nColQuantifiers++;
         if (this.look == 44) {
            throw new FormulaParseException("The formula " + this._formulaString + "is illegal: you should not use ',' with column quantifiers");
         }

         if (this.look == 58) {
            this.GetChar();
            endColumnName = this.parseAsColumnQuantifier();
            nColQuantifiers++;
            if (endColumnName == null) {
               throw new FormulaParseException("The formula " + this._formulaString + "is illegal: the string after ':' must be column quantifier");
            }
         }
      }

      if (nColQuantifiers == 0 && nSpecQuantifiers == 0) {
         this.resetPointer(savePtr0);
         savePtr0 = this._pointer;
         startColumnName = this.parseAsColumnQuantifier();
         if (startColumnName != null) {
            nColQuantifiers++;
         } else {
            this.resetPointer(savePtr0);
            String name = this.parseAsSpecialQuantifier();
            if (name == null) {
               throw new FormulaParseException("The formula " + this._formulaString + " is illegal");
            }

            if (name.equals("All")) {
               isAllSpec = true;
            } else if (name.equals("Data")) {
               isDataSpec = true;
            } else if (name.equals("Headers")) {
               isHeadersSpec = true;
            } else if (name.equals("This Row")) {
               isThisRowSpec = true;
            } else {
               if (!name.equals("Totals")) {
                  throw new FormulaParseException("Unknown special quantifier " + name);
               }

               isTotalsSpec = true;
            }

            nSpecQuantifiers++;
         }
      } else {
         this.Match(93);
      }

      if (isTotalsSpec && tbl.getTotalsRowCount() == 0) {
         return new ParseNode(ErrPtg.REF_INVALID);
      }

      if (!isThisRow && !isThisRowSpec || this._rowIndex >= startRow && endRow >= this._rowIndex) {
         int actualStartRow = startRow;
         int actualEndRow = endRow;
         int actualStartCol = startCol;
         int actualEndCol = endCol;
         if (nSpecQuantifiers > 0) {
            if (nSpecQuantifiers != 1 || !isAllSpec) {
               if (isDataSpec && isHeadersSpec) {
                  if (tbl.getTotalsRowCount() > 0) {
                     actualEndRow = endRow - 1;
                  }
               } else if (isDataSpec && isTotalsSpec) {
                  actualStartRow = startRow + 1;
               } else if (nSpecQuantifiers == 1 && isDataSpec) {
                  actualStartRow = startRow + 1;
                  if (tbl.getTotalsRowCount() > 0) {
                     actualEndRow = endRow - 1;
                  }
               } else if (nSpecQuantifiers == 1 && isHeadersSpec) {
                  actualEndRow = actualStartRow;
               } else if (nSpecQuantifiers == 1 && isTotalsSpec) {
                  actualStartRow = actualEndRow;
               } else {
                  if ((nSpecQuantifiers != 1 || !isThisRowSpec) && !isThisRow) {
                     throw new FormulaParseException("The formula " + this._formulaString + " is illegal");
                  }

                  actualStartRow = this._rowIndex;
                  actualEndRow = this._rowIndex;
               }
            }
         } else if (isThisRow) {
            actualStartRow = this._rowIndex;
            actualEndRow = this._rowIndex;
         } else {
            actualStartRow++;
            if (tbl.getTotalsRowCount() > 0) {
               actualEndRow--;
            }
         }

         if (nColQuantifiers == 2) {
            if (startColumnName == null || endColumnName == null) {
               throw new IllegalStateException("Fatal error");
            }

            int startIdx = tbl.findColumnIndex(startColumnName);
            int endIdx = tbl.findColumnIndex(endColumnName);
            if (startIdx == -1 || endIdx == -1) {
               throw new FormulaParseException("One of the columns " + startColumnName + ", " + endColumnName + " doesn't exist in table " + tbl.getName());
            }

            actualStartCol = startCol + startIdx;
            actualEndCol = startCol + endIdx;
         } else if (nColQuantifiers == 1 && !isThisRow) {
            if (startColumnName == null) {
               throw new IllegalStateException("Fatal error");
            }

            int idx = tbl.findColumnIndex(startColumnName);
            if (idx == -1) {
               throw new FormulaParseException("The column " + startColumnName + " doesn't exist in table " + tbl.getName());
            }

            actualStartCol = startCol + idx;
            actualEndCol = actualStartCol;
         }

         CellReference topLeft = new CellReference(actualStartRow, actualStartCol);
         CellReference bottomRight = new CellReference(actualEndRow, actualEndCol);
         SheetIdentifier sheetIden = new SheetIdentifier(null, new NameIdentifier(sheetName, true));
         Ptg ptg = this._book.get3DReferencePtg(new AreaReference(topLeft, bottomRight, this._ssVersion), sheetIden);
         return new ParseNode(ptg);
      } else if (this._rowIndex >= 0) {
         return new ParseNode(ErrPtg.VALUE_INVALID);
      } else {
         throw new FormulaParseException(
            "Formula contained [#This Row] or [@] structured reference but this row < 0. Row index must be specified for row-referencing structured references."
         );
      }
   }

   private String parseAsColumnQuantifier() {
      if (this.look != 91) {
         return null;
      }

      this.GetChar();
      if (this.look == 35) {
         return null;
      }

      if (this.look == 64) {
         this.GetChar();
      }

      StringBuilder name = new StringBuilder();

      while (this.look != 93) {
         name.appendCodePoint(this.look);
         this.GetChar();
      }

      this.Match(93);
      return name.toString();
   }

   private String parseAsSpecialQuantifier() {
      if (this.look != 91) {
         return null;
      }

      this.GetChar();
      if (this.look != 35) {
         return null;
      }

      this.GetChar();
      String name = this.parseAsName();
      if (name.equals("This")) {
         name = name + ' ' + this.parseAsName();
      }

      this.Match(93);
      return name;
   }

   private ParseNode parseNonRange(int savePointer) {
      this.resetPointer(savePointer);
      if (Character.isDigit(this.look)) {
         return new ParseNode(this.parseNumber());
      } else if (this.look == 34) {
         return new ParseNode(new StringPtg(this.parseStringLiteral()));
      } else {
         String name = this.parseAsName();
         if (this.look == 40) {
            return this.function(name);
         } else if (this.look == 91) {
            return this.parseStructuredReference(name);
         } else if (name.equalsIgnoreCase("TRUE") || name.equalsIgnoreCase("FALSE")) {
            return new ParseNode(BoolPtg.valueOf(name.equalsIgnoreCase("TRUE")));
         } else if (this._book == null) {
            throw new IllegalStateException("Need book to evaluate name '" + name + "'");
         } else {
            EvaluationName evalName = this._book.getName(name, this._sheetIndex);
            if (evalName == null) {
               throw new FormulaParseException("Specified named range '" + name + "' does not exist in the current workbook.");
            } else if (evalName.isRange()) {
               return new ParseNode(evalName.createPtg());
            } else {
               throw new FormulaParseException("Specified name '" + name + "' is not a range as expected.");
            }
         }
      }
   }

   private String parseAsName() {
      StringBuilder sb = new StringBuilder();
      if (!Character.isLetter(this.look) && this.look != 95 && this.look != 92) {
         throw this.expected("number, string, defined name, or data table");
      }

      while (isValidDefinedNameChar(this.look)) {
         sb.appendCodePoint(this.look);
         this.GetChar();
      }

      this.SkipWhite();
      return sb.toString();
   }

   private static boolean isValidDefinedNameChar(int ch) {
      if (Character.isLetterOrDigit(ch)) {
         return true;
      }

      if (ch > 128) {
         return true;
      }

      switch (ch) {
         case 46:
         case 63:
         case 92:
         case 95:
            return true;
         default:
            return false;
      }
   }

   private ParseNode createAreaRefParseNode(SheetIdentifier sheetIden, FormulaParser.SimpleRangePart part1, FormulaParser.SimpleRangePart part2) throws FormulaParseException {
      Ptg ptg;
      if (part2 == null) {
         CellReference cr = part1.getCellReference();
         if (sheetIden == null) {
            ptg = new RefPtg(cr);
         } else {
            ptg = this._book.get3DReferencePtg(cr, sheetIden);
         }
      } else {
         AreaReference areaRef = this.createAreaRef(part1, part2);
         if (sheetIden == null) {
            ptg = new AreaPtg(areaRef);
         } else {
            ptg = this._book.get3DReferencePtg(areaRef, sheetIden);
         }
      }

      return new ParseNode(ptg);
   }

   private AreaReference createAreaRef(FormulaParser.SimpleRangePart part1, FormulaParser.SimpleRangePart part2) {
      if (!part1.isCompatibleForArea(part2)) {
         throw new FormulaParseException("has incompatible parts: '" + part1.getRep() + "' and '" + part2.getRep() + "'.");
      } else if (part1.isRow()) {
         return AreaReference.getWholeRow(this._ssVersion, part1.getRep(), part2.getRep());
      } else {
         return part1.isColumn()
            ? AreaReference.getWholeColumn(this._ssVersion, part1.getRep(), part2.getRep())
            : new AreaReference(part1.getCellReference(), part2.getCellReference(), this._ssVersion);
      }
   }

   private FormulaParser.SimpleRangePart parseSimpleRangePart() {
      int ptr = this._pointer - 1;
      boolean hasDigits = false;
      boolean hasLetters = false;

      while (ptr < this._formulaLength) {
         char ch = this._formulaString.charAt(ptr);
         if (Character.isDigit(ch)) {
            hasDigits = true;
         } else if (Character.isLetter(ch)) {
            hasLetters = true;
         } else if (ch != '$' && ch != '_') {
            break;
         }

         ptr++;
      }

      if (ptr <= this._pointer - 1) {
         return null;
      }

      String rep = this._formulaString.substring(this._pointer - 1, ptr);
      if (!CELL_REF_PATTERN.matcher(rep).matches()) {
         return null;
      }

      if (hasLetters && hasDigits) {
         if (!this.isValidCellReference(rep)) {
            return null;
         }
      } else if (hasLetters) {
         if (!CellReference.isColumnWithinRange(rep.replace("$", ""), this._ssVersion)) {
            return null;
         }
      } else {
         if (!hasDigits) {
            return null;
         }

         int i;
         try {
            i = Integer.parseInt(rep.replace("$", ""));
         } catch (NumberFormatException e) {
            return null;
         }

         if (i < 1 || i > this._ssVersion.getMaxRows()) {
            return null;
         }
      }

      this.resetPointer(ptr + 1);
      return new FormulaParser.SimpleRangePart(rep, hasLetters, hasDigits);
   }

   private String getBookName() {
      StringBuilder sb = new StringBuilder();
      this.GetChar();

      while (this.look != 93) {
         sb.appendCodePoint(this.look);
         this.GetChar();
      }

      this.GetChar();
      return sb.toString();
   }

   private SheetIdentifier parseSheetName() {
      String bookName;
      if (this.look == 91) {
         bookName = this.getBookName();
      } else {
         bookName = null;
      }

      if (this.look == 39) {
         this.Match(39);
         if (this.look == 91) {
            bookName = this.getBookName();
         }

         StringBuilder sb = new StringBuilder();
         boolean done = this.look == 39;

         while (!done) {
            sb.appendCodePoint(this.look);
            this.GetChar();
            if (this.look == 39) {
               this.Match(39);
               done = this.look != 39;
            }
         }

         NameIdentifier iden = new NameIdentifier(sb.toString(), true);
         this.SkipWhite();
         if (this.look == 33) {
            this.GetChar();
            return new SheetIdentifier(bookName, iden);
         } else {
            return this.look == 58 ? this.parseSheetRange(bookName, iden) : null;
         }
      } else if (this.look == 95 || Character.isLetter(this.look)) {
         StringBuilder sb = new StringBuilder();

         while (isUnquotedSheetNameChar(this.look)) {
            sb.appendCodePoint(this.look);
            this.GetChar();
         }

         NameIdentifier iden = new NameIdentifier(sb.toString(), false);
         this.SkipWhite();
         if (this.look == 33) {
            this.GetChar();
            return new SheetIdentifier(bookName, iden);
         } else {
            return this.look == 58 ? this.parseSheetRange(bookName, iden) : null;
         }
      } else if (this.look == 33 && bookName != null) {
         this.GetChar();
         return new SheetIdentifier(bookName, null);
      } else {
         return null;
      }
   }

   private SheetIdentifier parseSheetRange(String bookname, NameIdentifier sheet1Name) {
      this.GetChar();
      SheetIdentifier sheet2 = this.parseSheetName();
      return sheet2 != null ? new SheetRangeIdentifier(bookname, sheet1Name, sheet2.getSheetIdentifier()) : null;
   }

   private static boolean isUnquotedSheetNameChar(int ch) {
      if (Character.isLetterOrDigit(ch)) {
         return true;
      }

      if (ch > 128) {
         return true;
      }

      switch (ch) {
         case 46:
         case 95:
            return true;
         default:
            return false;
      }
   }

   private boolean isValidCellReference(String str) {
      boolean result = CellReference.classifyCellReference(str, this._ssVersion) == NameType.CELL;
      if (result) {
         boolean isFunc = FunctionMetadataRegistry.getFunctionByName(str.toUpperCase(Locale.ROOT)) != null;
         if (isFunc) {
            int savePointer = this._pointer;
            this.resetPointer(this._pointer + str.length());
            this.SkipWhite();
            result = this.look != 40;
            this.resetPointer(savePointer);
         }
      }

      return result;
   }

   private ParseNode function(String name) {
      Ptg nameToken = null;
      if (!AbstractFunctionPtg.isBuiltInFunctionName(name)) {
         if (this._book == null) {
            throw new IllegalStateException("Need book to evaluate name '" + name + "'");
         }

         EvaluationName hName = this._book.getName(name, this._sheetIndex);
         if (hName != null) {
            if (!hName.isFunctionName()) {
               throw new FormulaParseException("Attempt to use name '" + name + "' as a function, but defined name in workbook does not refer to a function");
            }

            nameToken = hName.createPtg();
         } else {
            nameToken = this._book.getNameXPtg(name, null);
            if (nameToken == null) {
               if (log.check(5)) {
                  log.log(5, new Object[]{"FormulaParser.function: Name '" + name + "' is completely unknown in the current workbook."});
               }

               switch (this._book.getSpreadsheetVersion()) {
                  case EXCEL97:
                     this.addName(name);
                     hName = this._book.getName(name, this._sheetIndex);
                     nameToken = hName.createPtg();
                     break;
                  case EXCEL2007:
                     nameToken = new NameXPxg(name);
                     break;
                  default:
                     throw new IllegalStateException("Unexpected spreadsheet version: " + this._book.getSpreadsheetVersion().name());
               }
            }
         }
      }

      this.Match(40);
      ParseNode[] args = this.Arguments();
      this.Match(41);
      return this.getFunction(name, nameToken, args);
   }

   private void addName(String functionName) {
      Name name = this._book.createName();
      name.setFunction(true);
      name.setNameName(functionName);
      name.setSheetIndex(this._sheetIndex);
   }

   private ParseNode getFunction(String name, Ptg namePtg, ParseNode[] args) {
      FunctionMetadata fm = FunctionMetadataRegistry.getFunctionByName(name.toUpperCase(Locale.ROOT));
      int numArgs = args.length;
      if (fm == null) {
         if (namePtg == null) {
            throw new IllegalStateException("NamePtg must be supplied for external functions");
         }

         ParseNode[] allArgs = new ParseNode[numArgs + 1];
         allArgs[0] = new ParseNode(namePtg);
         System.arraycopy(args, 0, allArgs, 1, numArgs);
         return new ParseNode(FuncVarPtg.create(name, numArgs + 1), allArgs);
      } else {
         if (namePtg != null) {
            throw new IllegalStateException("NamePtg no applicable to internal functions");
         }

         boolean isVarArgs = !fm.hasFixedArgsLength();
         int funcIx = fm.getIndex();
         if (funcIx == 4 && args.length == 1) {
            return new ParseNode(AttrPtg.getSumSingle(), args);
         }

         this.validateNumArgs(args.length, fm);
         AbstractFunctionPtg retval;
         if (isVarArgs) {
            retval = FuncVarPtg.create(name, numArgs);
         } else {
            retval = FuncPtg.create(funcIx);
         }

         return new ParseNode(retval, args);
      }
   }

   private void validateNumArgs(int numArgs, FunctionMetadata fm) {
      if (numArgs < fm.getMinParams()) {
         String msg = "Too few arguments to function '" + fm.getName() + "'. ";
         if (fm.hasFixedArgsLength()) {
            msg = msg + "Expected " + fm.getMinParams();
         } else {
            msg = msg + "At least " + fm.getMinParams() + " were expected";
         }

         msg = msg + " but got " + numArgs + ".";
         throw new FormulaParseException(msg);
      } else {
         int maxArgs;
         if (fm.hasUnlimitedVarags()) {
            if (this._book != null) {
               maxArgs = this._book.getSpreadsheetVersion().getMaxFunctionArgs();
            } else {
               maxArgs = fm.getMaxParams();
            }
         } else {
            maxArgs = fm.getMaxParams();
         }

         if (numArgs > maxArgs) {
            String msg = "Too many arguments to function '" + fm.getName() + "'. ";
            if (fm.hasFixedArgsLength()) {
               msg = msg + "Expected " + maxArgs;
            } else {
               msg = msg + "At most " + maxArgs + " were expected";
            }

            msg = msg + " but got " + numArgs + ".";
            throw new FormulaParseException(msg);
         }
      }
   }

   private static boolean isArgumentDelimiter(int ch) {
      return ch == 44 || ch == 41;
   }

   private ParseNode[] Arguments() {
      List<ParseNode> temp = new ArrayList(2);
      this.SkipWhite();
      if (this.look == 41) {
         return ParseNode.EMPTY_ARRAY;
      }

      boolean missedPrevArg = true;

      while (true) {
         this.SkipWhite();
         if (isArgumentDelimiter(this.look)) {
            if (missedPrevArg) {
               temp.add(new ParseNode(MissingArgPtg.instance));
            }

            if (this.look == 41) {
               ParseNode[] result = new ParseNode[temp.size()];
               temp.toArray(result);
               return result;
            }

            this.Match(44);
            missedPrevArg = true;
         } else {
            temp.add(this.comparisonExpression());
            missedPrevArg = false;
            this.SkipWhite();
            if (!isArgumentDelimiter(this.look)) {
               throw this.expected("',' or ')'");
            }
         }
      }
   }

   private ParseNode powerFactor() {
      ParseNode result = this.percentFactor();

      while (true) {
         this.SkipWhite();
         if (this.look != 94) {
            return result;
         }

         this.Match(94);
         ParseNode other = this.percentFactor();
         result = new ParseNode(PowerPtg.instance, result, other);
      }
   }

   private ParseNode percentFactor() {
      ParseNode result = this.parseSimpleFactor();

      while (true) {
         this.SkipWhite();
         if (this.look != 37) {
            return result;
         }

         this.Match(37);
         result = new ParseNode(PercentPtg.instance, result);
      }
   }

   private ParseNode parseSimpleFactor() {
      this.SkipWhite();
      switch (this.look) {
         case 34:
            return new ParseNode(new StringPtg(this.parseStringLiteral()));
         case 35:
            return new ParseNode(ErrPtg.valueOf(this.parseErrorLiteral()));
         case 40:
            this.Match(40);
            ParseNode inside = this.unionExpression();
            this.Match(41);
            return new ParseNode(ParenthesisPtg.instance, inside);
         case 43:
            this.Match(43);
            return this.parseUnary(true);
         case 45:
            this.Match(45);
            return this.parseUnary(false);
         case 123:
            this.Match(123);
            ParseNode arrayNode = this.parseArray();
            this.Match(125);
            return arrayNode;
         default:
            if (IsAlpha(this.look) || Character.isDigit(this.look) || this.look == 39 || this.look == 91 || this.look == 95 || this.look == 92) {
               return this.parseRangeExpression();
            } else if (this.look == 46) {
               return new ParseNode(this.parseNumber());
            } else {
               throw this.expected("cell ref or constant literal");
            }
      }
   }

   private ParseNode parseUnary(boolean isPlus) {
      boolean numberFollows = IsDigit(this.look) || this.look == 46;
      ParseNode factor = this.powerFactor();
      if (numberFollows) {
         Ptg token = factor.getToken();
         if (token instanceof NumberPtg) {
            if (isPlus) {
               return factor;
            }

            Ptg var7 = new NumberPtg(-((NumberPtg)token).getValue());
            return new ParseNode(var7);
         }

         if (token instanceof IntPtg) {
            if (isPlus) {
               return factor;
            }

            int intVal = ((IntPtg)token).getValue();
            Ptg var6 = new NumberPtg(-intVal);
            return new ParseNode(var6);
         }
      }

      return new ParseNode(isPlus ? UnaryPlusPtg.instance : UnaryMinusPtg.instance, factor);
   }

   private ParseNode parseArray() {
      List<Object[]> rowsData = new ArrayList();

      while (true) {
         Object[] singleRowData = this.parseArrayRow();
         rowsData.add(singleRowData);
         if (this.look == 125) {
            int nRows = rowsData.size();
            Object[][] values2d = new Object[nRows][];
            rowsData.toArray(values2d);
            int nColumns = values2d[0].length;
            this.checkRowLengths(values2d, nColumns);
            return new ParseNode(new ArrayPtg(values2d));
         }

         if (this.look != 59) {
            throw this.expected("'}' or ';'");
         }

         this.Match(59);
      }
   }

   private void checkRowLengths(Object[][] values2d, int nColumns) {
      for (int i = 0; i < values2d.length; i++) {
         int rowLen = values2d[i].length;
         if (rowLen != nColumns) {
            throw new FormulaParseException("Array row " + i + " has length " + rowLen + " but row 0 has length " + nColumns);
         }
      }
   }

   private Object[] parseArrayRow() {
      List<Object> temp = new ArrayList();

      while (true) {
         temp.add(this.parseArrayItem());
         this.SkipWhite();
         switch (this.look) {
            case 44:
               this.Match(44);
               break;
            case 59:
            case 125:
               Object[] result = new Object[temp.size()];
               temp.toArray(result);
               return result;
            default:
               throw this.expected("'}' or ','");
         }
      }
   }

   private Object parseArrayItem() {
      this.SkipWhite();
      switch (this.look) {
         case 34:
            return this.parseStringLiteral();
         case 35:
            return ErrorConstant.valueOf(this.parseErrorLiteral());
         case 45:
            this.Match(45);
            this.SkipWhite();
            return convertArrayNumber(this.parseNumber(), false);
         case 70:
         case 84:
         case 102:
         case 116:
            return this.parseBooleanLiteral();
         default:
            return convertArrayNumber(this.parseNumber(), true);
      }
   }

   private Boolean parseBooleanLiteral() {
      String iden = this.parseUnquotedIdentifier();
      if ("TRUE".equalsIgnoreCase(iden)) {
         return Boolean.TRUE;
      } else if ("FALSE".equalsIgnoreCase(iden)) {
         return Boolean.FALSE;
      } else {
         throw this.expected("'TRUE' or 'FALSE'");
      }
   }

   private static Double convertArrayNumber(Ptg ptg, boolean isPositive) {
      double value;
      if (ptg instanceof IntPtg) {
         value = ((IntPtg)ptg).getValue();
      } else {
         if (!(ptg instanceof NumberPtg)) {
            throw new RuntimeException("Unexpected ptg (" + ptg.getClass().getName() + ")");
         }

         value = ((NumberPtg)ptg).getValue();
      }

      if (!isPositive) {
         value = -value;
      }

      return value;
   }

   private Ptg parseNumber() {
      String number2 = null;
      String exponent = null;
      String number1 = this.GetNum();
      if (this.look == 46) {
         this.GetChar();
         number2 = this.GetNum();
      }

      if (this.look == 69) {
         this.GetChar();
         String sign = "";
         if (this.look == 43) {
            this.GetChar();
         } else if (this.look == 45) {
            this.GetChar();
            sign = "-";
         }

         String number = this.GetNum();
         if (number == null) {
            throw this.expected("Integer");
         }

         exponent = sign + number;
      }

      if (number1 == null && number2 == null) {
         throw this.expected("Integer");
      } else {
         return getNumberPtgFromString(number1, number2, exponent);
      }
   }

   private int parseErrorLiteral() {
      this.Match(35);
      String part1 = this.parseUnquotedIdentifier();
      if (part1 == null) {
         throw this.expected("remainder of error constant literal");
      }

      part1 = part1.toUpperCase(Locale.ROOT);
      switch (part1.charAt(0)) {
         case 'D':
            FormulaError fex = FormulaError.DIV0;
            if (part1.equals("DIV")) {
               this.Match(47);
               this.Match(48);
               this.Match(33);
               return fex.getCode();
            }

            throw this.expected(fex.getString());
         case 'N':
            FormulaError fe = FormulaError.NAME;
            if (part1.equals(fe.name())) {
               this.Match(63);
               return fe.getCode();
            } else {
               fe = FormulaError.NUM;
               if (part1.equals(fe.name())) {
                  this.Match(33);
                  return fe.getCode();
               } else {
                  fe = FormulaError.NULL;
                  if (part1.equals(fe.name())) {
                     this.Match(33);
                     return fe.getCode();
                  } else {
                     fe = FormulaError.NA;
                     if (part1.equals("N")) {
                        this.Match(47);
                        if (this.look != 65 && this.look != 97) {
                           throw this.expected(fe.getString());
                        }

                        this.Match(this.look);
                        return fe.getCode();
                     }

                     throw this.expected("#NAME?, #NUM!, #NULL! or #N/A");
                  }
               }
            }
         case 'R':
            FormulaError fe = FormulaError.REF;
            if (part1.equals(fe.name())) {
               this.Match(33);
               return fe.getCode();
            }

            throw this.expected(fe.getString());
         case 'V':
            FormulaError fe = FormulaError.VALUE;
            if (part1.equals(fe.name())) {
               this.Match(33);
               return fe.getCode();
            }

            throw this.expected(fe.getString());
         default:
            throw this.expected("#VALUE!, #REF!, #DIV/0!, #NAME?, #NUM!, #NULL! or #N/A");
      }
   }

   private String parseUnquotedIdentifier() {
      if (this.look == 39) {
         throw this.expected("unquoted identifier");
      }

      StringBuilder sb = new StringBuilder();

      while (Character.isLetterOrDigit(this.look) || this.look == 46) {
         sb.appendCodePoint(this.look);
         this.GetChar();
      }

      return sb.length() < 1 ? null : sb.toString();
   }

   private static Ptg getNumberPtgFromString(String number1, String number2, String exponent) {
      StringBuilder number = new StringBuilder();
      if (number2 == null) {
         number.append(number1);
         if (exponent != null) {
            number.append('E');
            number.append(exponent);
         }

         String numberStr = number.toString();

         int intVal;
         try {
            intVal = Integer.parseInt(numberStr);
         } catch (NumberFormatException e) {
            return new NumberPtg(numberStr);
         }

         return (Ptg)(IntPtg.isInRange(intVal) ? new IntPtg(intVal) : new NumberPtg(numberStr));
      } else {
         if (number1 != null) {
            number.append(number1);
         }

         number.append('.');
         number.append(number2);
         if (exponent != null) {
            number.append('E');
            number.append(exponent);
         }

         return new NumberPtg(number.toString());
      }
   }

   private String parseStringLiteral() {
      this.Match(34);
      StringBuilder token = new StringBuilder();

      while (true) {
         if (this.look == 34) {
            this.GetChar();
            if (this.look != 34) {
               return token.toString();
            }
         }

         token.appendCodePoint(this.look);
         this.GetChar();
      }
   }

   private ParseNode Term() {
      ParseNode result = this.powerFactor();

      while (true) {
         this.SkipWhite();
         Ptg operator;
         switch (this.look) {
            case 42:
               this.Match(42);
               operator = MultiplyPtg.instance;
               break;
            case 47:
               this.Match(47);
               operator = DividePtg.instance;
               break;
            default:
               return result;
         }

         ParseNode other = this.powerFactor();
         result = new ParseNode(operator, result, other);
      }
   }

   private ParseNode unionExpression() {
      ParseNode result = this.intersectionExpression();
      boolean hasUnions = false;

      while (true) {
         this.SkipWhite();
         switch (this.look) {
            case 44:
               this.GetChar();
               hasUnions = true;
               ParseNode other = this.intersectionExpression();
               result = new ParseNode(UnionPtg.instance, result, other);
               break;
            default:
               return hasUnions ? augmentWithMemPtg(result) : result;
         }
      }
   }

   private ParseNode intersectionExpression() {
      ParseNode result = this.comparisonExpression();
      boolean hasIntersections = false;

      while (true) {
         this.SkipWhite();
         if (!this._inIntersection) {
            break;
         }

         int savePointer = this._pointer;

         try {
            ParseNode other = this.comparisonExpression();
            result = new ParseNode(IntersectionPtg.instance, result, other);
            hasIntersections = true;
         } catch (FormulaParseException e) {
            this.resetPointer(savePointer);
            break;
         }
      }

      return hasIntersections ? augmentWithMemPtg(result) : result;
   }

   private ParseNode comparisonExpression() {
      ParseNode result = this.concatExpression();

      while (true) {
         this.SkipWhite();
         switch (this.look) {
            case 60:
            case 61:
            case 62:
               Ptg comparisonToken = this.getComparisonToken();
               ParseNode other = this.concatExpression();
               result = new ParseNode(comparisonToken, result, other);
               break;
            default:
               return result;
         }
      }
   }

   private Ptg getComparisonToken() {
      if (this.look == 61) {
         this.Match(this.look);
         return EqualPtg.instance;
      }

      boolean isGreater = this.look == 62;
      this.Match(this.look);
      if (isGreater) {
         if (this.look == 61) {
            this.Match(61);
            return GreaterEqualPtg.instance;
         } else {
            return GreaterThanPtg.instance;
         }
      } else {
         switch (this.look) {
            case 61:
               this.Match(61);
               return LessEqualPtg.instance;
            case 62:
               this.Match(62);
               return NotEqualPtg.instance;
            default:
               return LessThanPtg.instance;
         }
      }
   }

   private ParseNode concatExpression() {
      ParseNode result = this.additiveExpression();

      while (true) {
         this.SkipWhite();
         if (this.look != 38) {
            return result;
         }

         this.Match(38);
         ParseNode other = this.additiveExpression();
         result = new ParseNode(ConcatPtg.instance, result, other);
      }
   }

   private ParseNode additiveExpression() {
      ParseNode result = this.Term();

      while (true) {
         this.SkipWhite();
         Ptg operator;
         switch (this.look) {
            case 43:
               this.Match(43);
               operator = AddPtg.instance;
               break;
            case 45:
               this.Match(45);
               operator = SubtractPtg.instance;
               break;
            default:
               return result;
         }

         ParseNode other = this.Term();
         result = new ParseNode(operator, result, other);
      }
   }

   private void parse() {
      this._pointer = 0;
      this.GetChar();
      this._rootNode = this.unionExpression();
      if (this._pointer <= this._formulaLength) {
         String msg = "Unused input ["
            + this._formulaString.substring(this._pointer - 1)
            + "] after attempting to parse the formula ["
            + this._formulaString
            + "]";
         throw new FormulaParseException(msg);
      }
   }

   private Ptg[] getRPNPtg(FormulaType formulaType) {
      OperandClassTransformer oct = new OperandClassTransformer(formulaType);
      oct.transformFormula(this._rootNode);
      return ParseNode.toTokenArray(this._rootNode);
   }

   private static final class SimpleRangePart {
      private final FormulaParser.SimpleRangePart.Type _type;
      private final String _rep;

      public SimpleRangePart(String rep, boolean hasLetters, boolean hasNumbers) {
         this._rep = rep;
         this._type = FormulaParser.SimpleRangePart.Type.get(hasLetters, hasNumbers);
      }

      public boolean isCell() {
         return this._type == FormulaParser.SimpleRangePart.Type.CELL;
      }

      public boolean isRowOrColumn() {
         return this._type != FormulaParser.SimpleRangePart.Type.CELL;
      }

      public CellReference getCellReference() {
         if (this._type != FormulaParser.SimpleRangePart.Type.CELL) {
            throw new IllegalStateException("Not applicable to this type");
         } else {
            return new CellReference(this._rep);
         }
      }

      public boolean isColumn() {
         return this._type == FormulaParser.SimpleRangePart.Type.COLUMN;
      }

      public boolean isRow() {
         return this._type == FormulaParser.SimpleRangePart.Type.ROW;
      }

      public String getRep() {
         return this._rep;
      }

      public boolean isCompatibleForArea(FormulaParser.SimpleRangePart part2) {
         return this._type == part2._type;
      }

      public String toString() {
         return this.getClass().getName() + " [" + this._rep + "]";
      }

      private enum Type {
         CELL,
         ROW,
         COLUMN;

         public static FormulaParser.SimpleRangePart.Type get(boolean hasLetters, boolean hasDigits) {
            if (hasLetters) {
               return hasDigits ? CELL : COLUMN;
            } else if (!hasDigits) {
               throw new IllegalArgumentException("must have either letters or numbers");
            } else {
               return ROW;
            }
         }
      }
   }
}
