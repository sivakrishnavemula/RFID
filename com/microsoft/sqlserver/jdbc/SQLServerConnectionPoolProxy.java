package com.microsoft.sqlserver.jdbc;

import java.io.Serializable;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLPermission;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Level;

class SQLServerConnectionPoolProxy implements ISQLServerConnection, Serializable {
   private SQLServerConnection wrappedConnection;
   private boolean bIsOpen;
   private static int baseConnectionID = 0;
   private final String traceID = " ProxyConnectionID:" + nextConnectionID();
   private static final String callAbortPerm = "callAbort";

   private static synchronized int nextConnectionID() {
      baseConnectionID++;
      return baseConnectionID;
   }

   public String toString() {
      return this.traceID;
   }

   SQLServerConnectionPoolProxy(SQLServerConnection var1) {
      this.wrappedConnection = var1;
      var1.setAssociatedProxy(this);
      this.bIsOpen = true;
   }

   void checkClosed() throws SQLServerException {
      if (!this.bIsOpen) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_connectionIsClosed"), null, false);
      }
   }

   public Statement createStatement() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.createStatement();
   }

   public PreparedStatement prepareStatement(String var1) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareStatement(var1);
   }

   public CallableStatement prepareCall(String var1) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareCall(var1);
   }

   public String nativeSQL(String var1) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.nativeSQL(var1);
   }

   public void setAutoCommit(boolean var1) throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.setAutoCommit(var1);
   }

   public boolean getAutoCommit() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.getAutoCommit();
   }

   public void commit() throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.commit();
   }

   public void rollback() throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.rollback();
   }

   public void abort(Executor var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC41();
      if (this.bIsOpen && null != this.wrappedConnection) {
         if (null == var1) {
            MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidArgument"));
            Object[] var3 = new Object[]{"executor"};
            SQLServerException.makeFromDriverError(null, null, var2.format(var3), null, false);
         }

         SecurityManager var7 = System.getSecurityManager();
         if (var7 != null) {
            try {
               SQLPermission var8 = new SQLPermission("callAbort");
               var7.checkPermission(var8);
            } catch (SecurityException var6) {
               MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_permissionDenied"));
               Object[] var5 = new Object[]{"callAbort"};
               throw new SQLServerException(var4.format(var5), null, 0, var6);
            }
         }

         this.bIsOpen = false;
         var1.execute(new Runnable() {
            public void run() {
               if (SQLServerConnectionPoolProxy.this.wrappedConnection.getConnectionLogger().isLoggable(Level.FINER)) {
                  SQLServerConnectionPoolProxy.this.wrappedConnection.getConnectionLogger().finer(this.toString() + " Connection proxy aborted ");
               }

               try {
                  SQLServerConnectionPoolProxy.this.wrappedConnection.poolCloseEventNotify();
                  SQLServerConnectionPoolProxy.this.wrappedConnection = null;
               } catch (SQLException var2) {
                  throw new RuntimeException(var2);
               }
            }
         });
      }
   }

   public void close() throws SQLServerException {
      if (this.bIsOpen && null != this.wrappedConnection) {
         if (this.wrappedConnection.getConnectionLogger().isLoggable(Level.FINER)) {
            this.wrappedConnection.getConnectionLogger().finer(this.toString() + " Connection proxy closed ");
         }

         this.wrappedConnection.poolCloseEventNotify();
         this.wrappedConnection = null;
      }

      this.bIsOpen = false;
   }

   void internalClose() {
      this.bIsOpen = false;
      this.wrappedConnection = null;
   }

   public boolean isClosed() throws SQLServerException {
      return !this.bIsOpen;
   }

   public DatabaseMetaData getMetaData() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.getMetaData();
   }

   public void setReadOnly(boolean var1) throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.setReadOnly(var1);
   }

   public boolean isReadOnly() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.isReadOnly();
   }

   public void setCatalog(String var1) throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.setCatalog(var1);
   }

   public String getCatalog() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.getCatalog();
   }

   public void setTransactionIsolation(int var1) throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.setTransactionIsolation(var1);
   }

   public int getTransactionIsolation() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.getTransactionIsolation();
   }

   public SQLWarning getWarnings() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.getWarnings();
   }

   public void clearWarnings() throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.clearWarnings();
   }

   public Statement createStatement(int var1, int var2) throws SQLException {
      this.checkClosed();
      return this.wrappedConnection.createStatement(var1, var2);
   }

   public PreparedStatement prepareStatement(String var1, int var2, int var3) throws SQLException {
      this.checkClosed();
      return this.wrappedConnection.prepareStatement(var1, var2, var3);
   }

   public CallableStatement prepareCall(String var1, int var2, int var3) throws SQLException {
      this.checkClosed();
      return this.wrappedConnection.prepareCall(var1, var2, var3);
   }

   public void setTypeMap(Map<String, Class<?>> var1) throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.setTypeMap(var1);
   }

   public Map<String, Class<?>> getTypeMap() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.getTypeMap();
   }

   public Statement createStatement(int var1, int var2, int var3) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.createStatement(var1, var2, var3);
   }

   public Statement createStatement(int var1, int var2, int var3, SQLServerStatementColumnEncryptionSetting var4) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.createStatement(var1, var2, var3, var4);
   }

   public PreparedStatement prepareStatement(String var1, int var2, int var3, int var4) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareStatement(var1, var2, var3, var4);
   }

   public PreparedStatement prepareStatement(String var1, int var2, int var3, int var4, SQLServerStatementColumnEncryptionSetting var5) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareStatement(var1, var2, var3, var4, var5);
   }

   public CallableStatement prepareCall(String var1, int var2, int var3, int var4) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareCall(var1, var2, var3, var4);
   }

   public CallableStatement prepareCall(String var1, int var2, int var3, int var4, SQLServerStatementColumnEncryptionSetting var5) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareCall(var1, var2, var3, var4, var5);
   }

   public PreparedStatement prepareStatement(String var1, int var2) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareStatement(var1, var2);
   }

   public PreparedStatement prepareStatement(String var1, int var2, SQLServerStatementColumnEncryptionSetting var3) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareStatement(var1, var2, var3);
   }

   public PreparedStatement prepareStatement(String var1, int[] var2) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareStatement(var1, var2);
   }

   public PreparedStatement prepareStatement(String var1, int[] var2, SQLServerStatementColumnEncryptionSetting var3) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareStatement(var1, var2, var3);
   }

   public PreparedStatement prepareStatement(String var1, String[] var2) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareStatement(var1, var2);
   }

   public PreparedStatement prepareStatement(String var1, String[] var2, SQLServerStatementColumnEncryptionSetting var3) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.prepareStatement(var1, var2, var3);
   }

   public void releaseSavepoint(Savepoint var1) throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.releaseSavepoint(var1);
   }

   public Savepoint setSavepoint(String var1) throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.setSavepoint(var1);
   }

   public Savepoint setSavepoint() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.setSavepoint();
   }

   public void rollback(Savepoint var1) throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.rollback(var1);
   }

   public int getHoldability() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.getHoldability();
   }

   public void setHoldability(int var1) throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.setHoldability(var1);
   }

   public int getNetworkTimeout() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC41();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public void setNetworkTimeout(Executor var1, int var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC41();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public String getSchema() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC41();
      this.checkClosed();
      return this.wrappedConnection.getSchema();
   }

   public void setSchema(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC41();
      this.checkClosed();
      this.wrappedConnection.setSchema(var1);
   }

   public Array createArrayOf(String var1, Object[] var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return this.wrappedConnection.createArrayOf(var1, var2);
   }

   public Blob createBlob() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return this.wrappedConnection.createBlob();
   }

   public Clob createClob() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return this.wrappedConnection.createClob();
   }

   public NClob createNClob() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return this.wrappedConnection.createNClob();
   }

   public SQLXML createSQLXML() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return this.wrappedConnection.createSQLXML();
   }

   public Struct createStruct(String var1, Object[] var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return this.wrappedConnection.createStruct(var1, var2);
   }

   public Properties getClientInfo() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return this.wrappedConnection.getClientInfo();
   }

   public String getClientInfo(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return this.wrappedConnection.getClientInfo(var1);
   }

   public void setClientInfo(Properties var1) throws SQLClientInfoException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.wrappedConnection.setClientInfo(var1);
   }

   public void setClientInfo(String var1, String var2) throws SQLClientInfoException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.wrappedConnection.setClientInfo(var1, var2);
   }

   public boolean isValid(int var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return this.wrappedConnection.isValid(var1);
   }

   public boolean isWrapperFor(Class<?> var1) throws SQLException {
      this.wrappedConnection.getConnectionLogger().entering(this.toString(), "isWrapperFor", var1);
      DriverJDBCVersion.checkSupportsJDBC4();
      boolean var2 = var1.isInstance(this);
      this.wrappedConnection.getConnectionLogger().exiting(this.toString(), "isWrapperFor", var2);
      return var2;
   }

   public <T> T unwrap(Class<T> var1) throws SQLException {
      this.wrappedConnection.getConnectionLogger().entering(this.toString(), "unwrap", var1);
      DriverJDBCVersion.checkSupportsJDBC4();

      Object var2;
      try {
         var2 = var1.cast(this);
      } catch (ClassCastException var5) {
         SQLServerException var4 = new SQLServerException(var5.getMessage(), var5);
         throw var4;
      }

      this.wrappedConnection.getConnectionLogger().exiting(this.toString(), "unwrap", var2);
      return (T)var2;
   }

   public UUID getClientConnectionId() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.getClientConnectionId();
   }

   public synchronized void setSendTimeAsDatetime(boolean var1) throws SQLServerException {
      this.checkClosed();
      this.wrappedConnection.setSendTimeAsDatetime(var1);
   }

   public final synchronized boolean getSendTimeAsDatetime() throws SQLServerException {
      this.checkClosed();
      return this.wrappedConnection.getSendTimeAsDatetime();
   }
}
