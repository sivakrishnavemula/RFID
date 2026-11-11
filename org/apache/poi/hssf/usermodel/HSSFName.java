package org.apache.poi.hssf.usermodel;

import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.record.NameCommentRecord;
import org.apache.poi.hssf.record.NameRecord;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.util.CellReference;

public final class HSSFName implements Name {
   private HSSFWorkbook _book;
   private NameRecord _definedNameRec;
   private NameCommentRecord _commentRec;

   HSSFName(HSSFWorkbook book, NameRecord name) {
      this(book, name, null);
   }

   HSSFName(HSSFWorkbook book, NameRecord name, NameCommentRecord comment) {
      this._book = book;
      this._definedNameRec = name;
      this._commentRec = comment;
   }

   public String getSheetName() {
      int indexToExternSheet = this._definedNameRec.getExternSheetNumber();
      return this._book.getWorkbook().findSheetFirstNameFromExternSheet(indexToExternSheet);
   }

   public String getNameName() {
      return this._definedNameRec.getNameText();
   }

   public void setNameName(String nameName) {
      validateName(nameName);
      InternalWorkbook wb = this._book.getWorkbook();
      this._definedNameRec.setNameText(nameName);
      int sheetNumber = this._definedNameRec.getSheetNumber();
      int lastNameIndex = wb.getNumNames() - 1;

      for (int i = lastNameIndex; i >= 0; i--) {
         NameRecord rec = wb.getNameRecord(i);
         if (rec != this._definedNameRec && rec.getNameText().equalsIgnoreCase(nameName) && sheetNumber == rec.getSheetNumber()) {
            String msg = "The " + (sheetNumber == 0 ? "workbook" : "sheet") + " already contains this name: " + nameName;
            this._definedNameRec.setNameText(nameName + "(2)");
            throw new IllegalArgumentException(msg);
         }
      }

      if (this._commentRec != null) {
         this._commentRec.setNameText(nameName);
         this._book.getWorkbook().updateNameCommentRecordCache(this._commentRec);
      }
   }

   private static void validateName(String name) {
      if (name.length() == 0) {
         throw new IllegalArgumentException("Name cannot be blank");
      }

      if (name.length() > 255) {
         throw new IllegalArgumentException("Invalid name: '" + name + "': cannot exceed 255 characters in length");
      }

      if (!name.equalsIgnoreCase("R") && !name.equalsIgnoreCase("C")) {
         char c = name.charAt(0);
         String allowedSymbols = "_\\";
         boolean characterIsValid = Character.isLetter(c) || allowedSymbols.indexOf(c) != -1;
         if (!characterIsValid) {
            throw new IllegalArgumentException("Invalid name: '" + name + "': first character must be underscore or a letter");
         }

         allowedSymbols = "_.\\";

         for (char ch : name.toCharArray()) {
            characterIsValid = Character.isLetterOrDigit(ch) || allowedSymbols.indexOf(ch) != -1;
            if (!characterIsValid) {
               throw new IllegalArgumentException("Invalid name: '" + name + "': name must be letter, digit, period, or underscore");
            }
         }

         if (name.matches("[A-Za-z]+\\d+")) {
            String col = name.replaceAll("\\d", "");
            String row = name.replaceAll("[A-Za-z]", "");
            if (CellReference.cellReferenceIsWithinRange(col, row, SpreadsheetVersion.EXCEL97)) {
               throw new IllegalArgumentException("Invalid name: '" + name + "': cannot be $A$1-style cell reference");
            }
         }

         if (name.matches("[Rr]\\d+[Cc]\\d+")) {
            throw new IllegalArgumentException("Invalid name: '" + name + "': cannot be R1C1-style cell reference");
         }
      } else {
         throw new IllegalArgumentException("Invalid name: '" + name + "': cannot be special shorthand R or C");
      }
   }

   public void setRefersToFormula(String formulaText) {
      Ptg[] ptgs = HSSFFormulaParser.parse(formulaText, this._book, FormulaType.NAMEDRANGE, this.getSheetIndex());
      this._definedNameRec.setNameDefinition(ptgs);
   }

   public String getRefersToFormula() {
      if (this._definedNameRec.isFunctionName()) {
         throw new IllegalStateException("Only applicable to named ranges");
      }

      Ptg[] ptgs = this._definedNameRec.getNameDefinition();
      return ptgs.length < 1 ? null : HSSFFormulaParser.toFormulaString(this._book, ptgs);
   }

   void setNameDefinition(Ptg[] ptgs) {
      this._definedNameRec.setNameDefinition(ptgs);
   }

   public boolean isDeleted() {
      Ptg[] ptgs = this._definedNameRec.getNameDefinition();
      return Ptg.doesFormulaReferToDeletedCell(ptgs);
   }

   public boolean isFunctionName() {
      return this._definedNameRec.isFunctionName();
   }

   public String toString() {
      return this.getClass().getName() + " [" + this._definedNameRec.getNameText() + "]";
   }

   public void setSheetIndex(int index) {
      int lastSheetIx = this._book.getNumberOfSheets() - 1;
      if (index >= -1 && index <= lastSheetIx) {
         this._definedNameRec.setSheetNumber(index + 1);
      } else {
         throw new IllegalArgumentException("Sheet index (" + index + ") is out of range" + (lastSheetIx == -1 ? "" : " (0.." + lastSheetIx + ")"));
      }
   }

   public int getSheetIndex() {
      return this._definedNameRec.getSheetNumber() - 1;
   }

   public String getComment() {
      return this._commentRec != null && this._commentRec.getCommentText() != null && this._commentRec.getCommentText().length() > 0
         ? this._commentRec.getCommentText()
         : this._definedNameRec.getDescriptionText();
   }

   public void setComment(String comment) {
      this._definedNameRec.setDescriptionText(comment);
      if (this._commentRec != null) {
         this._commentRec.setCommentText(comment);
      }
   }

   public void setFunction(boolean value) {
      this._definedNameRec.setFunction(value);
   }
}
