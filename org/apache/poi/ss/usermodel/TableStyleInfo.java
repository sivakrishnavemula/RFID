package org.apache.poi.ss.usermodel;

public interface TableStyleInfo {
   boolean isShowColumnStripes();

   boolean isShowRowStripes();

   boolean isShowFirstColumn();

   boolean isShowLastColumn();

   String getName();

   TableStyle getStyle();
}
