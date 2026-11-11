package org.apache.poi.hssf.usermodel;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.record.common.ExtendedColor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Internal;

public class HSSFCreationHelper implements CreationHelper {
   private final HSSFWorkbook workbook;

   @Internal(since = "3.15 beta 3")
   HSSFCreationHelper(HSSFWorkbook wb) {
      this.workbook = wb;
   }

   public HSSFRichTextString createRichTextString(String text) {
      return new HSSFRichTextString(text);
   }

   public HSSFDataFormat createDataFormat() {
      return this.workbook.createDataFormat();
   }

   public HSSFHyperlink createHyperlink(HyperlinkType type) {
      return new HSSFHyperlink(type);
   }

   public HSSFExtendedColor createExtendedColor() {
      return new HSSFExtendedColor(new ExtendedColor());
   }

   public HSSFFormulaEvaluator createFormulaEvaluator() {
      return new HSSFFormulaEvaluator(this.workbook);
   }

   public HSSFClientAnchor createClientAnchor() {
      return new HSSFClientAnchor();
   }

   public AreaReference createAreaReference(String reference) {
      return new AreaReference(reference, this.workbook.getSpreadsheetVersion());
   }

   public AreaReference createAreaReference(CellReference topLeft, CellReference bottomRight) {
      return new AreaReference(topLeft, bottomRight, this.workbook.getSpreadsheetVersion());
   }
}
