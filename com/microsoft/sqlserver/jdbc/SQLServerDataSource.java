package com.microsoft.sqlserver.jdbc;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;

public class SQLServerDataSource implements ISQLServerDataSource, DataSource, Serializable, Referenceable {
   static final Logger dsLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.SQLServerDataSource");
   static final Logger loggerExternal = Logger.getLogger("com.microsoft.sqlserver.jdbc.DataSource");
   private final String loggingClassName;
   private boolean trustStorePasswordStripped = false;
   private static final long serialVersionUID = 654861379544314296L;
   private Properties connectionProps = new Properties();
   private String dataSourceURL;
   private String dataSourceDescription;
   private static int baseDataSourceID = 0;
   private final String traceID;
   private transient PrintWriter logWriter;

   public SQLServerDataSource() {
      int var1 = nextDataSourceID();
      String var2 = this.getClass().getName();
      this.traceID = var2.substring(1 + var2.lastIndexOf(46)) + ":" + var1;
      this.loggingClassName = "com.microsoft.sqlserver.jdbc." + var2.substring(1 + var2.lastIndexOf(46)) + ":" + var1;
   }

   String getClassNameLogging() {
      return this.loggingClassName;
   }

   public String toString() {
      return this.traceID;
   }

   public Connection getConnection() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getConnection");
      SQLServerConnection var1 = this.getConnectionInternal(null, null, null);
      loggerExternal.exiting(this.getClassNameLogging(), "getConnection", var1);
      return var1;
   }

   public Connection getConnection(String var1, String var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getConnection", new Object[]{var1, "Password not traced"});
      }

      SQLServerConnection var3 = this.getConnectionInternal(var1, var2, null);
      loggerExternal.exiting(this.getClassNameLogging(), "getConnection", var3);
      return var3;
   }

   public void setLoginTimeout(int var1) {
      this.setIntProperty(this.connectionProps, SQLServerDriverIntProperty.LOGIN_TIMEOUT.toString(), var1);
   }

   public int getLoginTimeout() {
      int var1 = SQLServerDriverIntProperty.LOGIN_TIMEOUT.getDefaultValue();
      int var2 = this.getIntProperty(this.connectionProps, SQLServerDriverIntProperty.LOGIN_TIMEOUT.toString(), var1);
      return var2 == 0 ? var1 : var2;
   }

   public void setLogWriter(PrintWriter var1) {
      loggerExternal.entering(this.getClassNameLogging(), "setLogWriter", var1);
      this.logWriter = var1;
      loggerExternal.exiting(this.getClassNameLogging(), "setLogWriter");
   }

   public PrintWriter getLogWriter() {
      loggerExternal.entering(this.getClassNameLogging(), "getLogWriter");
      loggerExternal.exiting(this.getClassNameLogging(), "getLogWriter", this.logWriter);
      return this.logWriter;
   }

   public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      DriverJDBCVersion.checkSupportsJDBC41();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public void setApplicationName(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.APPLICATION_NAME.toString(), var1);
   }

   public String getApplicationName() {
      return this.getStringProperty(
         this.connectionProps, SQLServerDriverStringProperty.APPLICATION_NAME.toString(), SQLServerDriverStringProperty.APPLICATION_NAME.getDefaultValue()
      );
   }

   public void setDatabaseName(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.DATABASE_NAME.toString(), var1);
   }

   public String getDatabaseName() {
      return this.getStringProperty(this.connectionProps, SQLServerDriverStringProperty.DATABASE_NAME.toString(), null);
   }

   public void setInstanceName(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.INSTANCE_NAME.toString(), var1);
   }

   public String getInstanceName() {
      return this.getStringProperty(this.connectionProps, SQLServerDriverStringProperty.INSTANCE_NAME.toString(), null);
   }

   public void setIntegratedSecurity(boolean var1) {
      this.setBooleanProperty(this.connectionProps, SQLServerDriverBooleanProperty.INTEGRATED_SECURITY.toString(), var1);
   }

   public void setAuthenticationScheme(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.AUTHENTICATION_SCHEME.toString(), var1);
   }

   public void setAuthentication(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.AUTHENTICATION.toString(), var1);
   }

   public String getAuthentication() {
      return this.getStringProperty(
         this.connectionProps, SQLServerDriverStringProperty.AUTHENTICATION.toString(), SQLServerDriverStringProperty.AUTHENTICATION.getDefaultValue()
      );
   }

   public void setAccessToken(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.ACCESS_TOKEN.toString(), var1);
   }

   public String getAccessToken() {
      return this.getStringProperty(this.connectionProps, SQLServerDriverStringProperty.ACCESS_TOKEN.toString(), null);
   }

   public void setColumnEncryptionSetting(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.COLUMN_ENCRYPTION.toString(), var1);
   }

   public String getColumnEncryptionSetting() {
      return this.getStringProperty(
         this.connectionProps, SQLServerDriverStringProperty.COLUMN_ENCRYPTION.toString(), SQLServerDriverStringProperty.COLUMN_ENCRYPTION.getDefaultValue()
      );
   }

   public void setKeyStoreAuthentication(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.KEY_STORE_AUTHENTICATION.toString(), var1);
   }

   public String getKeyStoreAuthentication() {
      return this.getStringProperty(
         this.connectionProps,
         SQLServerDriverStringProperty.KEY_STORE_AUTHENTICATION.toString(),
         SQLServerDriverStringProperty.KEY_STORE_AUTHENTICATION.getDefaultValue()
      );
   }

   public void setKeyStoreSecret(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.KEY_STORE_SECRET.toString(), var1);
   }

   public void setKeyStoreLocation(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.KEY_STORE_LOCATION.toString(), var1);
   }

   public String getKeyStoreLocation() {
      return this.getStringProperty(
         this.connectionProps, SQLServerDriverStringProperty.KEY_STORE_LOCATION.toString(), SQLServerDriverStringProperty.KEY_STORE_LOCATION.getDefaultValue()
      );
   }

   public void setLastUpdateCount(boolean var1) {
      this.setBooleanProperty(this.connectionProps, SQLServerDriverBooleanProperty.LAST_UPDATE_COUNT.toString(), var1);
   }

   public boolean getLastUpdateCount() {
      return this.getBooleanProperty(
         this.connectionProps, SQLServerDriverBooleanProperty.LAST_UPDATE_COUNT.toString(), SQLServerDriverBooleanProperty.LAST_UPDATE_COUNT.getDefaultValue()
      );
   }

   public void setEncrypt(boolean var1) {
      this.setBooleanProperty(this.connectionProps, SQLServerDriverBooleanProperty.ENCRYPT.toString(), var1);
   }

   public boolean getEncrypt() {
      return this.getBooleanProperty(
         this.connectionProps, SQLServerDriverBooleanProperty.ENCRYPT.toString(), SQLServerDriverBooleanProperty.ENCRYPT.getDefaultValue()
      );
   }

   public void setTransparentNetworkIPResolution(boolean var1) {
      this.setBooleanProperty(this.connectionProps, SQLServerDriverBooleanProperty.TRANSPARENT_NETWORK_IP_RESOLUTION.toString(), var1);
   }

   public boolean getTransparentNetworkIPResolution() {
      return this.getBooleanProperty(
         this.connectionProps,
         SQLServerDriverBooleanProperty.TRANSPARENT_NETWORK_IP_RESOLUTION.toString(),
         SQLServerDriverBooleanProperty.TRANSPARENT_NETWORK_IP_RESOLUTION.getDefaultValue()
      );
   }

   public void setTrustServerCertificate(boolean var1) {
      this.setBooleanProperty(this.connectionProps, SQLServerDriverBooleanProperty.TRUST_SERVER_CERTIFICATE.toString(), var1);
   }

   public boolean getTrustServerCertificate() {
      return this.getBooleanProperty(
         this.connectionProps,
         SQLServerDriverBooleanProperty.TRUST_SERVER_CERTIFICATE.toString(),
         SQLServerDriverBooleanProperty.TRUST_SERVER_CERTIFICATE.getDefaultValue()
      );
   }

   public void setTrustStore(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.TRUST_STORE.toString(), var1);
   }

   public String getTrustStore() {
      return this.getStringProperty(this.connectionProps, SQLServerDriverStringProperty.TRUST_STORE.toString(), null);
   }

   public void setTrustStorePassword(String var1) {
      if (var1 != null) {
         this.trustStorePasswordStripped = false;
      }

      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.TRUST_STORE_PASSWORD.toString(), var1);
   }

   public void setHostNameInCertificate(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.HOSTNAME_IN_CERTIFICATE.toString(), var1);
   }

   public String getHostNameInCertificate() {
      return this.getStringProperty(this.connectionProps, SQLServerDriverStringProperty.HOSTNAME_IN_CERTIFICATE.toString(), null);
   }

   public void setLockTimeout(int var1) {
      this.setIntProperty(this.connectionProps, SQLServerDriverIntProperty.LOCK_TIMEOUT.toString(), var1);
   }

   public int getLockTimeout() {
      return this.getIntProperty(
         this.connectionProps, SQLServerDriverIntProperty.LOCK_TIMEOUT.toString(), SQLServerDriverIntProperty.LOCK_TIMEOUT.getDefaultValue()
      );
   }

   public void setPassword(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.PASSWORD.toString(), var1);
   }

   String getPassword() {
      return this.getStringProperty(this.connectionProps, SQLServerDriverStringProperty.PASSWORD.toString(), null);
   }

   public void setPortNumber(int var1) {
      this.setIntProperty(this.connectionProps, SQLServerDriverIntProperty.PORT_NUMBER.toString(), var1);
   }

   public int getPortNumber() {
      return this.getIntProperty(
         this.connectionProps, SQLServerDriverIntProperty.PORT_NUMBER.toString(), SQLServerDriverIntProperty.PORT_NUMBER.getDefaultValue()
      );
   }

   public void setSelectMethod(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.SELECT_METHOD.toString(), var1);
   }

   public String getSelectMethod() {
      return this.getStringProperty(
         this.connectionProps, SQLServerDriverStringProperty.SELECT_METHOD.toString(), SQLServerDriverStringProperty.SELECT_METHOD.getDefaultValue()
      );
   }

   public void setResponseBuffering(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.RESPONSE_BUFFERING.toString(), var1);
   }

   public String getResponseBuffering() {
      return this.getStringProperty(
         this.connectionProps, SQLServerDriverStringProperty.RESPONSE_BUFFERING.toString(), SQLServerDriverStringProperty.RESPONSE_BUFFERING.getDefaultValue()
      );
   }

   public void setApplicationIntent(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.APPLICATION_INTENT.toString(), var1);
   }

   public String getApplicationIntent() {
      return this.getStringProperty(
         this.connectionProps, SQLServerDriverStringProperty.APPLICATION_INTENT.toString(), SQLServerDriverStringProperty.APPLICATION_INTENT.getDefaultValue()
      );
   }

   public void setSendTimeAsDatetime(boolean var1) {
      this.setBooleanProperty(this.connectionProps, SQLServerDriverBooleanProperty.SEND_TIME_AS_DATETIME.toString(), var1);
   }

   public boolean getSendTimeAsDatetime() {
      return this.getBooleanProperty(
         this.connectionProps,
         SQLServerDriverBooleanProperty.SEND_TIME_AS_DATETIME.toString(),
         SQLServerDriverBooleanProperty.SEND_TIME_AS_DATETIME.getDefaultValue()
      );
   }

   public void setSendStringParametersAsUnicode(boolean var1) {
      this.setBooleanProperty(this.connectionProps, SQLServerDriverBooleanProperty.SEND_STRING_PARAMETERS_AS_UNICODE.toString(), var1);
   }

   public boolean getSendStringParametersAsUnicode() {
      return this.getBooleanProperty(
         this.connectionProps,
         SQLServerDriverBooleanProperty.SEND_STRING_PARAMETERS_AS_UNICODE.toString(),
         SQLServerDriverBooleanProperty.SEND_STRING_PARAMETERS_AS_UNICODE.getDefaultValue()
      );
   }

   public void setServerNameAsACE(boolean var1) {
      this.setBooleanProperty(this.connectionProps, SQLServerDriverBooleanProperty.SERVER_NAME_AS_ACE.toString(), var1);
   }

   public boolean getServerNameAsACE() {
      return this.getBooleanProperty(
         this.connectionProps,
         SQLServerDriverBooleanProperty.SERVER_NAME_AS_ACE.toString(),
         SQLServerDriverBooleanProperty.SERVER_NAME_AS_ACE.getDefaultValue()
      );
   }

   public void setServerName(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.SERVER_NAME.toString(), var1);
   }

   public String getServerName() {
      return this.getStringProperty(this.connectionProps, SQLServerDriverStringProperty.SERVER_NAME.toString(), null);
   }

   public void setServerSpn(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.SERVER_SPN.toString(), var1);
   }

   public String getServerSpn() {
      return this.getStringProperty(this.connectionProps, SQLServerDriverStringProperty.SERVER_SPN.toString(), null);
   }

   public void setFailoverPartner(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.FAILOVER_PARTNER.toString(), var1);
   }

   public String getFailoverPartner() {
      return this.getStringProperty(this.connectionProps, SQLServerDriverStringProperty.FAILOVER_PARTNER.toString(), null);
   }

   public void setMultiSubnetFailover(boolean var1) {
      this.setBooleanProperty(this.connectionProps, SQLServerDriverBooleanProperty.MULTI_SUBNET_FAILOVER.toString(), var1);
   }

   public boolean getMultiSubnetFailover() {
      return this.getBooleanProperty(
         this.connectionProps,
         SQLServerDriverBooleanProperty.MULTI_SUBNET_FAILOVER.toString(),
         SQLServerDriverBooleanProperty.MULTI_SUBNET_FAILOVER.getDefaultValue()
      );
   }

   public void setUser(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.USER.toString(), var1);
   }

   public String getUser() {
      return this.getStringProperty(this.connectionProps, SQLServerDriverStringProperty.USER.toString(), null);
   }

   public void setWorkstationID(String var1) {
      this.setStringProperty(this.connectionProps, SQLServerDriverStringProperty.WORKSTATION_ID.toString(), var1);
   }

   public String getWorkstationID() {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getWorkstationID");
      }

      String var1 = this.connectionProps.getProperty(SQLServerDriverStringProperty.WORKSTATION_ID.toString());
      if (null == var1) {
         var1 = Util.lookupHostName();
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getWorkstationID", var1);
      return var1;
   }

   public void setXopenStates(boolean var1) {
      this.setBooleanProperty(this.connectionProps, SQLServerDriverBooleanProperty.XOPEN_STATES.toString(), var1);
   }

   public boolean getXopenStates() {
      return this.getBooleanProperty(
         this.connectionProps, SQLServerDriverBooleanProperty.XOPEN_STATES.toString(), SQLServerDriverBooleanProperty.XOPEN_STATES.getDefaultValue()
      );
   }

   public void setURL(String var1) {
      loggerExternal.entering(this.getClassNameLogging(), "setURL", var1);
      this.dataSourceURL = var1;
      loggerExternal.exiting(this.getClassNameLogging(), "setURL");
   }

   public String getURL() {
      String var1 = this.dataSourceURL;
      loggerExternal.entering(this.getClassNameLogging(), "getURL");
      if (null == this.dataSourceURL) {
         var1 = "jdbc:sqlserver://";
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getURL", var1);
      return var1;
   }

   public void setDescription(String var1) {
      loggerExternal.entering(this.getClassNameLogging(), "setDescription", var1);
      this.dataSourceDescription = var1;
      loggerExternal.exiting(this.getClassNameLogging(), "setDescription");
   }

   public String getDescription() {
      loggerExternal.entering(this.getClassNameLogging(), "getDescription");
      loggerExternal.exiting(this.getClassNameLogging(), "getDescription", this.dataSourceDescription);
      return this.dataSourceDescription;
   }

   public void setPacketSize(int var1) {
      this.setIntProperty(this.connectionProps, SQLServerDriverIntProperty.PACKET_SIZE.toString(), var1);
   }

   public int getPacketSize() {
      return this.getIntProperty(
         this.connectionProps, SQLServerDriverIntProperty.PACKET_SIZE.toString(), SQLServerDriverIntProperty.PACKET_SIZE.getDefaultValue()
      );
   }

   private void setStringProperty(Properties var1, String var2, String var3) {
      if (loggerExternal.isLoggable(Level.FINER) && !var2.contains("password") && !var2.contains("Password")) {
         loggerExternal.entering(this.getClassNameLogging(), "set" + var2, var3);
      } else {
         loggerExternal.entering(this.getClassNameLogging(), "set" + var2);
      }

      if (null != var3) {
         var1.setProperty(var2, var3);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "set" + var2);
   }

   private String getStringProperty(Properties var1, String var2, String var3) {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "get" + var2);
      }

      String var4 = var1.getProperty(var2);
      if (null == var4) {
         var4 = var3;
      }

      if (loggerExternal.isLoggable(Level.FINER) && !var2.contains("password") && !var2.contains("Password")) {
         loggerExternal.exiting(this.getClassNameLogging(), "get" + var2, var4);
      }

      return var4;
   }

   private void setIntProperty(Properties var1, String var2, int var3) {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "set" + var2, new Integer(var3));
      }

      var1.setProperty(var2, new Integer(var3).toString());
      loggerExternal.exiting(this.getClassNameLogging(), "set" + var2);
   }

   private int getIntProperty(Properties var1, String var2, int var3) {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "get" + var2);
      }

      String var4 = var1.getProperty(var2);
      int var5 = var3;
      if (null != var4) {
         try {
            var5 = Integer.parseInt(var4);
         } catch (NumberFormatException var7) {
            assert false : "Bad portNumber:-" + var4;
         }
      }

      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.exiting(this.getClassNameLogging(), "get" + var2, new Integer(var5));
      }

      return var5;
   }

   private void setBooleanProperty(Properties var1, String var2, boolean var3) {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "set" + var2, var3);
      }

      var1.setProperty(var2, var3 ? "true" : "false");
      loggerExternal.exiting(this.getClassNameLogging(), "set" + var2);
   }

   private boolean getBooleanProperty(Properties var1, String var2, boolean var3) {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "get" + var2);
      }

      String var4 = var1.getProperty(var2);
      Boolean var5;
      if (null == var4) {
         var5 = var3;
      } else {
         var5 = Boolean.valueOf(var4);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "get" + var2, var5);
      return var5;
   }

   SQLServerConnection getConnectionInternal(String var1, String var2, SQLServerPooledConnection var3) throws SQLServerException {
      Properties var4 = null;
      Properties var5 = null;
      if (this.trustStorePasswordStripped) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_referencingFailedTSP"), null, true);
      }

      if (null == var1 && null == var2) {
         var4 = this.connectionProps;
      } else {
         var4 = (Properties)this.connectionProps.clone();
         var4.remove(SQLServerDriverStringProperty.USER.toString());
         var4.remove(SQLServerDriverStringProperty.PASSWORD.toString());
         if (null != var1) {
            var4.put(SQLServerDriverStringProperty.USER.toString(), var1);
         }

         if (null != var2) {
            var4.put(SQLServerDriverStringProperty.PASSWORD.toString(), var2);
         }
      }

      if (null != this.dataSourceURL) {
         Properties var6 = Util.parseUrl(this.dataSourceURL, dsLogger);
         if (null == var6) {
            SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_errorConnectionString"), null, true);
         }

         var5 = SQLServerDriver.mergeURLAndSuppliedProperties(var6, var4);
      } else {
         var5 = var4;
      }

      if (dsLogger.isLoggable(Level.FINER)) {
         dsLogger.finer(this.toString() + " Begin create new connection.");
      }

      SQLServerConnection var9 = new SQLServerConnection(this.toString());
      var9.connect(var5, var3);
      if (dsLogger.isLoggable(Level.FINER)) {
         dsLogger.finer(this.toString() + " End create new connection " + var9.toString());
      }

      return var9;
   }

   public Reference getReference() {
      loggerExternal.entering(this.getClassNameLogging(), "getReference");
      Reference var1 = this.getReferenceInternal("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
      loggerExternal.exiting(this.getClassNameLogging(), "getReference", var1);
      return var1;
   }

   Reference getReferenceInternal(String var1) {
      if (dsLogger.isLoggable(Level.FINER)) {
         dsLogger.finer(this.toString() + " creating reference for " + var1 + ".");
      }

      Reference var2 = new Reference(this.getClass().getName(), "com.microsoft.sqlserver.jdbc.SQLServerDataSourceObjectFactory", null);
      if (null != var1) {
         var2.add(new StringRefAddr("class", var1));
      }

      if (this.trustStorePasswordStripped) {
         var2.add(new StringRefAddr("trustStorePasswordStripped", "true"));
      }

      Enumeration var3 = this.connectionProps.keys();

      while (var3.hasMoreElements()) {
         String var4 = (String)var3.nextElement();
         if (var4.equals(SQLServerDriverStringProperty.TRUST_STORE_PASSWORD.toString())) {
            assert !this.trustStorePasswordStripped;
            var2.add(new StringRefAddr("trustStorePasswordStripped", "true"));
         } else if (!var4.contains(SQLServerDriverStringProperty.PASSWORD.toString())) {
            var2.add(new StringRefAddr(var4, this.connectionProps.getProperty(var4)));
         }
      }

      if (null != this.dataSourceURL) {
         var2.add(new StringRefAddr("dataSourceURL", this.dataSourceURL));
      }

      if (null != this.dataSourceDescription) {
         var2.add(new StringRefAddr("dataSourceDescription", this.dataSourceDescription));
      }

      return var2;
   }

   void initializeFromReference(Reference var1) {
      Enumeration var2 = var1.getAll();

      while (var2.hasMoreElements()) {
         StringRefAddr var3 = (StringRefAddr)var2.nextElement();
         String var4 = var3.getType();
         String var5 = (String)var3.getContent();
         if (var4.equals("dataSourceURL")) {
            this.dataSourceURL = var5;
         } else if (var4.equals("dataSourceDescription")) {
            this.dataSourceDescription = var5;
         } else if (var4.equals("trustStorePasswordStripped")) {
            this.trustStorePasswordStripped = true;
         } else if (!var4.equals("class")) {
            this.connectionProps.setProperty(var4, var5);
         }
      }
   }

   public boolean isWrapperFor(Class<?> var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "isWrapperFor", var1);
      DriverJDBCVersion.checkSupportsJDBC4();
      boolean var2 = var1.isInstance(this);
      loggerExternal.exiting(this.getClassNameLogging(), "isWrapperFor", var2);
      return var2;
   }

   public <T> T unwrap(Class<T> var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "unwrap", var1);
      DriverJDBCVersion.checkSupportsJDBC4();

      Object var2;
      try {
         var2 = var1.cast(this);
      } catch (ClassCastException var4) {
         throw new SQLServerException(var4.getMessage(), var4);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "unwrap", var2);
      return (T)var2;
   }

   private static synchronized int nextDataSourceID() {
      baseDataSourceID++;
      return baseDataSourceID;
   }

   private Object writeReplace() throws ObjectStreamException {
      return new SQLServerDataSource.SerializationProxy(this);
   }

   private void readObject(ObjectInputStream var1) throws InvalidObjectException {
      throw new InvalidObjectException("");
   }

   private static class SerializationProxy implements Serializable {
      private final Reference ref;
      private static final long serialVersionUID = 654661379542314226L;

      SerializationProxy(SQLServerDataSource var1) {
         this.ref = var1.getReferenceInternal(null);
      }

      private Object readResolve() {
         SQLServerDataSource var1 = new SQLServerDataSource();
         var1.initializeFromReference(this.ref);
         return var1;
      }
   }
}
