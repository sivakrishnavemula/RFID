package com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.IDN;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
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
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.XAConnection;
import javax.xml.bind.DatatypeConverter;

public class SQLServerConnection implements ISQLServerConnection {
   long timerExpire;
   boolean attemptRefreshTokenLocked = false;
   private boolean fedAuthRequiredByUser = false;
   private boolean fedAuthRequiredPreLoginResponse = false;
   private boolean federatedAuthenticationAcknowledged = false;
   private boolean federatedAuthenticationRequested = false;
   private boolean federatedAuthenticationInfoRequested = false;
   private SQLServerConnection.FederatedAuthenticationFeatureExtensionData fedAuthFeatureExtensionData = null;
   private String authenticationString = null;
   private byte[] accessTokenInByte = null;
   private SQLServerConnection.SqlFedAuthToken fedAuthToken = null;
   private static final float TIMEOUTSTEP = 0.08F;
   static final int TnirFirstAttemptTimeoutMs = 500;
   private static final int INTERMITTENT_TLS_MAX_RETRY = 5;
   private boolean isRoutedInCurrentAttempt = false;
   private ServerPortPlaceHolder routingInfo = null;
   private static final String callAbortPerm = "callAbort";
   private boolean sendStringParametersAsUnicode = SQLServerDriverBooleanProperty.SEND_STRING_PARAMETERS_AS_UNICODE.getDefaultValue();
   private boolean lastUpdateCount;
   private boolean serverNameAsACE = SQLServerDriverBooleanProperty.SERVER_NAME_AS_ACE.getDefaultValue();
   private boolean multiSubnetFailover;
   private boolean transparentNetworkIPResolution;
   private ApplicationIntent applicationIntent = null;
   private int nLockTimeout;
   private String selectMethod;
   private String responseBuffering;
   private boolean sendTimeAsDatetime = SQLServerDriverBooleanProperty.SEND_TIME_AS_DATETIME.getDefaultValue();
   private byte requestedEncryptionLevel = -1;
   private boolean trustServerCertificate;
   private byte negotiatedEncryptionLevel = -1;
   static final String RESERVED_PROVIDER_NAME_PREFIX = "MSSQL_";
   String columnEncryptionSetting = null;
   String keyStoreAuthentication = null;
   String keyStoreSecret = null;
   String keyStoreLocation = null;
   private boolean serverSupportsColumnEncryption = false;
   static boolean isWindows;
   static Map<String, SQLServerColumnEncryptionKeyStoreProvider> globalSystemColumnEncryptionKeyStoreProviders = new HashMap();
   static Map<String, SQLServerColumnEncryptionKeyStoreProvider> globalCustomColumnEncryptionKeyStoreProviders;
   Map<String, SQLServerColumnEncryptionKeyStoreProvider> systemColumnEncryptionKeyStoreProvider = new HashMap();
   private String trustedServerNameAE = null;
   private static Map<String, List<String>> columnEncryptionTrustedMasterKeyPaths;
   Properties activeConnectionProperties;
   private boolean integratedSecurity = SQLServerDriverBooleanProperty.INTEGRATED_SECURITY.getDefaultValue();
   private AuthenticationScheme intAuthScheme = AuthenticationScheme.nativeAuthentication;
   ServerPortPlaceHolder currentConnectPlaceHolder = null;
   String sqlServerVersion;
   boolean xopenStates;
   private boolean databaseAutoCommitMode;
   private boolean inXATransaction = false;
   private byte[] transactionDescriptor = new byte[8];
   private boolean rolledBackTransaction;
   private SQLServerConnection.State state = SQLServerConnection.State.Initialized;
   static final int maxDecimalPrecision = 38;
   static final int defaultDecimalPrecision = 18;
   final String traceID;
   private int maxFieldSize;
   private int maxRows;
   private SQLCollation databaseCollation;
   private static int baseConnectionID;
   private String sCatalog = "master";
   private String originalCatalog = "master";
   private int transactionIsolationLevel;
   private SQLServerPooledConnection pooledConnectionParent;
   private DatabaseMetaData databaseMetaData;
   private int nNextSavePointId = 10000;
   private static final Logger connectionlogger;
   private static final Logger loggerExternal;
   private final String loggingClassName;
   private String failoverPartnerServerProvided = null;
   private int holdability;
   private int tdsPacketSize = 4096;
   private int requestedPacketSize = 8000;
   private TDSChannel tdsChannel;
   private TDSCommand currentCommand = null;
   private int tdsVersion = 0;
   private int serverMajorVersion;
   private SQLServerConnectionPoolProxy proxy;
   private UUID clientConnectionId = null;
   static final int MAX_SQL_LOGIN_NAME_WCHARS = 128;
   static final int DEFAULTPORT;
   private final Object schedulerLock = new Object();
   volatile SQLWarning sqlWarnings;
   Integer warningSynchronization = new Integer(1);
   private static final int ENVCHANGE_DATABASE = 1;
   private static final int ENVCHANGE_LANGUAGE = 2;
   private static final int ENVCHANGE_CHARSET = 3;
   private static final int ENVCHANGE_PACKETSIZE = 4;
   private static final int ENVCHANGE_SORTLOCALEID = 5;
   private static final int ENVCHANGE_SORTFLAGS = 6;
   private static final int ENVCHANGE_SQLCOLLATION = 7;
   private static final int ENVCHANGE_XACT_BEGIN = 8;
   private static final int ENVCHANGE_XACT_COMMIT = 9;
   private static final int ENVCHANGE_XACT_ROLLBACK = 10;
   private static final int ENVCHANGE_DTC_ENLIST = 11;
   private static final int ENVCHANGE_DTC_DEFECT = 12;
   private static final int ENVCHANGE_CHANGE_MIRROR = 13;
   private static final int ENVCHANGE_UNUSED_14 = 14;
   private static final int ENVCHANGE_DTC_PROMOTE = 15;
   private static final int ENVCHANGE_DTC_MGR_ADDR = 16;
   private static final int ENVCHANGE_XACT_ENDED = 17;
   private static final int ENVCHANGE_RESET_COMPLETE = 18;
   private static final int ENVCHANGE_USER_INFO = 19;
   private static final int ENVCHANGE_ROUTING = 20;
   static final char[] OUT;
   private static final int BROWSER_PORT = 1434;
   private static long columnEncryptionKeyCacheTtl;

   SQLServerConnection.SqlFedAuthToken getAuthenticationResult() {
      return this.fedAuthToken;
   }

   boolean sendStringParametersAsUnicode() {
      return this.sendStringParametersAsUnicode;
   }

   final boolean useLastUpdateCount() {
      return this.lastUpdateCount;
   }

   boolean serverNameAsACE() {
      return this.serverNameAsACE;
   }

   final boolean getMultiSubnetFailover() {
      return this.multiSubnetFailover;
   }

   final boolean getTransparentNetworkIPResolution() {
      return this.transparentNetworkIPResolution;
   }

   final ApplicationIntent getApplicationIntent() {
      return this.applicationIntent;
   }

   final String getSelectMethod() {
      return this.selectMethod;
   }

   final String getResponseBuffering() {
      return this.responseBuffering;
   }

   public final synchronized boolean getSendTimeAsDatetime() {
      return !this.isKatmaiOrLater() || this.sendTimeAsDatetime;
   }

   final int baseYear() {
      return this.getSendTimeAsDatetime() ? 1970 : 1900;
   }

   final byte getRequestedEncryptionLevel() {
      assert -1 != this.requestedEncryptionLevel;
      return this.requestedEncryptionLevel;
   }

   final boolean trustServerCertificate() {
      return this.trustServerCertificate;
   }

   final byte getNegotiatedEncryptionLevel() {
      assert -1 != this.negotiatedEncryptionLevel;
      return this.negotiatedEncryptionLevel;
   }

   boolean isColumnEncryptionSettingEnabled() {
      return this.columnEncryptionSetting.equalsIgnoreCase(ColumnEncryptionSetting.Enabled.toString());
   }

   boolean getServerSupportsColumnEncryption() {
      return this.serverSupportsColumnEncryption;
   }

   public static synchronized void registerColumnEncryptionKeyStoreProviders(Map<String, SQLServerColumnEncryptionKeyStoreProvider> var0) throws SQLServerException {
      loggerExternal.entering(
         SQLServerConnection.class.getName(), "registerColumnEncryptionKeyStoreProviders", "Registering Column Encryption Key Store Providers"
      );
      if (null == var0) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_CustomKeyStoreProviderMapNull"), null, 0, false);
      }

      if (null != globalCustomColumnEncryptionKeyStoreProviders) {
         throw new SQLServerException(null, SQLServerException.getErrString("R_CustomKeyStoreProviderSetOnce"), null, 0, false);
      }

      globalCustomColumnEncryptionKeyStoreProviders = new HashMap();

      for (Entry var2 : var0.entrySet()) {
         String var3 = (String)var2.getKey();
         if (null == var3 || 0 == var3.length()) {
            throw new SQLServerException(null, SQLServerException.getErrString("R_EmptyCustomKeyStoreProviderName"), null, 0, false);
         }

         if (var3.substring(0, 6).equalsIgnoreCase("MSSQL_")) {
            MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_InvalidCustomKeyStoreProviderName"));
            Object[] var7 = new Object[]{var3, "MSSQL_"};
            throw new SQLServerException(null, var6.format(var7), null, 0, false);
         }

         if (null == var2.getValue()) {
            MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_CustomKeyStoreProviderValueNull"));
            Object[] var5 = new Object[]{var3, "MSSQL_"};
            throw new SQLServerException(null, var4.format(var5), null, 0, false);
         }

         globalCustomColumnEncryptionKeyStoreProviders.put(var2.getKey(), var2.getValue());
      }

