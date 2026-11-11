package com.microsoft.sqlserver.jdbc;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import microsoft.sql.DateTimeOffset;

public final class SQLServerCallableStatement extends SQLServerPreparedStatement implements ISQLServerCallableStatement {
   private ArrayList<String> paramNames;
   int nOutParams = 0;
   int nOutParamsAssigned = 0;
   private int outParamIndex = -1;
   private Parameter lastParamAccessed;
   private Closeable activeStream;

   String getClassNameInternal() {
      return "SQLServerCallableStatement";
   }

   SQLServerCallableStatement(SQLServerConnection var1, String var2, int var3, int var4, SQLServerStatementColumnEncryptionSetting var5) throws SQLServerException {
      super(var1, var2, var3, var4, var5);
   }

   public void registerOutParameter(int var1, int var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{new Integer(var1), new Integer(var2)});
      }

      this.checkClosed();
      if (var1 < 1 || var1 > this.inOutParam.length) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_indexOutOfRange"));
         Object[] var4 = new Object[]{new Integer(var1)};
         SQLServerException.makeFromDriverError(this.connection, this, var3.format(var4), "7009", false);
      }

      if (2012 == var2) {
         MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_featureNotSupported"));
         Object[] var7 = new Object[]{new String("REF_CURSOR")};
         SQLServerException.makeFromDriverError(this.connection, this, var5.format(var7), null, false);
      }

      JDBCType var6 = JDBCType.of(var2);
      this.discardLastExecutionResults();
      if (var6.isUnsupported()) {
         var6 = JDBCType.BINARY;
      }

      Parameter var8 = this.inOutParam[var1 - 1];
      assert null != var8;
      if (!var8.isOutput()) {
         this.nOutParams++;
      }

      var8.registerForOutput(var6, this.connection);
      switch (var2) {
         case -155:
         case 92:
         case 93:
            var8.setOutScale(7);
            break;
         case -151:
            var8.setOutScale(3);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(int var1, SQLType var2) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{new Integer(var1), var2});
      }

      this.registerOutParameter(var1, var2.getVendorTypeNumber());
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   private final Parameter getOutParameter(int var1) throws SQLServerException {
      this.processResults();
      if (this.inOutParam[var1 - 1] != this.lastParamAccessed && !this.inOutParam[var1 - 1].isValueGotten()) {
         while (this.outParamIndex != var1 - 1) {
            this.skipOutParameters(1, false);
         }

         return this.inOutParam[var1 - 1];
      } else {
         return this.inOutParam[var1 - 1];
      }
   }

   void startResults() {
      super.startResults();
      this.outParamIndex = -1;
      this.nOutParamsAssigned = 0;
      this.lastParamAccessed = null;
      assert null == this.activeStream;
   }

   void processBatch() throws SQLServerException {
      this.processResults();
      assert this.nOutParams >= 0;
      if (this.nOutParams > 0) {
         this.processOutParameters();
         this.processBatchRemainder();
      }
   }

   final void processOutParameters() throws SQLServerException {
      assert this.nOutParams > 0;
      assert null != this.inOutParam;
      this.closeActiveStream();
      if (this.outParamIndex >= 0) {
         for (int var1 = 0; var1 < this.inOutParam.length; var1++) {
            if (var1 != this.outParamIndex && this.inOutParam[var1].isValueGotten()) {
               assert this.inOutParam[var1].isOutput();
               this.inOutParam[var1].resetOutputValue();
            }
         }
      }

      assert this.nOutParamsAssigned <= this.nOutParams;
      if (this.nOutParamsAssigned < this.nOutParams) {
         this.skipOutParameters(this.nOutParams - this.nOutParamsAssigned, true);
      }

      if (this.outParamIndex >= 0) {
         this.inOutParam[this.outParamIndex].skipValue(this.resultsReader(), true);
         this.inOutParam[this.outParamIndex].resetOutputValue();
         this.outParamIndex = -1;
      }
   }

   private void processBatchRemainder() throws SQLServerException {
      final class ExecDoneHandler extends TDSTokenHandler {
         ExecDoneHandler() {
            super("ExecDoneHandler");
         }

         boolean onDone(TDSReader var1) throws SQLServerException {
            StreamDone var2 = new StreamDone();
            var2.setFromTDS(var1);
            if (var2.wasRPCInBatch()) {
               SQLServerCallableStatement.this.startResults();
               return false;
            } else {
               return true;
            }
         }
      }

      ExecDoneHandler var1 = new ExecDoneHandler();
      TDSParser.parse(this.resultsReader(), var1);
   }

   private void skipOutParameters(int var1, boolean var2) throws SQLServerException {
      final class OutParamHandler extends TDSTokenHandler {
         final StreamRetValue srv = new StreamRetValue();
         private boolean foundParam;

         final boolean foundParam() {
            return this.foundParam;
         }

         OutParamHandler() {
            super("OutParamHandler");
         }

         final void reset() {
            this.foundParam = false;
         }

         boolean onRetValue(TDSReader var1) throws SQLServerException {
            this.srv.setFromTDS(var1);
            this.foundParam = true;
            return false;
         }
      }

      OutParamHandler var3 = new OutParamHandler();
      assert var1 <= this.nOutParams - this.nOutParamsAssigned;

      for (int var4 = 0; var4 < var1; var4++) {
         if (-1 != this.outParamIndex) {
            this.inOutParam[this.outParamIndex].skipValue(this.resultsReader(), var2);
            if (var2) {
               this.inOutParam[this.outParamIndex].resetOutputValue();
            }
         }

         var3.reset();
         TDSParser.parse(this.resultsReader(), var3);
         if (!var3.foundParam()) {
            if (var2) {
               break;
            }

            MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_valueNotSetForParameter"));
            Object[] var6 = new Object[]{new Integer(this.outParamIndex + 1)};
            SQLServerException.makeFromDriverError(this.connection, this, var5.format(var6), null, false);
         }

         this.outParamIndex = var3.srv.getOrdinalOrLength();
         this.outParamIndex = this.outParamIndex - this.outParamIndexAdjustment;
         if (this.outParamIndex < 0 || this.outParamIndex >= this.inOutParam.length || !this.inOutParam[this.outParamIndex].isOutput()) {
            this.getStatementLogger()
               .info(this.toString() + " Unexpected outParamIndex: " + this.outParamIndex + "; adjustment: " + this.outParamIndexAdjustment);
            this.connection.throwInvalidTDS();
         }

         this.nOutParamsAssigned++;
      }
   }

   public void registerOutParameter(int var1, int var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{new Integer(var1), new Integer(var2), var3});
      }

      this.checkClosed();
      this.registerOutParameter(var1, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(int var1, SQLType var2, String var3) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{new Integer(var1), var2, var3});
      }

      this.registerOutParameter(var1, var2.getVendorTypeNumber(), var3);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(int var1, int var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{new Integer(var1), new Integer(var2), new Integer(var3)});
      }

      this.checkClosed();
      this.registerOutParameter(var1, var2);
      this.inOutParam[var1 - 1].setOutScale(var3);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(int var1, SQLType var2, int var3) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{new Integer(var1), var2, new Integer(var3)});
      }

      this.registerOutParameter(var1, var2.getVendorTypeNumber(), var3);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(int var1, SQLType var2, int var3, int var4) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{new Integer(var1), var2, new Integer(var4)});
      }

      this.registerOutParameter(var1, var2.getVendorTypeNumber(), var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(int var1, int var2, int var3, int var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(
            this.getClassNameLogging(), "registerOutParameter", new Object[]{new Integer(var1), new Integer(var2), new Integer(var4), new Integer(var3)}
         );
      }

      this.checkClosed();
      this.registerOutParameter(var1, var2);
      this.inOutParam[var1 - 1].setValueLength(var3);
      this.inOutParam[var1 - 1].setOutScale(var4);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   private Parameter getterGetParam(int var1) throws SQLServerException {
      this.checkClosed();
      if (var1 < 1 || var1 > this.inOutParam.length) {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidOutputParameter"));
         Object[] var3 = new Object[]{new Integer(var1)};
         SQLServerException.makeFromDriverError(this.connection, this, var2.format(var3), "07009", false);
      }

      if (!this.inOutParam[var1 - 1].isOutput()) {
         MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_outputParameterNotRegisteredForOutput"));
         Object[] var5 = new Object[]{new Integer(var1)};
         SQLServerException.makeFromDriverError(this.connection, this, var4.format(var5), "07009", true);
      }

      if (!this.wasExecuted()) {
         SQLServerException.makeFromDriverError(this.connection, this, SQLServerException.getErrString("R_statementMustBeExecuted"), "07009", false);
      }

      this.resultsReader().getCommand().checkForInterrupt();
      this.closeActiveStream();
      if (this.getStatementLogger().isLoggable(Level.FINER)) {
         this.getStatementLogger().finer(this.toString() + " Getting Param:" + var1);
      }

      this.lastParamAccessed = this.getOutParameter(var1);
      return this.lastParamAccessed;
   }

   private Object getValue(int var1, JDBCType var2) throws SQLServerException {
      return this.getterGetParam(var1).getValue(var2, null, null, this.resultsReader());
   }

   private Object getValue(int var1, JDBCType var2, Calendar var3) throws SQLServerException {
      return this.getterGetParam(var1).getValue(var2, null, var3, this.resultsReader());
   }

   private Object getStream(int var1, StreamType var2) throws SQLServerException {
      Object var3 = this.getterGetParam(var1)
         .getValue(
            var2.getJDBCType(),
            new InputStreamGetterArgs(var2, this.getIsResponseBufferingAdaptive(), this.getIsResponseBufferingAdaptive(), this.toString()),
            null,
            this.resultsReader()
         );
      this.activeStream = (Closeable)var3;
      return var3;
   }

   private Object getSQLXMLInternal(int var1) throws SQLServerException {
      SQLServerSQLXML var2 = (SQLServerSQLXML)this.getterGetParam(var1)
         .getValue(
            JDBCType.SQLXML,
            new InputStreamGetterArgs(StreamType.SQLXML, this.getIsResponseBufferingAdaptive(), this.getIsResponseBufferingAdaptive(), this.toString()),
            null,
            this.resultsReader()
         );
      if (null != var2) {
         this.activeStream = var2.getStream();
      }

      return var2;
   }

   public int getInt(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getInt", var1);
      this.checkClosed();
      Integer var2 = (Integer)this.getValue(var1, JDBCType.INTEGER);
      loggerExternal.exiting(this.getClassNameLogging(), "getInt", var2);
      return null != var2 ? var2 : 0;
   }

   public int getInt(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getInt", var1);
      this.checkClosed();
      Integer var2 = (Integer)this.getValue(this.findColumn(var1), JDBCType.INTEGER);
      loggerExternal.exiting(this.getClassNameLogging(), "getInt", var2);
      return null != var2 ? var2 : 0;
   }

   public String getString(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getString", var1);
      this.checkClosed();
      String var2 = (String)this.getValue(var1, JDBCType.CHAR);
      loggerExternal.exiting(this.getClassNameLogging(), "getString", var2);
      return var2;
   }

   public String getString(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getString", var1);
      this.checkClosed();
      String var2 = (String)this.getValue(this.findColumn(var1), JDBCType.CHAR);
      loggerExternal.exiting(this.getClassNameLogging(), "getString", var2);
      return var2;
   }

   public final String getNString(int var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getNString", var1);
      this.checkClosed();
      String var2 = (String)this.getValue(var1, JDBCType.NCHAR);
      loggerExternal.exiting(this.getClassNameLogging(), "getNString", var2);
      return var2;
   }

   public final String getNString(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getNString", var1);
      this.checkClosed();
      String var2 = (String)this.getValue(this.findColumn(var1), JDBCType.NCHAR);
      loggerExternal.exiting(this.getClassNameLogging(), "getNString", var2);
      return var2;
   }

   @Deprecated
   public BigDecimal getBigDecimal(int var1, int var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getBigDecimal", new Object[]{var1, var2});
      }

      this.checkClosed();
      BigDecimal var3 = (BigDecimal)this.getValue(var1, JDBCType.DECIMAL);
      if (null != var3) {
         var3 = var3.setScale(var2, 1);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getBigDecimal", var3);
      return var3;
   }

   @Deprecated
   public BigDecimal getBigDecimal(String var1, int var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getBigDecimal", new Object[]{var1, var2});
      }

      this.checkClosed();
      BigDecimal var3 = (BigDecimal)this.getValue(this.findColumn(var1), JDBCType.DECIMAL);
      if (null != var3) {
         var3 = var3.setScale(var2, 1);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getBigDecimal", var3);
      return var3;
   }

   public boolean getBoolean(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBoolean", var1);
      this.checkClosed();
      Boolean var2 = (Boolean)this.getValue(var1, JDBCType.BIT);
      loggerExternal.exiting(this.getClassNameLogging(), "getBoolean", var2);
      return null != var2 ? var2 : false;
   }

   public boolean getBoolean(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBoolean", var1);
      this.checkClosed();
      Boolean var2 = (Boolean)this.getValue(this.findColumn(var1), JDBCType.BIT);
      loggerExternal.exiting(this.getClassNameLogging(), "getBoolean", var2);
      return null != var2 ? var2 : false;
   }

   public byte getByte(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getByte", var1);
      this.checkClosed();
      Short var2 = (Short)this.getValue(var1, JDBCType.TINYINT);
      byte var3 = null != var2 ? var2.byteValue() : 0;
      loggerExternal.exiting(this.getClassNameLogging(), "getByte", var3);
      return var3;
   }

   public byte getByte(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getByte", var1);
      this.checkClosed();
      Short var2 = (Short)this.getValue(this.findColumn(var1), JDBCType.TINYINT);
      byte var3 = null != var2 ? var2.byteValue() : 0;
      loggerExternal.exiting(this.getClassNameLogging(), "getByte", var3);
      return var3;
   }

   public byte[] getBytes(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBytes", var1);
      this.checkClosed();
      byte[] var2 = (byte[])this.getValue(var1, JDBCType.BINARY);
      loggerExternal.exiting(this.getClassNameLogging(), "getBytes", var2);
      return var2;
   }

   public byte[] getBytes(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBytes", var1);
      this.checkClosed();
      byte[] var2 = (byte[])this.getValue(this.findColumn(var1), JDBCType.BINARY);
      loggerExternal.exiting(this.getClassNameLogging(), "getBytes", var2);
      return var2;
   }

   public Date getDate(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getDate", var1);
      this.checkClosed();
      Date var2 = (Date)this.getValue(var1, JDBCType.DATE);
      loggerExternal.exiting(this.getClassNameLogging(), "getDate", var2);
      return var2;
   }

   public Date getDate(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getDate", var1);
      this.checkClosed();
      Date var2 = (Date)this.getValue(this.findColumn(var1), JDBCType.DATE);
      loggerExternal.exiting(this.getClassNameLogging(), "getDate", var2);
      return var2;
   }

   public Date getDate(int var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getDate", new Object[]{var1, var2});
      }

      this.checkClosed();
      Date var3 = (Date)this.getValue(var1, JDBCType.DATE, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getDate", var3);
      return var3;
   }

   public Date getDate(String var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getDate", new Object[]{var1, var2});
      }

      this.checkClosed();
      Date var3 = (Date)this.getValue(this.findColumn(var1), JDBCType.DATE, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getDate", var3);
      return var3;
   }

   public double getDouble(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getDouble", var1);
      this.checkClosed();
      Double var2 = (Double)this.getValue(var1, JDBCType.DOUBLE);
      loggerExternal.exiting(this.getClassNameLogging(), "getDouble", var2);
      return null != var2 ? var2 : 0.0;
   }

   public double getDouble(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getDouble", var1);
      this.checkClosed();
      Double var2 = (Double)this.getValue(this.findColumn(var1), JDBCType.DOUBLE);
      loggerExternal.exiting(this.getClassNameLogging(), "getDouble", var2);
      return null != var2 ? var2 : 0.0;
   }

   public float getFloat(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getFloat", var1);
      this.checkClosed();
      Float var2 = (Float)this.getValue(var1, JDBCType.REAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getFloat", var2);
      return null != var2 ? var2 : 0.0F;
   }

   public float getFloat(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getFloat", var1);
      this.checkClosed();
      Float var2 = (Float)this.getValue(this.findColumn(var1), JDBCType.REAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getFloat", var2);
      return null != var2 ? var2 : 0.0F;
   }

   public long getLong(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getLong", var1);
      this.checkClosed();
      Long var2 = (Long)this.getValue(var1, JDBCType.BIGINT);
      loggerExternal.exiting(this.getClassNameLogging(), "getLong", var2);
      return null != var2 ? var2 : 0L;
   }

   public long getLong(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getLong", var1);
      this.checkClosed();
      Long var2 = (Long)this.getValue(this.findColumn(var1), JDBCType.BIGINT);
      loggerExternal.exiting(this.getClassNameLogging(), "getLong", var2);
      return null != var2 ? var2 : 0L;
   }

   public Object getObject(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getObject", var1);
      this.checkClosed();
      Object var2 = this.getValue(
         var1,
         this.getterGetParam(var1).getJdbcTypeSetByUser() != null ? this.getterGetParam(var1).getJdbcTypeSetByUser() : this.getterGetParam(var1).getJdbcType()
      );
      loggerExternal.exiting(this.getClassNameLogging(), "getObject", var2);
      return var2;
   }

   public <T> T getObject(int var1, Class<T> var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC41();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public Object getObject(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getObject", var1);
      this.checkClosed();
      int var2 = this.findColumn(var1);
      Object var3 = this.getValue(
         var2,
         this.getterGetParam(var2).getJdbcTypeSetByUser() != null ? this.getterGetParam(var2).getJdbcTypeSetByUser() : this.getterGetParam(var2).getJdbcType()
      );
      loggerExternal.exiting(this.getClassNameLogging(), "getObject", var3);
      return var3;
   }

   public <T> T getObject(String var1, Class<T> var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC41();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public short getShort(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getShort", var1);
      this.checkClosed();
      Short var2 = (Short)this.getValue(var1, JDBCType.SMALLINT);
      loggerExternal.exiting(this.getClassNameLogging(), "getShort", var2);
      return null != var2 ? var2 : 0;
   }

   public short getShort(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getShort", var1);
      this.checkClosed();
      Short var2 = (Short)this.getValue(this.findColumn(var1), JDBCType.SMALLINT);
      loggerExternal.exiting(this.getClassNameLogging(), "getShort", var2);
      return null != var2 ? var2 : 0;
   }

   public Time getTime(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getTime", var1);
      this.checkClosed();
      Time var2 = (Time)this.getValue(var1, JDBCType.TIME);
      loggerExternal.exiting(this.getClassNameLogging(), "getTime", var2);
      return var2;
   }

   public Time getTime(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getTime", var1);
      this.checkClosed();
      Time var2 = (Time)this.getValue(this.findColumn(var1), JDBCType.TIME);
      loggerExternal.exiting(this.getClassNameLogging(), "getTime", var2);
      return var2;
   }

   public Time getTime(int var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Time var3 = (Time)this.getValue(var1, JDBCType.TIME, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getTime", var3);
      return var3;
   }

   public Time getTime(String var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Time var3 = (Time)this.getValue(this.findColumn(var1), JDBCType.TIME, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getTime", var3);
      return var3;
   }

   public Timestamp getTimestamp(int var1) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getTimestamp", var1);
      }

      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(var1, JDBCType.TIMESTAMP);
      loggerExternal.exiting(this.getClassNameLogging(), "getTimestamp", var2);
      return var2;
   }

   public Timestamp getTimestamp(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getTimestamp", var1);
      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.TIMESTAMP);
      loggerExternal.exiting(this.getClassNameLogging(), "getTimestamp", var2);
      return var2;
   }

   public Timestamp getTimestamp(int var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getTimestamp", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(var1, JDBCType.TIMESTAMP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getTimestamp", var3);
      return var3;
   }

   public Timestamp getTimestamp(String var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getTimestamp", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getTimestamp", var3);
      return var3;
   }

   public Timestamp getDateTime(int var1) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getDateTime", var1);
      }

      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(var1, JDBCType.DATETIME);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTime", var2);
      return var2;
   }

   public Timestamp getDateTime(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getDateTime", var1);
      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.DATETIME);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTime", var2);
      return var2;
   }

   public Timestamp getDateTime(int var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(var1, JDBCType.DATETIME, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTime", var3);
      return var3;
   }

   public Timestamp getDateTime(String var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.DATETIME, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTime", var3);
      return var3;
   }

   public Timestamp getSmallDateTime(int var1) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getSmallDateTime", var1);
      }

      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(var1, JDBCType.SMALLDATETIME);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallDateTime", var2);
      return var2;
   }

   public Timestamp getSmallDateTime(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getSmallDateTime", var1);
      this.checkClosed();
      Timestamp var2 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.SMALLDATETIME);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallDateTime", var2);
      return var2;
   }

   public Timestamp getSmallDateTime(int var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getSmallDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(var1, JDBCType.SMALLDATETIME, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallDateTime", var3);
      return var3;
   }

   public Timestamp getSmallDateTime(String var1, Calendar var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getSmallDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      Timestamp var3 = (Timestamp)this.getValue(this.findColumn(var1), JDBCType.SMALLDATETIME, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallDateTime", var3);
      return var3;
   }

   public DateTimeOffset getDateTimeOffset(int var1) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getDateTimeOffset", var1);
      }

      this.checkClosed();
      if (!this.connection.isKatmaiOrLater()) {
         throw new SQLServerException(SQLServerException.getErrString("R_notSupported"), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET, null);
      }

      DateTimeOffset var2 = (DateTimeOffset)this.getValue(var1, JDBCType.DATETIMEOFFSET);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTimeOffset", var2);
      return var2;
   }

   public DateTimeOffset getDateTimeOffset(String var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "getDateTimeOffset", var1);
      this.checkClosed();
      if (!this.connection.isKatmaiOrLater()) {
         throw new SQLServerException(SQLServerException.getErrString("R_notSupported"), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET, null);
      }

      DateTimeOffset var2 = (DateTimeOffset)this.getValue(this.findColumn(var1), JDBCType.DATETIMEOFFSET);
      loggerExternal.exiting(this.getClassNameLogging(), "getDateTimeOffset", var2);
      return var2;
   }

   public boolean wasNull() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "wasNull");
      this.checkClosed();
      boolean var1 = false;
      if (null != this.lastParamAccessed) {
         var1 = this.lastParamAccessed.isNull();
      }

      loggerExternal.exiting(this.getClassNameLogging(), "wasNull", var1);
      return var1;
   }

   public final InputStream getAsciiStream(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getAsciiStream", var1);
      this.checkClosed();
      InputStream var2 = (InputStream)this.getStream(var1, StreamType.ASCII);
      loggerExternal.exiting(this.getClassNameLogging(), "getAsciiStream", var2);
      return var2;
   }

   public final InputStream getAsciiStream(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getAsciiStream", var1);
      this.checkClosed();
      InputStream var2 = (InputStream)this.getStream(this.findColumn(var1), StreamType.ASCII);
      loggerExternal.exiting(this.getClassNameLogging(), "getAsciiStream", var2);
      return var2;
   }

   public BigDecimal getBigDecimal(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBigDecimal", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(var1, JDBCType.DECIMAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getBigDecimal", var2);
      return var2;
   }

   public BigDecimal getBigDecimal(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBigDecimal", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(this.findColumn(var1), JDBCType.DECIMAL);
      loggerExternal.exiting(this.getClassNameLogging(), "getBigDecimal", var2);
      return var2;
   }

   public BigDecimal getMoney(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getMoney", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(var1, JDBCType.MONEY);
      loggerExternal.exiting(this.getClassNameLogging(), "getMoney", var2);
      return var2;
   }

   public BigDecimal getMoney(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getMoney", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(this.findColumn(var1), JDBCType.MONEY);
      loggerExternal.exiting(this.getClassNameLogging(), "getMoney", var2);
      return var2;
   }

   public BigDecimal getSmallMoney(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getSmallMoney", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(var1, JDBCType.SMALLMONEY);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallMoney", var2);
      return var2;
   }

   public BigDecimal getSmallMoney(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getSmallMoney", var1);
      this.checkClosed();
      BigDecimal var2 = (BigDecimal)this.getValue(this.findColumn(var1), JDBCType.SMALLMONEY);
      loggerExternal.exiting(this.getClassNameLogging(), "getSmallMoney", var2);
      return var2;
   }

   public final InputStream getBinaryStream(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBinaryStream", var1);
      this.checkClosed();
      InputStream var2 = (InputStream)this.getStream(var1, StreamType.BINARY);
      loggerExternal.exiting(this.getClassNameLogging(), "getBinaryStream", var2);
      return var2;
   }

   public final InputStream getBinaryStream(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBinaryStream", var1);
      this.checkClosed();
      InputStream var2 = (InputStream)this.getStream(this.findColumn(var1), StreamType.BINARY);
      loggerExternal.exiting(this.getClassNameLogging(), "getBinaryStream", var2);
      return var2;
   }

   public Blob getBlob(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBlob", var1);
      this.checkClosed();
      Blob var2 = (Blob)this.getValue(var1, JDBCType.BLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getBlob", var2);
      return var2;
   }

   public Blob getBlob(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getBlob", var1);
      this.checkClosed();
      Blob var2 = (Blob)this.getValue(this.findColumn(var1), JDBCType.BLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getBlob", var2);
      return var2;
   }

   public final Reader getCharacterStream(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getCharacterStream", var1);
      this.checkClosed();
      Reader var2 = (Reader)this.getStream(var1, StreamType.CHARACTER);
      loggerExternal.exiting(this.getClassNameLogging(), "getCharacterStream", var2);
      return var2;
   }

   public final Reader getCharacterStream(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getCharacterStream", var1);
      this.checkClosed();
      Reader var2 = (Reader)this.getStream(this.findColumn(var1), StreamType.CHARACTER);
      loggerExternal.exiting(this.getClassNameLogging(), "getCharacterSream", var2);
      return var2;
   }

   public final Reader getNCharacterStream(int var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getNCharacterStream", var1);
      this.checkClosed();
      Reader var2 = (Reader)this.getStream(var1, StreamType.NCHARACTER);
      loggerExternal.exiting(this.getClassNameLogging(), "getNCharacterStream", var2);
      return var2;
   }

   public final Reader getNCharacterStream(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getNCharacterStream", var1);
      this.checkClosed();
      Reader var2 = (Reader)this.getStream(this.findColumn(var1), StreamType.NCHARACTER);
      loggerExternal.exiting(this.getClassNameLogging(), "getNCharacterStream", var2);
      return var2;
   }

   void closeActiveStream() throws SQLServerException {
      if (null != this.activeStream) {
         try {
            this.activeStream.close();
         } catch (IOException var5) {
            SQLServerException.makeFromDriverError(null, null, var5.getMessage(), null, true);
         } finally {
            this.activeStream = null;
         }
      }
   }

   public Clob getClob(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getClob", var1);
      this.checkClosed();
      Clob var2 = (Clob)this.getValue(var1, JDBCType.CLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getClob", var2);
      return var2;
   }

   public Clob getClob(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getClob", var1);
      this.checkClosed();
      Clob var2 = (Clob)this.getValue(this.findColumn(var1), JDBCType.CLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getClob", var2);
      return var2;
   }

   public NClob getNClob(int var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getNClob", var1);
      this.checkClosed();
      NClob var2 = (NClob)this.getValue(var1, JDBCType.NCLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getNClob", var2);
      return var2;
   }

   public NClob getNClob(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getNClob", var1);
      this.checkClosed();
      NClob var2 = (NClob)this.getValue(this.findColumn(var1), JDBCType.NCLOB);
      loggerExternal.exiting(this.getClassNameLogging(), "getNClob", var2);
      return var2;
   }

   public Object getObject(int var1, Map<String, Class<?>> var2) throws SQLServerException {
      this.NotImplemented();
      return null;
   }

   public Object getObject(String var1, Map<String, Class<?>> var2) throws SQLServerException {
      this.checkClosed();
      return this.getObject(this.findColumn(var1), var2);
   }

   public Ref getRef(int var1) throws SQLServerException {
      this.NotImplemented();
      return null;
   }

   public Ref getRef(String var1) throws SQLServerException {
      this.checkClosed();
      return this.getRef(this.findColumn(var1));
   }

   public Array getArray(int var1) throws SQLServerException {
      this.NotImplemented();
      return null;
   }

   public Array getArray(String var1) throws SQLServerException {
      this.checkClosed();
      return this.getArray(this.findColumn(var1));
   }

   private int findColumn(String var1) throws SQLServerException {
      if (this.paramNames == null) {
         try {
            SQLServerStatement var2 = (SQLServerStatement)this.connection.createStatement();

            final class ThreePartNamesParser {
               private String procedurePart = null;
               private String ownerPart = null;
               private String databasePart = null;
               private final Pattern threePartName = Pattern.compile(JDBCSyntaxTranslator.getSQLIdentifierWithGroups());

               String getProcedurePart() {
                  return this.procedurePart;
               }

               String getOwnerPart() {
                  return this.ownerPart;
               }

               String getDatabasePart() {
                  return this.databasePart;
               }

               final void parseProcedureNameIntoParts(String var1) {
                  if (null != var1) {
                     Matcher var2 = this.threePartName.matcher(var1);
                     if (var2.matches()) {
                        if (var2.group(2) != null) {
                           this.databasePart = var2.group(1);
                           var2 = this.threePartName.matcher(var2.group(2));
                           if (var2.matches()) {
                              if (null != var2.group(2)) {
                                 this.ownerPart = var2.group(1);
                                 this.procedurePart = var2.group(2);
                              } else {
                                 this.ownerPart = this.databasePart;
                                 this.databasePart = null;
                                 this.procedurePart = var2.group(1);
                              }
                           }
                        } else {
                           this.procedurePart = var2.group(1);
                        }
                     } else {
                        this.procedurePart = var1;
                     }
                  }
               }
            }

            ThreePartNamesParser var3 = new ThreePartNamesParser();
            var3.parseProcedureNameIntoParts(this.procedureName);
            StringBuilder var4 = new StringBuilder("exec sp_sproc_columns ");
            if (null != var3.getDatabasePart()) {
               var4.append("@procedure_qualifier=");
               var4.append(var3.getDatabasePart());
               var4.append(", ");
            }

            if (null != var3.getOwnerPart()) {
               var4.append("@procedure_owner=");
               var4.append(var3.getOwnerPart());
               var4.append(", ");
            }

            if (null != var3.getProcedurePart()) {
               var4.append("@procedure_name=");
               var4.append(var3.getProcedurePart());
               var4.append(" , @ODBCVer=3");
            } else {
               MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_parameterNotDefinedForProcedure"));
               Object[] var6 = new Object[]{var1, ""};
               SQLServerException.makeFromDriverError(this.connection, this, var5.format(var6), "07009", false);
            }

            SQLServerResultSet var13 = var2.executeQueryInternal(var4.toString());
            this.paramNames = new ArrayList();

            while (var13.next()) {
               String var19 = var13.getString(4);
               this.paramNames.add(var19.trim());
            }
         } catch (SQLException var7) {
            SQLServerException.makeFromDriverError(this.connection, this, var7.toString(), null, false);
         }
      }

      int var8 = 0;
      if (this.paramNames != null) {
         var8 = this.paramNames.size();
      }

      boolean var9 = false;
      int var12 = -1;

      for (int var10 = 0; var10 < var8; var10++) {
         String var14 = (String)this.paramNames.get(var10);
         var14 = var14.substring(1, var14.length());
         if (var14.equals(var1)) {
            var12 = var10;
            break;
         }
      }

      if (-1 == var12) {
         for (int var11 = 0; var11 < var8; var11++) {
            String var16 = (String)this.paramNames.get(var11);
            var16 = var16.substring(1, var16.length());
            if (var16.equalsIgnoreCase(var1)) {
               var12 = var11;
               break;
            }
         }
      }

      if (-1 == var12) {
         MessageFormat var18 = new MessageFormat(SQLServerException.getErrString("R_parameterNotDefinedForProcedure"));
         Object[] var20 = new Object[]{var1, this.procedureName};
         SQLServerException.makeFromDriverError(this.connection, this, var18.format(var20), "07009", false);
      }

      return this.bReturnValueSyntax ? var12 + 1 : var12;
   }

   public void setTimestamp(String var1, Timestamp var2, Calendar var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTimeStamp", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTimeStamp");
   }

   public void setTimestamp(String var1, Timestamp var2, Calendar var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTimeStamp", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setTimeStamp");
   }

   public void setTime(String var1, Time var2, Calendar var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TIME, var2, JavaType.TIME, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTime");
   }

   public void setTime(String var1, Time var2, Calendar var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTime", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TIME, var2, JavaType.TIME, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setTime");
   }

   public void setDate(String var1, Date var2, Calendar var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDate", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DATE, var2, JavaType.DATE, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDate");
   }

   public void setDate(String var1, Date var2, Calendar var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDate", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DATE, var2, JavaType.DATE, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setDate");
   }

   public final void setCharacterStream(String var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setCharacterStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.CHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setCharacterStream");
   }

   public final void setCharacterStream(String var1, Reader var2, int var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setCharacterStream");
   }

   public final void setCharacterStream(String var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setCharacterStream");
   }

   public final void setNCharacterStream(String var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNCharacterStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.NCHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setNCharacterStream");
   }

   public final void setNCharacterStream(String var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNCharacterStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.NCHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setNCharacterStream");
   }

   public final void setClob(String var1, Clob var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.CLOB, var2, JavaType.CLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setClob");
   }

   public final void setClob(String var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.CHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setClob");
   }

   public final void setClob(String var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setClob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.CHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setClob");
   }

   public final void setNClob(String var1, NClob var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.NCLOB, var2, JavaType.NCLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setNClob");
   }

   public final void setNClob(String var1, Reader var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNClob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.NCHARACTER, var2, JavaType.READER, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setNClob");
   }

   public final void setNClob(String var1, Reader var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNClob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.NCHARACTER, var2, JavaType.READER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setNClob");
   }

   public final void setNString(String var1, String var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNString", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.NVARCHAR, var2, JavaType.STRING, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setNString");
   }

   public final void setNString(String var1, String var2, boolean var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNString", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.NVARCHAR, var2, JavaType.STRING, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setNString");
   }

   public void setObject(String var1, Object var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setObjectNoType(this.findColumn(var1), var2, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public void setObject(String var1, Object var2, int var3) throws SQLServerException {
      String var4 = null;
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      if (-153 == var3) {
         var4 = this.getTVPNameIfNull(this.findColumn(var1), null);
         this.setObject(this.setterGetParam(this.findColumn(var1)), var2, JavaType.TVP, JDBCType.TVP, null, null, false, this.findColumn(var1), var4);
      } else {
         this.setObject(this.setterGetParam(this.findColumn(var1)), var2, JavaType.of(var2), JDBCType.of(var3), null, null, false, this.findColumn(var1), var4);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public void setObject(String var1, Object var2, SQLType var3) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3});
      }

      this.setObject(var1, var2, var3.getVendorTypeNumber());
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public void setObject(String var1, Object var2, int var3, int var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setObject(this.setterGetParam(this.findColumn(var1)), var2, JavaType.of(var2), JDBCType.of(var3), var4, null, false, this.findColumn(var1), null);
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public void setObject(String var1, Object var2, int var3, int var4, boolean var5) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3, var4, var5});
      }

      this.checkClosed();
      this.setObject(
         this.setterGetParam(this.findColumn(var1)),
         var2,
         JavaType.of(var2),
         JDBCType.of(var3),
         2 != var3 && 3 != var3 ? null : var4,
         null,
         var5,
         this.findColumn(var1),
         null
      );
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public void setObject(String var1, Object var2, SQLType var3, int var4) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3, var4});
      }

      this.setObject(var1, var2, var3.getVendorTypeNumber(), var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public void setObject(String var1, Object var2, SQLType var3, int var4, boolean var5) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3, var4, var5});
      }

      this.setObject(var1, var2, var3.getVendorTypeNumber(), var4, var5);
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public final void setObject(String var1, Object var2, int var3, Integer var4, int var5) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setObject", new Object[]{var1, var2, var3, var4, var5});
      }

      this.checkClosed();
      this.setObject(
         this.setterGetParam(this.findColumn(var1)),
         var2,
         JavaType.of(var2),
         JDBCType.of(var3),
         2 != var3 && 3 != var3 && !InputStream.class.isInstance(var2) && !Reader.class.isInstance(var2) ? null : var5,
         var4,
         false,
         this.findColumn(var1),
         null
      );
      loggerExternal.exiting(this.getClassNameLogging(), "setObject");
   }

   public final void setAsciiStream(String var1, InputStream var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setAsciiStream", new Object[]{var1, var2});
      }

      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.ASCII, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setAsciiStream");
   }

   public final void setAsciiStream(String var1, InputStream var2, int var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setAsciiStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.ASCII, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setAsciiStream");
   }

   public final void setAsciiStream(String var1, InputStream var2, long var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setAsciiStream", new Object[]{var1, var2, var3});
      }

      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.ASCII, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setAsciiStream");
   }

   public final void setBinaryStream(String var1, InputStream var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBinaryStream", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.BINARY, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setBinaryStream");
   }

   public final void setBinaryStream(String var1, InputStream var2, int var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBinaryStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setBinaryStream");
   }

   public final void setBinaryStream(String var1, InputStream var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBinaryStream", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setBinaryStream");
   }

   public final void setBlob(String var1, Blob var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBlob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.BLOB, var2, JavaType.BLOB, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setBlob");
   }

   public final void setBlob(String var1, InputStream var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBlob", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.BINARY, var2, JavaType.INPUTSTREAM, -1L);
      loggerExternal.exiting(this.getClassNameLogging(), "setBlob");
   }

   public final void setBlob(String var1, InputStream var2, long var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBlob", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setStream(this.findColumn(var1), StreamType.BINARY, var2, JavaType.INPUTSTREAM, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setBlob");
   }

   public void setTimestamp(String var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTimestamp", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTimestamp");
   }

   public void setTimestamp(String var1, Timestamp var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTimestamp", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTimestamp");
   }

   public void setTimestamp(String var1, Timestamp var2, int var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTimestamp", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TIMESTAMP, var2, JavaType.TIMESTAMP, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setTimestamp");
   }

   public void setDateTimeOffset(String var1, DateTimeOffset var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDateTimeOffset", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDateTimeOffset");
   }

   public void setDateTimeOffset(String var1, DateTimeOffset var2, int var3) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDateTimeOffset", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDateTimeOffset");
   }

   public void setDateTimeOffset(String var1, DateTimeOffset var2, int var3, boolean var4) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDateTimeOffset", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DATETIMEOFFSET, var2, JavaType.DATETIMEOFFSET, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setDateTimeOffset");
   }

   public void setDate(String var1, Date var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDate", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DATE, var2, JavaType.DATE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDate");
   }

   public void setTime(String var1, Time var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TIME, var2, JavaType.TIME, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTime");
   }

   public void setTime(String var1, Time var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TIME, var2, JavaType.TIME, null, var3, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setTime");
   }

   public void setTime(String var1, Time var2, int var3, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTime", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TIME, var2, JavaType.TIME, null, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setTime");
   }

   public void setDateTime(String var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DATETIME, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDateTime");
   }

   public void setDateTime(String var1, Timestamp var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDateTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DATETIME, var2, JavaType.TIMESTAMP, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setDateTime");
   }

   public void setSmallDateTime(String var1, Timestamp var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setSmallDateTime", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.SMALLDATETIME, var2, JavaType.TIMESTAMP, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setSmallDateTime");
   }

   public void setSmallDateTime(String var1, Timestamp var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setSmallDateTime", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.SMALLDATETIME, var2, JavaType.TIMESTAMP, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setSmallDateTime");
   }

   public void setUniqueIdentifier(String var1, String var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setUniqueIdentifier", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.GUID, var2, JavaType.STRING, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setUniqueIdentifier");
   }

   public void setUniqueIdentifier(String var1, String var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setUniqueIdentifier", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.GUID, var2, JavaType.STRING, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setUniqueIdentifier");
   }

   public void setBytes(String var1, byte[] var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBytes", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.BINARY, var2, JavaType.BYTEARRAY, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setBytes");
   }

   public void setBytes(String var1, byte[] var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBytes", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.BINARY, var2, JavaType.BYTEARRAY, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setBytes");
   }

   public void setByte(String var1, byte var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setByte", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TINYINT, var2, JavaType.BYTE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setByte");
   }

   public void setByte(String var1, byte var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setByte", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TINYINT, var2, JavaType.BYTE, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setByte");
   }

   public void setString(String var1, String var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setString", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.VARCHAR, var2, JavaType.STRING, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setString");
   }

   public void setString(String var1, String var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setString", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.VARCHAR, var2, JavaType.STRING, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setString");
   }

   public void setMoney(String var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setMoney", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.MONEY, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setMoney");
   }

   public void setMoney(String var1, BigDecimal var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setMoney", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.MONEY, var2, JavaType.BIGDECIMAL, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setMoney");
   }

   public void setSmallMoney(String var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setSmallMoney", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.SMALLMONEY, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setSmallMoney");
   }

   public void setSmallMoney(String var1, BigDecimal var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setSmallMoney", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.SMALLMONEY, var2, JavaType.BIGDECIMAL, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setSmallMoney");
   }

   public void setBigDecimal(String var1, BigDecimal var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBigDecimal", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setBigDecimal");
   }

   public void setBigDecimal(String var1, BigDecimal var2, int var3, int var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBigDecimal", new Object[]{var1, var2, var3, var4});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, var3, var4, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setBigDecimal");
   }

   public void setBigDecimal(String var1, BigDecimal var2, int var3, int var4, boolean var5) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBigDecimal", new Object[]{var1, var2, var3, var4, var5});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DECIMAL, var2, JavaType.BIGDECIMAL, var3, var4, var5);
      loggerExternal.exiting(this.getClassNameLogging(), "setBigDecimal");
   }

   public void setDouble(String var1, double var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDouble", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DOUBLE, var2, JavaType.DOUBLE, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setDouble");
   }

   public void setDouble(String var1, double var2, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setDouble", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.DOUBLE, var2, JavaType.DOUBLE, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setDouble");
   }

   public void setFloat(String var1, float var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setFloat", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.REAL, var2, JavaType.FLOAT, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setFloat");
   }

   public void setFloat(String var1, float var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setFloat", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.REAL, var2, JavaType.FLOAT, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setFloat");
   }

   public void setInt(String var1, int var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setInt", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.INTEGER, var2, JavaType.INTEGER, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setInt");
   }

   public void setInt(String var1, int var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setInt", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.INTEGER, var2, JavaType.INTEGER, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setInt");
   }

   public void setLong(String var1, long var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setLong", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.BIGINT, var2, JavaType.LONG, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setLong");
   }

   public void setLong(String var1, long var2, boolean var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setLong", new Object[]{var1, var2, var4});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.BIGINT, var2, JavaType.LONG, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "setLong");
   }

   public void setShort(String var1, short var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setShort", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.SMALLINT, var2, JavaType.SHORT, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setShort");
   }

   public void setShort(String var1, short var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setShort", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.SMALLINT, var2, JavaType.SHORT, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setShort");
   }

   public void setBoolean(String var1, boolean var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBoolean", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.BIT, var2, JavaType.BOOLEAN, false);
      loggerExternal.exiting(this.getClassNameLogging(), "setBoolean");
   }

   public void setBoolean(String var1, boolean var2, boolean var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setBoolean", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.BIT, var2, JavaType.BOOLEAN, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setBoolean");
   }

   public void setNull(String var1, int var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNull", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setObject(this.setterGetParam(this.findColumn(var1)), null, JavaType.OBJECT, JDBCType.of(var2), null, null, false, this.findColumn(var1), null);
      loggerExternal.exiting(this.getClassNameLogging(), "setNull");
   }

   public void setNull(String var1, int var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setNull", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setObject(this.setterGetParam(this.findColumn(var1)), null, JavaType.OBJECT, JDBCType.of(var2), null, null, false, this.findColumn(var1), var3);
      loggerExternal.exiting(this.getClassNameLogging(), "setNull");
   }

   public void setURL(String var1, URL var2) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "setURL", var1);
      this.checkClosed();
      this.setURL(this.findColumn(var1), var2);
      loggerExternal.exiting(this.getClassNameLogging(), "setURL");
   }

   public final void setStructured(String var1, String var2, SQLServerDataTable var3) throws SQLServerException {
      var2 = this.getTVPNameIfNull(this.findColumn(var1), var2);
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setStructured", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TVP, var3, JavaType.TVP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "setStructured");
   }

   public final void setStructured(String var1, String var2, ResultSet var3) throws SQLServerException {
      var2 = this.getTVPNameIfNull(this.findColumn(var1), var2);
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setStructured", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TVP, var3, JavaType.TVP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "setStructured");
   }

   public final void setStructured(String var1, String var2, ISQLServerDataRecord var3) throws SQLServerException {
      var2 = this.getTVPNameIfNull(this.findColumn(var1), var2);
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setStructured", new Object[]{var1, var2, var3});
      }

      this.checkClosed();
      this.setValue(this.findColumn(var1), JDBCType.TVP, var3, JavaType.TVP, var2);
      loggerExternal.exiting(this.getClassNameLogging(), "setStructured");
   }

   public URL getURL(int var1) throws SQLServerException {
      this.NotImplemented();
      return null;
   }

   public URL getURL(String var1) throws SQLServerException {
      this.NotImplemented();
      return null;
   }

   public final void setSQLXML(String var1, SQLXML var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setSQLXML", new Object[]{var1, var2});
      }

      this.checkClosed();
      this.setSQLXMLInternal(this.findColumn(var1), var2);
      loggerExternal.exiting(this.getClassNameLogging(), "setSQLXML");
   }

   public final SQLXML getSQLXML(int var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getSQLXML", var1);
      this.checkClosed();
      SQLServerSQLXML var2 = (SQLServerSQLXML)this.getSQLXMLInternal(var1);
      loggerExternal.exiting(this.getClassNameLogging(), "getSQLXML", var2);
      return var2;
   }

   public final SQLXML getSQLXML(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getSQLXML", var1);
      this.checkClosed();
      SQLServerSQLXML var2 = (SQLServerSQLXML)this.getSQLXMLInternal(this.findColumn(var1));
      loggerExternal.exiting(this.getClassNameLogging(), "getSQLXML", var2);
      return var2;
   }

   public final void setRowId(String var1, RowId var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public final RowId getRowId(int var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public final RowId getRowId(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public void registerOutParameter(String var1, int var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{var1, new Integer(var2), var3});
      }

      this.checkClosed();
      this.registerOutParameter(this.findColumn(var1), var2, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(String var1, SQLType var2, String var3) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{var1, var2, var3});
      }

      this.registerOutParameter(var1, var2.getVendorTypeNumber(), var3);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(String var1, int var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{var1, new Integer(var2), new Integer(var3)});
      }

      this.checkClosed();
      this.registerOutParameter(this.findColumn(var1), var2, var3);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(String var1, int var2, int var3, int var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{var1, new Integer(var2), new Integer(var4)});
      }

      this.checkClosed();
      this.registerOutParameter(this.findColumn(var1), var2, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(String var1, SQLType var2, int var3) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{var1, var2, new Integer(var3)});
      }

      this.registerOutParameter(var1, var2.getVendorTypeNumber(), var3);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(String var1, SQLType var2, int var3, int var4) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{var1, var2, new Integer(var4)});
      }

      this.registerOutParameter(var1, var2.getVendorTypeNumber(), var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(String var1, int var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{var1, new Integer(var2)});
      }

      this.checkClosed();
      this.registerOutParameter(this.findColumn(var1), var2);
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }

   public void registerOutParameter(String var1, SQLType var2) throws SQLServerException {
      DriverJDBCVersion.checkSupportsJDBC42();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "registerOutParameter", new Object[]{var1, var2});
      }

      this.registerOutParameter(var1, var2.getVendorTypeNumber());
      loggerExternal.exiting(this.getClassNameLogging(), "registerOutParameter");
   }
}
