package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.UUID;

final class Column {
   private TypeInfo typeInfo;
   private CryptoMetadata cryptoMetadata;
   private DTV updaterDTV;
   private final DTV getterDTV = new DTV();
   private JDBCType jdbcTypeSetByUser = null;
   private int valueLength = 0;
   private String columnName;
   private String baseColumnName;
   private int tableNum;
   private int infoStatus;
   private SQLIdentifier tableName;
   ColumnFilter filter;

   final TypeInfo getTypeInfo() {
      return this.typeInfo;
   }

   final void setColumnName(String var1) {
      this.columnName = var1;
   }

   final String getColumnName() {
      return this.columnName;
   }

   final void setBaseColumnName(String var1) {
      this.baseColumnName = var1;
   }

   final String getBaseColumnName() {
      return this.baseColumnName;
   }

   final void setTableNum(int var1) {
      this.tableNum = var1;
   }

   final int getTableNum() {
      return this.tableNum;
   }

   final void setInfoStatus(int var1) {
      this.infoStatus = var1;
   }

   final boolean hasDifferentName() {
      return 0 != (this.infoStatus & 32);
   }

   final boolean isHidden() {
      return 0 != (this.infoStatus & 16);
   }

   final boolean isKey() {
      return 0 != (this.infoStatus & 8);
   }

   final boolean isExpression() {
      return 0 != (this.infoStatus & 4);
   }

   final boolean isUpdatable() {
      return !this.isExpression() && !this.isHidden() && this.tableName.getObjectName().length() > 0;
   }

   final void setTableName(SQLIdentifier var1) {
      this.tableName = var1;
   }

   final SQLIdentifier getTableName() {
      return this.tableName;
   }

   Column(TypeInfo var1, String var2, SQLIdentifier var3, CryptoMetadata var4) {
      this.typeInfo = var1;
      this.columnName = var2;
      this.baseColumnName = var2;
      this.tableName = var3;
      this.cryptoMetadata = var4;
   }

   CryptoMetadata getCryptoMetadata() {
      return this.cryptoMetadata;
   }

   final void clear() {
      this.getterDTV.clear();
   }

   final void skipValue(TDSReader var1, boolean var2) throws SQLServerException {
      this.getterDTV.skipValue(this.typeInfo, var1, var2);
   }

   final void initFromCompressedNull() {
      this.getterDTV.initFromCompressedNull();
   }

   void setFilter(ColumnFilter var1) {
      this.filter = var1;
   }

   final boolean isNull() {
      return this.getterDTV.isNull();
   }

   final boolean isInitialized() {
      return this.getterDTV.isInitialized();
   }

   Object getValue(JDBCType var1, InputStreamGetterArgs var2, Calendar var3, TDSReader var4) throws SQLServerException {
      Object var5 = this.getterDTV.getValue(var1, this.typeInfo.getScale(), var2, var3, this.typeInfo, this.cryptoMetadata, var4);
      return null != this.filter ? this.filter.apply(var5, var1) : var5;
   }

   int getInt(TDSReader var1) throws SQLServerException {
      return (Integer)this.getValue(JDBCType.INTEGER, null, null, var1);
   }

