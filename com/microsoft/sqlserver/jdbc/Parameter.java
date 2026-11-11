package com.microsoft.sqlserver.jdbc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Calendar;
import java.util.Locale;
import microsoft.sql.DateTimeOffset;

final class Parameter {
   private TypeInfo typeInfo;
   CryptoMetadata cryptoMeta = null;
   private boolean shouldHonorAEForParameter = false;
   private boolean userProvidesPrecision = false;
   private boolean userProvidesScale = false;
   private String typeDefinition = null;
   boolean renewDefinition = false;
   private JDBCType jdbcTypeSetByUser = null;
   private int valueLength = 0;
   private boolean forceEncryption = false;
   int scale = 0;
   private int outScale = 4;
   private String name;
   private DTV getterDTV;
   private DTV registeredOutDTV = null;
   private DTV setterDTV = null;
   private DTV inputDTV = null;

   TypeInfo getTypeInfo() {
      return this.typeInfo;
   }

   final CryptoMetadata getCryptoMetadata() {
      return this.cryptoMeta;
   }

   Parameter(boolean var1) {
      this.shouldHonorAEForParameter = var1;
   }

   boolean isOutput() {
      return null != this.registeredOutDTV;
   }

   JDBCType getJdbcType() throws SQLServerException {
      return null != this.inputDTV ? this.inputDTV.getJdbcType() : JDBCType.UNKNOWN;
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

   void registerForOutput(JDBCType var1, SQLServerConnection var2) throws SQLServerException {
      if (JDBCType.DATETIMEOFFSET == var1 && !var2.isKatmaiOrLater()) {
         throw new SQLServerException(SQLServerException.getErrString("R_notSupported"), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET, null);
      }

      if (var2.sendStringParametersAsUnicode()) {
         if (this.shouldHonorAEForParameter) {
            this.setJdbcTypeSetByUser(var1);
         }

         var1 = getSSPAUJDBCType(var1);
      }

      this.registeredOutDTV = new DTV();
      this.registeredOutDTV.setJdbcType(var1);
      if (null == this.setterDTV) {
         this.inputDTV = this.registeredOutDTV;
      }

      this.resetOutputValue();
   }

   int getOutScale() {
      return this.outScale;
   }

   void setOutScale(int var1) {
      this.outScale = var1;
      this.userProvidesScale = true;
   }

   final Parameter cloneForBatch() {
      Parameter var1 = new Parameter(this.shouldHonorAEForParameter);
      var1.typeInfo = this.typeInfo;
      var1.typeDefinition = this.typeDefinition;
      var1.outScale = this.outScale;
      var1.name = this.name;
      var1.getterDTV = this.getterDTV;
      var1.registeredOutDTV = this.registeredOutDTV;
      var1.setterDTV = this.setterDTV;
      var1.inputDTV = this.inputDTV;
      var1.cryptoMeta = this.cryptoMeta;
      var1.jdbcTypeSetByUser = this.jdbcTypeSetByUser;
      var1.valueLength = this.valueLength;
      var1.userProvidesPrecision = this.userProvidesPrecision;
      var1.userProvidesScale = this.userProvidesScale;
      return var1;
   }

   final void skipValue(TDSReader var1, boolean var2) throws SQLServerException {
      if (null == this.getterDTV) {
         this.getterDTV = new DTV();
      }

      this.deriveTypeInfo(var1);
      this.getterDTV.skipValue(this.typeInfo, var1, var2);
   }

   final void skipRetValStatus(TDSReader var1) throws SQLServerException {
      StreamRetValue var2 = new StreamRetValue();
      var2.setFromTDS(var1);
   }

   void clearInputValue() {
      this.setterDTV = null;
      this.inputDTV = this.registeredOutDTV;
   }

   void resetOutputValue() {
      this.getterDTV = null;
      this.typeInfo = null;
   }

   void deriveTypeInfo(TDSReader var1) throws SQLServerException {
      if (null == this.typeInfo) {
         this.typeInfo = TypeInfo.getInstance(var1, true);
         if (this.shouldHonorAEForParameter && this.typeInfo.isEncrypted()) {
            CekTableEntry var2 = this.cryptoMeta.getCekTableEntry();
            this.cryptoMeta = new StreamRetValue().getCryptoMetadata(var1);
            this.cryptoMeta.setCekTableEntry(var2);
         }
      }
   }

   void setFromReturnStatus(int var1, SQLServerConnection var2) throws SQLServerException {
      if (null == this.getterDTV) {
         this.getterDTV = new DTV();
      }

      this.getterDTV.setValue(null, JDBCType.INTEGER, new Integer(var1), JavaType.INTEGER, null, null, null, var2, this.getForceEncryption());
   }

   void setValue(
      JDBCType var1,
      Object var2,
      JavaType var3,
      StreamSetterArgs var4,
      Calendar var5,
      Integer var6,
      Integer var7,
      SQLServerConnection var8,
      boolean var9,
      SQLServerStatementColumnEncryptionSetting var10,
      int var11,
      String var12,
      String var13
   ) throws SQLServerException {
      if (this.shouldHonorAEForParameter) {
         this.userProvidesPrecision = false;
         this.userProvidesScale = false;
         if (null != var6) {
            this.userProvidesPrecision = true;
         }

         if (null != var7) {
            this.userProvidesScale = true;
         }

         if (!this.isOutput() && JavaType.SHORT == var3 && (JDBCType.TINYINT == var1 || JDBCType.SMALLINT == var1)) {
            if ((Short)var2 >= 0 && (Short)var2 <= 255) {
               var2 = ((Short)var2).byteValue();
               var3 = JavaType.of(var2);
               var1 = var3.getJDBCType(SSType.UNKNOWN, var1);
            } else if (JDBCType.TINYINT == var1) {
               MessageFormat var14 = new MessageFormat(SQLServerException.getErrString("R_InvalidDataForAE"));
               Object[] var15 = new Object[]{var3.toString().toLowerCase(Locale.ENGLISH), var1.toString().toLowerCase(Locale.ENGLISH)};
               throw new SQLServerException(var14.format(var15), null);
            }
         }
      }

      if (var9 && !Util.shouldHonorAEForParameters(var10, var8)) {
         MessageFormat var17 = new MessageFormat(SQLServerException.getErrString("R_ForceEncryptionTrue_HonorAEFalse"));
         Object[] var21 = new Object[]{var11, var12};
         SQLServerException.makeFromDriverError(var8, this, var17.format(var21), null, true);
      }

      if ((JDBCType.DATETIMEOFFSET == var1 || JavaType.DATETIMEOFFSET == var3) && !var8.isKatmaiOrLater()) {
         throw new SQLServerException(SQLServerException.getErrString("R_notSupported"), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET, null);
      }

      if (JavaType.TVP == var3) {
         TVP var18 = null;
         if (null == var2) {
            var18 = new TVP(var13);
         } else if (var2 instanceof SQLServerDataTable) {
            var18 = new TVP(var13, (SQLServerDataTable)var2);
         } else if (var2 instanceof ResultSet) {
            if (var8.getSelectMethod().equalsIgnoreCase("cursor") && var2 instanceof SQLServerResultSet) {
               SQLServerStatement var22 = (SQLServerStatement)((SQLServerResultSet)var2).getStatement();
               if (var8.equals(var22.connection)) {
                  if (Locale.getDefault().getLanguage() == Locale.ENGLISH.getLanguage()) {
                     throw new SQLServerException(SQLServerException.getErrString("R_invalidServerCursorForTVP"), null);
                  }

                  throw new SQLServerException(SQLServerException.getErrString("R_notSupported"), null, 0, null);
               }
            }

            var18 = new TVP(var13, (ResultSet)var2);
         } else {
            if (!(var2 instanceof ISQLServerDataRecord)) {
               MessageFormat var23 = new MessageFormat(SQLServerException.getErrString("R_TVPInvalidValue"));
               Object[] var16 = new Object[]{var11};
               throw new SQLServerException(var23.format(var16), null);
            }

            var18 = new TVP(var13, (ISQLServerDataRecord)var2);
         }

         if (!var18.isNull() && 0 == var18.getTVPColumnCount()) {
            throw new SQLServerException(SQLServerException.getErrString("R_TVPEmptyMetadata"), null);
         }

         this.name = var18.getTVPName();
         var2 = var18;
      }

      if (this.shouldHonorAEForParameter) {
         this.setForceEncryption(var9);
         if (!this.isOutput() || this.jdbcTypeSetByUser == null) {
            this.setJdbcTypeSetByUser(var1);
         }

         if (!var1.isTextual() && !var1.isBinary() || !this.isOutput() || this.valueLength == 0) {
            this.valueLength = Util.getValueLengthBaseOnJavaType(var2, var3, var6, var7, var1);
         }

         if (null != var7) {
            this.outScale = var7;
         }
      }

      if (var8.sendStringParametersAsUnicode() && (JavaType.STRING == var3 || JavaType.READER == var3 || JavaType.CLOB == var3)) {
         var1 = getSSPAUJDBCType(var1);
      }

      DTV var20 = new DTV();
      var20.setValue(var8.getDatabaseCollation(), var1, var2, var3, var4, var5, var7, var8, var9);
      this.inputDTV = this.setterDTV = var20;
   }

   boolean isNull() {
      return null != this.getterDTV ? this.getterDTV.isNull() : false;
   }

   boolean isValueGotten() {
      return null != this.getterDTV;
   }

   Object getValue(JDBCType var1, InputStreamGetterArgs var2, Calendar var3, TDSReader var4) throws SQLServerException {
      if (null == this.getterDTV) {
         this.getterDTV = new DTV();
      }

      this.deriveTypeInfo(var4);
      return this.getterDTV.getValue(var1, this.outScale, var2, var3, this.typeInfo, this.cryptoMeta, var4);
   }

   int getInt(TDSReader var1) throws SQLServerException {
      Integer var2 = (Integer)this.getValue(JDBCType.INTEGER, null, null, var1);
      return null != var2 ? var2 : 0;
   }

   String getTypeDefinition(SQLServerConnection var1, TDSReader var2) throws SQLServerException {
      if (null == this.inputDTV) {
         return null;
      }

      this.inputDTV.executeOp(new Parameter.GetTypeDefinitionOp(this, var1));
      return this.typeDefinition;
   }

   void sendByRPC(TDSWriter var1, SQLServerConnection var2) throws SQLServerException {
      assert null != this.inputDTV : "Parameter was neither set nor registered";

      try {
         this.inputDTV.sendCryptoMetaData(this.cryptoMeta, var1);
         this.inputDTV.jdbcTypeSetByUser(this.getJdbcTypeSetByUser(), this.getValueLength());
         this.inputDTV
            .sendByRPC(
               this.name, null, var2.getDatabaseCollation(), this.valueLength, this.isOutput() ? this.outScale : this.scale, this.isOutput(), var1, var2
            );
      } finally {
         this.inputDTV.sendCryptoMetaData(null, var1);
      }

      if (JavaType.INPUTSTREAM == this.inputDTV.getJavaType() || JavaType.READER == this.inputDTV.getJavaType()) {
         this.inputDTV = this.setterDTV = null;
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

   void setValueLength(int var1) {
      this.valueLength = var1;
      this.userProvidesPrecision = true;
   }

   boolean getForceEncryption() {
      return this.forceEncryption;
   }

   void setForceEncryption(boolean var1) {
      this.forceEncryption = var1;
   }

   final class GetTypeDefinitionOp extends DTVExecuteOp {
      private static final String NVARCHAR_MAX = "nvarchar(max)";
      private static final String NVARCHAR_4K = "nvarchar(4000)";
      private static final String NTEXT = "ntext";
      private static final String VARCHAR_MAX = "varchar(max)";
      private static final String VARCHAR_8K = "varchar(8000)";
      private static final String TEXT = "text";
      private static final String VARBINARY_MAX = "varbinary(max)";
      private static final String VARBINARY_8K = "varbinary(8000)";
      private static final String IMAGE = "image";
      private final Parameter param;
      private final SQLServerConnection con;

      GetTypeDefinitionOp(Parameter var2, SQLServerConnection var3) {
         this.param = var2;
         this.con = var3;
      }

      private void setTypeDefinition(DTV var1) {
         switch (var1.getJdbcType()) {
            case CHAR:
            case VARCHAR:
               if ("varchar(max)" != this.param.typeDefinition && "text" != this.param.typeDefinition) {
                  if (this.param.shouldHonorAEForParameter
                     && null != Parameter.this.jdbcTypeSetByUser
                     && (null != this.param.getCryptoMetadata() || !this.param.renewDefinition)) {
                     if (0 == Parameter.this.valueLength) {
                        this.param.typeDefinition = "varchar";
                     } else {
                        this.param.typeDefinition = "varchar(" + Parameter.this.valueLength + ")";
                        if (8000 <= Parameter.this.valueLength) {
                           this.param.typeDefinition = "varchar(max)";
                        }
                     }
                  } else {
                     this.param.typeDefinition = "varchar(8000)";
                  }
               }
               break;
            case LONGVARCHAR:
            case CLOB:
               this.param.typeDefinition = "varchar(max)";
               break;
            case TINYINT:
               this.param.typeDefinition = SSType.TINYINT.toString();
               break;
            case SMALLINT:
               this.param.typeDefinition = SSType.SMALLINT.toString();
               break;
            case INTEGER:
               this.param.typeDefinition = SSType.INTEGER.toString();
               break;
            case BIGINT:
               this.param.typeDefinition = SSType.BIGINT.toString();
               break;
            case REAL:
               if (this.param.shouldHonorAEForParameter
                  && null != Parameter.this.jdbcTypeSetByUser
                  && (null != this.param.getCryptoMetadata() || !this.param.renewDefinition)) {
                  this.param.typeDefinition = SSType.REAL.toString();
                  break;
               }
            case FLOAT:
            case DOUBLE:
               this.param.typeDefinition = SSType.FLOAT.toString();
               break;
            case DECIMAL:
            case NUMERIC:
               if (Parameter.this.scale > 38) {
                  Parameter.this.scale = 38;
               }

               Integer var2 = var1.getScale();
               if (null != var2 && Parameter.this.scale < var2) {
                  Parameter.this.scale = var2;
               }

               if (this.param.isOutput() && Parameter.this.scale < this.param.getOutScale()) {
                  Parameter.this.scale = this.param.getOutScale();
               }

               if (this.param.shouldHonorAEForParameter
                  && null != Parameter.this.jdbcTypeSetByUser
                  && (null != this.param.getCryptoMetadata() || !this.param.renewDefinition)) {
                  if (0 == Parameter.this.valueLength) {
                     if (!Parameter.this.isOutput()) {
                        this.param.typeDefinition = "decimal(18, " + Parameter.this.scale + ")";
                     }
                  } else if (18 >= Parameter.this.valueLength) {
                     this.param.typeDefinition = "decimal(18," + Parameter.this.scale + ")";
                     if (18 < Parameter.this.valueLength + Parameter.this.scale) {
                        this.param.typeDefinition = "decimal(" + (18 + Parameter.this.scale) + "," + Parameter.this.scale + ")";
                     }
                  } else {
                     this.param.typeDefinition = "decimal(38," + Parameter.this.scale + ")";
                  }

                  if (Parameter.this.isOutput()) {
                     this.param.typeDefinition = "decimal(38, " + Parameter.this.scale + ")";
                  }

                  if (Parameter.this.userProvidesPrecision) {
                     this.param.typeDefinition = "decimal(" + Parameter.this.valueLength + "," + Parameter.this.scale + ")";
                  }
               } else {
                  this.param.typeDefinition = "decimal(38," + Parameter.this.scale + ")";
               }
               break;
            case MONEY:
               this.param.typeDefinition = SSType.MONEY.toString();
               break;
            case SMALLMONEY:
               this.param.typeDefinition = SSType.SMALLMONEY.toString();
               break;
            case BIT:
            case BOOLEAN:
               this.param.typeDefinition = SSType.BIT.toString();
               break;
            case LONGVARBINARY:
            case BLOB:
               this.param.typeDefinition = "varbinary(max)";
               break;
            case BINARY:
            case VARBINARY:
               if ("varbinary(max)" != this.param.typeDefinition && "image" != this.param.typeDefinition) {
                  if (this.param.shouldHonorAEForParameter
                     && null != Parameter.this.jdbcTypeSetByUser
                     && (null != this.param.getCryptoMetadata() || !this.param.renewDefinition)) {
                     if (0 == Parameter.this.valueLength) {
                        this.param.typeDefinition = "varbinary";
                     } else {
                        this.param.typeDefinition = "varbinary(" + Parameter.this.valueLength + ")";
                     }

                     if (JDBCType.LONGVARBINARY == Parameter.this.jdbcTypeSetByUser) {
                        this.param.typeDefinition = "varbinary(max)";
                     }
                  } else {
                     this.param.typeDefinition = "varbinary(8000)";
                  }
               }
               break;
            case DATE:
               this.param.typeDefinition = this.con.isKatmaiOrLater() ? SSType.DATE.toString() : SSType.DATETIME.toString();
               break;
            case TIME:
               if (this.param.shouldHonorAEForParameter && (null != this.param.getCryptoMetadata() || !this.param.renewDefinition)) {
                  if (Parameter.this.userProvidesScale) {
                     this.param.typeDefinition = SSType.TIME.toString() + "(" + Parameter.this.outScale + ")";
                  } else {
                     this.param.typeDefinition = this.param.typeDefinition = SSType.TIME.toString() + "(" + Parameter.this.valueLength + ")";
                  }
               } else {
                  this.param.typeDefinition = this.con.getSendTimeAsDatetime() ? SSType.DATETIME.toString() : SSType.TIME.toString();
               }
               break;
            case TIMESTAMP:
               if (this.param.shouldHonorAEForParameter && (null != this.param.getCryptoMetadata() || !this.param.renewDefinition)) {
                  if (Parameter.this.userProvidesScale) {
                     this.param.typeDefinition = this.con.isKatmaiOrLater()
                        ? SSType.DATETIME2.toString() + "(" + Parameter.this.outScale + ")"
                        : SSType.DATETIME.toString();
                  } else {
                     this.param.typeDefinition = this.con.isKatmaiOrLater()
                        ? SSType.DATETIME2.toString() + "(" + Parameter.this.valueLength + ")"
                        : SSType.DATETIME.toString();
                  }
               } else {
                  this.param.typeDefinition = this.con.isKatmaiOrLater() ? SSType.DATETIME2.toString() : SSType.DATETIME.toString();
               }
               break;
            case DATETIME:
               this.param.typeDefinition = SSType.DATETIME.toString();
               if (!this.param.shouldHonorAEForParameter) {
                  if (this.param.isOutput()) {
                     this.param.typeDefinition = SSType.DATETIME2.toString() + "(" + Parameter.this.outScale + ")";
                  }
               } else if (null == this.param.getCryptoMetadata() && this.param.renewDefinition && this.param.isOutput()) {
                  this.param.typeDefinition = SSType.DATETIME2.toString() + "(" + Parameter.this.outScale + ")";
               }
               break;
            case SMALLDATETIME:
               this.param.typeDefinition = SSType.SMALLDATETIME.toString();
               break;
            case TIME_WITH_TIMEZONE:
            case TIMESTAMP_WITH_TIMEZONE:
            case DATETIMEOFFSET:
               if (this.param.shouldHonorAEForParameter && (null != this.param.getCryptoMetadata() || !this.param.renewDefinition)) {
                  if (Parameter.this.userProvidesScale) {
                     this.param.typeDefinition = SSType.DATETIMEOFFSET.toString() + "(" + Parameter.this.outScale + ")";
                  } else {
                     this.param.typeDefinition = SSType.DATETIMEOFFSET.toString() + "(" + Parameter.this.valueLength + ")";
                  }
               } else {
                  this.param.typeDefinition = SSType.DATETIMEOFFSET.toString();
               }
               break;
            case LONGNVARCHAR:
               if (this.param.shouldHonorAEForParameter && (null != this.param.getCryptoMetadata() || !this.param.renewDefinition)) {
                  if (null == Parameter.this.jdbcTypeSetByUser
                     || Parameter.this.jdbcTypeSetByUser != JDBCType.VARCHAR
                        && Parameter.this.jdbcTypeSetByUser != JDBCType.CHAR
                        && Parameter.this.jdbcTypeSetByUser != JDBCType.LONGVARCHAR) {
                     if (null != Parameter.this.jdbcTypeSetByUser
                        && (Parameter.this.jdbcTypeSetByUser == JDBCType.NVARCHAR || Parameter.this.jdbcTypeSetByUser == JDBCType.LONGNVARCHAR)) {
                        if (0 == Parameter.this.valueLength) {
                           this.param.typeDefinition = "nvarchar";
                        } else if (4000 < Parameter.this.valueLength) {
                           this.param.typeDefinition = "nvarchar(max)";
                        } else {
                           this.param.typeDefinition = "nvarchar(" + Parameter.this.valueLength + ")";
                        }

                        if (Parameter.this.jdbcTypeSetByUser == JDBCType.LONGNVARCHAR) {
                           this.param.typeDefinition = "nvarchar(max)";
                        }
                     } else if (0 == Parameter.this.valueLength) {
                        this.param.typeDefinition = "nvarchar";
                     } else {
                        this.param.typeDefinition = "nvarchar(" + Parameter.this.valueLength + ")";
                        if (8000 <= Parameter.this.valueLength) {
                           this.param.typeDefinition = "nvarchar(max)";
                        }
                     }
                  } else {
                     if (0 == Parameter.this.valueLength) {
                        this.param.typeDefinition = "varchar";
                     } else if (8000 < Parameter.this.valueLength) {
                        this.param.typeDefinition = "varchar(max)";
                     } else {
                        this.param.typeDefinition = "varchar(" + Parameter.this.valueLength + ")";
                     }

                     if (Parameter.this.jdbcTypeSetByUser == JDBCType.LONGVARCHAR) {
                        this.param.typeDefinition = "varchar(max)";
                     }
                  }
               } else {
                  this.param.typeDefinition = "nvarchar(max)";
               }
               break;
            case NCLOB:
               this.param.typeDefinition = "nvarchar(max)";
               break;
            case NCHAR:
            case NVARCHAR:
               if ("nvarchar(max)" != this.param.typeDefinition && "ntext" != this.param.typeDefinition) {
                  if (this.param.shouldHonorAEForParameter && (null != this.param.getCryptoMetadata() || !this.param.renewDefinition)) {
                     if (null == Parameter.this.jdbcTypeSetByUser
                        || Parameter.this.jdbcTypeSetByUser != JDBCType.VARCHAR
                           && Parameter.this.jdbcTypeSetByUser != JDBCType.CHAR
                           && JDBCType.LONGVARCHAR != Parameter.this.jdbcTypeSetByUser) {
                        if (null != Parameter.this.jdbcTypeSetByUser
                           && (
                              Parameter.this.jdbcTypeSetByUser == JDBCType.NVARCHAR
                                 || Parameter.this.jdbcTypeSetByUser == JDBCType.NCHAR
                                 || JDBCType.LONGNVARCHAR == Parameter.this.jdbcTypeSetByUser
                           )) {
                           if (0 == Parameter.this.valueLength) {
                              this.param.typeDefinition = "nvarchar";
                           } else {
                              this.param.typeDefinition = "nvarchar(" + Parameter.this.valueLength + ")";
                              if (8000 <= Parameter.this.valueLength) {
                                 this.param.typeDefinition = "nvarchar(max)";
                              }
                           }

                           if (JDBCType.LONGNVARCHAR == Parameter.this.jdbcTypeSetByUser) {
                              this.param.typeDefinition = "nvarchar(max)";
                           }
                        } else if (0 == Parameter.this.valueLength) {
                           this.param.typeDefinition = "nvarchar";
                        } else {
                           this.param.typeDefinition = "nvarchar(" + Parameter.this.valueLength + ")";
                           if (8000 <= Parameter.this.valueLength) {
                              this.param.typeDefinition = "nvarchar(max)";
                           }
                        }
                     } else {
                        if (0 == Parameter.this.valueLength) {
                           this.param.typeDefinition = "varchar";
                        } else {
                           this.param.typeDefinition = "varchar(" + Parameter.this.valueLength + ")";
                           if (8000 <= Parameter.this.valueLength) {
                              this.param.typeDefinition = "varchar(max)";
                           }
                        }

                        if (JDBCType.LONGVARCHAR == Parameter.this.jdbcTypeSetByUser) {
                           this.param.typeDefinition = "varchar(max)";
                        }
                     }
                  } else {
                     this.param.typeDefinition = "nvarchar(4000)";
                  }
               }
               break;
            case SQLXML:
               this.param.typeDefinition = SSType.XML.toString();
               break;
            case TVP:
               this.param.typeDefinition = "[" + this.param.name + "] READONLY";
               break;
            case GUID:
               this.param.typeDefinition = SSType.GUID.toString();
               break;
            default:
               assert false : "Unexpected JDBC type " + var1.getJdbcType();
         }
      }

      void execute(DTV var1, String var2) throws SQLServerException {
         if (null != var2 && var2.length() > 4000) {
            var1.setJdbcType(JDBCType.LONGNVARCHAR);
         }

         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Clob var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Byte var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Integer var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Time var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Date var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Timestamp var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, java.util.Date var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Calendar var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, LocalDate var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, LocalTime var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, LocalDateTime var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, OffsetTime var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, OffsetDateTime var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, DateTimeOffset var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Float var2) throws SQLServerException {
         Parameter.this.scale = 4;
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Double var2) throws SQLServerException {
         Parameter.this.scale = 4;
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, BigDecimal var2) throws SQLServerException {
         if (null != var2) {
            Parameter.this.scale = var2.scale();
            if (Parameter.this.scale < 0) {
               Parameter.this.scale = 0;
            }
         }

         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Long var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, BigInteger var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Short var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Boolean var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, byte[] var2) throws SQLServerException {
         if (null != var2 && var2.length > 8000) {
            var1.setJdbcType(var1.getJdbcType().isBinary() ? JDBCType.LONGVARBINARY : JDBCType.LONGVARCHAR);
         }

         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Blob var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, InputStream var2) throws SQLServerException {
         StreamSetterArgs var3 = var1.getStreamSetterArgs();
         JDBCType var4 = var1.getJdbcType();
         if (JDBCType.CHAR == var4 || JDBCType.VARCHAR == var4 || JDBCType.BINARY == var4 || JDBCType.VARBINARY == var4) {
            if (var3.getLength() > 8000L) {
               var1.setJdbcType(var4.isBinary() ? JDBCType.LONGVARBINARY : JDBCType.LONGVARCHAR);
            } else if (-1L == var3.getLength()) {
               byte[] var5 = new byte[8001];
               BufferedInputStream var6 = new BufferedInputStream(var2, var5.length);
               int var7 = 0;

               try {
                  var6.mark(var5.length);
                  var7 = var6.read(var5, 0, var5.length);
                  if (-1 == var7) {
                     var7 = 0;
                  }

                  var6.reset();
               } catch (IOException var11) {
                  MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_errorReadingStream"));
                  Object[] var10 = new Object[]{var11.toString()};
                  SQLServerException.makeFromDriverError(null, null, var9.format(var10), "", true);
               }

               var1.setValue(var6, JavaType.INPUTSTREAM);
               if (var7 > 8000) {
                  var1.setJdbcType(var4.isBinary() ? JDBCType.LONGVARBINARY : JDBCType.LONGVARCHAR);
               } else {
                  var3.setLength(var7);
               }
            }
         }

         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, Reader var2) throws SQLServerException {
         if (JDBCType.NCHAR == var1.getJdbcType() || JDBCType.NVARCHAR == var1.getJdbcType()) {
            StreamSetterArgs var3 = var1.getStreamSetterArgs();
            if (var3.getLength() > 4000L) {
               var1.setJdbcType(JDBCType.LONGNVARCHAR);
            } else if (-1L == var3.getLength()) {
               char[] var4 = new char[4001];
               BufferedReader var5 = new BufferedReader(var2, var4.length);
               int var6 = 0;

               try {
                  var5.mark(var4.length);
                  var6 = var5.read(var4, 0, var4.length);
                  if (-1 == var6) {
                     var6 = 0;
                  }

                  var5.reset();
               } catch (IOException var10) {
                  MessageFormat var8 = new MessageFormat(SQLServerException.getErrString("R_errorReadingStream"));
                  Object[] var9 = new Object[]{var10.toString()};
                  SQLServerException.makeFromDriverError(null, null, var8.format(var9), "", true);
               }

               var1.setValue(var5, JavaType.READER);
               if (var6 > 4000) {
                  var1.setJdbcType(JDBCType.LONGNVARCHAR);
               } else {
                  var3.setLength(var6);
               }
            }
         }

         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, SQLServerSQLXML var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }

      void execute(DTV var1, TVP var2) throws SQLServerException {
         this.setTypeDefinition(var1);
      }
   }
}
