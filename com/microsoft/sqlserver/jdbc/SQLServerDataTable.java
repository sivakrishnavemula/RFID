package com.microsoft.sqlserver.jdbc;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import microsoft.sql.DateTimeOffset;

public final class SQLServerDataTable {
   int rowCount = 0;
   int columnCount = 0;
   Map<Integer, SQLServerDataColumn> columnMetadata = null;
   Map<Integer, Object[]> rows = null;

   public SQLServerDataTable() throws SQLServerException {
      this.columnMetadata = new LinkedHashMap();
      this.rows = new HashMap();
   }

   public synchronized void clear() {
      this.rowCount = 0;
      this.columnCount = 0;
      this.columnMetadata.clear();
      this.rows.clear();
   }

   public synchronized Iterator<Entry<Integer, Object[]>> getIterator() {
      return null != this.rows && null != this.rows.entrySet() ? this.rows.entrySet().iterator() : null;
   }

   public synchronized void addColumnMetadata(String var1, int var2) throws SQLServerException {
      Util.checkDuplicateColumnName(var1, this.columnMetadata);
      this.columnMetadata.put(this.columnCount++, new SQLServerDataColumn(var1, var2));
   }

   public synchronized void addColumnMetadata(SQLServerDataColumn var1) throws SQLServerException {
      Util.checkDuplicateColumnName(var1.columnName, this.columnMetadata);
      this.columnMetadata.put(this.columnCount++, var1);
   }

   public synchronized void addRow(Object... var1) throws SQLServerException {
      try {
         int var2 = this.columnMetadata.size();
         if (null != var1 && var1.length > var2) {
            MessageFormat var18 = new MessageFormat(SQLServerException.getErrString("R_moreDataInRowThanColumnInTVP"));
            Object[] var19 = new Object[0];
            throw new SQLServerException(null, var18.format(var19), null, 0, false);
         }

         Iterator var3 = this.columnMetadata.entrySet().iterator();
         Object[] var4 = new Object[var2];
         int var5 = 0;

         while (var3.hasNext()) {
            Object var6 = null;
            if (null != var1 && var5 < var1.length && null != var1[var5]) {
               var6 = null == var1[var5] ? null : var1[var5];
            }

            var5++;
            Entry var9 = (Entry)var3.next();
            SQLServerDataColumn var10 = (SQLServerDataColumn)var9.getValue();
            JDBCType var11 = JDBCType.of(((SQLServerDataColumn)var9.getValue()).javaSqlType);
            boolean var12 = false;
            switch (var11) {
               case BIGINT:
                  var4[var9.getKey()] = null == var6 ? null : Long.parseLong(var6.toString());
                  break;
               case BIT:
                  var4[var9.getKey()] = null == var6 ? null : Boolean.parseBoolean(var6.toString());
                  break;
               case INTEGER:
                  var4[var9.getKey()] = null == var6 ? null : Integer.parseInt(var6.toString());
                  break;
               case SMALLINT:
               case TINYINT:
                  var4[var9.getKey()] = null == var6 ? null : Short.parseShort(var6.toString());
                  break;
               case DECIMAL:
               case NUMERIC:
                  BigDecimal var13 = null;
                  if (null != var6) {
                     var13 = new BigDecimal(var6.toString());
                     if (var13.scale() > var10.scale) {
                        var10.scale = var13.scale();
                        var12 = true;
                     }

                     if (var13.precision() > var10.precision) {
                        var10.precision = var13.precision();
                        var12 = true;
                     }

                     if (var12) {
                        this.columnMetadata.put(var9.getKey(), var10);
                     }
                  }

                  var4[var9.getKey()] = var13;
                  break;
               case DOUBLE:
                  var4[var9.getKey()] = null == var6 ? null : Double.parseDouble(var6.toString());
                  break;
               case FLOAT:
               case REAL:
                  var4[var9.getKey()] = null == var6 ? null : Float.parseFloat(var6.toString());
                  break;
               case TIMESTAMP_WITH_TIMEZONE:
               case TIME_WITH_TIMEZONE:
                  DriverJDBCVersion.checkSupportsJDBC42();
               case DATE:
               case TIME:
               case TIMESTAMP:
               case DATETIMEOFFSET:
                  if (null == var6) {
                     var4[var9.getKey()] = null;
                  } else if (var6 instanceof Date) {
                     var4[var9.getKey()] = ((Date)var6).toString();
                  } else if (var6 instanceof DateTimeOffset) {
                     var4[var9.getKey()] = ((DateTimeOffset)var6).toString();
                  } else if (var6 instanceof OffsetDateTime) {
                     var4[var9.getKey()] = ((OffsetDateTime)var6).toString();
                  } else if (var6 instanceof OffsetTime) {
                     var4[var9.getKey()] = ((OffsetTime)var6).toString();
                  } else {
                     var4[var9.getKey()] = null == var6 ? null : (String)var6;
                  }
                  break;
               case BINARY:
               case VARBINARY:
                  boolean var20 = null == var6;
                  int var21 = var20 ? 0 : ((byte[])var6).length;
                  if (var21 > var10.precision) {
                     var10.precision = var21;
                     this.columnMetadata.put(var9.getKey(), var10);
                  }

                  var4[var9.getKey()] = var20 ? null : (byte[])var6;
                  break;
               case CHAR:
                  if (var6 instanceof UUID && var6 != null) {
                     var6 = ((UUID)var6).toString();
                  }
               case VARCHAR:
               case NCHAR:
               case NVARCHAR:
                  boolean var7 = null == var6;
                  int var8 = var7 ? 0 : 2 * ((String)var6).length();
                  if (var8 > var10.precision) {
                     var10.precision = var8;
                     this.columnMetadata.put(var9.getKey(), var10);
                  }

                  var4[var9.getKey()] = var7 ? null : (String)var6;
                  break;
               default:
                  MessageFormat var14 = new MessageFormat(SQLServerException.getErrString("R_unsupportedDataTypeTVP"));
                  Object[] var15 = new Object[]{var11};
                  throw new SQLServerException(null, var14.format(var15), null, 0, false);
            }
         }

         this.rows.put(this.rowCount++, var4);
      } catch (NumberFormatException var16) {
         throw new SQLServerException(SQLServerException.getErrString("R_TVPInvalidColumnValue"), var16);
      } catch (ClassCastException var17) {
         throw new SQLServerException(SQLServerException.getErrString("R_TVPInvalidColumnValue"), var17);
      }
   }

   public synchronized Map<Integer, SQLServerDataColumn> getColumnMetadata() {
      return this.columnMetadata;
   }
}
