package org.apache.poi.hssf.usermodel.helpers;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.helpers.ColumnShifter;
import org.apache.poi.util.NotImplemented;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public final class HSSFColumnShifter extends ColumnShifter {
   private static final POILogger logger = POILogFactory.getLogger(HSSFColumnShifter.class);

   public HSSFColumnShifter(HSSFSheet sh) {
      super(sh);
   }

   @NotImplemented
   public void updateNamedRanges(FormulaShifter formulaShifter) {
      throw new NotImplementedException("HSSFColumnShifter.updateNamedRanges");
   }

   @NotImplemented
   public void updateFormulas(FormulaShifter formulaShifter) {
      throw new NotImplementedException("updateFormulas");
   }

   @NotImplemented
   public void updateConditionalFormatting(FormulaShifter formulaShifter) {
      throw new NotImplementedException("updateConditionalFormatting");
   }

   @NotImplemented
   public void updateHyperlinks(FormulaShifter formulaShifter) {
      throw new NotImplementedException("updateHyperlinks");
   }
}
