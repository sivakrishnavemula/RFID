package org.apache.poi.ss.usermodel;

import org.apache.poi.ss.util.CellRangeAddressList;

public interface DataValidation {
   DataValidationConstraint getValidationConstraint();

   void setErrorStyle(int var1);

   int getErrorStyle();

   void setEmptyCellAllowed(boolean var1);

   boolean getEmptyCellAllowed();

   void setSuppressDropDownArrow(boolean var1);

   boolean getSuppressDropDownArrow();

   void setShowPromptBox(boolean var1);

   boolean getShowPromptBox();

   void setShowErrorBox(boolean var1);

   boolean getShowErrorBox();

   void createPromptBox(String var1, String var2);

   String getPromptBoxTitle();

   String getPromptBoxText();

   void createErrorBox(String var1, String var2);

   String getErrorBoxTitle();

   String getErrorBoxText();

   CellRangeAddressList getRegions();

   final class ErrorStyle {
      public static final int STOP = 0;
      public static final int WARNING = 1;
      public static final int INFO = 2;
   }
}
