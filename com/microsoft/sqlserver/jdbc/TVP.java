package com.microsoft.sqlserver.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

class TVP {
   String TVPName;
   String TVP_owningSchema;
   String TVP_dbName;
   ResultSet sourceResultSet = null;
   SQLServerDataTable sourceDataTable = null;
   Map<Integer, SQLServerMetaData> columnMetadata = null;
   Iterator<Entry<Integer, Object[]>> sourceDataTableRowIterator = null;
   ISQLServerDataRecord sourceRecord = null;
   TVPType tvpType = null;

   void initTVP(TVPType var1, String var2) throws SQLServerException {
      this.tvpType = var1;
      this.columnMetadata = new LinkedHashMap();
      this.parseTypeName(var2);
   }

   TVP(String var1) throws SQLServerException {
      this.initTVP(TVPType.Null, var1);
   }

   TVP(String var1, SQLServerDataTable var2) throws SQLServerException {
      this.initTVP(TVPType.SQLServerDataTable, var1);
      this.sourceDataTable = var2;
      this.sourceDataTableRowIterator = this.sourceDataTable.getIterator();
      this.populateMetadataFromDataTable();
   }

   TVP(String var1, ResultSet var2) throws SQLServerException {
      this.initTVP(TVPType.ResultSet, var1);
      this.sourceResultSet = var2;
      this.populateMetadataFromResultSet();
   }

   TVP(String var1, ISQLServerDataRecord var2) throws SQLServerException {
      this.initTVP(TVPType.ISQLServerDataRecord, var1);
      this.sourceRecord = var2;
      this.populateMetadataFromDataRecord();
      this.validateOrderProperty();
   }

   boolean isNull() {
      return TVPType.Null == this.tvpType;
   }

   Object[] getRowData() throws SQLServerException {
      if (TVPType.ResultSet != this.tvpType) {
         if (TVPType.SQLServerDataTable == this.tvpType) {
            Entry var6 = (Entry)this.sourceDataTableRowIterator.next();
            return (Object[])var6.getValue();
         } else {
            return this.sourceRecord.getRowData();
         }
      } else {
         int var1 = this.columnMetadata.size();
         Object[] var2 = new Object[var1];

         for (int var3 = 0; var3 < var1; var3++) {
            try {
               var2[var3] = this.sourceResultSet.getObject(var3 + 1);
            } catch (SQLException var5) {
               throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveSourceData"), var5);
            }
         }

         return var2;
      }
   }

   boolean next() throws SQLServerException {
      if (TVPType.ResultSet == this.tvpType) {
         try {
            return this.sourceResultSet.next();
         } catch (SQLException var2) {
            throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveSourceData"), var2);
         }
      } else {
         return TVPType.SQLServerDataTable == this.tvpType ? this.sourceDataTableRowIterator.hasNext() : this.sourceRecord.next();
      }
   }

   void populateMetadataFromDataTable() throws SQLServerException {
      assert null != this.sourceDataTable;
      Map var1 = this.sourceDataTable.getColumnMetadata();
      if (null != var1 && !var1.isEmpty()) {
         for (Entry var3 : var1.entrySet()) {
            this.columnMetadata
               .put(
                  var3.getKey(),
                  new SQLServerMetaData(
                     ((SQLServerDataColumn)var3.getValue()).columnName,
                     ((SQLServerDataColumn)var3.getValue()).javaSqlType,
                     ((SQLServerDataColumn)var3.getValue()).precision,
                     ((SQLServerDataColumn)var3.getValue()).scale
                  )
               );
         }
      } else {
         throw new SQLServerException(SQLServerException.getErrString("R_TVPEmptyMetadata"), null);
      }
   }

   void populateMetadataFromResultSet() throws SQLServerException {
      assert null != this.sourceResultSet;

      try {
         ResultSetMetaData var1 = this.sourceResultSet.getMetaData();

         for (int var2 = 0; var2 < var1.getColumnCount(); var2++) {
            SQLServerMetaData var3 = new SQLServerMetaData(
               var1.getColumnName(var2 + 1), var1.getColumnType(var2 + 1), var1.getPrecision(var2 + 1), var1.getScale(var2 + 1)
            );
            this.columnMetadata.put(var2, var3);
         }
      } catch (SQLException var4) {
         throw new SQLServerException(SQLServerException.getErrString("R_unableRetrieveColMeta"), var4);
      }
   }

