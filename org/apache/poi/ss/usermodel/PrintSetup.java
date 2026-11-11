package org.apache.poi.ss.usermodel;

public interface PrintSetup {
   short PRINTER_DEFAULT_PAPERSIZE = 0;
   short LETTER_PAPERSIZE = 1;
   short LETTER_SMALL_PAGESIZE = 2;
   short TABLOID_PAPERSIZE = 3;
   short LEDGER_PAPERSIZE = 4;
   short LEGAL_PAPERSIZE = 5;
   short STATEMENT_PAPERSIZE = 6;
   short EXECUTIVE_PAPERSIZE = 7;
   short A3_PAPERSIZE = 8;
   short A4_PAPERSIZE = 9;
   short A4_SMALL_PAPERSIZE = 10;
   short A5_PAPERSIZE = 11;
   short B4_PAPERSIZE = 12;
   short B5_PAPERSIZE = 13;
   short FOLIO8_PAPERSIZE = 14;
   short QUARTO_PAPERSIZE = 15;
   short TEN_BY_FOURTEEN_PAPERSIZE = 16;
   short ELEVEN_BY_SEVENTEEN_PAPERSIZE = 17;
   short NOTE8_PAPERSIZE = 18;
   short ENVELOPE_9_PAPERSIZE = 19;
   short ENVELOPE_10_PAPERSIZE = 20;
   short ENVELOPE_DL_PAPERSIZE = 27;
   short ENVELOPE_CS_PAPERSIZE = 28;
   short ENVELOPE_C5_PAPERSIZE = 28;
   short ENVELOPE_C3_PAPERSIZE = 29;
   short ENVELOPE_C4_PAPERSIZE = 30;
   short ENVELOPE_C6_PAPERSIZE = 31;
   short ENVELOPE_MONARCH_PAPERSIZE = 37;
   short A4_EXTRA_PAPERSIZE = 53;
   short A4_TRANSVERSE_PAPERSIZE = 55;
   short A4_PLUS_PAPERSIZE = 60;
   short LETTER_ROTATED_PAPERSIZE = 75;
   short A4_ROTATED_PAPERSIZE = 77;

   void setPaperSize(short var1);

   void setScale(short var1);

   void setPageStart(short var1);

   void setFitWidth(short var1);

   void setFitHeight(short var1);

   void setLeftToRight(boolean var1);

   void setLandscape(boolean var1);

   void setValidSettings(boolean var1);

   void setNoColor(boolean var1);

   void setDraft(boolean var1);

   void setNotes(boolean var1);

   void setNoOrientation(boolean var1);

   void setUsePage(boolean var1);

   void setHResolution(short var1);

   void setVResolution(short var1);

   void setHeaderMargin(double var1);

   void setFooterMargin(double var1);

   void setCopies(short var1);

   short getPaperSize();

   short getScale();

   short getPageStart();

   short getFitWidth();

   short getFitHeight();

   boolean getLeftToRight();

   boolean getLandscape();

   boolean getValidSettings();

   boolean getNoColor();

   boolean getDraft();

   boolean getNotes();

   boolean getNoOrientation();

   boolean getUsePage();

   short getHResolution();

   short getVResolution();

   double getHeaderMargin();

   double getFooterMargin();

   short getCopies();
}
