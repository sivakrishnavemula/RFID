package org.apache.poi.ss.usermodel;

public interface RichTextString {
   void applyFont(int var1, int var2, short var3);

   void applyFont(int var1, int var2, Font var3);

   void applyFont(Font var1);

   void clearFormatting();

   String getString();

   int length();

   int numFormattingRuns();

   int getIndexOfFormattingRun(int var1);

   void applyFont(short var1);
}
