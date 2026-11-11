package org.apache.poi.ss.usermodel;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PaneInformation;

public interface Sheet extends Iterable<Row> {
   short LeftMargin = 0;
   short RightMargin = 1;
   short TopMargin = 2;
   short BottomMargin = 3;
   short HeaderMargin = 4;
   short FooterMargin = 5;
   byte PANE_LOWER_RIGHT = 0;
   byte PANE_UPPER_RIGHT = 1;
   byte PANE_LOWER_LEFT = 2;
   byte PANE_UPPER_LEFT = 3;

   Row createRow(int var1);

   void removeRow(Row var1);

   Row getRow(int var1);

   int getPhysicalNumberOfRows();

   int getFirstRowNum();

   int getLastRowNum();

   void setColumnHidden(int var1, boolean var2);

   boolean isColumnHidden(int var1);

   void setRightToLeft(boolean var1);

   boolean isRightToLeft();

   void setColumnWidth(int var1, int var2);

   int getColumnWidth(int var1);

   float getColumnWidthInPixels(int var1);

   void setDefaultColumnWidth(int var1);

   int getDefaultColumnWidth();

   short getDefaultRowHeight();

   float getDefaultRowHeightInPoints();

   void setDefaultRowHeight(short var1);

   void setDefaultRowHeightInPoints(float var1);

   CellStyle getColumnStyle(int var1);

   int addMergedRegion(CellRangeAddress var1);

   int addMergedRegionUnsafe(CellRangeAddress var1);

   void validateMergedRegions();

   void setVerticallyCenter(boolean var1);

   void setHorizontallyCenter(boolean var1);

   boolean getHorizontallyCenter();

   boolean getVerticallyCenter();

   void removeMergedRegion(int var1);

   void removeMergedRegions(Collection<Integer> var1);

   int getNumMergedRegions();

   CellRangeAddress getMergedRegion(int var1);

   List<CellRangeAddress> getMergedRegions();

   Iterator<Row> rowIterator();

   void setForceFormulaRecalculation(boolean var1);

   boolean getForceFormulaRecalculation();

   void setAutobreaks(boolean var1);

   void setDisplayGuts(boolean var1);

   void setDisplayZeros(boolean var1);

   boolean isDisplayZeros();

   void setFitToPage(boolean var1);

   void setRowSumsBelow(boolean var1);

   void setRowSumsRight(boolean var1);

   boolean getAutobreaks();

   boolean getDisplayGuts();

   boolean getFitToPage();

   boolean getRowSumsBelow();

   boolean getRowSumsRight();

   boolean isPrintGridlines();

   void setPrintGridlines(boolean var1);

   boolean isPrintRowAndColumnHeadings();

   void setPrintRowAndColumnHeadings(boolean var1);

   PrintSetup getPrintSetup();

   Header getHeader();

   Footer getFooter();

   void setSelected(boolean var1);

   double getMargin(short var1);

   void setMargin(short var1, double var2);

   boolean getProtect();

   void protectSheet(String var1);

   boolean getScenarioProtect();

   void setZoom(int var1);

   short getTopRow();

   short getLeftCol();

   void showInPane(int var1, int var2);

   void shiftRows(int var1, int var2, int var3);

   void shiftRows(int var1, int var2, int var3, boolean var4, boolean var5);

   void shiftColumns(int var1, int var2, int var3);

   void createFreezePane(int var1, int var2, int var3, int var4);

   void createFreezePane(int var1, int var2);

   void createSplitPane(int var1, int var2, int var3, int var4, int var5);

   PaneInformation getPaneInformation();

   void setDisplayGridlines(boolean var1);

   boolean isDisplayGridlines();

   void setDisplayFormulas(boolean var1);

   boolean isDisplayFormulas();

   void setDisplayRowColHeadings(boolean var1);

   boolean isDisplayRowColHeadings();

   void setRowBreak(int var1);

   boolean isRowBroken(int var1);

   void removeRowBreak(int var1);

   int[] getRowBreaks();

   int[] getColumnBreaks();

   void setColumnBreak(int var1);

   boolean isColumnBroken(int var1);

   void removeColumnBreak(int var1);

   void setColumnGroupCollapsed(int var1, boolean var2);

   void groupColumn(int var1, int var2);

   void ungroupColumn(int var1, int var2);

   void groupRow(int var1, int var2);

   void ungroupRow(int var1, int var2);

   void setRowGroupCollapsed(int var1, boolean var2);

   void setDefaultColumnStyle(int var1, CellStyle var2);

   void autoSizeColumn(int var1);

   void autoSizeColumn(int var1, boolean var2);

   Comment getCellComment(CellAddress var1);

   Map<CellAddress, ? extends Comment> getCellComments();

   Drawing<?> getDrawingPatriarch();

   Drawing<?> createDrawingPatriarch();

   Workbook getWorkbook();

   String getSheetName();

   boolean isSelected();

   CellRange<? extends Cell> setArrayFormula(String var1, CellRangeAddress var2);

   CellRange<? extends Cell> removeArrayFormula(Cell var1);

   DataValidationHelper getDataValidationHelper();

   List<? extends DataValidation> getDataValidations();

   void addValidationData(DataValidation var1);

   AutoFilter setAutoFilter(CellRangeAddress var1);

   SheetConditionalFormatting getSheetConditionalFormatting();

   CellRangeAddress getRepeatingRows();

   CellRangeAddress getRepeatingColumns();

   void setRepeatingRows(CellRangeAddress var1);

   void setRepeatingColumns(CellRangeAddress var1);

   int getColumnOutlineLevel(int var1);

   Hyperlink getHyperlink(int var1, int var2);

   Hyperlink getHyperlink(CellAddress var1);

   List<? extends Hyperlink> getHyperlinkList();

   CellAddress getActiveCell();

   void setActiveCell(CellAddress var1);
}