   void populateMetadataFromDataRecord() throws SQLServerException {
      assert null != this.sourceRecord;
      if (0 >= this.sourceRecord.getColumnCount()) {
         throw new SQLServerException(SQLServerException.getErrString("R_TVPEmptyMetadata"), null);
      }

      for (int var1 = 0; var1 < this.sourceRecord.getColumnCount(); var1++) {
         Util.checkDuplicateColumnName(this.sourceRecord.getColumnMetaData(var1 + 1).columnName, this.columnMetadata);
         SQLServerMetaData var2 = new SQLServerMetaData(this.sourceRecord.getColumnMetaData(var1 + 1));
         this.columnMetadata.put(var1, var2);
      }
   }

   void validateOrderProperty() throws SQLServerException {
      int var1 = this.columnMetadata.size();
      boolean[] var2 = new boolean[var1];
      int var3 = -1;
      int var4 = 0;

      for (Entry var6 : this.columnMetadata.entrySet()) {
         SQLServerSortOrder var7 = ((SQLServerMetaData)var6.getValue()).sortOrder;
         int var8 = ((SQLServerMetaData)var6.getValue()).sortOrdinal;
         if (SQLServerSortOrder.Unspecified != var7) {
            if (var1 <= var8) {
               MessageFormat var12 = new MessageFormat(SQLServerException.getErrString("R_TVPSortOrdinalGreaterThanFieldCount"));
               throw new SQLServerException(var12.format(new Object[]{var8, var6.getKey()}), null, 0, null);
            }

            if (var2[var8]) {
               MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_TVPDuplicateSortOrdinal"));
               throw new SQLServerException(var9.format(new Object[]{var8}), null, 0, null);
            }

            var2[var8] = true;
            if (var8 > var3) {
               var3 = var8;
            }

            var4++;
         }
      }

      if (0 < var4 && var3 >= var4) {
         int var10 = 0;

         while (var10 < var4 && var2[var10]) {
            var10++;
         }

         MessageFormat var11 = new MessageFormat(SQLServerException.getErrString("R_TVPMissingSortOrdinal"));
         throw new SQLServerException(var11.format(new Object[]{var10}), null, 0, null);
      }
   }