      loggerExternal.exiting(
         SQLServerConnection.class.getName(),
         "registerColumnEncryptionKeyStoreProviders",
         "Number of Key store providers that are registered:" + globalCustomColumnEncryptionKeyStoreProviders.size()
      );
   }

   static synchronized SQLServerColumnEncryptionKeyStoreProvider getGlobalSystemColumnEncryptionKeyStoreProvider(String var0) {
      return null != globalSystemColumnEncryptionKeyStoreProviders && globalSystemColumnEncryptionKeyStoreProviders.containsKey(var0)
         ? (SQLServerColumnEncryptionKeyStoreProvider)globalSystemColumnEncryptionKeyStoreProviders.get(var0)
         : null;
   }

   static synchronized String getAllGlobalCustomSystemColumnEncryptionKeyStoreProviders() {
      return null != globalCustomColumnEncryptionKeyStoreProviders ? globalCustomColumnEncryptionKeyStoreProviders.keySet().toString() : null;
   }

   synchronized String getAllSystemColumnEncryptionKeyStoreProviders() {
      String var1 = "";
      if (0 != this.systemColumnEncryptionKeyStoreProvider.size()) {
         var1 = this.systemColumnEncryptionKeyStoreProvider.keySet().toString();
      }

      if (0 != globalSystemColumnEncryptionKeyStoreProviders.size()) {
         var1 = var1 + "," + globalSystemColumnEncryptionKeyStoreProviders.keySet().toString();
      }

      return var1;
   }

   static synchronized SQLServerColumnEncryptionKeyStoreProvider getGlobalCustomColumnEncryptionKeyStoreProvider(String var0) {
      return null != globalCustomColumnEncryptionKeyStoreProviders && globalCustomColumnEncryptionKeyStoreProviders.containsKey(var0)
         ? (SQLServerColumnEncryptionKeyStoreProvider)globalCustomColumnEncryptionKeyStoreProviders.get(var0)
         : null;
   }

   synchronized SQLServerColumnEncryptionKeyStoreProvider getSystemColumnEncryptionKeyStoreProvider(String var1) {
      return null != this.systemColumnEncryptionKeyStoreProvider && this.systemColumnEncryptionKeyStoreProvider.containsKey(var1)
         ? (SQLServerColumnEncryptionKeyStoreProvider)this.systemColumnEncryptionKeyStoreProvider.get(var1)
         : null;
   }

   public static synchronized void setColumnEncryptionTrustedMasterKeyPaths(Map<String, List<String>> var0) {
      loggerExternal.entering(SQLServerConnection.class.getName(), "setColumnEncryptionTrustedMasterKeyPaths", "Setting Trusted Master Key Paths");
      columnEncryptionTrustedMasterKeyPaths.clear();

      for (Entry var2 : var0.entrySet()) {
         columnEncryptionTrustedMasterKeyPaths.put(((String)var2.getKey()).toUpperCase(), var2.getValue());
      }

      loggerExternal.exiting(
         SQLServerConnection.class.getName(),
         "setColumnEncryptionTrustedMasterKeyPaths",
         "Number of Trusted Master Key Paths: " + columnEncryptionTrustedMasterKeyPaths.size()
      );
   }

   public static synchronized void updateColumnEncryptionTrustedMasterKeyPaths(String var0, List<String> var1) {
      loggerExternal.entering(SQLServerConnection.class.getName(), "updateColumnEncryptionTrustedMasterKeyPaths", "Updating Trusted Master Key Paths");
      columnEncryptionTrustedMasterKeyPaths.put(var0.toUpperCase(), var1);
      loggerExternal.exiting(
         SQLServerConnection.class.getName(),
         "updateColumnEncryptionTrustedMasterKeyPaths",
         "Number of Trusted Master Key Paths: " + columnEncryptionTrustedMasterKeyPaths.size()
      );
   }

   public static synchronized void removeColumnEncryptionTrustedMasterKeyPaths(String var0) {
      loggerExternal.entering(SQLServerConnection.class.getName(), "removeColumnEncryptionTrustedMasterKeyPaths", "Removing Trusted Master Key Paths");
      columnEncryptionTrustedMasterKeyPaths.remove(var0.toUpperCase());
      loggerExternal.exiting(
         SQLServerConnection.class.getName(),
         "removeColumnEncryptionTrustedMasterKeyPaths",
         "Number of Trusted Master Key Paths: " + columnEncryptionTrustedMasterKeyPaths.size()
      );
   }

   public static synchronized Map<String, List<String>> getColumnEncryptionTrustedMasterKeyPaths() {
      loggerExternal.entering(SQLServerConnection.class.getName(), "getColumnEncryptionTrustedMasterKeyPaths", "Getting Trusted Master Key Paths");
      HashMap var0 = new HashMap();

      for (Entry var2 : columnEncryptionTrustedMasterKeyPaths.entrySet()) {
         var0.put(var2.getKey(), var2.getValue());
      }

      loggerExternal.exiting(
         SQLServerConnection.class.getName(), "getColumnEncryptionTrustedMasterKeyPaths", "Number of Trusted Master Key Paths: " + var0.size()
      );
      return var0;
   }

   static synchronized List<String> getColumnEncryptionTrustedMasterKeyPaths(String var0, Boolean[] var1) {
      if (columnEncryptionTrustedMasterKeyPaths.containsKey(var0)) {
         var1[0] = true;
         return (List<String>)columnEncryptionTrustedMasterKeyPaths.get(var0);
      } else {
         var1[0] = false;
         return null;
      }
   }

   final boolean rolledBackTransaction() {
      return this.rolledBackTransaction;
   }

   private void setState(SQLServerConnection.State var1) {
      this.state = var1;
   }

   final boolean isSessionUnAvailable() {
      return !this.state.equals(SQLServerConnection.State.Opened);
   }

   final void setMaxFieldSize(int var1) throws SQLServerException {
      if (this.maxFieldSize != var1) {
         if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
            loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
         }

         this.connectionCommand("SET TEXTSIZE " + (0 == var1 ? Integer.MAX_VALUE : var1), "setMaxFieldSize");
         this.maxFieldSize = var1;
      }
   }

   final void initResettableValues() {
      this.rolledBackTransaction = false;
      this.transactionIsolationLevel = 2;
      this.maxFieldSize = 0;
      this.maxRows = 0;
      this.nLockTimeout = -1;
      this.databaseAutoCommitMode = true;
      this.holdability = 1;
      this.sqlWarnings = null;
      this.sCatalog = this.originalCatalog;
      this.databaseMetaData = null;
   }

   final void setMaxRows(int var1) throws SQLServerException {
      if (this.maxRows != var1) {
         if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
            loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
         }

         this.connectionCommand("SET ROWCOUNT " + var1, "setMaxRows");
         this.maxRows = var1;
      }
   }

   final SQLCollation getDatabaseCollation() {
      return this.databaseCollation;
   }

   final int getHoldabilityInternal() {
      return this.holdability;
   }

   final int getTDSPacketSize() {
      return this.tdsPacketSize;
   }

   final boolean isKatmaiOrLater() {
      assert 0 != this.tdsVersion;
      assert this.tdsVersion >= 1913192450;
      return this.tdsVersion >= 1930100739;
   }

   final boolean isDenaliOrLater() {
      return this.tdsVersion >= 1946157060;
   }

   int getServerMajorVersion() {
      return this.serverMajorVersion;
   }

   public UUID getClientConnectionId() throws SQLServerException {
      this.checkClosed();
      return this.clientConnectionId;
   }

   final UUID getClientConIdInternal() {
      return this.clientConnectionId;
   }

   final boolean attachConnId() {
      return this.state.equals(SQLServerConnection.State.Connected);
   }

   SQLServerConnection(String var1) throws SQLServerException {
      int var2 = nextConnectionID();
      this.traceID = "ConnectionID:" + var2;
      this.loggingClassName = "com.microsoft.sqlserver.jdbc.SQLServerConnection:" + var2;
      if (connectionlogger.isLoggable(Level.FINE)) {
         connectionlogger.fine(this.toString() + " created by (" + var1 + ")");
      }

      this.initResettableValues();
   }

   void setFailoverPartnerServerProvided(String var1) {
      this.failoverPartnerServerProvided = var1;
   }

   final void setAssociatedProxy(SQLServerConnectionPoolProxy var1) {
      this.proxy = var1;
   }

   final Connection getConnection() {
      return (Connection)(null != this.proxy ? this.proxy : this);
   }

   final void resetPooledConnection() {
      this.tdsChannel.resetPooledConnection();
      this.initResettableValues();
   }

   private static synchronized int nextConnectionID() {
      baseConnectionID++;
      return baseConnectionID;
   }

   Logger getConnectionLogger() {
      return connectionlogger;
   }

   String getClassNameLogging() {
      return this.loggingClassName;
   }

   public String toString() {
      return null != this.clientConnectionId ? this.traceID + " ClientConnectionId: " + this.clientConnectionId.toString() : this.traceID;
   }

   void NotImplemented() throws SQLServerException {
      SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_notSupported"), null, false);
   }

   void checkClosed() throws SQLServerException {
      if (this.isSessionUnAvailable()) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_connectionIsClosed"), null, false);
      }

      if (null != this.fedAuthToken && Util.checkIfNeedNewAccessToken(this)) {
         this.connect(this.activeConnectionProperties, null);
      }
   }

   private boolean booleanPropertyOn(String var1, String var2) throws SQLServerException {
      if (null == var2) {
         return false;
      }

      String var3 = var2.toLowerCase(Locale.US);
      if (var3.equals("true")) {
         return true;
      }

      if (var3.equals("false")) {
         return false;
      }

      MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_invalidBooleanValue"));
      Object[] var5 = new Object[]{new String(var1)};
      SQLServerException.makeFromDriverError(this, this, var4.format(var5), null, false);
      return false;
   }

   void ValidateMaxSQLLoginName(String var1, String var2) throws SQLServerException {
      if (var2 != null && var2.length() > 128) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_propertyMaximumExceedsChars"));
         Object[] var4 = new Object[]{var1, Integer.toString(128)};
         SQLServerException.makeFromDriverError(this, this, var3.format(var4), null, false);
      }
   }

   Connection connect(Properties var1, SQLServerPooledConnection var2) throws SQLServerException {
      int var3 = 0;
      long var4 = System.currentTimeMillis();
      int var6 = 0;

      while (true) {
         try {
            return this.connectInternal(var1, var2);
         } catch (SQLServerException var10) {
            if (7 != var10.getDriverErrorCode()) {
               throw var10;
            }

            if (0 == var6) {
               var3 = SQLServerDriverIntProperty.LOGIN_TIMEOUT.getDefaultValue();
               String var8 = var1.getProperty(SQLServerDriverIntProperty.LOGIN_TIMEOUT.toString());
               if (null != var8 && var8.length() > 0) {
                  var3 = Integer.parseInt(var8);
               }
            }

            var6++;
            long var11 = (System.currentTimeMillis() - var4) / 1000L;
            if (5 < var6) {
               if (connectionlogger.isLoggable(Level.FINE)) {
                  connectionlogger.fine("Connection failed during SSL handshake. Maximum retry attempt (5) reached.  ");
               }

               throw var10;
            }

            if (var11 >= var3) {
               if (connectionlogger.isLoggable(Level.FINE)) {
                  connectionlogger.fine("Connection failed during SSL handshake. Not retrying as timeout expired.");
               }

               throw var10;
            }

            if (connectionlogger.isLoggable(Level.FINE)) {
               connectionlogger.fine(
                  "Connection failed during SSL handshake. Retrying due to an intermittent TLS 1.2 failure issue. Retry attempt = " + var6 + "."
               );
            }
         }
      }
   }

   private void registerKeyStoreProviderOnConnection(String var1, String var2, String var3) throws SQLServerException {
      if (null == var1) {
         if (null != var2) {
            MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_keyStoreAuthenticationNotSet"));
            Object[] var8 = new Object[]{"keyStoreSecret"};
            throw new SQLServerException(var6.format(var8), null);
         }

         if (null != var3) {
            MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_keyStoreAuthenticationNotSet"));
            Object[] var5 = new Object[]{"keyStoreLocation"};
            throw new SQLServerException(var4.format(var5), null);
         }
      } else {
         KeyStoreAuthentication var7 = KeyStoreAuthentication.valueOfString(var1);
         switch (var7) {
            case JavaKeyStorePassword:
               if (null == var2 || null == var3) {
                  throw new SQLServerException(SQLServerException.getErrString("R_keyStoreSecretOrLocationNotSet"), null);
               }

               SQLServerColumnEncryptionJavaKeyStoreProvider var9 = new SQLServerColumnEncryptionJavaKeyStoreProvider(var3, var2.toCharArray());
               this.systemColumnEncryptionKeyStoreProvider.put(var9.getName(), var9);
         }
      }
   }

   Connection connectInternal(Properties var1, SQLServerPooledConnection var2) throws SQLServerException {
      try {
         this.activeConnectionProperties = (Properties)var1.clone();
         this.pooledConnectionParent = var2;
         String var3 = null;
         String var4 = null;
         var3 = SQLServerDriverStringProperty.USER.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = SQLServerDriverStringProperty.USER.getDefaultValue();
            this.activeConnectionProperties.setProperty(var3, var4);
         }

         this.ValidateMaxSQLLoginName(var3, var4);
         var3 = SQLServerDriverStringProperty.PASSWORD.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = SQLServerDriverStringProperty.PASSWORD.getDefaultValue();
            this.activeConnectionProperties.setProperty(var3, var4);
         }

         this.ValidateMaxSQLLoginName(var3, var4);
         var3 = SQLServerDriverStringProperty.DATABASE_NAME.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         this.ValidateMaxSQLLoginName(var3, var4);
         int var5 = SQLServerDriverIntProperty.LOGIN_TIMEOUT.getDefaultValue();
         var4 = this.activeConnectionProperties.getProperty(SQLServerDriverIntProperty.LOGIN_TIMEOUT.toString());
         if (null != var4 && var4.length() > 0) {
            try {
               var5 = Integer.parseInt(var4);
            } catch (NumberFormatException var38) {
               MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_invalidTimeOut"));
               Object[] var8 = new Object[]{var4};
               SQLServerException.makeFromDriverError(this, this, var7.format(var8), null, false);
            }

            if (var5 < 0 || var5 > 65535) {
               MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_invalidTimeOut"));
               Object[] var103 = new Object[]{var4};
               SQLServerException.makeFromDriverError(this, this, var6.format(var103), null, false);
            }
         }

         var3 = SQLServerDriverBooleanProperty.SERVER_NAME_AS_ACE.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = Boolean.toString(SQLServerDriverBooleanProperty.SERVER_NAME_AS_ACE.getDefaultValue());
            this.activeConnectionProperties.setProperty(var3, var4);
         }

         this.serverNameAsACE = this.booleanPropertyOn(var3, var4);
         var3 = SQLServerDriverStringProperty.SERVER_NAME.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = "localhost";
         }

         String var102 = SQLServerDriverIntProperty.PORT_NUMBER.toString();
         String var104 = this.activeConnectionProperties.getProperty(var102);
         int var105 = var4.indexOf(92);
         Object var9 = null;
         String var10 = null;
         String var11 = SQLServerDriverStringProperty.INSTANCE_NAME.toString();
         if (var105 >= 0) {
            var10 = var4.substring(var105 + 1, var4.length());
            this.ValidateMaxSQLLoginName(var11, var10);
            var4 = var4.substring(0, var105);
         }

         this.trustedServerNameAE = var4;
         if (this.serverNameAsACE) {
            try {
               var4 = IDN.toASCII(var4);
            } catch (IllegalArgumentException var37) {
               MessageFormat var13 = new MessageFormat(SQLServerException.getErrString("R_InvalidConnectionSetting"));
               Object[] var14 = new Object[]{"serverNameAsACE", var4};
               throw new SQLServerException(var13.format(var14), var37);
            }
         }

         this.activeConnectionProperties.setProperty(var3, var4);
         String var12 = this.activeConnectionProperties.getProperty(var11);
         if (null != var12) {
            var10 = var12;
         }

         if (var10 != null) {
            this.ValidateMaxSQLLoginName(var11, var10);
            this.activeConnectionProperties.setProperty(var11, var10);
            this.trustedServerNameAE = this.trustedServerNameAE + "\\" + var10;
         }

         if (null != var104) {
            this.trustedServerNameAE = this.trustedServerNameAE + ":" + var104;
         }

         var3 = SQLServerDriverStringProperty.APPLICATION_NAME.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 != null) {
            this.ValidateMaxSQLLoginName(var3, var4);
         } else {
            this.activeConnectionProperties.setProperty(var3, "Microsoft JDBC Driver for SQL Server");
         }

         var3 = SQLServerDriverBooleanProperty.LAST_UPDATE_COUNT.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = Boolean.toString(SQLServerDriverBooleanProperty.LAST_UPDATE_COUNT.getDefaultValue());
            this.activeConnectionProperties.setProperty(var3, var4);
         }

         var3 = SQLServerDriverStringProperty.COLUMN_ENCRYPTION.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (null == var4) {
            var4 = SQLServerDriverStringProperty.COLUMN_ENCRYPTION.getDefaultValue();
            this.activeConnectionProperties.setProperty(var3, var4);
         }

         this.columnEncryptionSetting = ColumnEncryptionSetting.valueOfString(var4).toString();
         var3 = SQLServerDriverStringProperty.KEY_STORE_AUTHENTICATION.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (null != var4) {
            this.keyStoreAuthentication = KeyStoreAuthentication.valueOfString(var4).toString();
         }

         var3 = SQLServerDriverStringProperty.KEY_STORE_SECRET.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (null != var4) {
            this.keyStoreSecret = var4;
         }

         var3 = SQLServerDriverStringProperty.KEY_STORE_LOCATION.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (null != var4) {
            this.keyStoreLocation = var4;
         }

         this.registerKeyStoreProviderOnConnection(this.keyStoreAuthentication, this.keyStoreSecret, this.keyStoreLocation);
         var3 = SQLServerDriverBooleanProperty.MULTI_SUBNET_FAILOVER.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = Boolean.toString(SQLServerDriverBooleanProperty.MULTI_SUBNET_FAILOVER.getDefaultValue());
            this.activeConnectionProperties.setProperty(var3, var4);
         }

         this.multiSubnetFailover = this.booleanPropertyOn(var3, var4);
         var3 = SQLServerDriverBooleanProperty.TRANSPARENT_NETWORK_IP_RESOLUTION.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = Boolean.toString(SQLServerDriverBooleanProperty.TRANSPARENT_NETWORK_IP_RESOLUTION.getDefaultValue());
            this.activeConnectionProperties.setProperty(var3, var4);
         }

         this.transparentNetworkIPResolution = this.booleanPropertyOn(var3, var4);
         var3 = SQLServerDriverBooleanProperty.ENCRYPT.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = Boolean.toString(SQLServerDriverBooleanProperty.ENCRYPT.getDefaultValue());
            this.activeConnectionProperties.setProperty(var3, var4);
         }

         this.requestedEncryptionLevel = (byte)(this.booleanPropertyOn(var3, var4) ? 1 : 0);
         var3 = SQLServerDriverBooleanProperty.TRUST_SERVER_CERTIFICATE.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = Boolean.toString(SQLServerDriverBooleanProperty.TRUST_SERVER_CERTIFICATE.getDefaultValue());
            this.activeConnectionProperties.setProperty(var3, var4);
         }

         this.trustServerCertificate = this.booleanPropertyOn(var3, var4);
         var3 = SQLServerDriverStringProperty.SELECT_METHOD.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = SQLServerDriverStringProperty.SELECT_METHOD.getDefaultValue();
         }

         if (!var4.equalsIgnoreCase("cursor") && !var4.equalsIgnoreCase("direct")) {
            MessageFormat var106 = new MessageFormat(SQLServerException.getErrString("R_invalidselectMethod"));
            Object[] var110 = new Object[]{new String(var4)};
            SQLServerException.makeFromDriverError(this, this, var106.format(var110), null, false);
         } else {
            this.activeConnectionProperties.setProperty(var3, var4.toLowerCase());
         }

         var3 = SQLServerDriverStringProperty.RESPONSE_BUFFERING.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = SQLServerDriverStringProperty.RESPONSE_BUFFERING.getDefaultValue();
         }

         if (!var4.equalsIgnoreCase("full") && !var4.equalsIgnoreCase("adaptive")) {
            MessageFormat var107 = new MessageFormat(SQLServerException.getErrString("R_invalidresponseBuffering"));
            Object[] var111 = new Object[]{new String(var4)};
            SQLServerException.makeFromDriverError(this, this, var107.format(var111), null, false);
         } else {
            this.activeConnectionProperties.setProperty(var3, var4.toLowerCase());
         }

         var3 = SQLServerDriverStringProperty.APPLICATION_INTENT.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = SQLServerDriverStringProperty.APPLICATION_INTENT.getDefaultValue();
         }

         this.applicationIntent = ApplicationIntent.valueOfString(var4);
         this.activeConnectionProperties.setProperty(var3, this.applicationIntent.toString());
         var3 = SQLServerDriverBooleanProperty.SEND_TIME_AS_DATETIME.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = Boolean.toString(SQLServerDriverBooleanProperty.SEND_TIME_AS_DATETIME.getDefaultValue());
            this.activeConnectionProperties.setProperty(var3, var4);
         }

         this.sendTimeAsDatetime = this.booleanPropertyOn(var3, var4);
         var3 = SQLServerDriverBooleanProperty.DISABLE_STATEMENT_POOLING.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 != null && !this.booleanPropertyOn(var3, var4)) {
            MessageFormat var108 = new MessageFormat(SQLServerException.getErrString("R_invaliddisableStatementPooling"));
            Object[] var112 = new Object[]{new String(var4)};
            SQLServerException.makeFromDriverError(this, this, var108.format(var112), null, false);
         }

         var3 = SQLServerDriverBooleanProperty.INTEGRATED_SECURITY.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 != null) {
            this.integratedSecurity = this.booleanPropertyOn(var3, var4);
         }

         if (this.integratedSecurity) {
            var3 = SQLServerDriverStringProperty.AUTHENTICATION_SCHEME.toString();
            var4 = this.activeConnectionProperties.getProperty(var3);
            if (var4 != null) {
               this.intAuthScheme = AuthenticationScheme.valueOfString(var4);
            }
         }

         var3 = SQLServerDriverStringProperty.AUTHENTICATION.toString();
         var4 = this.activeConnectionProperties.getProperty(var3);
         if (var4 == null) {
            var4 = SQLServerDriverStringProperty.AUTHENTICATION.getDefaultValue();
         }

         this.authenticationString = SqlAuthentication.valueOfString(var4).toString();
         if (this.integratedSecurity && !this.authenticationString.equalsIgnoreCase(SqlAuthentication.NotSpecified.toString())) {
            connectionlogger.severe(this.toString() + " " + SQLServerException.getErrString("R_SetAuthenticationWhenIntegratedSecurityTrue"));
            throw new SQLServerException(SQLServerException.getErrString("R_SetAuthenticationWhenIntegratedSecurityTrue"), null);
         }

         if (this.authenticationString.equalsIgnoreCase(SqlAuthentication.ActiveDirectoryIntegrated.toString())
            && (
               !this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.USER.toString()).isEmpty()
                  || !this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.PASSWORD.toString()).isEmpty()
            )) {
            connectionlogger.severe(this.toString() + " " + SQLServerException.getErrString("R_IntegratedAuthenticationWithUserPassword"));
            throw new SQLServerException(SQLServerException.getErrString("R_IntegratedAuthenticationWithUserPassword"), null);
         }

         if (this.authenticationString.equalsIgnoreCase(SqlAuthentication.ActiveDirectoryPassword.toString())
            && (
               this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.USER.toString()).isEmpty()
                  || this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.PASSWORD.toString()).isEmpty()
            )) {
            connectionlogger.severe(this.toString() + " " + SQLServerException.getErrString("R_NoUserPasswordForActivePassword"));
            throw new SQLServerException(SQLServerException.getErrString("R_NoUserPasswordForActivePassword"), null);
         }

         if (!this.authenticationString.equalsIgnoreCase(SqlAuthentication.SqlPassword.toString())
            || !this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.USER.toString()).isEmpty()
               && !this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.PASSWORD.toString()).isEmpty()) {
            var3 = SQLServerDriverStringProperty.ACCESS_TOKEN.toString();
            var4 = this.activeConnectionProperties.getProperty(var3);
            if (null != var4) {
               try {
                  this.accessTokenInByte = var4.getBytes("UTF-16LE");
               } catch (UnsupportedEncodingException var36) {
                  MessageFormat var113 = new MessageFormat(SQLServerException.getErrString("R_unsupportedEncoding"));
                  Object[] var15 = new Object[]{"UTF-16LE"};
                  throw new SQLServerException(this, var113.format(var15), null, 0, false);
               }
            }

            if (null != this.accessTokenInByte && 0 == this.accessTokenInByte.length) {
               connectionlogger.severe(this.toString() + " " + SQLServerException.getErrString("R_AccessTokenCannotBeEmpty"));
               throw new SQLServerException(SQLServerException.getErrString("R_AccessTokenCannotBeEmpty"), null);
            }

            if (this.integratedSecurity && null != this.accessTokenInByte) {
               connectionlogger.severe(this.toString() + " " + SQLServerException.getErrString("R_SetAccesstokenWhenIntegratedSecurityTrue"));
               throw new SQLServerException(SQLServerException.getErrString("R_SetAccesstokenWhenIntegratedSecurityTrue"), null);
            }

            if (!this.authenticationString.equalsIgnoreCase(SqlAuthentication.NotSpecified.toString()) && null != this.accessTokenInByte) {
               connectionlogger.severe(this.toString() + " " + SQLServerException.getErrString("R_SetBothAuthenticationAndAccessToken"));
               throw new SQLServerException(SQLServerException.getErrString("R_SetBothAuthenticationAndAccessToken"), null);
            }

            if (null != this.accessTokenInByte
               && (
                  !this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.USER.toString()).isEmpty()
                     || !this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.PASSWORD.toString()).isEmpty()
               )) {
               connectionlogger.severe(this.toString() + " " + SQLServerException.getErrString("R_AccessTokenWithUserPassword"));
               throw new SQLServerException(SQLServerException.getErrString("R_AccessTokenWithUserPassword"), null);
            }

            if (System.getProperty("os.name").toLowerCase().startsWith("windows")
               || null == this.accessTokenInByte && this.authenticationString.equalsIgnoreCase(SqlAuthentication.NotSpecified.toString())) {
               var3 = SQLServerDriverStringProperty.WORKSTATION_ID.toString();
               var4 = this.activeConnectionProperties.getProperty(var3);
               this.ValidateMaxSQLLoginName(var3, var4);
               int var109 = 0;
               var3 = SQLServerDriverIntProperty.PORT_NUMBER.toString();

               try {
                  String var114 = this.activeConnectionProperties.getProperty(var3);
                  if (null != var114) {
                     var109 = new Integer(var114);
                     if (var109 < 0 || var109 > 65535) {
                        MessageFormat var118 = new MessageFormat(SQLServerException.getErrString("R_invalidPortNumber"));
                        Object[] var122 = new Object[]{Integer.toString(var109)};
                        SQLServerException.makeFromDriverError(this, this, var118.format(var122), null, false);
                     }
                  }
               } catch (NumberFormatException var39) {
                  MessageFormat var117 = new MessageFormat(SQLServerException.getErrString("R_invalidPortNumber"));
                  Object[] var16 = new Object[]{this.activeConnectionProperties.getProperty(var3)};
                  SQLServerException.makeFromDriverError(this, this, var117.format(var16), null, false);
               }

               var3 = SQLServerDriverIntProperty.PACKET_SIZE.toString();
               var4 = this.activeConnectionProperties.getProperty(var3);
               if (null != var4 && var4.length() > 0) {
                  try {
                     this.requestedPacketSize = Integer.parseInt(var4);
                     if (-1 == this.requestedPacketSize) {
                        this.requestedPacketSize = 0;
                     } else if (0 == this.requestedPacketSize) {
                        this.requestedPacketSize = 32767;
                     }
                  } catch (NumberFormatException var35) {
                     this.requestedPacketSize = -1;
                  }

                  if (0 != this.requestedPacketSize && (this.requestedPacketSize < 512 || this.requestedPacketSize > 32767)) {
                     MessageFormat var115 = new MessageFormat(SQLServerException.getErrString("R_invalidPacketSize"));
                     Object[] var119 = new Object[]{var4};
                     SQLServerException.makeFromDriverError(this, this, var115.format(var119), null, false);
                  }
               }

               var3 = SQLServerDriverBooleanProperty.SEND_STRING_PARAMETERS_AS_UNICODE.toString();
               if (null == this.activeConnectionProperties.getProperty(var3)) {
                  this.sendStringParametersAsUnicode = SQLServerDriverBooleanProperty.SEND_STRING_PARAMETERS_AS_UNICODE.getDefaultValue();
               } else {
                  this.sendStringParametersAsUnicode = this.booleanPropertyOn(var3, this.activeConnectionProperties.getProperty(var3));
               }

               var3 = SQLServerDriverBooleanProperty.LAST_UPDATE_COUNT.toString();
               this.lastUpdateCount = this.booleanPropertyOn(var3, this.activeConnectionProperties.getProperty(var3));
               var3 = SQLServerDriverBooleanProperty.XOPEN_STATES.toString();
               this.xopenStates = this.booleanPropertyOn(var3, this.activeConnectionProperties.getProperty(var3));
               var3 = SQLServerDriverStringProperty.SELECT_METHOD.toString();
               this.selectMethod = null;
               if (this.activeConnectionProperties.getProperty(var3) != null && this.activeConnectionProperties.getProperty(var3).length() > 0) {
                  this.selectMethod = this.activeConnectionProperties.getProperty(var3);
               }

               var3 = SQLServerDriverStringProperty.RESPONSE_BUFFERING.toString();
               this.responseBuffering = null;
               if (this.activeConnectionProperties.getProperty(var3) != null && this.activeConnectionProperties.getProperty(var3).length() > 0) {
                  this.responseBuffering = this.activeConnectionProperties.getProperty(var3);
               }

               var3 = SQLServerDriverIntProperty.LOCK_TIMEOUT.toString();
               int var116 = SQLServerDriverIntProperty.LOCK_TIMEOUT.getDefaultValue();
               this.nLockTimeout = var116;
               if (this.activeConnectionProperties.getProperty(var3) != null && this.activeConnectionProperties.getProperty(var3).length() > 0) {
                  try {
                     int var120 = new Integer(this.activeConnectionProperties.getProperty(var3));
                     if (var120 >= var116) {
                        this.nLockTimeout = var120;
                     } else {
                        MessageFormat var124 = new MessageFormat(SQLServerException.getErrString("R_invalidLockTimeOut"));
                        Object[] var126 = new Object[]{this.activeConnectionProperties.getProperty(var3)};
                        SQLServerException.makeFromDriverError(this, this, var124.format(var126), null, false);
                     }
                  } catch (NumberFormatException var34) {
                     MessageFormat var123 = new MessageFormat(SQLServerException.getErrString("R_invalidLockTimeOut"));
                     Object[] var17 = new Object[]{this.activeConnectionProperties.getProperty(var3)};
                     SQLServerException.makeFromDriverError(this, this, var123.format(var17), null, false);
                  }
               }

               FailoverInfo var121 = null;
               String var125 = SQLServerDriverStringProperty.DATABASE_NAME.toString();
               String var127 = SQLServerDriverStringProperty.SERVER_NAME.toString();
               String var18 = SQLServerDriverStringProperty.FAILOVER_PARTNER.toString();
               String var19 = this.activeConnectionProperties.getProperty(var18);
               if (this.multiSubnetFailover && var19 != null) {
                  SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_dbMirroringWithMultiSubnetFailover"), null, false);
               }

               if (this.multiSubnetFailover || null != var19) {
                  this.transparentNetworkIPResolution = false;
               }

               if (this.applicationIntent != null && this.applicationIntent.equals(ApplicationIntent.READ_ONLY) && var19 != null) {
                  SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_dbMirroringWithReadOnlyIntent"), null, false);
               }

               if (null != this.activeConnectionProperties.getProperty(var125)) {
                  var121 = FailoverMapSingleton.getFailoverInfo(
                     this,
                     this.activeConnectionProperties.getProperty(var127),
                     this.activeConnectionProperties.getProperty(var11),
                     this.activeConnectionProperties.getProperty(var125)
                  );
               } else if (null != var19) {
                  SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_failoverPartnerWithoutDB"), null, true);
               }

               String var20 = null;
               if (null == var121) {
                  var20 = var19;
               }

               long var21 = System.currentTimeMillis();
               this.login(this.activeConnectionProperties.getProperty(var127), var10, var109, var20, var121, var5, var21);
               if (1 == this.negotiatedEncryptionLevel || 3 == this.negotiatedEncryptionLevel) {
                  int var23 = Util.isIBM() ? 8192 : 16384;
                  if (this.tdsPacketSize > var23) {
                     connectionlogger.finer(
                        this.toString()
                           + " Negotiated tdsPacketSize "
                           + this.tdsPacketSize
                           + " is too large for SSL with JRE "
                           + Util.SYSTEM_JRE
                           + " (max size is "
                           + var23
                           + ")"
                     );
                     MessageFormat var24 = new MessageFormat(SQLServerException.getErrString("R_packetSizeTooBigForSSL"));
                     Object[] var25 = new Object[]{Integer.toString(var23)};
                     this.terminate(6, var24.format(var25));
                  }
               }

               this.state = SQLServerConnection.State.Opened;
               if (connectionlogger.isLoggable(Level.FINER)) {
                  connectionlogger.finer(this.toString() + " End of connect");
               }

               return this;
            } else {
               throw new SQLServerException(SQLServerException.getErrString("R_FedAuthOnNonWindows"), null);
            }
         } else {
            connectionlogger.severe(this.toString() + " " + SQLServerException.getErrString("R_NoUserPasswordForSqlPassword"));
            throw new SQLServerException(SQLServerException.getErrString("R_NoUserPasswordForSqlPassword"), null);
         }
      } finally {
         if (!this.state.equals(SQLServerConnection.State.Opened) && !this.state.equals(SQLServerConnection.State.Closed)) {
            this.close();
         }
      }
   }

   private void login(String var1, String var2, int var3, String var4, FailoverInfo var5, int var6, long var7) throws SQLServerException {
      boolean var9 = null != var4 || null != var5;
      byte var10 = 100;
      boolean var13 = false;
      FailoverInfo var14 = null;
      ServerPortPlaceHolder var15 = null;
      ServerPortPlaceHolder var16 = null;
      if (null != var5) {
         var14 = var5;
         var13 = var5.getUseFailoverPartner();
      } else if (var9) {
         var14 = new FailoverInfo(var4, this, false);
      }

      boolean var17 = this.getMultiSubnetFailover();
      boolean var18 = this.getTransparentNetworkIPResolution();
      if (0 == var6) {
         var6 = SQLServerDriverIntProperty.LOGIN_TIMEOUT.getDefaultValue();
      }

      long var21 = var6 * 1000;
      this.timerExpire = var7 + var21;
      long var11;
      if (!var9 && !var17 && !var18) {
         var11 = var21;
      } else {
         var11 = (long)(0.08F * (float)var21);
      }

      long var19 = var7 + var11;
      long var23 = var7 + var21;
      if (connectionlogger.isLoggable(Level.FINER)) {
         connectionlogger.finer(this.toString() + " Start time: " + var7 + " Time out time: " + this.timerExpire + " Timeout Unit Interval: " + var11);
      }

      int var25 = 0;
      int var26 = 0;

      while (true) {
         this.clientConnectionId = null;
         this.state = SQLServerConnection.State.Initialized;

         try {
            if (var9 && var13) {
               if (null == var15) {
                  var15 = var14.failoverPermissionCheck(this, this.integratedSecurity);
               }

               this.currentConnectPlaceHolder = var15;
            } else {
               if (this.routingInfo != null) {
                  var16 = this.routingInfo;
                  this.routingInfo = null;
               } else if (null == var16) {
                  var16 = this.primaryPermissionCheck(var1, var2, var3);
               }

               this.currentConnectPlaceHolder = var16;
            }

            if (connectionlogger.isLoggable(Level.FINE)) {
               connectionlogger.fine(
                  this.toString()
                     + " This attempt server name: "
                     + this.currentConnectPlaceHolder.getServerName()
                     + " port: "
                     + this.currentConnectPlaceHolder.getPortNumber()
                     + " InstanceName: "
                     + this.currentConnectPlaceHolder.getInstanceName()
                     + " useParallel: "
                     + var17
               );
               connectionlogger.fine(this.toString() + " This attempt endtime: " + var19);
               connectionlogger.fine(this.toString() + " This attempt No: " + var25);
            }

            this.connectHelper(this.currentConnectPlaceHolder, TimerRemaining(var19), var6, var17, var18, 0 == var25, TimerRemaining(var23));
            if (!this.isRoutedInCurrentAttempt) {
               break;
            }

            if (var9) {
               String var27 = SQLServerException.getErrString("R_invalidRoutingInfo");
               this.terminate(6, var27);
            }

            if (++var26 > 1) {
               String var32 = SQLServerException.getErrString("R_multipleRedirections");
               this.terminate(6, var32);
            }

            if (this.tdsChannel != null) {
               this.tdsChannel.close();
            }

            this.initResettableValues();
            this.resetNonRoutingEnvchangeValues();
            var25++;
            this.isRoutedInCurrentAttempt = false;
            var17 = false;
            var18 = false;
            var19 = this.timerExpire;
            if (!timerHasExpired(this.timerExpire)) {
               continue;
            }

            MessageFormat var33 = new MessageFormat(SQLServerException.getErrString("R_tcpipConnectionFailed"));
            Object[] var39 = new Object[]{
               this.currentConnectPlaceHolder.getServerName(),
               Integer.toString(this.currentConnectPlaceHolder.getPortNumber()),
               SQLServerException.getErrString("R_timedOutBeforeRouting")
            };
            String var29 = var33.format(var39);
            this.terminate(6, var29);
         } catch (SQLServerException var31) {
            if (18456 == var31.getErrorCode()
               || 18488 == var31.getErrorCode()
               || 4 == var31.getDriverErrorCode()
               || 5 == var31.getDriverErrorCode()
               || 7 == var31.getDriverErrorCode()
               || 6 == var31.getDriverErrorCode()
               || timerHasExpired(this.timerExpire)
               || this.state.equals(SQLServerConnection.State.Connected) && !var9) {
               this.close();
               throw var31;
            }

            if (null != this.tdsChannel) {
               this.tdsChannel.close();
            }

            if (!var9 || 1 == var25 % 2) {
               long var28 = TimerRemaining(this.timerExpire);
               if (var28 <= var10) {
                  throw var31;
               }
            }
         }

         if (!var9 || 1 == var25 % 2) {
            if (connectionlogger.isLoggable(Level.FINE)) {
               connectionlogger.fine(this.toString() + " sleeping milisec: " + var10);
            }

            try {
               Thread.sleep(var10);
            } catch (InterruptedException var30) {
            }

            var10 = (byte)(var10 < 500 ? var10 * 2 : 1000);
         }

         var25++;
         if (var17 || var18) {
            var19 = System.currentTimeMillis() + var11 * (var25 + 1);
         } else if (var9) {
            var19 = System.currentTimeMillis() + var11 * (var25 / 2 + 1);
         } else {
            var19 = this.timerExpire;
         }

         if (var19 > this.timerExpire) {
            var19 = this.timerExpire;
         }

         if (var9) {
            var13 = !var13;
         }
      }

      if (var13 && null == this.failoverPartnerServerProvided) {
         String var34 = this.currentConnectPlaceHolder.getServerName();
         if (null != var15.getInstanceName()) {
            var34 = var34 + "\\";
            var34 = var34 + var15.getInstanceName();
         }

         MessageFormat var40 = new MessageFormat(SQLServerException.getErrString("R_invalidPartnerConfiguration"));
         Object[] var42 = new Object[]{new String(this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.DATABASE_NAME.toString())), var34};
         this.terminate(6, var40.format(var42));
      }

      if (null != this.failoverPartnerServerProvided) {
         if (this.multiSubnetFailover) {
            String var36 = SQLServerException.getErrString("R_dbMirroringWithMultiSubnetFailover");
            this.terminate(6, var36);
         }

         if (this.applicationIntent != null && this.applicationIntent.equals(ApplicationIntent.READ_ONLY)) {
            String var37 = SQLServerException.getErrString("R_dbMirroringWithReadOnlyIntent");
            this.terminate(6, var37);
         }

         if (null == var14) {
            var14 = new FailoverInfo(this.failoverPartnerServerProvided, this, false);
         }

         if (null != var5) {
            var5.failoverAdd(this, var13, this.failoverPartnerServerProvided);
         } else {
            String var38 = SQLServerDriverStringProperty.DATABASE_NAME.toString();
            String var41 = SQLServerDriverStringProperty.INSTANCE_NAME.toString();
            String var43 = SQLServerDriverStringProperty.SERVER_NAME.toString();
            if (connectionlogger.isLoggable(Level.FINE)) {
               connectionlogger.fine(
                  this.toString()
                     + " adding new failover info server: "
                     + this.activeConnectionProperties.getProperty(var43)
                     + " instance: "
                     + this.activeConnectionProperties.getProperty(var41)
                     + " database: "
                     + this.activeConnectionProperties.getProperty(var38)
                     + " server provided failover: "
                     + this.failoverPartnerServerProvided
               );
            }

            var14.failoverAdd(this, var13, this.failoverPartnerServerProvided);
            FailoverMapSingleton.putFailoverInfo(
               this,
               var1,
               this.activeConnectionProperties.getProperty(var41),
               this.activeConnectionProperties.getProperty(var38),
               var14,
               var13,
               this.failoverPartnerServerProvided
            );
         }
      }
   }

   void resetNonRoutingEnvchangeValues() {
      this.tdsPacketSize = 4096;
      this.databaseCollation = null;
      this.rolledBackTransaction = false;
      Arrays.fill(this.getTransactionDescriptor(), (byte)0);
      this.sCatalog = this.originalCatalog;
      this.failoverPartnerServerProvided = null;
   }

   ServerPortPlaceHolder primaryPermissionCheck(String var1, String var2, int var3) throws SQLServerException {
      if (0 == var3) {
         if (null != var2) {
            String var4 = this.getInstancePort(var1, var2);
            if (connectionlogger.isLoggable(Level.FINER)) {
               connectionlogger.fine(this.toString() + " SQL Server port returned by SQL Browser: " + var4);
            }

            try {
               if (null != var4) {
                  var3 = new Integer(var4);
                  if (var3 < 0 || var3 > 65535) {
                     MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_invalidPortNumber"));
                     Object[] var9 = new Object[]{Integer.toString(var3)};
                     SQLServerException.makeFromDriverError(this, this, var5.format(var9), null, false);
                  }
               } else {
                  var3 = DEFAULTPORT;
               }
            } catch (NumberFormatException var8) {
               MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_invalidPortNumber"));
               Object[] var7 = new Object[]{var3};
               SQLServerException.makeFromDriverError(this, this, var6.format(var7), null, false);
            }
         } else {
            var3 = DEFAULTPORT;
         }
      }

      this.activeConnectionProperties.setProperty(SQLServerDriverIntProperty.PORT_NUMBER.toString(), String.valueOf(var3));
      return new ServerPortPlaceHolder(var1, var3, var2, this.integratedSecurity);
   }

   static boolean timerHasExpired(long var0) {
      return System.currentTimeMillis() > var0;
   }

   static int TimerRemaining(long var0) {
      long var2 = System.currentTimeMillis();
      long var4 = var0 - var2;
      if (var4 > 2147483647L) {
         var4 = 2147483647L;
      }

      if (var4 <= 0L) {
         var4 = 1L;
      }

      return (int)var4;
   }

   private void connectHelper(ServerPortPlaceHolder var1, int var2, int var3, boolean var4, boolean var5, boolean var6, int var7) throws SQLServerException {
      if (connectionlogger.isLoggable(Level.FINE)) {
         connectionlogger.fine(
            this.toString()
               + " Connecting with server: "
               + var1.getServerName()
               + " port: "
               + var1.getPortNumber()
               + " Timeout slice: "
               + var2
               + " Timeout Full: "
               + var3
         );
      }

      this.tdsChannel = new TDSChannel(this);
      if (0 == var3) {
         this.tdsChannel.open(var1.getServerName(), var1.getPortNumber(), 0, var4, var5, var6, var7);
      } else {
         this.tdsChannel.open(var1.getServerName(), var1.getPortNumber(), var2, var4, var5, var6, var7);
      }

      this.setState(SQLServerConnection.State.Connected);
      this.clientConnectionId = UUID.randomUUID();
      assert null != this.clientConnectionId;
      this.Prelogin(var1.getServerName(), var1.getPortNumber());
      if (2 != this.negotiatedEncryptionLevel) {
         this.tdsChannel.enableSSL(var1.getServerName(), var1.getPortNumber());
      }

      this.executeCommand(new SQLServerConnection.LogonCommand());
   }

   void Prelogin(String var1, int var2) throws SQLServerException {
      if (!this.authenticationString.trim().equalsIgnoreCase(SqlAuthentication.NotSpecified.toString()) || null != this.accessTokenInByte) {
         this.fedAuthRequiredByUser = true;
      }

      byte var3;
      byte var4;
      if (this.fedAuthRequiredByUser) {
         var3 = 73;
         this.requestedEncryptionLevel = 1;
         var4 = 5;
      } else {
         var3 = 67;
         var4 = 0;
      }

      byte[] var5 = new byte[var3];
      int var6 = 0;
      byte[] var7 = new byte[]{18, 1, 0, var3, 0, 0, 0, 0};
      System.arraycopy(var7, 0, var5, var6, var7.length);
      var6 += var7.length;
      byte[] var8 = new byte[]{0, 0, (byte)(16 + var4), 0, 6, 1, 0, (byte)(22 + var4), 0, 1, 5, 0, (byte)(23 + var4), 0, 36};
      System.arraycopy(var8, 0, var5, var6, var8.length);
      var6 += var8.length;
      if (this.fedAuthRequiredByUser) {
         byte[] var9 = new byte[]{6, 0, 64, 0, 1};
         System.arraycopy(var9, 0, var5, var6, var9.length);
         var6 += var9.length;
      }

      var5[var6] = -1;
      var6++;
      byte[] var35 = new byte[]{
         0,
         0,
         0,
         0,
         0,
         0,
         this.requestedEncryptionLevel,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0
      };
      System.arraycopy(var35, 0, var5, var6, var35.length);
      var6 += var35.length;
      if (this.fedAuthRequiredByUser) {
         var5[var6] = 1;
         var6++;
      }

      byte[] var10 = new byte[4096];
      String var11 = " Prelogin error: host " + var1 + " port " + var2;
      ActivityId var12 = ActivityCorrelator.getNext();
      byte[] var13 = Util.asGuidByteArray(var12.getId());
      byte[] var14 = Util.asGuidByteArray(this.clientConnectionId);
      int var15;
      if (this.fedAuthRequiredByUser) {
         var15 = var5.length - 36 - 1;
      } else {
         var15 = var5.length - 36;
      }

      System.arraycopy(var14, 0, var5, var15, var14.length);
      var15 += var14.length;
      System.arraycopy(var13, 0, var5, var15, var13.length);
      var15 += var13.length;
      long var16 = var12.getSequence();
      Util.writeInt((int)var16, var5, var15);
      var15 += 4;
      if (connectionlogger.isLoggable(Level.FINER)) {
         connectionlogger.finer(this.toString() + " Requesting encryption level:" + TDS.getEncryptionLevel(this.requestedEncryptionLevel));
         connectionlogger.finer(this.toString() + " ActivityId " + var12.toString());
      }

      if (this.tdsChannel.isLoggingPackets()) {
         this.tdsChannel.logPacket(var5, 0, var5.length, this.toString() + " Prelogin request");
      }

      try {
         this.tdsChannel.write(var5, 0, var5.length);
         this.tdsChannel.flush();
      } catch (SQLServerException var29) {
         connectionlogger.warning(this.toString() + var11 + " Error sending prelogin request: " + var29.getMessage());
         throw var29;
      }

      ActivityCorrelator.setCurrentActivityIdSentFlag();
      int var18 = var10.length;
      int var19 = 0;
      boolean var20 = false;

      while (var19 < var18) {
         int var21;
         try {
            var21 = this.tdsChannel.read(var10, var19, var18 - var19);
         } catch (SQLServerException var28) {
            connectionlogger.warning(this.toString() + var11 + " Error reading prelogin response: " + var28.getMessage());
            throw var28;
         }

         if (-1 == var21) {
            connectionlogger.warning(this.toString() + var11 + " Unexpected end of prelogin response after " + var19 + " bytes read");
            MessageFormat var22 = new MessageFormat(SQLServerException.getErrString("R_tcpipConnectionFailed"));
            Object[] var23 = new Object[]{var1, Integer.toString(var2), SQLServerException.getErrString("R_notSQLServer")};
            this.terminate(3, var22.format(var23));
         }

         assert var21 >= 0;
         assert var21 <= var18 - var19;
         if (this.tdsChannel.isLoggingPackets()) {
            this.tdsChannel.logPacket(var10, var19, var21, this.toString() + " Prelogin response");
         }

         var19 += var21;
         if (!var20 && var19 >= 8) {
            if (4 != var10[0]) {
               connectionlogger.warning(this.toString() + var11 + " Unexpected response type:" + var10[0]);
               MessageFormat var40 = new MessageFormat(SQLServerException.getErrString("R_tcpipConnectionFailed"));
               Object[] var46 = new Object[]{var1, Integer.toString(var2), SQLServerException.getErrString("R_notSQLServer")};
               this.terminate(3, var40.format(var46));
            }

            if (1 != (1 & var10[1])) {
               connectionlogger.warning(this.toString() + var11 + " Unexpected response status:" + var10[1]);
               MessageFormat var41 = new MessageFormat(SQLServerException.getErrString("R_tcpipConnectionFailed"));
               Object[] var47 = new Object[]{var1, Integer.toString(var2), SQLServerException.getErrString("R_notSQLServer")};
               this.terminate(3, var41.format(var47));
            }

            var18 = Util.readUnsignedShortBigEndian(var10, 2);
            assert var18 >= 0;
            if (var18 >= var10.length) {
               connectionlogger.warning(this.toString() + var11 + " Response length:" + var18 + " is greater than allowed length:" + var10.length);
               MessageFormat var42 = new MessageFormat(SQLServerException.getErrString("R_tcpipConnectionFailed"));
               Object[] var48 = new Object[]{var1, Integer.toString(var2), SQLServerException.getErrString("R_notSQLServer")};
               this.terminate(3, var42.format(var48));
            }

            var20 = true;
         }
      }

      boolean var39 = false;
      this.negotiatedEncryptionLevel = -1;
      int var43 = 8;

      while (true) {
         if (var43 >= var18) {
            connectionlogger.warning(this.toString() + " Option token not found");
            this.throwInvalidTDS();
         }

         byte var49 = var10[var43++];
         if (-1 == var49) {
            if (!var39 || -1 == this.negotiatedEncryptionLevel) {
               connectionlogger.warning(this.toString() + " Prelogin response is missing version and/or encryption option.");
               this.throwInvalidTDS();
            }

            return;
         }

         if (var43 + 4 >= var18) {
            connectionlogger.warning(this.toString() + " Offset/Length not found for option:" + var49);
            this.throwInvalidTDS();
         }

         int var24 = Util.readUnsignedShortBigEndian(var10, var43) + 8;
         var43 += 2;
         assert var24 >= 0;
         int var25 = Util.readUnsignedShortBigEndian(var10, var43);
         var43 += 2;
         assert var25 >= 0;
         if (var24 + var25 > var18) {
            connectionlogger.warning(this.toString() + " Offset:" + var24 + " and length:" + var25 + " exceed response length:" + var18);
            this.throwInvalidTDS();
         }

         switch (var49) {
            case 0:
               if (var39) {
                  connectionlogger.warning(this.toString() + " Version option already received");
                  this.throwInvalidTDS();
               }

               if (6 != var25) {
                  connectionlogger.warning(this.toString() + " Version option length:" + var25 + " is incorrect.  Correct value is 6.");
                  this.throwInvalidTDS();
               }

               this.serverMajorVersion = var10[var24];
               if (this.serverMajorVersion < 9) {
                  connectionlogger.warning(this.toString() + " Server major version:" + this.serverMajorVersion + " is not supported by this driver.");
                  MessageFormat var50 = new MessageFormat(SQLServerException.getErrString("R_unsupportedServerVersion"));
                  Object[] var27 = new Object[]{Integer.toString(var10[var24])};
                  this.terminate(6, var50.format(var27));
               }

               if (connectionlogger.isLoggable(Level.FINE)) {
                  connectionlogger.fine(this.toString() + " Server returned major version:" + var10[var24]);
               }

               var39 = true;
               break;
            case 1:
               if (-1 != this.negotiatedEncryptionLevel) {
                  connectionlogger.warning(this.toString() + " Encryption option already received");
                  this.throwInvalidTDS();
               }

               if (1 != var25) {
                  connectionlogger.warning(this.toString() + " Encryption option length:" + var25 + " is incorrect.  Correct value is 1.");
                  this.throwInvalidTDS();
               }

               this.negotiatedEncryptionLevel = var10[var24];
               if (0 != this.negotiatedEncryptionLevel
                  && 1 != this.negotiatedEncryptionLevel
                  && 3 != this.negotiatedEncryptionLevel
                  && 2 != this.negotiatedEncryptionLevel) {
                  connectionlogger.warning(this.toString() + " Server returned " + TDS.getEncryptionLevel(this.negotiatedEncryptionLevel));
                  this.throwInvalidTDS();
               }

               if (connectionlogger.isLoggable(Level.FINER)) {
                  connectionlogger.finer(this.toString() + " Negotiated encryption level:" + TDS.getEncryptionLevel(this.negotiatedEncryptionLevel));
               }

               if (1 == this.requestedEncryptionLevel && 1 != this.negotiatedEncryptionLevel && 3 != this.negotiatedEncryptionLevel) {
                  this.terminate(5, SQLServerException.getErrString("R_sslRequiredNoServerSupport"));
               }

               if (2 == this.requestedEncryptionLevel && 2 != this.negotiatedEncryptionLevel) {
                  if (3 == this.negotiatedEncryptionLevel) {
                     this.terminate(5, SQLServerException.getErrString("R_sslRequiredByServer"));
                  }

                  connectionlogger.warning(
                     this.toString()
                        + " Client requested encryption level: "
                        + TDS.getEncryptionLevel(this.requestedEncryptionLevel)
                        + " Server returned unexpected encryption level: "
                        + TDS.getEncryptionLevel(this.negotiatedEncryptionLevel)
                  );
                  this.throwInvalidTDS();
               }
               break;
            case 6:
               if (0 != var10[var24] && 1 != var10[var24]) {
                  connectionlogger.severe(this.toString() + " Server sent an unexpected value for FedAuthRequired PreLogin Option. Value was " + var10[var24]);
                  MessageFormat var26 = new MessageFormat(SQLServerException.getErrString("R_FedAuthRequiredPreLoginResponseInvalidValue"));
                  throw new SQLServerException(var26.format(new Object[]{var10[var24]}), null);
               }

               if (null != this.authenticationString && !this.authenticationString.equalsIgnoreCase(SqlAuthentication.NotSpecified.toString())
                  || null != this.accessTokenInByte) {
                  this.fedAuthRequiredPreLoginResponse = var10[var24] == 1;
               }
               break;
            default:
               if (connectionlogger.isLoggable(Level.FINER)) {
                  connectionlogger.finer(this.toString() + " Ignoring prelogin response option:" + var49);
               }
         }
      }
   }

   final void throwInvalidTDS() throws SQLServerException {
      this.terminate(4, SQLServerException.getErrString("R_invalidTDS"));
   }

   final void throwInvalidTDSToken(String var1) throws SQLServerException {
      MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_unexpectedToken"));
      Object[] var3 = new Object[]{var1};
      String var4 = SQLServerException.getErrString("R_invalidTDS") + var2.format(var3);
      this.terminate(4, var4);
   }

   final void terminate(int var1, String var2) throws SQLServerException {
      this.terminate(var1, var2, null);
   }

   final void terminate(int var1, String var2, Throwable var3) throws SQLServerException {
      String var4 = this.state.equals(SQLServerConnection.State.Opened) ? "08006" : "08001";
      if (!this.xopenStates) {
         var4 = SQLServerException.mapFromXopen(var4);
      }

      SQLServerException var5 = new SQLServerException(this, SQLServerException.checkAndAppendClientConnId(var2, this), var4, 0, true);
      if (null != var3) {
         var5.initCause(var3);
      }

      var5.setDriverErrorCode(var1);
      this.notifyPooledConnection(var5);
      this.close();
      throw var5;
   }

   boolean executeCommand(TDSCommand var1) throws SQLServerException {
      synchronized (this.schedulerLock) {
         if (null != this.currentCommand) {
            this.currentCommand.detach();
            this.currentCommand = null;
         }

         boolean var3 = false;

         try {
            var3 = var1.execute(this.tdsChannel.getWriter(), this.tdsChannel.getReader(var1));
         } finally {
            if (!var3 && !this.isSessionUnAvailable()) {
               this.currentCommand = var1;
            }
         }

         return var3;
      }
   }

   void resetCurrentCommand() throws SQLServerException {
      if (null != this.currentCommand) {
         this.currentCommand.detach();
         this.currentCommand = null;
      }
   }

   private final void connectionCommand(String var1, String var2) throws SQLServerException {
      final class ConnectionCommand extends UninterruptableTDSCommand {
         final String sql;

         ConnectionCommand(String var2, String var3) {
            super(var3);
            this.sql = var2;
         }

         final boolean doExecute() throws SQLServerException {
            this.startRequest((byte)1).writeString(this.sql);
            TDSParser.parse(this.startResponse(), this.getLogContext());
            return true;
         }
      }

      this.executeCommand(new ConnectionCommand(var1, var2));
   }

   private String sqlStatementToInitialize() {
      String var1 = null;
      if (this.nLockTimeout > -1) {
         var1 = " set lock_timeout " + this.nLockTimeout;
      }

      return var1;
   }

   void setCatalogName(String var1) {
      if (var1 != null && var1.length() > 0) {
         this.sCatalog = var1;
      }
   }

   String sqlStatementToSetTransactionIsolationLevel() throws SQLServerException {
      String var1 = "set transaction isolation level ";
      switch (this.transactionIsolationLevel) {
         case 1:
            var1 = var1 + " read uncommitted ";
            break;
         case 2:
            var1 = var1 + " read committed ";
            break;
         case 4:
            var1 = var1 + " repeatable read ";
            break;
         case 8:
            var1 = var1 + " serializable ";
            break;
         case 4096:
            var1 = var1 + " snapshot ";
            break;
         default:
            MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidTransactionLevel"));
            Object[] var3 = new Object[]{Integer.toString(this.transactionIsolationLevel)};
            SQLServerException.makeFromDriverError(this, this, var2.format(var3), null, false);
      }

      return var1;
   }

   static String sqlStatementToSetCommit(boolean var0) {
      return var0 ? "set implicit_transactions off " : "set implicit_transactions on ";
   }

   public Statement createStatement() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "createStatement");
      Statement var1 = this.createStatement(1003, 1007);
      loggerExternal.exiting(this.getClassNameLogging(), "createStatement", var1);
      return var1;
   }

   public PreparedStatement prepareStatement(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", var1);
      PreparedStatement var2 = this.prepareStatement(var1, 1003, 1007);
      loggerExternal.exiting(this.getClassNameLogging(), "prepareStatement", var2);
      return var2;
   }

   public CallableStatement prepareCall(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareCall", var1);
      CallableStatement var2 = this.prepareCall(var1, 1003, 1007);
      loggerExternal.exiting(this.getClassNameLogging(), "prepareCall", var2);
      return var2;
   }

   public String nativeSQL(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "nativeSQL", var1);
      this.checkClosed();
      loggerExternal.exiting(this.getClassNameLogging(), "nativeSQL", var1);
      return var1;
   }

   public void setAutoCommit(boolean var1) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setAutoCommit", var1);
         if (Util.IsActivityTraceOn()) {
            loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
         }
      }

      String var2 = "";
      this.checkClosed();
      if (var1 != this.databaseAutoCommitMode) {
         if (var1) {
            var2 = "IF @@TRANCOUNT > 0 COMMIT TRAN ";
         }

         if (connectionlogger.isLoggable(Level.FINER)) {
            connectionlogger.finer(this.toString() + " Autocommitmode current :" + this.databaseAutoCommitMode + " new: " + var1);
         }

         this.rolledBackTransaction = false;
         this.connectionCommand(var2 + sqlStatementToSetCommit(var1), "setAutoCommit");
         this.databaseAutoCommitMode = var1;
         loggerExternal.exiting(this.getClassNameLogging(), "setAutoCommit");
      }
   }

   public boolean getAutoCommit() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getAutoCommit");
      this.checkClosed();
      boolean var1 = !this.inXATransaction && this.databaseAutoCommitMode;
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.exiting(this.getClassNameLogging(), "getAutoCommit", var1);
      }

      return var1;
   }

   final byte[] getTransactionDescriptor() {
      return this.transactionDescriptor;
   }

   public void commit() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "commit");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      if (!this.databaseAutoCommitMode) {
         this.connectionCommand("IF @@TRANCOUNT > 0 COMMIT TRAN", "Connection.commit");
      }

      loggerExternal.exiting(this.getClassNameLogging(), "commit");
   }

   public void rollback() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "rollback");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      if (this.databaseAutoCommitMode) {
         SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_cantInvokeRollback"), null, true);
      } else {
         this.connectionCommand("IF @@TRANCOUNT > 0 ROLLBACK TRAN", "Connection.rollback");
      }

      loggerExternal.exiting(this.getClassNameLogging(), "rollback");
   }

   public void abort(Executor var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "abort", var1);
      DriverJDBCVersion.checkSupportsJDBC41();
      if (!this.isClosed()) {
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
               SQLServerException.makeFromDriverError(this, this, var4.format(var5), null, true);
            }
         }

         this.setState(SQLServerConnection.State.Closed);
         var1.execute(new Runnable() {
            public void run() {
               if (null != SQLServerConnection.this.tdsChannel) {
                  SQLServerConnection.this.tdsChannel.close();
               }
            }
         });
         loggerExternal.exiting(this.getClassNameLogging(), "abort");
      }
   }

   public void close() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "close");
      this.setState(SQLServerConnection.State.Closed);
      if (null != this.tdsChannel) {
         this.tdsChannel.close();
      }

      loggerExternal.exiting(this.getClassNameLogging(), "close");
   }

   final void poolCloseEventNotify() throws SQLServerException {
      if (this.state.equals(SQLServerConnection.State.Opened) && null != this.pooledConnectionParent) {
         if (!this.databaseAutoCommitMode && !(this.pooledConnectionParent instanceof XAConnection)) {
            this.connectionCommand("IF @@TRANCOUNT > 0 ROLLBACK TRAN", "close connection");
         }

         this.notifyPooledConnection(null);
         if (connectionlogger.isLoggable(Level.FINER)) {
            connectionlogger.finer(this.toString() + " Connection closed and returned to connection pool");
         }
      }
   }

   public boolean isClosed() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "isClosed");
      loggerExternal.exiting(this.getClassNameLogging(), "isClosed", this.isSessionUnAvailable());
      return this.isSessionUnAvailable();
   }

   public DatabaseMetaData getMetaData() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getMetaData");
      this.checkClosed();
      if (this.databaseMetaData == null) {
         this.databaseMetaData = new SQLServerDatabaseMetaData(this);
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getMetaData", this.databaseMetaData);
      return this.databaseMetaData;
   }

   public void setReadOnly(boolean var1) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setReadOnly", var1);
      }

      this.checkClosed();
      loggerExternal.exiting(this.getClassNameLogging(), "setReadOnly");
   }

   public boolean isReadOnly() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "isReadOnly");
      this.checkClosed();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.exiting(this.getClassNameLogging(), "isReadOnly", false);
      }

      return false;
   }

   public void setCatalog(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "setCatalog", var1);
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      if (var1 != null) {
         this.connectionCommand("use " + Util.escapeSQLId(var1), "setCatalog");
         this.sCatalog = var1;
      }

      loggerExternal.exiting(this.getClassNameLogging(), "setCatalog");
   }

   public String getCatalog() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getCatalog");
      this.checkClosed();
      loggerExternal.exiting(this.getClassNameLogging(), "getCatalog", this.sCatalog);
      return this.sCatalog;
   }

   public void setTransactionIsolation(int var1) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "setTransactionIsolation", new Integer(var1));
         if (Util.IsActivityTraceOn()) {
            loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
         }
      }

      this.checkClosed();
      if (var1 != 0) {
         this.transactionIsolationLevel = var1;
         String var2 = this.sqlStatementToSetTransactionIsolationLevel();
         this.connectionCommand(var2, "setTransactionIsolation");
         loggerExternal.exiting(this.getClassNameLogging(), "setTransactionIsolation");
      }
   }

   public int getTransactionIsolation() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getTransactionIsolation");
      this.checkClosed();
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.exiting(this.getClassNameLogging(), "getTransactionIsolation", new Integer(this.transactionIsolationLevel));
      }

      return this.transactionIsolationLevel;
   }

   public SQLWarning getWarnings() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getWarnings");
      this.checkClosed();
      loggerExternal.exiting(this.getClassNameLogging(), "getWarnings", this.sqlWarnings);
      return this.sqlWarnings;
   }

   private void addWarning(String var1) {
      synchronized (this.warningSynchronization) {
         SQLWarning var3 = new SQLWarning(var1);
         if (null == this.sqlWarnings) {
            this.sqlWarnings = var3;
         } else {
            this.sqlWarnings.setNextWarning(var3);
         }
      }
   }

   public void clearWarnings() throws SQLServerException {
      synchronized (this.warningSynchronization) {
         loggerExternal.entering(this.getClassNameLogging(), "clearWarnings");
         this.checkClosed();
         this.sqlWarnings = null;
         loggerExternal.exiting(this.getClassNameLogging(), "clearWarnings");
      }
   }

   public Statement createStatement(int var1, int var2) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "createStatement", new Object[]{new Integer(var1), new Integer(var2)});
      }

      this.checkClosed();
      SQLServerStatement var3 = new SQLServerStatement(this, var1, var2, SQLServerStatementColumnEncryptionSetting.UseConnectionSetting);
      loggerExternal.exiting(this.getClassNameLogging(), "createStatement", var3);
      return var3;
   }

   public PreparedStatement prepareStatement(String var1, int var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{var1, new Integer(var2), new Integer(var3)});
      }

      this.checkClosed();
      SQLServerPreparedStatement var4 = new SQLServerPreparedStatement(this, var1, var2, var3, SQLServerStatementColumnEncryptionSetting.UseConnectionSetting);
      loggerExternal.exiting(this.getClassNameLogging(), "prepareStatement", var4);
      return var4;
   }

   private PreparedStatement prepareStatement(String var1, int var2, int var3, SQLServerStatementColumnEncryptionSetting var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{var1, new Integer(var2), new Integer(var3), var4});
      }

      this.checkClosed();
      SQLServerPreparedStatement var5 = new SQLServerPreparedStatement(this, var1, var2, var3, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "prepareStatement", var5);
      return var5;
   }

   public CallableStatement prepareCall(String var1, int var2, int var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "prepareCall", new Object[]{var1, new Integer(var2), new Integer(var3)});
      }

      this.checkClosed();
      SQLServerCallableStatement var4 = new SQLServerCallableStatement(this, var1, var2, var3, SQLServerStatementColumnEncryptionSetting.UseConnectionSetting);
      loggerExternal.exiting(this.getClassNameLogging(), "prepareCall", var4);
      return var4;
   }

   public void setTypeMap(Map<String, Class<?>> var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "setTypeMap", var1);
      this.checkClosed();
      if (var1 != null && var1 instanceof HashMap && var1.isEmpty()) {
         loggerExternal.exiting(this.getClassNameLogging(), "setTypeMap");
      } else {
         this.NotImplemented();
      }
   }

   public Map<String, Class<?>> getTypeMap() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getTypeMap");
      this.checkClosed();
      HashMap var1 = new HashMap();
      loggerExternal.exiting(this.getClassNameLogging(), "getTypeMap", var1);
      return var1;
   }

   int writeAEFeatureRequest(boolean var1, TDSWriter var2) throws SQLServerException {
      byte var3 = 6;
      if (var1) {
         var2.writeByte((byte)4);
         var2.writeInt(1);
         var2.writeByte((byte)1);
      }

      return var3;
   }

   int writeFedAuthFeatureRequest(boolean var1, TDSWriter var2, SQLServerConnection.FederatedAuthenticationFeatureExtensionData var3) throws SQLServerException {
      assert var3.libraryType == 2 || var3.libraryType == 1;
      int var4 = 0;
      int var5 = 0;
      switch (var3.libraryType) {
         case 1:
            assert null != var3.accessToken;
            var4 = 5 + var3.accessToken.length;
            break;
         case 2:
            var4 = 2;
            break;
         default:
            assert false;
      }

      var5 = var4 + 5;
      if (var1) {
         var2.writeByte((byte)2);
         byte var6 = 0;
         switch (var3.libraryType) {
            case 1:
               assert this.federatedAuthenticationRequested;
               var6 = (byte)(var6 | 2);
               break;
            case 2:
               assert this.federatedAuthenticationInfoRequested;
               var6 = (byte)(var6 | 4);
               break;
            default:
               assert false;
         }

         var6 = (byte)(var6 | (byte)(var3.fedAuthRequiredPreLoginResponse ? 1 : 0));
         var2.writeInt(var4);
         var2.writeByte(var6);
         switch (var3.libraryType) {
            case 1:
               var2.writeInt(var3.accessToken.length);
               var2.writeBytes(var3.accessToken, 0, var3.accessToken.length);
               break;
            case 2:
               byte var7 = 0;
               switch (var3.authentication) {
                  case ActiveDirectoryPassword:
                     var7 = 1;
                     break;
                  case ActiveDirectoryIntegrated:
                     var7 = 2;
                     break;
                  default:
                     assert false;
               }

               var2.writeByte(var7);
               break;
            default:
               assert false;
         }
      }

      return var5;
   }

   private final void logon(SQLServerConnection.LogonCommand var1) throws SQLServerException {
      Object var2 = null;
      if (this.integratedSecurity && AuthenticationScheme.nativeAuthentication == this.intAuthScheme) {
         var2 = new AuthenticationJNI(this, this.currentConnectPlaceHolder.getServerName(), this.currentConnectPlaceHolder.getPortNumber());
      }

      if (this.integratedSecurity && AuthenticationScheme.javaKerberos == this.intAuthScheme) {
         var2 = new KerbAuthentication(this, this.currentConnectPlaceHolder.getServerName(), this.currentConnectPlaceHolder.getPortNumber());
      }

      if (this.authenticationString.trim().equalsIgnoreCase(SqlAuthentication.ActiveDirectoryPassword.toString())
         || this.authenticationString.trim().equalsIgnoreCase(SqlAuthentication.ActiveDirectoryIntegrated.toString()) && this.fedAuthRequiredPreLoginResponse) {
         this.federatedAuthenticationInfoRequested = true;
         this.fedAuthFeatureExtensionData = new SQLServerConnection.FederatedAuthenticationFeatureExtensionData(
            2, this.authenticationString, this.fedAuthRequiredPreLoginResponse
         );
      }

      if (null != this.accessTokenInByte) {
         this.fedAuthFeatureExtensionData = new SQLServerConnection.FederatedAuthenticationFeatureExtensionData(
            1, this.fedAuthRequiredPreLoginResponse, this.accessTokenInByte
         );
         this.federatedAuthenticationRequested = true;
      }

      try {
         this.sendLogon(var1, (SSPIAuthentication)var2, this.fedAuthFeatureExtensionData);
         if (!this.isRoutedInCurrentAttempt) {
            this.originalCatalog = this.sCatalog;
            String var3 = this.sqlStatementToInitialize();
            if (var3 != null) {
               this.connectionCommand(var3, "Change Settings");
            }
         }
      } finally {
         if (this.integratedSecurity) {
            if (null != var2) {
               var2.ReleaseClientContext();
            }

            var2 = null;
         }
      }
   }

   final void processEnvChange(TDSReader var1) throws SQLServerException {
      var1.readUnsignedByte();
      int var2 = var1.readUnsignedShort();
      TDSReaderMark var3 = var1.mark();
      int var4 = var1.readUnsignedByte();
      switch (var4) {
         case 1:
            this.setCatalogName(var1.readUnicodeString(var1.readUnsignedByte()));
            break;
         case 2:
         case 3:
         case 5:
         case 6:
         case 15:
         case 16:
         case 17:
         case 18:
         case 19:
            if (connectionlogger.isLoggable(Level.FINER)) {
               connectionlogger.finer(this.toString() + " Ignored env change: " + var4);
            }
            break;
         case 4:
            try {
               this.tdsPacketSize = Integer.parseInt(var1.readUnicodeString(var1.readUnsignedByte()));
            } catch (NumberFormatException var21) {
               var1.throwInvalidTDS();
            }

            if (connectionlogger.isLoggable(Level.FINER)) {
               connectionlogger.finer(this.toString() + " Network packet size is " + this.tdsPacketSize + " bytes");
            }
            break;
         case 7:
            if (SQLCollation.tdsLength() != var1.readUnsignedByte()) {
               var1.throwInvalidTDS();
            }

            try {
               this.databaseCollation = new SQLCollation(var1);
            } catch (UnsupportedEncodingException var20) {
               this.terminate(4, var20.getMessage(), var20);
            }
            break;
         case 8:
         case 11:
            this.rolledBackTransaction = false;
            byte[] var5 = this.getTransactionDescriptor();
            if (var5.length != var1.readUnsignedByte()) {
               var1.throwInvalidTDS();
            }

            var1.readBytes(var5, 0, var5.length);
            if (connectionlogger.isLoggable(Level.FINER)) {
               String var23;
               if (8 == var4) {
                  var23 = " started";
               } else {
                  var23 = " enlisted";
               }

               connectionlogger.finer(this.toString() + var23);
            }
            break;
         case 9:
            if (connectionlogger.isLoggable(Level.FINER)) {
               connectionlogger.finer(this.toString() + " committed");
            }

            Arrays.fill(this.getTransactionDescriptor(), (byte)0);
            break;
         case 10:
            this.rolledBackTransaction = true;
            if (this.inXATransaction) {
               if (connectionlogger.isLoggable(Level.FINER)) {
                  connectionlogger.finer(this.toString() + " rolled back. (DTC)");
               }
            } else {
               if (connectionlogger.isLoggable(Level.FINER)) {
                  connectionlogger.finer(this.toString() + " rolled back");
               }

               Arrays.fill(this.getTransactionDescriptor(), (byte)0);
            }
            break;
         case 12:
            if (connectionlogger.isLoggable(Level.FINER)) {
               connectionlogger.finer(this.toString() + " defected");
            }

            Arrays.fill(this.getTransactionDescriptor(), (byte)0);
            break;
         case 13:
            this.setFailoverPartnerServerProvided(var1.readUnicodeString(var1.readUnsignedByte()));
            break;
         case 14:
         default:
            connectionlogger.warning(this.toString() + " Unknown environment change: " + var4);
            this.throwInvalidTDS();
            break;
         case 20:
            int var9 = -1;
            int var8 = -1;
            int var7 = -1;
            int var6 = -1;
            String var10 = null;

            try {
               var6 = var1.readUnsignedShort();
               if (var6 <= 5) {
                  this.throwInvalidTDS();
               }

               var7 = var1.readUnsignedByte();
               if (var7 != 0) {
                  this.throwInvalidTDS();
               }

               var8 = var1.readUnsignedShort();
               if (var8 <= 0 || var8 > 65535) {
                  this.throwInvalidTDS();
               }

               var9 = var1.readUnsignedShort();
               if (var9 <= 0 || var9 > 1024) {
                  this.throwInvalidTDS();
               }

               var10 = var1.readUnicodeString(var9);
               assert var10 != null;
            } finally {
               if (connectionlogger.isLoggable(Level.FINER)) {
                  connectionlogger.finer(
                     this.toString()
                        + " Received routing ENVCHANGE with the following values."
                        + " routingDataValueLength:"
                        + var6
                        + " protocol:"
                        + var7
                        + " portNumber:"
                        + var8
                        + " serverNameLength:"
                        + var9
                        + " serverName:"
                        + (var10 != null ? var10 : "null")
                  );
               }
            }

            String var11 = this.activeConnectionProperties.getProperty("hostNameInCertificate");
            if (null != var11 && var11.startsWith("*") && var10.indexOf(46) != -1) {
               char[] var12 = var11.toCharArray();
               char[] var13 = var10.toCharArray();
               boolean var14 = true;
               int var15 = var11.length() - 1;

               for (int var16 = var10.length() - 1; var15 > 0 && var16 > 0; var16--) {
                  if (var13[var16] != var12[var15]) {
                     var14 = false;
                     break;
                  }

                  var15--;
               }

               if (var14) {
                  String var24 = "*" + var10.substring(var10.indexOf(46));
                  this.activeConnectionProperties.setProperty("hostNameInCertificate", var24);
                  if (connectionlogger.isLoggable(Level.FINER)) {
                     connectionlogger.finer(this.toString() + "Using new host to validate the SSL certificate");
                  }
               }
            }

            this.isRoutedInCurrentAttempt = true;
            this.routingInfo = new ServerPortPlaceHolder(var10, var8, null, this.integratedSecurity);
      }

      var1.reset(var3);
      var1.readBytes(new byte[var2], 0, var2);
   }

   final void processFedAuthInfo(TDSReader var1, TDSTokenHandler var2) throws SQLServerException {
      SQLServerConnection.SqlFedAuthInfo var3 = new SQLServerConnection.SqlFedAuthInfo();
      var1.readUnsignedByte();
      int var4 = var1.readInt();
      if (connectionlogger.isLoggable(Level.FINER)) {
         connectionlogger.fine(this.toString() + " FEDAUTHINFO token stream length = " + var4);
      }

      if (var4 < 4) {
         connectionlogger.severe(this.toString() + "FEDAUTHINFO token stream length too short for CountOfInfoIDs.");
         throw new SQLServerException(SQLServerException.getErrString("R_FedAuthInfoLengthTooShortForCountOfInfoIds"), null);
      }

      int var5 = var1.readInt();
      var4 -= 4;
      if (connectionlogger.isLoggable(Level.FINER)) {
         connectionlogger.fine(this.toString() + " CountOfInfoIDs = " + var5);
      }

      if (var4 <= 0) {
         connectionlogger.severe(this.toString() + "FEDAUTHINFO token stream is not long enough to contain the data it claims to.");
         MessageFormat var23 = new MessageFormat(SQLServerException.getErrString("R_FedAuthInfoLengthTooShortForData"));
         throw new SQLServerException(var23.format(new Object[]{var4}), null);
      }

      byte[] var6 = new byte[var4];
      var1.readBytes(var6, 0, var4);
      if (connectionlogger.isLoggable(Level.FINER)) {
         connectionlogger.fine(this.toString() + " Read rest of FEDAUTHINFO token stream: " + Arrays.toString(var6));
      }

      int var8 = var5 * 9;

      for (int var9 = 0; var9 < var5; var9++) {
         int var10 = var9 * 9;
         byte var11 = var6[var10];
         byte[] var12 = new byte[4];
         var12[3] = var6[var10 + 1];
         var12[2] = var6[var10 + 2];
         var12[1] = var6[var10 + 3];
         var12[0] = var6[var10 + 4];
         ByteBuffer var13 = ByteBuffer.wrap(var12);
         int var14 = var13.getInt();
         var12 = new byte[4];
         var12[3] = var6[var10 + 5];
         var12[2] = var6[var10 + 6];
         var12[1] = var6[var10 + 7];
         var12[0] = var6[var10 + 8];
         var13 = ByteBuffer.wrap(var12);
         int var15 = var13.getInt();
         if (connectionlogger.isLoggable(Level.FINER)) {
            connectionlogger.fine(this.toString() + " FedAuthInfoOpt: ID=" + var11 + ", DataLen=" + var14 + ", Offset=" + var15);
         }

         var15 -= 4;
         if (var15 < var8 || var15 >= var4) {
            connectionlogger.severe(this.toString() + "FedAuthInfoDataOffset points to an invalid location.");
            MessageFormat var28 = new MessageFormat(SQLServerException.getErrString("R_FedAuthInfoInvalidOffset"));
            throw new SQLServerException(var28.format(new Object[]{var15}), null);
         }

         String var16 = null;

         try {
            byte[] var17 = new byte[var14];
            System.arraycopy(var6, var15, var17, 0, var14);
            var16 = new String(var17, "UTF-16LE");
         } catch (UnsupportedEncodingException var20) {
            MessageFormat var18 = new MessageFormat(SQLServerException.getErrString("R_unsupportedEncoding"));
            Object[] var19 = new Object[]{"UTF-16LE"};
            throw new SQLServerException(this, var18.format(var19), null, 0, false);
         } catch (Exception var21) {
            connectionlogger.severe(this.toString() + "Failed to read FedAuthInfoData.");
            throw new SQLServerException(SQLServerException.getErrString("R_FedAuthInfoFailedToReadData"), null);
         }

         if (connectionlogger.isLoggable(Level.FINER)) {
            connectionlogger.fine(this.toString() + " FedAuthInfoData: " + var16);
         }

         switch (var11) {
            case 1:
               var3.stsurl = var16;
               break;
            case 2:
               var3.spn = var16;
               break;
            default:
               if (connectionlogger.isLoggable(Level.FINER)) {
                  connectionlogger.fine(this.toString() + " Ignoring unknown federated authentication info option: " + var11);
               }
         }
      }

      if (null != var3.spn && null != var3.stsurl && !var3.spn.trim().isEmpty() && !var3.stsurl.trim().isEmpty()) {
         this.onFedAuthInfo(var3, var2);
      } else {
         connectionlogger.severe(this.toString() + "FEDAUTHINFO token stream does not contain both STSURL and SPN.");
         throw new SQLServerException(SQLServerException.getErrString("R_FedAuthInfoDoesNotContainStsurlAndSpn"), null);
      }
   }

   void onFedAuthInfo(SQLServerConnection.SqlFedAuthInfo var1, TDSTokenHandler var2) throws SQLServerException {
      assert null != this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.USER.toString())
            && null != this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.PASSWORD.toString())
         || this.authenticationString.trim().equalsIgnoreCase(SqlAuthentication.ActiveDirectoryIntegrated.toString()) && this.fedAuthRequiredPreLoginResponse;
      assert null != var1;
      this.attemptRefreshTokenLocked = true;
      this.fedAuthToken = this.getFedAuthToken(var1);
      this.attemptRefreshTokenLocked = false;
      assert null != this.fedAuthToken;
      SQLServerConnection.FedAuthTokenCommand var3 = new SQLServerConnection.FedAuthTokenCommand(this.fedAuthToken, var2);
      var3.execute(this.tdsChannel.getWriter(), this.tdsChannel.getReader(var3));
   }

   private SQLServerConnection.SqlFedAuthToken getFedAuthToken(SQLServerConnection.SqlFedAuthInfo var1) throws SQLServerException {
      assert null != var1;
      int var2 = 100;
      int var3 = 0;
      FedAuthDllInfo var4 = null;
      String var5 = this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.USER.toString());
      String var6 = this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.PASSWORD.toString());
      long var7 = 0L;

      while (true) {
         var3++;

         try {
            if (this.authenticationString.trim().equalsIgnoreCase(SqlAuthentication.ActiveDirectoryPassword.toString())) {
               var4 = AuthenticationJNI.getAccessToken(
                  var5, var6, var1.stsurl, var1.spn, this.clientConnectionId.toString(), "7f98cb04-cd1e-40df-9140-3bf7e2cea4db", var7
               );
            } else if (this.authenticationString.trim().equalsIgnoreCase(SqlAuthentication.ActiveDirectoryIntegrated.toString())) {
               var4 = AuthenticationJNI.getAccessTokenForWindowsIntegrated(
                  var1.stsurl, var1.spn, this.clientConnectionId.toString(), "7f98cb04-cd1e-40df-9140-3bf7e2cea4db", var7
               );
            }

            assert null != var4.accessTokenBytes;
            break;
         } catch (DLLException var21) {
            int var10 = var21.GetCategory();
            if (-1 == var10) {
               MessageFormat var24 = new MessageFormat(SQLServerException.getErrString("R_UnableLoadADALSqlDll"));
               Object[] var25 = new Object[]{Integer.toHexString(var21.GetState())};
               throw new SQLServerException(var24.format(var25), null);
            }

            int var11 = TimerRemaining(this.timerExpire);
            if (2 != var10 || timerHasExpired(this.timerExpire) || var2 >= var11) {
               String var12 = Integer.toHexString(var21.GetStatus());
               if (connectionlogger.isLoggable(Level.FINER)) {
                  connectionlogger.fine(this.toString() + " SQLServerConnection.getFedAuthToken.AdalException category:" + var10 + " error: " + var12);
               }

               MessageFormat var13 = new MessageFormat(SQLServerException.getErrString("R_ADALAuthenticationMiddleErrorMessage"));
               String var14 = Integer.toHexString(var21.GetStatus()).toUpperCase();
               Object[] var15 = new Object[]{var14, var21.GetState()};
               SQLServerException var16 = new SQLServerException(var13.format(var15), var21);
               MessageFormat var17 = new MessageFormat(SQLServerException.getErrString("R_ADALExecution"));
               Object[] var18 = new Object[]{var5, this.authenticationString};
               throw new SQLServerException(var17.format(var18), null, 0, var16);
            }

            if (connectionlogger.isLoggable(Level.FINER)) {
               connectionlogger.fine(this.toString() + " SQLServerConnection.getFedAuthToken sleeping: " + var2 + " milliseconds.");
               connectionlogger.fine(this.toString() + " SQLServerConnection.getFedAuthToken remaining: " + var11 + " milliseconds.");
            }

            try {
               Thread.sleep(var2);
            } catch (InterruptedException var20) {
            }

            var2 *= 2;
         }
      }

      byte[] var9 = var4.accessTokenBytes;
      String var22 = null;

      try {
         var22 = new String(var9, "UTF-16LE");
      } catch (UnsupportedEncodingException var19) {
         MessageFormat var26 = new MessageFormat(SQLServerException.getErrString("R_unsupportedEncoding"));
         Object[] var27 = new Object[]{"UTF-16LE"};
         throw new SQLServerException(null, var26.format(var27), null, 0, false);
      }

      return new SQLServerConnection.SqlFedAuthToken(var22, var4.expiresIn);
   }

   private void sendFedAuthToken(SQLServerConnection.FedAuthTokenCommand var1, SQLServerConnection.SqlFedAuthToken var2, TDSTokenHandler var3) throws SQLServerException {
      assert null != var2;
      assert null != var2.accessToken;
      if (connectionlogger.isLoggable(Level.FINER)) {
         connectionlogger.fine(this.toString() + " Sending federated authentication token.");
      }

      TDSWriter var4 = var1.startRequest((byte)8);
      byte[] var5 = null;

      try {
         var5 = var2.accessToken.getBytes("UTF-16LE");
      } catch (UnsupportedEncodingException var9) {
         MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_unsupportedEncoding"));
         Object[] var8 = new Object[]{"UTF-16LE"};
         throw new SQLServerException(this, var7.format(var8), null, 0, false);
      }

      var4.writeInt(var5.length + 4);
      var4.writeInt(var5.length);
      var4.writeBytes(var5, 0, var5.length);
      TDSReader var6 = var1.startResponse();
      this.federatedAuthenticationRequested = true;
      TDSParser.parse(var6, var3);
   }

   final void processFeatureExtAck(TDSReader var1) throws SQLServerException {
      var1.readUnsignedByte();

      byte var2;
      do {
         var2 = (byte)var1.readUnsignedByte();
         if (var2 != -1) {
            int var3 = var1.readInt();
            byte[] var4 = new byte[var3];
            if (var3 > 0) {
               var1.readBytes(var4, 0, var3);
            }

            this.onFeatureExtAck(var2, var4);
         }
      } while (var2 != -1);
   }

   private void onFeatureExtAck(int var1, byte[] var2) throws SQLServerException {
      if (null == this.routingInfo) {
         switch (var1) {
            case 2:
               if (connectionlogger.isLoggable(Level.FINER)) {
                  connectionlogger.fine(this.toString() + " Received feature extension acknowledgement for federated authentication.");
               }

               if (!this.federatedAuthenticationRequested) {
                  connectionlogger.severe(this.toString() + " Did not request federated authentication.");
                  MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_UnrequestedFeatureAckReceived"));
                  Object[] var7 = new Object[]{var1};
                  throw new SQLServerException(var6.format(var7), null);
               } else {
                  assert null != this.fedAuthFeatureExtensionData;
                  switch (this.fedAuthFeatureExtensionData.libraryType) {
                     case 1:
                     case 2:
                        if (0 != var2.length) {
                           connectionlogger.severe(
                              this.toString() + " Federated authentication feature extension ack for ADAL and Security Token includes extra data."
                           );
                           throw new SQLServerException(SQLServerException.getErrString("R_FedAuthFeatureAckContainsExtraData"), null);
                        }

                        this.federatedAuthenticationAcknowledged = true;
                        return;
                     default:
                        assert false;
                        connectionlogger.severe(this.toString() + " Attempting to use unknown federated authentication library.");
                        MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_FedAuthFeatureAckUnknownLibraryType"));
                        Object[] var4 = new Object[]{this.fedAuthFeatureExtensionData.libraryType};
                        throw new SQLServerException(var5.format(var4), null);
                  }
               }
            case 4:
               if (connectionlogger.isLoggable(Level.FINER)) {
                  connectionlogger.fine(this.toString() + " Received feature extension acknowledgement for AE.");
               }

               if (1 > var2.length) {
                  connectionlogger.severe(this.toString() + " Unknown version number for AE.");
                  throw new SQLServerException(SQLServerException.getErrString("R_InvalidAEVersionNumber"), null);
               } else {
                  byte var3 = var2[0];
                  if (0 == var3 || var3 > 1) {
                     connectionlogger.severe(this.toString() + " Invalid version number for AE.");
                     throw new SQLServerException(SQLServerException.getErrString("R_InvalidAEVersionNumber"), null);
                  }

                  assert var3 == 1;
                  this.serverSupportsColumnEncryption = true;
                  return;
               }
            default:
               connectionlogger.severe(this.toString() + " Unknown feature ack.");
               throw new SQLServerException(SQLServerException.getErrString("R_UnknownFeatureAck"), null);
         }
      }
   }

   private final void executeDTCCommand(int var1, byte[] var2, String var3) throws SQLServerException {
      final class DTCCommand extends UninterruptableTDSCommand {
         private final int requestType;
         private final byte[] payload;

         DTCCommand(int var2, byte[] var3, String var4) {
            super(var4);
            this.requestType = var2;
            this.payload = var3;
         }

         final boolean doExecute() throws SQLServerException {
            TDSWriter var1 = this.startRequest((byte)14);
            var1.writeShort((short)this.requestType);
            if (null == this.payload) {
               var1.writeShort((short)0);
            } else {
               assert this.payload.length <= 32767;
               var1.writeShort((short)this.payload.length);
               var1.writeBytes(this.payload);
            }

            TDSParser.parse(this.startResponse(), this.getLogContext());
            return true;
         }
      }

      this.executeCommand(new DTCCommand(var1, var2, var3));
   }

   final void JTAUnenlistConnection() throws SQLServerException {
      this.executeDTCCommand(1, null, "MS_DTC unenlist connection");
      this.inXATransaction = false;
   }

   final void JTAEnlistConnection(byte[] var1) throws SQLServerException {
      this.executeDTCCommand(1, var1, "MS_DTC enlist connection");
      this.connectionCommand(this.sqlStatementToSetTransactionIsolationLevel(), "JTAEnlistConnection");
      this.inXATransaction = true;
   }

   private byte[] toUCS16(String var1) throws SQLServerException {
      if (var1 == null) {
         return new byte[0];
      }

      int var2 = var1.length();
      byte[] var3 = new byte[var2 * 2];
      int var4 = 0;

      for (int var5 = 0; var5 < var2; var5++) {
         char var6 = var1.charAt(var5);
         byte var7 = (byte)(var6 & 0xFF);
         var3[var4++] = var7;
         var3[var4++] = (byte)(var6 >> '\b' & 0xFF);
      }

      return var3;
   }

   private byte[] encryptPassword(String var1) {
      if (var1 == null) {
         var1 = "";
      }

      int var2 = var1.length();
      byte[] var3 = new byte[var2 * 2];

      for (int var4 = 0; var4 < var2; var4++) {
         int var5 = var1.charAt(var4) ^ 23130;
         var5 = (var5 & 15) << 4 | (var5 & 240) >> 4 | (var5 & 3840) << 4 | (var5 & 61440) >> 4;
         byte var6 = (byte)((var5 & 0xFF00) >> 8);
         var3[var4 * 2 + 1] = var6;
         byte var7 = (byte)(var5 & 0xFF);
         var3[var4 * 2 + 0] = var7;
      }

      return var3;
   }

   private void sendLogon(SQLServerConnection.LogonCommand var1, SSPIAuthentication var2, SQLServerConnection.FederatedAuthenticationFeatureExtensionData var3) throws SQLServerException {
      assert !this.integratedSecurity || !this.fedAuthRequiredPreLoginResponse;
      assert !this.integratedSecurity || !this.federatedAuthenticationInfoRequested && !this.federatedAuthenticationRequested;
      assert null == var3 || this.federatedAuthenticationInfoRequested || this.federatedAuthenticationRequested;
      assert null != var3 || !this.federatedAuthenticationInfoRequested && !this.federatedAuthenticationRequested;
      String var4 = this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.WORKSTATION_ID.toString());
      String var5 = this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.USER.toString());
      String var6 = this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.PASSWORD.toString());
      String var7 = this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.APPLICATION_NAME.toString());
      String var8 = "Microsoft JDBC Driver 6.0";
      String var9 = this.generateInterfaceLibVersion();
      String var10 = this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.DATABASE_NAME.toString());
      String var11 = null;
      if (null != this.currentConnectPlaceHolder) {
         var11 = this.currentConnectPlaceHolder.getServerName();
      } else {
         var11 = this.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.SERVER_NAME.toString());
      }

      if (var11 != null && var11.length() > 128) {
         var11 = var11.substring(0, 128);
      }

      if (var4 == null || var4.length() == 0) {
         var4 = Util.lookupHostName();
      }

      byte[] var12 = new byte[0];
      boolean[] var13 = new boolean[]{false};
      if (null != var2) {
         var12 = var2.GenerateClientContext(var12, var13);
         var5 = null;
         var6 = null;
      }

      byte[] var14 = this.toUCS16(var4);
      byte[] var15 = this.toUCS16(var5);
      byte[] var16 = this.encryptPassword(var6);
      int var17 = var16 != null ? var16.length : 0;
      byte[] var18 = this.toUCS16(var7);
      byte[] var19 = this.toUCS16(var11);
      byte[] var20 = this.toUCS16(var8);
      byte[] var21 = DatatypeConverter.parseHexBinary(var9);
      byte[] var22 = this.toUCS16(var10);
      byte[] var23 = new byte[6];
      int var24 = 0;
      int var25 = 0;
      if (this.serverMajorVersion >= 11) {
         this.tdsVersion = 1946157060;
      } else if (this.serverMajorVersion >= 10) {
         this.tdsVersion = 1930100739;
      } else if (this.serverMajorVersion >= 9) {
         this.tdsVersion = 1913192450;
      } else {
         assert false : "prelogin did not disconnect for the old version: " + this.serverMajorVersion;
      }

      TDSWriter var27 = var1.startRequest((byte)16);
      var24 = 94 + var14.length + var18.length + var19.length + var20.length + var22.length + var12.length + 4;
      if (!this.integratedSecurity && !this.federatedAuthenticationInfoRequested && !this.federatedAuthenticationRequested) {
         var24 = var24 + var17 + var15.length;
      }

      int var28 = var24;
      var24 += this.writeAEFeatureRequest(false, var27);
      if (this.federatedAuthenticationInfoRequested || this.federatedAuthenticationRequested) {
         var24 += this.writeFedAuthFeatureRequest(false, var27, var3);
      }

      var27.writeInt(++var24);
      var27.writeInt(this.tdsVersion);
      var27.writeInt(this.requestedPacketSize);
      var27.writeBytes(var21);
      var27.writeInt(0);
      var27.writeInt(0);
      var27.writeByte((byte)-32);
      var27.writeByte((byte)(3 | (this.integratedSecurity ? -128 : 0)));
      var27.writeByte((byte)(0 | (this.applicationIntent != null && this.applicationIntent.equals(ApplicationIntent.READ_ONLY) ? 32 : 0)));
      byte var29 = 0;
      var29 = 16;
      var27.writeByte((byte)(0 | var29 | (this.serverMajorVersion >= 10 ? 8 : 0)));
      var27.writeInt(0);
      var27.writeInt(0);
      var27.writeShort((short)94);
      var27.writeShort((short)(var4 == null ? 0 : var4.length()));
      var25 += var14.length;
      if (!this.integratedSecurity && !this.federatedAuthenticationInfoRequested && !this.federatedAuthenticationRequested) {
         var27.writeShort((short)(94 + var25));
         var27.writeShort((short)(var5 == null ? 0 : var5.length()));
         var25 += var15.length;
         var27.writeShort((short)(94 + var25));
         var27.writeShort((short)(var6 == null ? 0 : var6.length()));
         var25 += var17;
      } else {
         var27.writeShort((short)0);
         var27.writeShort((short)0);
         var27.writeShort((short)0);
         var27.writeShort((short)0);
      }

      var27.writeShort((short)(94 + var25));
      var27.writeShort((short)(var7 == null ? 0 : var7.length()));
      var25 += var18.length;
      var27.writeShort((short)(94 + var25));
      var27.writeShort((short)(var11 == null ? 0 : var11.length()));
      var25 += var19.length;
      var27.writeShort((short)(94 + var25));
      var27.writeShort((short)4);
      var25 += 4;
      assert null != var8;
      var27.writeShort((short)(94 + var25));
      var27.writeShort((short)var8.length());
      var25 += var20.length;
      var27.writeShort((short)0);
      var27.writeShort((short)0);
      var27.writeShort((short)(94 + var25));
      var27.writeShort((short)(var10 == null ? 0 : var10.length()));
      var25 += var22.length;
      var27.writeBytes(var23);
      if (!this.integratedSecurity) {
         var27.writeShort((short)0);
         var27.writeShort((short)0);
      } else {
         var27.writeShort((short)(94 + var25));
         if (65535 <= var12.length) {
            var27.writeShort((short)-1);
         } else {
            var27.writeShort((short)var12.length);
         }
      }

      var27.writeShort((short)0);
      var27.writeShort((short)0);
      if (this.tdsVersion >= 1913192450) {
         var27.writeShort((short)0);
         var27.writeShort((short)0);
         if (65535 <= var12.length) {
            var27.writeInt(var12.length);
         } else {
            var27.writeInt(0);
         }
      }

      var27.writeBytes(var14);
      var27.setDataLoggable(false);
      if (!this.integratedSecurity && !this.federatedAuthenticationInfoRequested && !this.federatedAuthenticationRequested) {
         var27.writeBytes(var15);
         var27.writeBytes(var16);
      }

      var27.setDataLoggable(true);
      var27.writeBytes(var18);
      var27.writeBytes(var19);
      var27.writeInt(var28);
      var27.writeBytes(var20);
      var27.writeBytes(var22);
      var27.setDataLoggable(false);
      if (this.integratedSecurity) {
         var27.writeBytes(var12, 0, var12.length);
      }

      this.writeAEFeatureRequest(true, var27);
      if (this.federatedAuthenticationInfoRequested || this.federatedAuthenticationRequested) {
         this.writeFedAuthFeatureRequest(true, var27, var3);
      }

      var27.writeByte((byte)-1);
      var27.setDataLoggable(true);

      final class LogonProcessor extends TDSTokenHandler {
         private final SSPIAuthentication auth;
         private byte[] secBlobOut = null;
         StreamLoginAck loginAckToken;

         LogonProcessor(SSPIAuthentication var2) {
            super("logon");
            this.auth = var2;
            this.loginAckToken = null;
         }

         boolean onSSPI(TDSReader var1) throws SQLServerException {
            StreamSSPI var2 = new StreamSSPI();
            var2.setFromTDS(var1);
            boolean[] var3 = new boolean[]{false};
            this.secBlobOut = this.auth.GenerateClientContext(var2.sspiBlob, var3);
            return true;
         }

         boolean onLoginAck(TDSReader var1) throws SQLServerException {
            this.loginAckToken = new StreamLoginAck();
            this.loginAckToken.setFromTDS(var1);
            SQLServerConnection.this.sqlServerVersion = this.loginAckToken.sSQLServerVersion;
            SQLServerConnection.this.tdsVersion = this.loginAckToken.tdsVersion;
            return true;
         }

         final boolean complete(SQLServerConnection.LogonCommand var1, TDSReader var2) throws SQLServerException {
            if (null != this.loginAckToken) {
               return true;
            } else if (null != this.secBlobOut && 0 != this.secBlobOut.length) {
               var1.startRequest((byte)17).writeBytes(this.secBlobOut, 0, this.secBlobOut.length);
               return false;
            } else {
               var1.startRequest((byte)17);
               var1.onRequestComplete();
               SQLServerConnection.this.tdsChannel.numMsgsSent++;
               TDSParser.parse(var2, this);
               return true;
            }
         }
      }

      LogonProcessor var31 = new LogonProcessor(var2);
      TDSReader var32 = null;

      do {
         var32 = var1.startResponse();
         TDSParser.parse(var32, var31);
      } while (!var31.complete(var1, var32));
   }

   private String generateInterfaceLibVersion() {
      StringBuffer var1 = new StringBuffer();
      int var2 = String.valueOf(8112).length();
      int var3 = Integer.valueOf(String.valueOf(8112).substring(0, var2 - 2));
      int var4 = Integer.valueOf(String.valueOf(8112).substring(var2 - 2));
      String var5 = Integer.toHexString(6);
      String var6 = Integer.toHexString(0);
      String var7 = Integer.toHexString(var3);
      String var8 = Integer.toHexString(var4);
      if (2 == var8.length()) {
         var1.append(var8);
      } else {
         var1.append("0");
         var1.append(var8);
      }

      if (2 == var7.length()) {
         var1.append(var7);
      } else {
         var1.append("0");
         var1.append(var7);
      }

      if (2 == var6.length()) {
         var1.append(var6);
      } else {
         var1.append("0");
         var1.append(var6);
      }

      if (2 == var5.length()) {
         var1.append(var5);
      } else {
         var1.append("0");
         var1.append(var5);
      }

      return var1.toString();
   }

   private void checkValidHoldability(int var1) throws SQLServerException {
      if (var1 != 1 && var1 != 2) {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidHoldability"));
         SQLServerException.makeFromDriverError(this, this, var2.format(new Object[]{var1}), null, true);
      }
   }

   private void checkMatchesCurrentHoldability(int var1) throws SQLServerException {
      if (var1 != this.holdability) {
         SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_sqlServerHoldability"), null, false);
      }
   }

   public Statement createStatement(int var1, int var2, int var3) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "createStatement", new Object[]{new Integer(var1), new Integer(var2), var3});
      Statement var4 = this.createStatement(var1, var2, var3, SQLServerStatementColumnEncryptionSetting.UseConnectionSetting);
      loggerExternal.exiting(this.getClassNameLogging(), "createStatement", var4);
      return var4;
   }

   public Statement createStatement(int var1, int var2, int var3, SQLServerStatementColumnEncryptionSetting var4) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "createStatement", new Object[]{new Integer(var1), new Integer(var2), var3, var4});
      this.checkClosed();
      this.checkValidHoldability(var3);
      this.checkMatchesCurrentHoldability(var3);
      SQLServerStatement var5 = new SQLServerStatement(this, var1, var2, var4);
      loggerExternal.exiting(this.getClassNameLogging(), "createStatement", var5);
      return var5;
   }

   public PreparedStatement prepareStatement(String var1, int var2, int var3, int var4) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{new Integer(var2), new Integer(var3), var4});
      PreparedStatement var5 = this.prepareStatement(var1, var2, var3, var4, SQLServerStatementColumnEncryptionSetting.UseConnectionSetting);
      loggerExternal.exiting(this.getClassNameLogging(), "prepareStatement", var5);
      return var5;
   }

   public PreparedStatement prepareStatement(String var1, int var2, int var3, int var4, SQLServerStatementColumnEncryptionSetting var5) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{new Integer(var2), new Integer(var3), var4, var5});
      this.checkClosed();
      this.checkValidHoldability(var4);
      this.checkMatchesCurrentHoldability(var4);
      SQLServerPreparedStatement var6 = new SQLServerPreparedStatement(this, var1, var2, var3, var5);
      loggerExternal.exiting(this.getClassNameLogging(), "prepareStatement", var6);
      return var6;
   }

   public CallableStatement prepareCall(String var1, int var2, int var3, int var4) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{new Integer(var2), new Integer(var3), var4});
      CallableStatement var5 = this.prepareCall(var1, var2, var3, var4, SQLServerStatementColumnEncryptionSetting.UseConnectionSetting);
      loggerExternal.exiting(this.getClassNameLogging(), "prepareCall", var5);
      return var5;
   }

   public CallableStatement prepareCall(String var1, int var2, int var3, int var4, SQLServerStatementColumnEncryptionSetting var5) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{new Integer(var2), new Integer(var3), var4, var5});
      this.checkClosed();
      this.checkValidHoldability(var4);
      this.checkMatchesCurrentHoldability(var4);
      SQLServerCallableStatement var6 = new SQLServerCallableStatement(this, var1, var2, var3, var5);
      loggerExternal.exiting(this.getClassNameLogging(), "prepareCall", var6);
      return var6;
   }

   public PreparedStatement prepareStatement(String var1, int var2) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{var1, new Integer(var2)});
      SQLServerPreparedStatement var3 = (SQLServerPreparedStatement)this.prepareStatement(
         var1, var2, SQLServerStatementColumnEncryptionSetting.UseConnectionSetting
      );
      loggerExternal.exiting(this.getClassNameLogging(), "prepareStatement", var3);
      return var3;
   }

   public PreparedStatement prepareStatement(String var1, int var2, SQLServerStatementColumnEncryptionSetting var3) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{var1, new Integer(var2), var3});
      this.checkClosed();
      SQLServerPreparedStatement var4 = (SQLServerPreparedStatement)this.prepareStatement(var1, 1003, 1007, var3);
      var4.bRequestedGeneratedKeys = var2 == 1;
      loggerExternal.exiting(this.getClassNameLogging(), "prepareStatement", var4);
      return var4;
   }

   public PreparedStatement prepareStatement(String var1, int[] var2) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{var1, var2});
      SQLServerPreparedStatement var3 = (SQLServerPreparedStatement)this.prepareStatement(
         var1, var2, SQLServerStatementColumnEncryptionSetting.UseConnectionSetting
      );
      loggerExternal.exiting(this.getClassNameLogging(), "prepareStatement", var3);
      return var3;
   }

   public PreparedStatement prepareStatement(String var1, int[] var2, SQLServerStatementColumnEncryptionSetting var3) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{var1, var2, var3});
      this.checkClosed();
      if (var2 == null || var2.length != 1) {
         SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_invalidColumnArrayLength"), null, false);
      }

      SQLServerPreparedStatement var4 = (SQLServerPreparedStatement)this.prepareStatement(var1, 1003, 1007, var3);
      var4.bRequestedGeneratedKeys = true;
      loggerExternal.exiting(this.getClassNameLogging(), "prepareStatement", var4);
      return var4;
   }

   public PreparedStatement prepareStatement(String var1, String[] var2) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{var1, var2});
      SQLServerPreparedStatement var3 = (SQLServerPreparedStatement)this.prepareStatement(
         var1, var2, SQLServerStatementColumnEncryptionSetting.UseConnectionSetting
      );
      loggerExternal.exiting(this.getClassNameLogging(), "prepareStatement", var3);
      return var3;
   }

   public PreparedStatement prepareStatement(String var1, String[] var2, SQLServerStatementColumnEncryptionSetting var3) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "prepareStatement", new Object[]{var1, var2, var3});
      this.checkClosed();
      if (var2 == null || var2.length != 1) {
         SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_invalidColumnArrayLength"), null, false);
      }

      SQLServerPreparedStatement var4 = (SQLServerPreparedStatement)this.prepareStatement(var1, 1003, 1007, var3);
      var4.bRequestedGeneratedKeys = true;
      loggerExternal.exiting(this.getClassNameLogging(), "prepareStatement", var4);
      return var4;
   }

   public void releaseSavepoint(Savepoint var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "releaseSavepoint", var1);
      this.NotImplemented();
   }

   private final Savepoint setNamedSavepoint(String var1) throws SQLServerException {
      if (this.databaseAutoCommitMode) {
         SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_cantSetSavepoint"), null, false);
      }

      SQLServerSavepoint var2 = new SQLServerSavepoint(this, var1);
      this.connectionCommand(
         "IF @@TRANCOUNT = 0 BEGIN BEGIN TRAN IF @@TRANCOUNT = 2 COMMIT TRAN END SAVE TRAN " + Util.escapeSQLId(var2.getLabel()), "setSavepoint"
      );
      return var2;
   }

   public Savepoint setSavepoint(String var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "setSavepoint", var1);
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      Savepoint var2 = this.setNamedSavepoint(var1);
      loggerExternal.exiting(this.getClassNameLogging(), "setSavepoint", var2);
      return var2;
   }

   public Savepoint setSavepoint() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "setSavepoint");
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      Savepoint var1 = this.setNamedSavepoint(null);
      loggerExternal.exiting(this.getClassNameLogging(), "setSavepoint", var1);
      return var1;
   }

   public void rollback(Savepoint var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "rollback", var1);
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      if (this.databaseAutoCommitMode) {
         SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_cantInvokeRollback"), null, false);
      }

      this.connectionCommand("IF @@TRANCOUNT > 0 ROLLBACK TRAN " + Util.escapeSQLId(((SQLServerSavepoint)var1).getLabel()), "rollbackSavepoint");
      loggerExternal.exiting(this.getClassNameLogging(), "rollback");
   }

   public int getHoldability() throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "getHoldability");
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.exiting(this.getClassNameLogging(), "getHoldability", this.holdability);
      }

      return this.holdability;
   }

   public void setHoldability(int var1) throws SQLServerException {
      loggerExternal.entering(this.getClassNameLogging(), "setHoldability", var1);
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkValidHoldability(var1);
      this.checkClosed();
      if (this.holdability != var1) {
         assert 1 == var1 || 2 == var1 : "invalid holdability " + var1;
         this.connectionCommand(var1 == 2 ? "SET CURSOR_CLOSE_ON_COMMIT ON" : "SET CURSOR_CLOSE_ON_COMMIT OFF", "setHoldability");
         this.holdability = var1;
      }

      loggerExternal.exiting(this.getClassNameLogging(), "setHoldability");
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
      loggerExternal.entering(this.getClassNameLogging(), "getSchema");
      DriverJDBCVersion.checkSupportsJDBC41();
      this.checkClosed();
      SQLServerStatement var1 = null;
      SQLServerResultSet var2 = null;

      try {
         var1 = (SQLServerStatement)this.createStatement();
         var2 = var1.executeQueryInternal("SELECT SCHEMA_NAME()");
         if (var2 != null) {
            var2.next();
            return var2.getString(1);
         }

         SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_getSchemaError"), null, true);
      } catch (SQLException var7) {
         if (this.isSessionUnAvailable()) {
            throw var7;
         }

         SQLServerException.makeFromDriverError(this, this, SQLServerException.getErrString("R_getSchemaError"), null, true);
      } finally {
         if (var2 != null) {
            var2.close();
         }

         if (var1 != null) {
            var1.close();
         }
      }

      loggerExternal.exiting(this.getClassNameLogging(), "getSchema");
      return null;
   }

   public void setSchema(String var1) throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "setSchema", var1);
      DriverJDBCVersion.checkSupportsJDBC41();
      this.checkClosed();
      this.addWarning(SQLServerException.getErrString("R_setSchemaWarning"));
      loggerExternal.exiting(this.getClassNameLogging(), "setSchema");
   }

   public synchronized void setSendTimeAsDatetime(boolean var1) {
      this.sendTimeAsDatetime = var1;
   }

   public Array createArrayOf(String var1, Object[] var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   public Blob createBlob() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return new SQLServerBlob(this);
   }

   public Clob createClob() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return new SQLServerClob(this);
   }

   public NClob createNClob() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return new SQLServerNClob(this);
   }

   public SQLXML createSQLXML() throws SQLException {
      loggerExternal.entering(this.getClassNameLogging(), "createSQLXML");
      DriverJDBCVersion.checkSupportsJDBC4();
      SQLXML var1 = null;
      var1 = new SQLServerSQLXML(this);
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.exiting(this.getClassNameLogging(), "createSQLXML", var1);
      }

      return var1;
   }

   public Struct createStruct(String var1, Object[] var2) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   String getTrustedServerNameAE() throws SQLServerException {
      return this.trustedServerNameAE.toUpperCase();
   }

   public Properties getClientInfo() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getClientInfo");
      this.checkClosed();
      Properties var1 = new Properties();
      loggerExternal.exiting(this.getClassNameLogging(), "getClientInfo", var1);
      return var1;
   }

   public String getClientInfo(String var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "getClientInfo", var1);
      this.checkClosed();
      loggerExternal.exiting(this.getClassNameLogging(), "getClientInfo", null);
      return null;
   }

   public void setClientInfo(Properties var1) throws SQLClientInfoException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "setClientInfo", var1);

      try {
         this.checkClosed();
      } catch (SQLServerException var5) {
         SQLClientInfoException var3 = new SQLClientInfoException();
         var3.initCause(var5);
         throw var3;
      }

      if (!var1.isEmpty()) {
         Enumeration var2 = var1.keys();

         while (var2.hasMoreElements()) {
            MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_invalidProperty"));
            Object[] var4 = new Object[]{var2.nextElement()};
            this.addWarning(var6.format(var4));
         }
      }

      loggerExternal.exiting(this.getClassNameLogging(), "setClientInfo");
   }

   public void setClientInfo(String var1, String var2) throws SQLClientInfoException {
      DriverJDBCVersion.checkSupportsJDBC4();
      loggerExternal.entering(this.getClassNameLogging(), "setClientInfo", new Object[]{var1, var2});

      try {
         this.checkClosed();
      } catch (SQLServerException var5) {
         SQLClientInfoException var4 = new SQLClientInfoException();
         var4.initCause(var5);
         throw var4;
      }

      MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_invalidProperty"));
      Object[] var6 = new Object[]{var1};
      this.addWarning(var3.format(var6));
      loggerExternal.exiting(this.getClassNameLogging(), "setClientInfo");
   }

   public boolean isValid(int var1) throws SQLException {
      boolean var2 = false;
      loggerExternal.entering(this.getClassNameLogging(), "isValid", var1);
      DriverJDBCVersion.checkSupportsJDBC4();
      if (var1 < 0) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_invalidQueryTimeOutValue"));
         Object[] var4 = new Object[]{var1};
         SQLServerException.makeFromDriverError(this, this, var3.format(var4), null, true);
      }

      if (this.isSessionUnAvailable()) {
         return false;
      }

      try {
         SQLServerStatement var6 = new SQLServerStatement(this, 1003, 1007, SQLServerStatementColumnEncryptionSetting.UseConnectionSetting);
         if (0 != var1) {
            var6.setQueryTimeout(var1);
         }

         var6.executeQueryInternal("SELECT 1");
         var6.close();
         var2 = true;
      } catch (SQLException var5) {
         connectionlogger.fine(this.toString() + " Exception checking connection validity: " + var5.getMessage());
      }

      loggerExternal.exiting(this.getClassNameLogging(), "isValid", var2);
      return var2;
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
      } catch (ClassCastException var5) {
         SQLServerException var4 = new SQLServerException(var5.getMessage(), var5);
         throw var4;
      }

      loggerExternal.exiting(this.getClassNameLogging(), "unwrap", var2);
      return (T)var2;
   }

   String replaceParameterMarkers(String var1, Parameter[] var2, boolean var3) throws SQLServerException {
      char[] var5 = new char[var1.length() + var2.length * (6 + OUT.length)];
      int var6 = 0;
      int var7 = 0;
      int var8 = 0;
      int var9 = 0;

      while (true) {
         int var10 = ParameterUtils.scanSQLForChar('?', var1, var7);
         var1.getChars(var7, var10, var5, var6);
         var6 += var10 - var7;
         if (var1.length() == var10) {
            while (var6 < var5.length) {
               var5[var6++] = ' ';
            }

            return new String(var5);
         }

         var6 += makeParamName(var8++, var5, var6);
         var7 = var10 + 1;
         if (var2[var9++].isOutput() && (!var3 || var9 > 1)) {
            System.arraycopy(OUT, 0, var5, var6, OUT.length);
            var6 += OUT.length;
         }
      }
   }

   static int makeParamName(int var0, char[] var1, int var2) {
      var1[var2 + 0] = '@';
      var1[var2 + 1] = 'P';
      if (var0 < 10) {
         var1[var2 + 2] = (char)(48 + var0);
         return 3;
      }

      if (var0 >= 100) {
         String var4 = "" + var0;
         var4.getChars(0, var4.length(), var1, var2 + 2);
         return 2 + var4.length();
      }

      int var3 = 2;

      while (var0 >= var3 * 10) {
         var3++;
      }

      var1[var2 + 2] = (char)(48 + (var3 - 1));
      var1[var2 + 3] = (char)(48 + (var0 - (var3 - 1) * 10));
      return 4;
   }

   void notifyPooledConnection(SQLServerException var1) {
      synchronized (this) {
         if (null != this.pooledConnectionParent) {
            this.pooledConnectionParent.notifyEvent(var1);
         }
      }
   }

   void DetachFromPool() {
      synchronized (this) {
         this.pooledConnectionParent = null;
      }
   }

   String getInstancePort(String var1, String var2) throws SQLServerException {
      String var3 = null;
      DatagramSocket var4 = null;
      String var5 = null;

      try {
         var5 = "Failed to determine instance for the : " + var1 + " instance:" + var2;
         if (null == var4) {
            try {
               var4 = new DatagramSocket();
               var4.setSoTimeout(1000);
            } catch (SocketException var24) {
               var5 = "Unable to create local datagram socket";
               throw var24;
            }
         }

         assert null != var4;

         try {
            if (this.multiSubnetFailover) {
               InetAddress[] var28 = InetAddress.getAllByName(var1);
               assert null != var28;

               for (InetAddress var10 : var28) {
                  try {
                     byte[] var11 = (" " + var2).getBytes();
                     var11[0] = 4;
                     DatagramPacket var12 = new DatagramPacket(var11, var11.length, var10, 1434);
                     var4.send(var12);
                  } catch (IOException var23) {
                     var5 = "Error sending SQL Server Browser Service UDP request to address: " + var10 + ", port: " + 1434;
                     throw var23;
                  }
               }
            } else {
               InetAddress var6 = InetAddress.getByName(var1);
               assert null != var6;

               try {
                  byte[] var31 = (" " + var2).getBytes();
                  var31[0] = 4;
                  DatagramPacket var36 = new DatagramPacket(var31, var31.length, var6, 1434);
                  var4.send(var36);
               } catch (IOException var22) {
                  var5 = "Error sending SQL Server Browser Service UDP request to address: " + var6 + ", port: " + 1434;
                  throw var22;
               }
            }
         } catch (UnknownHostException var25) {
            var5 = "Unable to determine IP address of host: " + var1;
            throw var25;
         }

         try {
            byte[] var29 = new byte[4096];
            DatagramPacket var33 = new DatagramPacket(var29, var29.length);
            var4.receive(var33);
            var3 = new String(var29, 3, var29.length - 3);
            if (connectionlogger.isLoggable(Level.FINER)) {
               connectionlogger.fine(this.toString() + " Received SSRP UDP response from IP address: " + var33.getAddress().getHostAddress().toString());
            }
         } catch (IOException var21) {
            var5 = "Error receiving SQL Server Browser Service UDP response from server: " + var1;
            throw var21;
         }
      } catch (IOException var26) {
         MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_sqlBrowserFailed"));
         Object[] var8 = new Object[]{var1, var2, var26.toString()};
         connectionlogger.log(Level.FINE, this.toString() + " " + var5, var26);
         SQLServerException.makeFromDriverError(this, this, var7.format(var8), "08001", false);
      } finally {
         if (null != var4) {
            var4.close();
         }
      }

      assert null != var3;
      int var30 = var3.indexOf("tcp;");
      if (-1 == var30) {
         MessageFormat var34 = new MessageFormat(SQLServerException.getErrString("R_notConfiguredToListentcpip"));
         Object[] var38 = new Object[]{var2};
         SQLServerException.makeFromDriverError(this, this, var34.format(var38), "08001", false);
      }

      int var35 = var30 + 4;
      int var39 = var3.indexOf(59, var35);
      return var3.substring(var35, var39);
   }

   int getNextSavepointId() {
      this.nNextSavePointId++;
      return this.nNextSavePointId;
   }

   void doSecurityCheck() {
      assert null != this.currentConnectPlaceHolder;
      this.currentConnectPlaceHolder.doSecurityCheck();
   }

   public static synchronized void setColumnEncryptionKeyCacheTtl(int var0, TimeUnit var1) throws SQLServerException {
      if (var0 >= 0 && !var1.equals(TimeUnit.MILLISECONDS) && !var1.equals(TimeUnit.MICROSECONDS) && !var1.equals(TimeUnit.NANOSECONDS)) {
         columnEncryptionKeyCacheTtl = TimeUnit.SECONDS.convert(var0, var1);
      } else {
         throw new SQLServerException(null, SQLServerException.getErrString("R_invalidCEKCacheTtl"), null, 0, false);
      }
   }

   static synchronized long getColumnEncryptionKeyCacheTtl() {
      return columnEncryptionKeyCacheTtl;
   }

   static {
      if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).startsWith("windows")) {
         isWindows = true;
         SQLServerColumnEncryptionCertificateStoreProvider var0 = new SQLServerColumnEncryptionCertificateStoreProvider();
         globalSystemColumnEncryptionKeyStoreProviders.put(var0.getName(), var0);
      } else {
         isWindows = false;
      }

      globalCustomColumnEncryptionKeyStoreProviders = null;
      columnEncryptionTrustedMasterKeyPaths = new HashMap();
      baseConnectionID = 0;
      connectionlogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.SQLServerConnection");
      loggerExternal = Logger.getLogger("com.microsoft.sqlserver.jdbc.Connection");
      DEFAULTPORT = SQLServerDriverIntProperty.PORT_NUMBER.getDefaultValue();
      OUT = new char[]{' ', 'O', 'U', 'T'};
      columnEncryptionKeyCacheTtl = TimeUnit.SECONDS.convert(2L, TimeUnit.HOURS);
   }

   private class ActiveDirectoryAuthentication {
      private static final String jdbcFedauthClientId = "7f98cb04-cd1e-40df-9140-3bf7e2cea4db";
      private static final String AdalGetAccessTokenFunctionName = "ADALGetAccessToken";
      private static final int GetAccessTokenSuccess = 0;
      private static final int GetAccessTokenInvalidGrant = 1;
      private static final int GetAccessTokenTansisentError = 2;
      private static final int GetAccessTokenOtherError = 3;
   }

   final class FedAuthTokenCommand extends UninterruptableTDSCommand {
      TDSTokenHandler tdsTokenHandler = null;
      SQLServerConnection.SqlFedAuthToken fedAuthToken = null;

      FedAuthTokenCommand(SQLServerConnection.SqlFedAuthToken var2, TDSTokenHandler var3) {
         super("FedAuth");
         this.tdsTokenHandler = var3;
         this.fedAuthToken = var2;
      }

      final boolean doExecute() throws SQLServerException {
         SQLServerConnection.this.sendFedAuthToken(this, this.fedAuthToken, this.tdsTokenHandler);
         return true;
      }
   }

   class FederatedAuthenticationFeatureExtensionData {
      boolean fedAuthRequiredPreLoginResponse;
      int libraryType = -1;
      byte[] accessToken = null;
      SqlAuthentication authentication = null;

      FederatedAuthenticationFeatureExtensionData(int var2, String var3, boolean var4) throws SQLServerException {
         this.libraryType = var2;
         this.fedAuthRequiredPreLoginResponse = var4;
         switch (var3.toUpperCase(Locale.ENGLISH).trim()) {
            case "ACTIVEDIRECTORYPASSWORD":
               this.authentication = SqlAuthentication.ActiveDirectoryPassword;
               break;
            case "ACTIVEDIRECTORYINTEGRATED":
               this.authentication = SqlAuthentication.ActiveDirectoryIntegrated;
               break;
            default:
               assert false;
               MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_InvalidConnectionSetting"));
               Object[] var8 = new Object[]{"authentication", var3};
               throw new SQLServerException(null, var7.format(var8), null, 0, false);
         }
      }

      FederatedAuthenticationFeatureExtensionData(int var2, boolean var3, byte[] var4) {
         this.libraryType = var2;
         this.fedAuthRequiredPreLoginResponse = var3;
         this.accessToken = var4;
      }
   }

   private final class LogonCommand extends UninterruptableTDSCommand {
      LogonCommand() {
         super("logon");
      }

      final boolean doExecute() throws SQLServerException {
         SQLServerConnection.this.logon(this);
         return true;
      }
   }

   class SqlFedAuthInfo {
      private String spn;
      private String stsurl;

      public String toString() {
         return "STSURL: " + this.stsurl + ", SPN: " + this.spn;
      }
   }

   final class SqlFedAuthToken {
      private final Date expiresOn;
      private final String accessToken;

      SqlFedAuthToken(String var2, long var3) {
         this.accessToken = var2;
         Date var5 = new Date();
         var5.setTime(var5.getTime() + var3 * 1000L);
         this.expiresOn = var5;
      }

      Date getExpiresOnDate() {
         return this.expiresOn;
      }
   }

   private enum State {
      Initialized,
      Connected,
      Opened,
      Closed;
   }
}