   void updateValue(
      JDBCType var1,
      Object var2,
      JavaType var3,
      StreamSetterArgs var4,
      Calendar var5,
      Integer var6,
      SQLServerConnection var7,
      SQLServerStatementColumnEncryptionSetting var8,
      Integer var9,
      boolean var10,
      int var11
   ) throws SQLServerException {
      SSType var12 = this.typeInfo.getSSType();
      if (null != this.cryptoMetadata) {
         if (null != var2) {
            if (JDBCType.TINYINT == this.cryptoMetadata.getBaseTypeInfo().getSSType().getJDBCType() && var3 == JavaType.SHORT) {
               if (var2 instanceof Boolean) {
                  if ((Boolean)var2) {
                     var2 = 1;
                  } else {
                     var2 = 0;
                  }
               }

               String var13 = "" + var2;
               Short var14 = Short.valueOf(var13);
               if (var14 >= 0 && var14 <= 255) {
                  var2 = var14.byteValue();
                  var3 = JavaType.BYTE;
                  var1 = JDBCType.TINYINT;
               }
            }
         } else if (var1.isBinary()) {
            var1 = this.cryptoMetadata.getBaseTypeInfo().getSSType().getJDBCType();
         }
      }

      if (null == var6 && null != this.cryptoMetadata) {
         var6 = this.cryptoMetadata.getBaseTypeInfo().getScale();
      }

      if (null != this.cryptoMetadata
         && (JDBCType.CHAR == var1 || JDBCType.VARCHAR == var1)
         && (
            JDBCType.NVARCHAR == this.cryptoMetadata.getBaseTypeInfo().getSSType().getJDBCType()
               || JDBCType.NCHAR == this.cryptoMetadata.getBaseTypeInfo().getSSType().getJDBCType()
               || JDBCType.LONGNVARCHAR == this.cryptoMetadata.getBaseTypeInfo().getSSType().getJDBCType()
         )) {
         var1 = this.cryptoMetadata.getBaseTypeInfo().getSSType().getJDBCType();
      }

      if (Util.shouldHonorAEForParameters(var8, var7)) {
         if (null == this.cryptoMetadata && var10) {
            MessageFormat var15 = new MessageFormat(SQLServerException.getErrString("R_ForceEncryptionTrue_HonorAETrue_UnencryptedColumnRS"));
            Object[] var18 = new Object[]{var11};
            throw new SQLServerException(null, var15.format(var18), null, 0, false);
         }

         this.setJdbcTypeSetByUser(var1);
         this.valueLength = Util.getValueLengthBaseOnJavaType(var2, var3, var9, var6, var1);
         if (null != this.cryptoMetadata
            && (
               JDBCType.NCHAR == this.cryptoMetadata.getBaseTypeInfo().getSSType().getJDBCType()
                  || JDBCType.NVARCHAR == this.cryptoMetadata.getBaseTypeInfo().getSSType().getJDBCType()
                  || JDBCType.LONGNVARCHAR == this.cryptoMetadata.getBaseTypeInfo().getSSType().getJDBCType()
            )) {
            this.valueLength *= 2;
         }
      } else if (var10) {
         MessageFormat var17 = new MessageFormat(SQLServerException.getErrString("R_ForceEncryptionTrue_HonorAEFalseRS"));
         Object[] var20 = new Object[]{var11};
         throw new SQLServerException(null, var17.format(var20), null, 0, false);
      }

      if (null != var4) {
         if (!var4.streamType.convertsTo(this.typeInfo)) {
            DataTypes.throwConversionError(var4.streamType.toString(), var12.toString());
         }
      } else if (null != this.cryptoMetadata) {
         if (JDBCType.UNKNOWN == var1 && var2 instanceof UUID) {
            var3 = JavaType.STRING;
            var1 = JDBCType.GUID;
            this.setJdbcTypeSetByUser(var1);
         }

         SSType var16 = this.cryptoMetadata.baseTypeInfo.getSSType();
         if (!var1.convertsTo(var16)) {
            DataTypes.throwConversionError(var1.toString(), var12.toString());
         }

         JDBCType var19 = getJDBCTypeFromBaseSSType(var16, var1);
         if (var19 != var1) {
            this.setJdbcTypeSetByUser(var19);
            var1 = var19;
            this.valueLength = Util.getValueLengthBaseOnJavaType(var2, var3, var9, var6, var1);
         }
      } else if (!var1.convertsTo(var12)) {
         DataTypes.throwConversionError(var1.toString(), var12.toString());
      }

      if ((JDBCType.DATETIMEOFFSET == var1 || JavaType.DATETIMEOFFSET == var3) && !var7.isKatmaiOrLater()) {
         throw new SQLServerException(SQLServerException.getErrString("R_notSupported"), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET, null);
      }

      if (null != this.cryptoMetadata && var7.sendStringParametersAsUnicode() && (JavaType.STRING == var3 || JavaType.READER == var3 || JavaType.CLOB == var3)) {
         var1 = getSSPAUJDBCType(var1);
      }

      if (SSType.NCHAR != var12 && SSType.NVARCHAR != var12 && SSType.NVARCHARMAX != var12 && SSType.NTEXT != var12 && SSType.XML != var12
         || JDBCType.CHAR != var1 && JDBCType.VARCHAR != var1 && JDBCType.LONGVARCHAR != var1 && JDBCType.CLOB != var1) {
         if (SSType.BINARY != var12 && SSType.VARBINARY != var12 && SSType.VARBINARYMAX != var12 && SSType.IMAGE != var12 && SSType.UDT != var12
            || JDBCType.CHAR != var1 && JDBCType.VARCHAR != var1 && JDBCType.LONGVARCHAR != var1) {
            if ((JDBCType.TIMESTAMP == var1 || JDBCType.DATE == var1 || JDBCType.TIME == var1 || JDBCType.DATETIMEOFFSET == var1)
               && (
                  SSType.CHAR == var12
                     || SSType.VARCHAR == var12
                     || SSType.VARCHARMAX == var12
                     || SSType.TEXT == var12
                     || SSType.NCHAR == var12
                     || SSType.NVARCHAR == var12
                     || SSType.NVARCHARMAX == var12
                     || SSType.NTEXT == var12
               )) {
               var1 = JDBCType.NCHAR;
            }
         } else {
            var1 = JDBCType.VARBINARY;
         }
      } else {
         var1 = JDBCType.CLOB == var1 ? JDBCType.NCLOB : JDBCType.NVARCHAR;
      }

      if (null == this.updaterDTV) {
         this.updaterDTV = new DTV();
      }

      this.updaterDTV.setValue(this.typeInfo.getSQLCollation(), var1, var2, var3, var4, var5, var6, var7, false);
   }

