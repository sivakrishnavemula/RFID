package org.apache.poi.ss.usermodel;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.util.Removal;

public interface Workbook extends Closeable, Iterable<Sheet> {
   int PICTURE_TYPE_EMF = 2;
   int PICTURE_TYPE_WMF = 3;
   int PICTURE_TYPE_PICT = 4;
   int PICTURE_TYPE_JPEG = 5;
   int PICTURE_TYPE_PNG = 6;
   int PICTURE_TYPE_DIB = 7;

   int getActiveSheetIndex();

   void setActiveSheet(int var1);

   int getFirstVisibleTab();

   void setFirstVisibleTab(int var1);

   void setSheetOrder(String var1, int var2);

   void setSelectedTab(int var1);

   void setSheetName(int var1, String var2);

   String getSheetName(int var1);

   int getSheetIndex(String var1);

   int getSheetIndex(Sheet var1);

   Sheet createSheet();

   Sheet createSheet(String var1);

   Sheet cloneSheet(int var1);

   Iterator<Sheet> sheetIterator();

   int getNumberOfSheets();

   Sheet getSheetAt(int var1);

   Sheet getSheet(String var1);

   void removeSheetAt(int var1);

   Font createFont();

   Font findFont(boolean var1, short var2, short var3, String var4, boolean var5, boolean var6, short var7, byte var8);

   /** @deprecated */
   @Removal(version = "4.2")
   short getNumberOfFonts();

   int getNumberOfFontsAsInt();

   /** @deprecated */
   @Removal(version = "4.2")
   Font getFontAt(short var1);

   Font getFontAt(int var1);

   CellStyle createCellStyle();

   int getNumCellStyles();

   CellStyle getCellStyleAt(int var1);

   void write(OutputStream var1) throws IOException;

   void close() throws IOException;

   int getNumberOfNames();

   Name getName(String var1);

   List<? extends Name> getNames(String var1);

   List<? extends Name> getAllNames();

   @Deprecated
   @Removal(version = "5.0.0")
   Name getNameAt(int var1);

   Name createName();

   @Deprecated
   @Removal(version = "3.20")
   int getNameIndex(String var1);

   @Deprecated
   @Removal(version = "3.20")
   void removeName(int var1);

   @Deprecated
   @Removal(version = "3.20")
   void removeName(String var1);

   void removeName(Name var1);

   int linkExternalWorkbook(String var1, Workbook var2);

   void setPrintArea(int var1, String var2);

   void setPrintArea(int var1, int var2, int var3, int var4, int var5);

   String getPrintArea(int var1);

   void removePrintArea(int var1);

   MissingCellPolicy getMissingCellPolicy();

   void setMissingCellPolicy(MissingCellPolicy var1);

   DataFormat createDataFormat();

   int addPicture(byte[] var1, int var2);

   List<? extends PictureData> getAllPictures();

   CreationHelper getCreationHelper();

   boolean isHidden();

   void setHidden(boolean var1);

   boolean isSheetHidden(int var1);

   boolean isSheetVeryHidden(int var1);

   void setSheetHidden(int var1, boolean var2);

   SheetVisibility getSheetVisibility(int var1);

   void setSheetVisibility(int var1, SheetVisibility var2);

   void addToolPack(UDFFinder var1);

   void setForceFormulaRecalculation(boolean var1);

   boolean getForceFormulaRecalculation();

   SpreadsheetVersion getSpreadsheetVersion();

   int addOlePackage(byte[] var1, String var2, String var3, String var4) throws IOException;
}
