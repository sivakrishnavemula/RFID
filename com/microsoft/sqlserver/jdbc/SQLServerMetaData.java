package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;

public class SQLServerMetaData {
   String columnName = null;
   int javaSqlType;
   int precision = 0;
   int scale = 0;
   boolean useServerDefault = false;
   boolean isUniqueKey = false;
   SQLServerSortOrder sortOrder = SQLServerSortOrder.Unspecified;
   int sortOrdinal;
   static final int defaultSortOrdinal = -1;

   public SQLServerMetaData(String var1, int var2) {
      this.columnName = var1;
      this.javaSqlType = var2;
   }

   public SQLServerMetaData(String var1, int var2, int var3, int var4) {
      this.columnName = var1;
      this.javaSqlType = var2;
      this.precision = var3;
      this.scale = var4;
   }

   public SQLServerMetaData(String var1, int var2, int var3, int var4, boolean var5, boolean var6, SQLServerSortOrder var7, int var8) throws SQLServerException {
      this.columnName = var1;
      this.javaSqlType = var2;
      this.precision = var3;
      this.scale = var4;
      this.useServerDefault = var5;
      this.isUniqueKey = var6;
      this.sortOrder = var7;
      this.sortOrdinal = var8;
      this.validateSortOrder();
   }

   public SQLServerMetaData(SQLServerMetaData var1) {
      this.columnName = var1.columnName;
      this.javaSqlType = var1.javaSqlType;
      this.precision = var1.precision;
      this.scale = var1.scale;
      this.useServerDefault = var1.useServerDefault;
      this.isUniqueKey = var1.isUniqueKey;
      this.sortOrder = var1.sortOrder;
      this.sortOrdinal = var1.sortOrdinal;
   }

   public String getColumName() {
      return this.columnName;
   }

   public int getSqlType() {
      return this.javaSqlType;
   }

   public int getPrecision() {
      return this.precision;
   }

   public int getScale() {
      return this.scale;
   }

   public boolean useServerDefault() {
      return this.useServerDefault;
   }

   public boolean isUniqueKey() {
      return this.isUniqueKey;
   }

   public SQLServerSortOrder getSortOrder() {
      return this.sortOrder;
   }

   public int getSortOrdinal() {
      return this.sortOrdinal;
   }

   void validateSortOrder() throws SQLServerException {
      if (SQLServerSortOrder.Unspecified == this.sortOrder != (-1 == this.sortOrdinal)) {
         MessageFormat var1 = new MessageFormat(SQLServerException.getErrString("R_TVPMissingSortOrderOrOrdinal"));
         throw new SQLServerException(var1.format(new Object[]{this.sortOrder, this.sortOrdinal}), null, 0, null);
      }
   }
}