   private static JDBCType getSSPAUJDBCType(JDBCType var0) {
      switch (var0) {
         case CHAR:
            return JDBCType.NCHAR;
         case VARCHAR:
            return JDBCType.NVARCHAR;
         case LONGVARCHAR:
            return JDBCType.LONGNVARCHAR;
         case CLOB:
            return JDBCType.NCLOB;
         default:
            return var0;
      }
   }

   private static JDBCType getJDBCTypeFromBaseSSType(SSType var0, JDBCType var1) {
      switch (var1) {
         case CHAR:
            if (SSType.GUID == var0) {
               return JDBCType.GUID;
            }
         case VARCHAR:
         case LONGVARCHAR:
         case CLOB:
         default:
            return var1;
         case TIMESTAMP:
            if (SSType.DATETIME == var0) {
               return JDBCType.DATETIME;
            } else {
               if (SSType.SMALLDATETIME == var0) {
                  return JDBCType.SMALLDATETIME;
               }

               return var1;
            }
         case NUMERIC:
         case DECIMAL:
            if (SSType.MONEY == var0) {
               return JDBCType.MONEY;
            } else {
               return SSType.SMALLMONEY == var0 ? JDBCType.SMALLMONEY : var1;
            }
      }
   }

   boolean hasUpdates() {
      return null != this.updaterDTV;
   }

   void cancelUpdates() {
      this.updaterDTV = null;
   }

   void sendByRPC(TDSWriter var1, SQLServerConnection var2) throws SQLServerException {
      if (null != this.updaterDTV) {
         try {
            this.updaterDTV.sendCryptoMetaData(this.cryptoMetadata, var1);
            this.updaterDTV.jdbcTypeSetByUser(this.getJdbcTypeSetByUser(), this.getValueLength());
            this.updaterDTV
               .sendByRPC(
                  this.baseColumnName,
                  this.typeInfo,
                  null != this.cryptoMetadata ? this.cryptoMetadata.getBaseTypeInfo().getSQLCollation() : this.typeInfo.getSQLCollation(),
                  null != this.cryptoMetadata ? this.cryptoMetadata.getBaseTypeInfo().getPrecision() : this.typeInfo.getPrecision(),
                  null != this.cryptoMetadata ? this.cryptoMetadata.getBaseTypeInfo().getScale() : this.typeInfo.getScale(),
                  false,
                  var1,
                  var2
               );
         } finally {
            this.updaterDTV.sendCryptoMetaData(null, var1);
         }
      }
   }

   JDBCType getJdbcTypeSetByUser() {
      return this.jdbcTypeSetByUser;
   }

   void setJdbcTypeSetByUser(JDBCType var1) {
      this.jdbcTypeSetByUser = var1;
   }

   int getValueLength() {
      return this.valueLength;
   }
}