   void parseTypeName(String var1) throws SQLServerException {
      String var2 = "[\"";
      String var3 = "]\"";
      byte var4 = 46;
      byte var5 = 3;
      String[] var6 = new String[var5];
      int var7 = 0;
      if (null != var1 && 0 != var1.length()) {
         StringBuilder var16 = new StringBuilder(var1.length());
         StringBuilder var17 = null;
         char var10 = ' ';
         TVP.MPIState var11 = TVP.MPIState.MPI_Value;

         for (int var12 = 0; var12 < var1.length(); var12++) {
            char var13 = var1.charAt(var12);
            switch (var11) {
               case MPI_Value:
                  if (!Character.isWhitespace(var13)) {
                     if (var13 == var4) {
                        var6[var7] = "";
                        var7++;
                     } else {
                        int var14;
                        if (-1 != (var14 = var2.indexOf(var13))) {
                           var10 = var3.charAt(var14);
                           var16.setLength(0);
                           var11 = TVP.MPIState.MPI_ParseQuote;
                        } else {
                           if (-1 != var3.indexOf(var13)) {
                              MessageFormat var25 = new MessageFormat(SQLServerException.getErrString("R_invalidThreePartName"));
                              throw new SQLServerException(null, var25.format(new Object[0]), null, 0, false);
                           }

                           var16.setLength(0);
                           var16.append(var13);
                           var11 = TVP.MPIState.MPI_ParseNonQuote;
                        }
                     }
                  }
                  break;
               case MPI_ParseNonQuote:
                  if (var13 == var4) {
                     var6[var7] = var16.toString();
                     var7 = this.incrementStringCount(var6, var7);
                     var11 = TVP.MPIState.MPI_Value;
                  } else {
                     if (-1 != var3.indexOf(var13) || -1 != var2.indexOf(var13)) {
                        MessageFormat var24 = new MessageFormat(SQLServerException.getErrString("R_invalidThreePartName"));
                        throw new SQLServerException(null, var24.format(new Object[0]), null, 0, false);
                     }

                     if (Character.isWhitespace(var13)) {
                        var6[var7] = var16.toString();
                        if (null == var17) {
                           var17 = new StringBuilder();
                        }

                        var17.setLength(0);
                        var17.append(var13);
                        var11 = TVP.MPIState.MPI_LookForNextCharOrSeparator;
                     } else {
                        var16.append(var13);
                     }
                  }
                  break;
               case MPI_LookForNextCharOrSeparator:
                  if (!Character.isWhitespace(var13)) {
                     if (var13 == var4) {
                        var7 = this.incrementStringCount(var6, var7);
                        var11 = TVP.MPIState.MPI_Value;
                     } else {
                        var16.append(var17);
                        var16.append(var13);
                        var6[var7] = var16.toString();
                        var11 = TVP.MPIState.MPI_ParseNonQuote;
                     }
                  } else {
                     var17.append(var13);
                  }
                  break;
               case MPI_ParseQuote:
                  if (var13 == var10) {
                     var11 = TVP.MPIState.MPI_RightQuote;
                  } else {
                     var16.append(var13);
                  }
                  break;
               case MPI_RightQuote:
                  if (var13 == var10) {
                     var16.append(var13);
                     var11 = TVP.MPIState.MPI_ParseQuote;
                  } else if (var13 == var4) {
                     var6[var7] = var16.toString();
                     var7 = this.incrementStringCount(var6, var7);
                     var11 = TVP.MPIState.MPI_Value;
                  } else {
                     if (!Character.isWhitespace(var13)) {
                        MessageFormat var23 = new MessageFormat(SQLServerException.getErrString("R_invalidThreePartName"));
                        throw new SQLServerException(null, var23.format(new Object[0]), null, 0, false);
                     }

                     var6[var7] = var16.toString();
                     var11 = TVP.MPIState.MPI_LookForSeparator;
                  }
                  break;
               case MPI_LookForSeparator:
                  if (!Character.isWhitespace(var13)) {
                     if (var13 != var4) {
                        MessageFormat var15 = new MessageFormat(SQLServerException.getErrString("R_invalidThreePartName"));
                        throw new SQLServerException(null, var15.format(new Object[0]), null, 0, false);
                     }

                     var7 = this.incrementStringCount(var6, var7);
                     var11 = TVP.MPIState.MPI_Value;
                  }
            }
         }

         if (var7 > var5 - 1) {
            MessageFormat var21 = new MessageFormat(SQLServerException.getErrString("R_invalidThreePartName"));
            throw new SQLServerException(null, var21.format(new Object[0]), null, 0, false);
         }

         switch (var11) {
            case MPI_ParseNonQuote:
            case MPI_RightQuote:
               var6[var7] = var16.toString();
            case MPI_Value:
            case MPI_LookForNextCharOrSeparator:
            case MPI_LookForSeparator:
               if (var6[0] == null) {
                  MessageFormat var20 = new MessageFormat(SQLServerException.getErrString("R_invalidThreePartName"));
                  throw new SQLServerException(null, var20.format(new Object[0]), null, 0, false);
               }

               int var19 = var5 - var7 - 1;
               if (var19 > 0) {
                  for (int var22 = var5 - 1; var22 >= var19; var22--) {
                     var6[var22] = var6[var22 - var19];
                     var6[var22 - var19] = null;
                  }
               }

               this.TVPName = var6[2];
               this.TVP_owningSchema = var6[1];
               this.TVP_dbName = var6[0];
               return;
            case MPI_ParseQuote:
            default:
               MessageFormat var18 = new MessageFormat(SQLServerException.getErrString("R_invalidThreePartName"));
               throw new SQLServerException(null, var18.format(new Object[0]), null, 0, false);
         }
      } else {
         MessageFormat var8 = new MessageFormat(SQLServerException.getErrString("R_invalidTVPName"));
         Object[] var9 = new Object[0];
         throw new SQLServerException(null, var8.format(var9), null, 0, false);
      }
   }

   private int incrementStringCount(String[] var1, int var2) throws SQLServerException {
      var2++;
      int var3 = var1.length;
      if (var2 >= var3) {
         MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_invalidThreePartName"));
         throw new SQLServerException(null, var4.format(new Object[0]), null, 0, false);
      } else {
         var1[var2] = new String();
         return var2;
      }
   }

   String getTVPName() {
      return this.TVPName;
   }

   String getDbNameTVP() {
      return this.TVP_dbName;
   }

   String getOwningSchemaNameTVP() {
      return this.TVP_owningSchema;
   }

   int getTVPColumnCount() {
      return this.columnMetadata.size();
   }

   Map<Integer, SQLServerMetaData> getColumnMetadata() {
      return this.columnMetadata;
   }

   enum MPIState {
      MPI_Value,
      MPI_ParseNonQuote,
      MPI_LookForSeparator,
      MPI_LookForNextCharOrSeparator,
      MPI_ParseQuote,
      MPI_RightQuote;
   }
}
