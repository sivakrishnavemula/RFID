package com.microsoft.sqlserver.jdbc;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SQLServerDatabaseMetaData implements DatabaseMetaData, Serializable {
   private SQLServerConnection connection;
   static final String urlprefix = "jdbc:sqlserver://";
   private static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.SQLServerDatabaseMetaData");
   private static final Logger loggerExternal = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.DatabaseMetaData");
   private static int baseID = 0;
   private final String traceID;
   static final int MAXLOBSIZE = Integer.MAX_VALUE;
   static final int uniqueidentifierSize = 36;
   EnumMap<SQLServerDatabaseMetaData.CallableHandles, SQLServerDatabaseMetaData.HandleAssociation> handleMap = new EnumMap(
      SQLServerDatabaseMetaData.CallableHandles.class
   );
   private static final String ASC_OR_DESC = "ASC_OR_DESC";
   private static final String ATTR_NAME = "ATTR_NAME";
   private static final String ATTR_TYPE_NAME = "ATTR_TYPE_NAME";
   private static final String ATTR_SIZE = "ATTR_SIZE";
   private static final String ATTR_DEF = "ATTR_DEF";
   private static final String BASE_TYPE = "BASE_TYPE";
   private static final String BUFFER_LENGTH = "BUFFER_LENGTH";
   private static final String CARDINALITY = "CARDINALITY";
   private static final String CHAR_OCTET_LENGTH = "CHAR_OCTET_LENGTH";
   private static final String CLASS_NAME = "CLASS_NAME";
   private static final String COLUMN_DEF = "COLUMN_DEF";
   private static final String COLUMN_NAME = "COLUMN_NAME";
   private static final String COLUMN_SIZE = "COLUMN_SIZE";
   private static final String COLUMN_TYPE = "COLUMN_TYPE";
   private static final String DATA_TYPE = "DATA_TYPE";
   private static final String DECIMAL_DIGITS = "DECIMAL_DIGITS";
   private static final String DEFERRABILITY = "DEFERRABILITY";
   private static final String DELETE_RULE = "DELETE_RULE";
   private static final String FILTER_CONDITION = "FILTER_CONDITION";
   private static final String FK_NAME = "FK_NAME";
   private static final String FKCOLUMN_NAME = "FKCOLUMN_NAME";
   private static final String FKTABLE_CAT = "FKTABLE_CAT";
   private static final String FKTABLE_NAME = "FKTABLE_NAME";
   private static final String FKTABLE_SCHEM = "FKTABLE_SCHEM";
   private static final String GRANTEE = "GRANTEE";
   private static final String GRANTOR = "GRANTOR";
   private static final String INDEX_NAME = "INDEX_NAME";
   private static final String INDEX_QUALIFIER = "INDEX_QUALIFIER";
   private static final String IS_GRANTABLE = "IS_GRANTABLE";
   private static final String IS_NULLABLE = "IS_NULLABLE";
   private static final String KEY_SEQ = "KEY_SEQ";
   private static final String LENGTH = "LENGTH";
   private static final String NON_UNIQUE = "NON_UNIQUE";
   private static final String NULLABLE = "NULLABLE";
   private static final String NUM_INPUT_PARAMS = "NUM_INPUT_PARAMS";
   private static final String NUM_OUTPUT_PARAMS = "NUM_OUTPUT_PARAMS";
   private static final String NUM_PREC_RADIX = "NUM_PREC_RADIX";
   private static final String NUM_RESULT_SETS = "NUM_RESULT_SETS";
   private static final String ORDINAL_POSITION = "ORDINAL_POSITION";
   private static final String PAGES = "PAGES";
   private static final String PK_NAME = "PK_NAME";
   private static final String PKCOLUMN_NAME = "PKCOLUMN_NAME";
   private static final String PKTABLE_CAT = "PKTABLE_CAT";
   private static final String PKTABLE_NAME = "PKTABLE_NAME";
   private static final String PKTABLE_SCHEM = "PKTABLE_SCHEM";
   private static final String PRECISION = "PRECISION";
   private static final String PRIVILEGE = "PRIVILEGE";
   private static final String PROCEDURE_CAT = "PROCEDURE_CAT";
   private static final String PROCEDURE_NAME = "PROCEDURE_NAME";
   private static final String PROCEDURE_SCHEM = "PROCEDURE_SCHEM";
   private static final String PROCEDURE_TYPE = "PROCEDURE_TYPE";
   private static final String PSEUDO_COLUMN = "PSEUDO_COLUMN";
   private static final String RADIX = "RADIX";
   private static final String REMARKS = "REMARKS";
   private static final String SCALE = "SCALE";
   private static final String SCOPE = "SCOPE";
   private static final String SCOPE_CATALOG = "SCOPE_CATALOG";
   private static final String SCOPE_SCHEMA = "SCOPE_SCHEMA";
   private static final String SCOPE_TABLE = "SCOPE_TABLE";
   private static final String SOURCE_DATA_TYPE = "SOURCE_DATA_TYPE";
   private static final String SQL_DATA_TYPE = "SQL_DATA_TYPE";
   private static final String SQL_DATETIME_SUB = "SQL_DATETIME_SUB";
   private static final String SS_DATA_TYPE = "SS_DATA_TYPE";
   private static final String SUPERTABLE_NAME = "SUPERTABLE_NAME";
   private static final String SUPERTYPE_CAT = "SUPERTYPE_CAT";
   private static final String SUPERTYPE_NAME = "SUPERTYPE_NAME";
   private static final String SUPERTYPE_SCHEM = "SUPERTYPE_SCHEM";
   private static final String TABLE_CAT = "TABLE_CAT";
   private static final String TABLE_NAME = "TABLE_NAME";
   private static final String TABLE_SCHEM = "TABLE_SCHEM";
   private static final String TABLE_TYPE = "TABLE_TYPE";
   private static final String TYPE = "TYPE";
   private static final String TYPE_CAT = "TYPE_CAT";
   private static final String TYPE_NAME = "TYPE_NAME";
   private static final String TYPE_SCHEM = "TYPE_SCHEM";
   private static final String UPDATE_RULE = "UPDATE_RULE";
   private static final String FUNCTION_CAT = "FUNCTION_CAT";
   private static final String FUNCTION_NAME = "FUNCTION_NAME";
   private static final String FUNCTION_SCHEM = "FUNCTION_SCHEM";
   private static final String FUNCTION_TYPE = "FUNCTION_TYPE";
   private static final String SS_IS_SPARSE = "SS_IS_SPARSE";
   private static final String SS_IS_COLUMN_SET = "SS_IS_COLUMN_SET";
   private static final String SS_IS_COMPUTED = "SS_IS_COMPUTED";
   private static final String IS_AUTOINCREMENT = "IS_AUTOINCREMENT";
   private static final String[] getColumnPrivilegesColumnNames = new String[]{
      "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "GRANTOR", "GRANTEE", "PRIVILEGE", "IS_GRANTABLE"
   };
   private static final String[] getTablesColumnNames = new String[]{"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS"};
   static final char LEFT_BRACKET = '[';
   static final char RIGHT_BRACKET = ']';
   static final char ESCAPE = '\\';
   static final char PERCENT = '%';
   static final char UNDERSCORE = '_';
   static final char[] DOUBLE_RIGHT_BRACKET = new char[]{']', ']'};
   private static final String[] getColumnsColumnNames = new String[]{
      "TABLE_CAT",
      "TABLE_SCHEM",
      "TABLE_NAME",
      "COLUMN_NAME",
      "DATA_TYPE",
      "TYPE_NAME",
      "COLUMN_SIZE",
      "BUFFER_LENGTH",
      "DECIMAL_DIGITS",
      "NUM_PREC_RADIX",
      "NULLABLE",
      "REMARKS",
      "COLUMN_DEF",
      "SQL_DATA_TYPE",
      "SQL_DATETIME_SUB",
      "CHAR_OCTET_LENGTH",
      "ORDINAL_POSITION",
      "IS_NULLABLE"
   };
   private static final String[] getColumnsColumnNamesKatmai = new String[]{
      "TABLE_CAT",
      "TABLE_SCHEM",
      "TABLE_NAME",
      "COLUMN_NAME",
      "DATA_TYPE",
      "TYPE_NAME",
      "COLUMN_SIZE",
      "BUFFER_LENGTH",
      "DECIMAL_DIGITS",
      "NUM_PREC_RADIX",
      "NULLABLE",
      "REMARKS",
      "COLUMN_DEF",
      "SQL_DATA_TYPE",
      "SQL_DATETIME_SUB",
      "CHAR_OCTET_LENGTH",
      "ORDINAL_POSITION",
      "IS_NULLABLE",
      "SS_IS_SPARSE",
      "SS_IS_COLUMN_SET",
      "SS_IS_COMPUTED",
      "IS_AUTOINCREMENT"
   };
   private static final String[] getFunctionsColumnNames = new String[]{
      "FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME", "NUM_INPUT_PARAMS", "NUM_OUTPUT_PARAMS", "NUM_RESULT_SETS", "REMARKS", "FUNCTION_TYPE"
   };
   private static final String[] getFunctionsColumnsColumnNames = new String[]{
      "FUNCTION_CAT",
      "FUNCTION_SCHEM",
      "FUNCTION_NAME",
      "COLUMN_NAME",
      "COLUMN_TYPE",
      "DATA_TYPE",
      "TYPE_NAME",
      "PRECISION",
      "LENGTH",
      "SCALE",
      "RADIX",
      "NULLABLE",
      "REMARKS",
      "COLUMN_DEF",
      "SQL_DATA_TYPE",
      "SQL_DATETIME_SUB",
      "CHAR_OCTET_LENGTH",
      "ORDINAL_POSITION",
      "IS_NULLABLE"
   };
   private static final String[] getBestRowIdentifierColumnNames = new String[]{
      "SCOPE", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "PSEUDO_COLUMN"
   };
   private static final String[] pkfkColumnNames = new String[]{
      "PKTABLE_CAT",
      "PKTABLE_SCHEM",
      "PKTABLE_NAME",
      "PKCOLUMN_NAME",
      "FKTABLE_CAT",
      "FKTABLE_SCHEM",
      "FKTABLE_NAME",
      "FKCOLUMN_NAME",
      "KEY_SEQ",
      "UPDATE_RULE",
      "DELETE_RULE",
      "FK_NAME",
      "PK_NAME",
      "DEFERRABILITY"
   };
   private static final String[] getIndexInfoColumnNames = new String[]{
      "TABLE_CAT",
      "TABLE_SCHEM",
      "TABLE_NAME",
      "NON_UNIQUE",
      "INDEX_QUALIFIER",
      "INDEX_NAME",
      "TYPE",
      "ORDINAL_POSITION",
      "COLUMN_NAME",
      "ASC_OR_DESC",
      "CARDINALITY",
      "PAGES",
      "FILTER_CONDITION"
   };
   private static final String[] getPrimaryKeysColumnNames = new String[]{"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "KEY_SEQ", "PK_NAME"};
   private static final String[] getProcedureColumnsColumnNames = new String[]{
      "PROCEDURE_CAT",
      "PROCEDURE_SCHEM",
      "PROCEDURE_NAME",
      "COLUMN_NAME",
      "COLUMN_TYPE",
      "DATA_TYPE",
      "TYPE_NAME",
      "PRECISION",
      "LENGTH",
      "SCALE",
      "RADIX",
      "NULLABLE",
      "REMARKS",
      "COLUMN_DEF",
      "SQL_DATA_TYPE",
      "SQL_DATETIME_SUB",
      "CHAR_OCTET_LENGTH",
      "ORDINAL_POSITION",
      "IS_NULLABLE"
   };
   private static final String[] getProceduresColumnNames = new String[]{
      "PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME", "NUM_INPUT_PARAMS", "NUM_OUTPUT_PARAMS", "NUM_RESULT_SETS", "REMARKS", "PROCEDURE_TYPE"
   };
   private static final String[] getTablePrivilegesColumnNames = new String[]{
      "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "GRANTOR", "GRANTEE", "PRIVILEGE", "IS_GRANTABLE"
   };
   private static final String[] getVersionColumnsColumnNames = new String[]{
      "SCOPE", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "PSEUDO_COLUMN"
   };

   private static synchronized int nextInstanceID() {
      baseID++;
      return baseID;
   }

   public final String toString() {
      return this.traceID;
   }

   public SQLServerDatabaseMetaData(SQLServerConnection var1) {
      this.traceID = " SQLServerDatabaseMetaData:" + nextInstanceID();
      this.connection = var1;
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(this.toString() + " created by (" + this.connection.toString() + ")");
      }
   }

   public boolean isWrapperFor(Class<?> var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      return false;
   }

   public <T> T unwrap(Class<T> var1) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new SQLFeatureNotSupportedException(SQLServerException.getErrString("R_notSupported"));
   }

   private void checkClosed() throws SQLServerException {
      if (this.connection.isClosed()) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_connectionIsClosed"), "08003", false);
      }
   }

   private final SQLServerResultSet getResultSetFromInternalQueries(String var1, String var2) throws SQLServerException {
      this.checkClosed();
      String var3 = null;
      var3 = this.switchCatalogs(var1);
      SQLServerResultSet var4 = null;

      try {
         var4 = ((SQLServerStatement)this.connection.createStatement()).executeQueryInternal(var2);
      } finally {
         if (null != var3) {
            this.connection.setCatalog(var3);
         }
      }

      return var4;
   }

   private CallableStatement getCallableStatementHandle(SQLServerDatabaseMetaData.CallableHandles var1, String var2) throws SQLServerException {
      CallableStatement var3 = null;
      SQLServerDatabaseMetaData.HandleAssociation var4 = (SQLServerDatabaseMetaData.HandleAssociation)this.handleMap.get(var1);
      if (null == var4 || null == var4.databaseName || !var4.databaseName.equals(var2)) {
         var3 = var1.prepare(this.connection);
         var4 = new SQLServerDatabaseMetaData.HandleAssociation(var2, var3);
         SQLServerDatabaseMetaData.HandleAssociation var5 = (SQLServerDatabaseMetaData.HandleAssociation)this.handleMap.put(var1, var4);
         if (null != var5) {
            var5.close();
         }
      }

      return var4.stmt;
   }

   private final SQLServerResultSet getResultSetFromStoredProc(String var1, SQLServerDatabaseMetaData.CallableHandles var2, String[] var3) throws SQLServerException {
      this.checkClosed();
      assert null != var3;
      String var4 = null;
      var4 = this.switchCatalogs(var1);
      SQLServerResultSet var5 = null;

      try {
         SQLServerCallableStatement var6 = (SQLServerCallableStatement)this.getCallableStatementHandle(var2, var1);

         for (int var7 = 1; var7 <= var3.length; var7++) {
            var6.setString(var7, var3[var7 - 1]);
         }

         var5 = (SQLServerResultSet)var6.executeQueryInternal();
      } finally {
         if (null != var4) {
            this.connection.setCatalog(var4);
         }
      }

      return var5;
   }

   private final SQLServerResultSet getResultSetWithProvidedColumnNames(
      String var1, SQLServerDatabaseMetaData.CallableHandles var2, String[] var3, String[] var4
   ) throws SQLServerException {
      SQLServerResultSet var5 = this.getResultSetFromStoredProc(var1, var2, var3);

      for (int var6 = 0; var6 < var4.length; var6++) {
         var5.setColumnName(1 + var6, var4[var6]);
      }

      return var5;
   }

   private String switchCatalogs(String var1) throws SQLServerException {
      if (var1 == null) {
         return null;
      }

      String var2 = null;
      var2 = this.connection.getCatalog().trim();
      String var3 = var1.trim();
      if (var2.equals(var3)) {
         return null;
      }

      this.connection.setCatalog(var3);
      return var2 != null && var2.length() != 0 ? var2 : null;
   }

   public boolean allProceduresAreCallable() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean allTablesAreSelectable() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return false;
   }

   public boolean dataDefinitionCausesTransactionCommit() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean dataDefinitionIgnoredInTransactions() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean doesMaxRowSizeIncludeBlobs() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean generatedKeyAlwaysReturned() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC41();
      this.checkClosed();
      return true;
   }

   public long getMaxLogicalLobSize() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC42();
      this.checkClosed();
      return 2147483647L;
   }

   public boolean supportsRefCursors() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC42();
      this.checkClosed();
      return false;
   }

   public ResultSet getCatalogs() throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String var1 = "SELECT name AS TABLE_CAT FROM sys.databases order by name";
      return this.getResultSetFromInternalQueries(null, var1);
   }

   public String getCatalogSeparator() throws SQLServerException {
      this.checkClosed();
      return ".";
   }

   public String getCatalogTerm() throws SQLServerException {
      this.checkClosed();
      return "database";
   }

   public ResultSet getColumnPrivileges(String var1, String var2, String var3, String var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      var4 = EscapeIDName(var4);
      String[] var5 = new String[]{var3, var2, var1, var4};
      return this.getResultSetWithProvidedColumnNames(
         var1, SQLServerDatabaseMetaData.CallableHandles.SP_COLUMN_PRIVILEGES, var5, getColumnPrivilegesColumnNames
      );
   }

   public ResultSet getTables(String var1, String var2, String var3, String[] var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      var3 = EscapeIDName(var3);
      var2 = EscapeIDName(var2);
      String[] var5 = new String[]{var3, var2, var1, null};
      String var6 = null;
      if (var4 != null) {
         var6 = "'";

         for (int var7 = 0; var7 < var4.length; var7++) {
            if (var7 > 0) {
               var6 = var6 + ",";
            }

            var6 = var6 + "''" + var4[var7] + "''";
         }

         var6 = var6 + "'";
      }

      var5[3] = var6;
      return this.getResultSetWithProvidedColumnNames(var1, SQLServerDatabaseMetaData.CallableHandles.SP_TABLES, var5, getTablesColumnNames);
   }

   private static String EscapeIDName(String var0) throws SQLServerException {
      if (null == var0) {
         return var0;
      }

      StringBuilder var2 = new StringBuilder(var0.length() + 2);

      for (int var3 = 0; var3 < var0.length(); var3++) {
         char var1 = var0.charAt(var3);
         if ('\\' == var1) {
            if (++var3 < var0.length()) {
               var1 = var0.charAt(var3);
               switch (var1) {
                  case '%':
                  case '[':
                  case '_':
                     var2.append('[');
                     var2.append(var1);
                     var2.append(']');
                     continue;
                  case '\\':
                  case ']':
                     var2.append(var1);
                     continue;
                  default:
                     var2.append('\\');
                     var2.append(var1);
                     continue;
               }
            }
         }

         var2.append(var1);
      }

      return var2.toString();
   }

   public ResultSet getColumns(String var1, String var2, String var3, String var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String var5 = EscapeIDName(var4);
      var3 = EscapeIDName(var3);
      var2 = EscapeIDName(var2);
      String[] var6;
      if (this.connection.isKatmaiOrLater()) {
         var6 = new String[6];
      } else {
         var6 = new String[5];
      }

      var6[0] = var3;
      var6[1] = var2;
      var6[2] = var1;
      var6[3] = var5;
      if (this.connection.isKatmaiOrLater()) {
         var6[4] = "2";
         var6[5] = "3";
      } else {
         var6[4] = "3";
      }

      SQLServerResultSet var7;
      if (this.connection.isKatmaiOrLater()) {
         var7 = this.getResultSetWithProvidedColumnNames(var1, SQLServerDatabaseMetaData.CallableHandles.SP_COLUMNS, var6, getColumnsColumnNamesKatmai);
      } else {
         var7 = this.getResultSetWithProvidedColumnNames(var1, SQLServerDatabaseMetaData.CallableHandles.SP_COLUMNS, var6, getColumnsColumnNames);
      }

      var7.getColumn(5).setFilter(new DataTypeFilter());
      if (this.connection.isKatmaiOrLater()) {
         var7.getColumn(22).setFilter(new IntColumnIdentityFilter());
         var7.getColumn(7).setFilter(new ZeroFixupFilter());
         var7.getColumn(8).setFilter(new ZeroFixupFilter());
         var7.getColumn(16).setFilter(new ZeroFixupFilter());
      }

      return var7;
   }

   public ResultSet getFunctions(String var1, String var2, String var3) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      if (var1 != null && var1.length() == 0) {
         MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_invalidArgument"));
         Object[] var5 = new Object[]{"catalog"};
         SQLServerException.makeFromDriverError(null, null, var4.format(var5), null, false);
      }

      String[] var6 = new String[]{EscapeIDName(var3), EscapeIDName(var2), var1};
      return this.getResultSetWithProvidedColumnNames(var1, SQLServerDatabaseMetaData.CallableHandles.SP_STORED_PROCEDURES, var6, getFunctionsColumnNames);
   }

   public ResultSet getFunctionColumns(String var1, String var2, String var3, String var4) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      if (var1 != null && var1.length() == 0) {
         MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_invalidArgument"));
         Object[] var6 = new Object[]{"catalog"};
         SQLServerException.makeFromDriverError(null, null, var5.format(var6), null, false);
      }

      String[] var7 = new String[]{EscapeIDName(var3), EscapeIDName(var2), var1, EscapeIDName(var4), "3"};
      SQLServerResultSet var8 = this.getResultSetWithProvidedColumnNames(
         var1, SQLServerDatabaseMetaData.CallableHandles.SP_SPROC_COLUMNS, var7, getFunctionsColumnsColumnNames
      );
      var8.getColumn(6).setFilter(new DataTypeFilter());
      if (this.connection.isKatmaiOrLater()) {
         var8.getColumn(8).setFilter(new ZeroFixupFilter());
         var8.getColumn(9).setFilter(new ZeroFixupFilter());
         var8.getColumn(17).setFilter(new ZeroFixupFilter());
      }

      return var8;
   }

   public ResultSet getClientInfoProperties() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return this.getResultSetFromInternalQueries(
         null,
         "SELECT cast(NULL as char(1)) as NAME, cast(0 as int) as MAX_LEN, cast(NULL as char(1)) as DEFAULT_VALUE, cast(NULL as char(1)) as DESCRIPTION  where 0 = 1"
      );
   }

   public ResultSet getBestRowIdentifier(String var1, String var2, String var3, int var4, boolean var5) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String[] var6 = new String[]{var3, var2, var1, "R", null, null, null};
      if (0 == var4) {
         var6[4] = "C";
      } else {
         var6[4] = "T";
      }

      if (var5) {
         var6[5] = "U";
      } else {
         var6[5] = "O";
      }

      var6[6] = "3";
      SQLServerResultSet var7 = this.getResultSetWithProvidedColumnNames(
         var1, SQLServerDatabaseMetaData.CallableHandles.SP_SPECIAL_COLUMNS, var6, getBestRowIdentifierColumnNames
      );
      var7.getColumn(3).setFilter(new DataTypeFilter());
      return var7;
   }

   public ResultSet getCrossReference(String var1, String var2, String var3, String var4, String var5, String var6) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String[] var7 = new String[]{var3, var2, var1, var6, var5, var4};
      return this.getResultSetWithProvidedColumnNames(null, SQLServerDatabaseMetaData.CallableHandles.SP_FKEYS, var7, pkfkColumnNames);
   }

   public String getDatabaseProductName() throws SQLServerException {
      this.checkClosed();
      return "Microsoft SQL Server";
   }

   public String getDatabaseProductVersion() throws SQLServerException {
      this.checkClosed();
      return this.connection.sqlServerVersion;
   }

   public int getDefaultTransactionIsolation() throws SQLServerException {
      this.checkClosed();
      return 2;
   }

   public int getDriverMajorVersion() {
      return 6;
   }

   public int getDriverMinorVersion() {
      return 0;
   }

   public String getDriverName() throws SQLServerException {
      this.checkClosed();
      return "Microsoft JDBC Driver 6.0 for SQL Server";
   }

   public String getDriverVersion() throws SQLServerException {
      int var1 = this.getDriverMinorVersion();
      String var2 = this.getDriverMajorVersion() + ".";
      var2 = var2 + "" + var1;
      var2 = var2 + ".";
      var2 = var2 + 8112;
      var2 = var2 + ".";
      return var2 + 200;
   }

   public ResultSet getExportedKeys(String var1, String var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String[] var4 = new String[]{var3, var2, var1, null, null, null};
      return this.getResultSetWithProvidedColumnNames(var1, SQLServerDatabaseMetaData.CallableHandles.SP_FKEYS, var4, pkfkColumnNames);
   }

   public String getExtraNameCharacters() throws SQLServerException {
      this.checkClosed();
      return "$#@";
   }

   public String getIdentifierQuoteString() throws SQLServerException {
      this.checkClosed();
      return "\"";
   }

   public ResultSet getImportedKeys(String var1, String var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String[] var4 = new String[]{null, null, null, var3, var2, var1};
      return this.getResultSetWithProvidedColumnNames(var1, SQLServerDatabaseMetaData.CallableHandles.SP_FKEYS, var4, pkfkColumnNames);
   }

   public ResultSet getIndexInfo(String var1, String var2, String var3, boolean var4, boolean var5) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String[] var6 = new String[]{var3, var2, var1, "%", null, null};
      if (var4) {
         var6[4] = "Y";
      } else {
         var6[4] = "N";
      }

      if (var5) {
         var6[5] = "Q";
      } else {
         var6[5] = "E";
      }

      return this.getResultSetWithProvidedColumnNames(var1, SQLServerDatabaseMetaData.CallableHandles.SP_STATISTICS, var6, getIndexInfoColumnNames);
   }

   public int getMaxBinaryLiteralLength() throws SQLServerException {
      this.checkClosed();
      return 0;
   }

   public int getMaxCatalogNameLength() throws SQLServerException {
      this.checkClosed();
      return 128;
   }

   public int getMaxCharLiteralLength() throws SQLServerException {
      this.checkClosed();
      return 0;
   }

   public int getMaxColumnNameLength() throws SQLServerException {
      this.checkClosed();
      return 128;
   }

   public int getMaxColumnsInGroupBy() throws SQLServerException {
      this.checkClosed();
      return 0;
   }

   public int getMaxColumnsInIndex() throws SQLServerException {
      this.checkClosed();
      return 16;
   }

   public int getMaxColumnsInOrderBy() throws SQLServerException {
      this.checkClosed();
      return 0;
   }

   public int getMaxColumnsInSelect() throws SQLServerException {
      this.checkClosed();
      return 4096;
   }

   public int getMaxColumnsInTable() throws SQLServerException {
      this.checkClosed();
      return 1024;
   }

   public int getMaxConnections() throws SQLServerException {
      this.checkClosed();

      try {
         String var1 = "sp_configure 'user connections'";
         SQLServerResultSet var2 = this.getResultSetFromInternalQueries(null, var1);
         return !var2.next() ? 0 : var2.getInt("maximum");
      } catch (SQLServerException var3) {
         return 0;
      }
   }

   public int getMaxCursorNameLength() throws SQLServerException {
      this.checkClosed();
      return 0;
   }

   public int getMaxIndexLength() throws SQLServerException {
      this.checkClosed();
      return 900;
   }

   public int getMaxProcedureNameLength() throws SQLServerException {
      this.checkClosed();
      return 128;
   }

   public int getMaxRowSize() throws SQLServerException {
      this.checkClosed();
      return 8060;
   }

   public int getMaxSchemaNameLength() throws SQLServerException {
      this.checkClosed();
      return 128;
   }

   public int getMaxStatementLength() throws SQLServerException {
      this.checkClosed();
      return 65536 * this.connection.getTDSPacketSize();
   }

   public int getMaxStatements() throws SQLServerException {
      this.checkClosed();
      return 0;
   }

   public int getMaxTableNameLength() throws SQLServerException {
      this.checkClosed();
      return 128;
   }

   public int getMaxTablesInSelect() throws SQLServerException {
      this.checkClosed();
      return 256;
   }

   public int getMaxUserNameLength() throws SQLServerException {
      this.checkClosed();
      return 128;
   }

   public String getNumericFunctions() throws SQLServerException {
      this.checkClosed();
      return "ABS,ACOS,ASIN,ATAN,ATAN2,CEILING,COS,COT,DEGREES,EXP, FLOOR,LOG,LOG10,MOD,PI,POWER,RADIANS,RAND,ROUND,SIGN,SIN,SQRT,TAN,TRUNCATE";
   }

   public ResultSet getPrimaryKeys(String var1, String var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String[] var4 = new String[]{var3, var2, var1};
      return this.getResultSetWithProvidedColumnNames(var1, SQLServerDatabaseMetaData.CallableHandles.SP_PKEYS, var4, getPrimaryKeysColumnNames);
   }

   public ResultSet getProcedureColumns(String var1, String var2, String var3, String var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String[] var5 = new String[5];
      var3 = EscapeIDName(var3);
      var5[0] = var3;
      var5[1] = var2;
      var5[2] = var1;
      var4 = EscapeIDName(var4);
      var5[3] = var4;
      var5[4] = "3";
      SQLServerResultSet var6 = this.getResultSetWithProvidedColumnNames(
         var1, SQLServerDatabaseMetaData.CallableHandles.SP_SPROC_COLUMNS, var5, getProcedureColumnsColumnNames
      );
      var6.getColumn(6).setFilter(new DataTypeFilter());
      if (this.connection.isKatmaiOrLater()) {
         var6.getColumn(8).setFilter(new ZeroFixupFilter());
         var6.getColumn(9).setFilter(new ZeroFixupFilter());
         var6.getColumn(17).setFilter(new ZeroFixupFilter());
      }

      return var6;
   }

   public ResultSet getProcedures(String var1, String var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String[] var4 = new String[]{EscapeIDName(var3), var2, var1};
      return this.getResultSetWithProvidedColumnNames(var1, SQLServerDatabaseMetaData.CallableHandles.SP_STORED_PROCEDURES, var4, getProceduresColumnNames);
   }

   public String getProcedureTerm() throws SQLServerException {
      this.checkClosed();
      return "stored procedure";
   }

   public ResultSet getPseudoColumns(String var1, String var2, String var3, String var4) throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC41();
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      return this.getResultSetFromInternalQueries(
         var1,
         "SELECT cast(NULL as char(1)) as TABLE_CAT, cast(NULL as char(1)) as TABLE_SCHEM, cast(NULL as char(1)) as TABLE_NAME, cast(NULL as char(1)) as COLUMN_NAME, cast(0 as int) as DATA_TYPE, cast(0 as int) as COLUMN_SIZE, cast(0 as int) as DECIMAL_DIGITS, cast(0 as int) as NUM_PREC_RADIX, cast(NULL as char(1)) as COLUMN_USAGE, cast(NULL as char(1)) as REMARKS, cast(0 as int) as CHAR_OCTET_LENGTH, cast(NULL as char(1)) as IS_NULLABLE where 0 = 1"
      );
   }

   public ResultSet getSchemas() throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      return this.getSchemasInternal(null, null);
   }

   private ResultSet getSchemasInternal(String var1, String var2) throws SQLServerException {
      String var4 = " ('dbo', 'guest','INFORMATION_SCHEMA','sys','db_owner', 'db_accessadmin', 'db_securityadmin', 'db_ddladmin'  ,'db_backupoperator','db_datareader','db_datawriter','db_denydatareader','db_denydatawriter') ";
      String var5 = "sys.schemas";
      String var6 = "sys.schemas.name";
      if (null != var1 && var1.length() != 0) {
         var5 = var1 + "." + var5;
         var6 = var1 + "." + var6;
      }

      String var3 = "select " + var6 + " 'TABLE_SCHEM',";
      if (null != var1 && var1.length() == 0) {
         var3 = var3 + "null 'TABLE_CATALOG' ";
      } else {
         var3 = var3 + " CASE WHEN " + var6 + "  IN " + var4 + " THEN null ELSE ";
         if (null != var1 && var1.length() != 0) {
            var3 = var3 + "'" + var1 + "' ";
         } else {
            var3 = var3 + " DB_NAME() ";
         }

         var3 = var3 + " END 'TABLE_CATALOG' ";
      }

      var3 = var3 + "   from " + var5;
      if (null != var1 && var1.length() == 0) {
         if (null != var2) {
            var3 = var3 + " where " + var6 + " like ?  and ";
         } else {
            var3 = var3 + " where ";
         }

         var3 = var3 + var6 + " in " + var4;
      } else if (null != var2) {
         var3 = var3 + " where " + var6 + " like ?  ";
      }

      var3 = var3 + " order by 2, 1";
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(this.toString() + " schema query (" + var3 + ")");
      }

      SQLServerResultSet var7;
      if (null == var2) {
         var1 = null;
         var7 = this.getResultSetFromInternalQueries(var1, var3);
      } else {
         SQLServerPreparedStatement var8 = (SQLServerPreparedStatement)this.connection.prepareStatement(var3);
         var8.setString(1, var2);
         var7 = (SQLServerResultSet)var8.executeQueryInternal();
      }

      return var7;
   }

   public ResultSet getSchemas(String var1, String var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      DriverJDBCVersion.checkSupportsJDBC4();
      return this.getSchemasInternal(var1, var2);
   }

   public String getSchemaTerm() throws SQLServerException {
      this.checkClosed();
      return "schema";
   }

   public String getSearchStringEscape() throws SQLServerException {
      this.checkClosed();
      return "\\";
   }

   public String getSQLKeywords() throws SQLServerException {
      this.checkClosed();
      return "BACKUP,BREAK,BROWSE,BULK,CHECKPOINT,CLUSTERED,COMPUTE,CONTAINS,CONTAINSTABLE,DATABASE,DBCC,DENY,DISK,DISTRIBUTED,DUMMY,DUMP,ERRLVL,EXIT,FILE,FILLFACTOR,FREETEXT,FREETEXTTABLE,FUNCTION,HOLDLOCK,IDENTITY_INSERT,IDENTITYCOL,IF,KILL,LINENO,LOAD,NOCHECK,NONCLUSTERED,OFF,OFFSETS,OPENDATASOURCE,OPENQUERY,OPENROWSET,OPENXML,OVER,PERCENT,PLAN,PRINT,PROC,RAISERROR,READTEXT,RECONFIGURE,REPLICATION,RESTORE,RETURN,ROWCOUNT,ROWGUIDCOL,RULE,SAVE,SETUSER,SHUTDOWN,STATISTICS,TEXTSIZE,TOP,TRAN,TRIGGER,TRUNCATE,TSEQUAL,UPDATETEXT,USE,WAITFOR,WHILE,WRITETEXT";
   }

   public String getStringFunctions() throws SQLServerException {
      this.checkClosed();
      return "ASCII,CHAR,CONCAT, DIFFERENCE,INSERT,LCASE,LEFT,LENGTH,LOCATE,LTRIM,REPEAT,REPLACE,RIGHT,RTRIM,SOUNDEX,SPACE,SUBSTRING,UCASE";
   }

   public String getSystemFunctions() throws SQLServerException {
      this.checkClosed();
      return "DATABASE,IFNULL,USER";
   }

   public ResultSet getTablePrivileges(String var1, String var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      var3 = EscapeIDName(var3);
      var2 = EscapeIDName(var2);
      String[] var4 = new String[]{var3, var2, var1};
      return this.getResultSetWithProvidedColumnNames(var1, SQLServerDatabaseMetaData.CallableHandles.SP_TABLE_PRIVILEGES, var4, getTablePrivilegesColumnNames);
   }

   public ResultSet getTableTypes() throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String var1 = "SELECT 'VIEW' 'TABLE_TYPE' UNION SELECT 'TABLE' UNION SELECT 'SYSTEM TABLE'";
      return this.getResultSetFromInternalQueries(null, var1);
   }

   public String getTimeDateFunctions() throws SQLServerException {
      this.checkClosed();
      return "CURDATE,CURTIME,DAYNAME,DAYOFMONTH,DAYOFWEEK,DAYOFYEAR,HOUR,MINUTE,MONTH,MONTHNAME,NOW,QUARTER,SECOND,TIMESTAMPADD,TIMESTAMPDIFF,WEEK,YEAR";
   }

   public ResultSet getTypeInfo() throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      SQLServerResultSet var1;
      if (this.connection.isKatmaiOrLater()) {
         var1 = this.getResultSetFromInternalQueries(null, "sp_datatype_info_100 @ODBCVer=3");
      } else {
         var1 = this.getResultSetFromInternalQueries(null, "sp_datatype_info @ODBCVer=3");
      }

      var1.setColumnName(11, "FIXED_PREC_SCALE");
      var1.getColumn(2).setFilter(new DataTypeFilter());
      return var1;
   }

   public String getURL() throws SQLServerException {
      this.checkClosed();
      StringBuilder var1 = new StringBuilder();
      Properties var2 = this.connection.activeConnectionProperties;
      DriverPropertyInfo[] var3 = SQLServerDriver.getPropertyInfoFromProperties(var2);
      String var4 = null;
      String var5 = null;
      String var6 = null;
      int var7 = var3.length;

      while (--var7 >= 0) {
         String var8 = var3[var7].name;
         if (!var8.equals(SQLServerDriverBooleanProperty.INTEGRATED_SECURITY.toString())
            && !var8.equals(SQLServerDriverStringProperty.USER.toString())
            && !var8.equals(SQLServerDriverStringProperty.PASSWORD.toString())
            && !var8.equals(SQLServerDriverStringProperty.KEY_STORE_SECRET.toString())) {
            String var9 = var3[var7].value;
            if (0 != var9.length()) {
               if (var8.equals(SQLServerDriverStringProperty.SERVER_NAME.toString())) {
                  var4 = var9;
               } else if (var8.equals(SQLServerDriverStringProperty.INSTANCE_NAME.toString())) {
                  var6 = var9;
               } else if (var8.equals(SQLServerDriverIntProperty.PORT_NUMBER.toString())) {
                  var5 = var9;
               } else {
                  var1.append(var8);
                  var1.append("=");
                  var1.append(var9);
                  var1.append(";");
               }
            }
         }
      }

      var1.insert(0, ";");
      var1.insert(0, var5);
      var1.insert(0, ":");
      if (null != var6) {
         var1.insert(0, var6);
         var1.insert(0, "\\");
      }

      var1.insert(0, var4);
      var1.insert(0, "jdbc:sqlserver://");
      return var1.toString();
   }

   public String getUserName() throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      SQLServerStatement var1 = null;
      SQLServerResultSet var2 = null;
      String var3 = "";

      try {
         var1 = (SQLServerStatement)this.connection.createStatement();
         var2 = var1.executeQueryInternal("select system_user");
         boolean var4 = var2.next();
         assert var4;
         var3 = var2.getString(1);
      } finally {
         if (var2 != null) {
            var2.close();
         }

         if (var1 != null) {
            var1.close();
         }
      }

      return var3;
   }

   public ResultSet getVersionColumns(String var1, String var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      String[] var4 = new String[]{var3, var2, var1, "V", "T", "U", "3"};
      SQLServerResultSet var5 = this.getResultSetWithProvidedColumnNames(
         var1, SQLServerDatabaseMetaData.CallableHandles.SP_SPECIAL_COLUMNS, var4, getVersionColumnsColumnNames
      );
      var5.getColumn(3).setFilter(new DataTypeFilter());
      return var5;
   }

   public boolean isCatalogAtStart() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean isReadOnly() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean nullPlusNonNullIsNull() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean nullsAreSortedAtEnd() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean nullsAreSortedAtStart() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean nullsAreSortedHigh() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean nullsAreSortedLow() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean storesLowerCaseIdentifiers() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean storesLowerCaseQuotedIdentifiers() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean storesMixedCaseIdentifiers() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean storesMixedCaseQuotedIdentifiers() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean storesUpperCaseIdentifiers() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean storesUpperCaseQuotedIdentifiers() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsAlterTableWithAddColumn() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsAlterTableWithDropColumn() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsANSI92EntryLevelSQL() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsANSI92FullSQL() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsANSI92IntermediateSQL() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsCatalogsInDataManipulation() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsCatalogsInIndexDefinitions() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsCatalogsInProcedureCalls() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsCatalogsInTableDefinitions() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsColumnAliasing() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsConvert() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsConvert(int var1, int var2) throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsCoreSQLGrammar() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsCorrelatedSubqueries() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsDataManipulationTransactionsOnly() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsDifferentTableCorrelationNames() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsExpressionsInOrderBy() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsExtendedSQLGrammar() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsFullOuterJoins() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsGroupBy() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsGroupByBeyondSelect() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsGroupByUnrelated() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsIntegrityEnhancementFacility() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsLikeEscapeClause() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsLimitedOuterJoins() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsMinimumSQLGrammar() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsMixedCaseIdentifiers() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsMixedCaseQuotedIdentifiers() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsMultipleResultSets() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsMultipleTransactions() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsNonNullableColumns() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsOpenCursorsAcrossCommit() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsOpenCursorsAcrossRollback() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsOpenStatementsAcrossCommit() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsOpenStatementsAcrossRollback() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsOrderByUnrelated() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsOuterJoins() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsPositionedDelete() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsPositionedUpdate() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsSchemasInDataManipulation() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsSchemasInIndexDefinitions() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsSchemasInPrivilegeDefinitions() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsSchemasInProcedureCalls() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsSchemasInTableDefinitions() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsSelectForUpdate() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsStoredProcedures() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsSubqueriesInComparisons() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsSubqueriesInExists() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsSubqueriesInIns() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsSubqueriesInQuantifieds() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsTableCorrelationNames() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsTransactionIsolationLevel(int var1) throws SQLServerException {
      this.checkClosed();
      switch (var1) {
         case 1:
         case 2:
         case 4:
         case 8:
         case 4096:
            return true;
         default:
            return false;
      }
   }

   public boolean supportsTransactions() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsUnion() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsUnionAll() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean usesLocalFilePerTable() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean usesLocalFiles() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsResultSetType(int var1) throws SQLServerException {
      this.checkClosed();
      this.checkResultType(var1);
      switch (var1) {
         case 1003:
         case 1004:
         case 1005:
         case 1006:
         case 2003:
         case 2004:
            return true;
         default:
            return false;
      }
   }

   public boolean supportsResultSetConcurrency(int var1, int var2) throws SQLServerException {
      this.checkClosed();
      this.checkResultType(var1);
      this.checkConcurrencyType(var2);
      switch (var1) {
         case 1003:
         case 1005:
         case 1006:
         case 2004:
            return true;
         case 1004:
         case 2003:
            if (1007 == var2) {
               return true;
            }

            return false;
         default:
            return false;
      }
   }

   public boolean ownUpdatesAreVisible(int var1) throws SQLServerException {
      this.checkClosed();
      this.checkResultType(var1);
      return var1 == 1006 || 1003 == var1 || 1005 == var1 || 1005 == var1 || 2004 == var1;
   }

   public boolean ownDeletesAreVisible(int var1) throws SQLServerException {
      this.checkClosed();
      this.checkResultType(var1);
      return var1 == 1006 || 1003 == var1 || 1005 == var1 || 1005 == var1 || 2004 == var1;
   }

   public boolean ownInsertsAreVisible(int var1) throws SQLServerException {
      this.checkClosed();
      this.checkResultType(var1);
      return var1 == 1006 || 1003 == var1 || 1005 == var1 || 1005 == var1 || 2004 == var1;
   }

   public boolean othersUpdatesAreVisible(int var1) throws SQLServerException {
      this.checkClosed();
      this.checkResultType(var1);
      return var1 == 1006 || 1003 == var1 || 1005 == var1 || 1005 == var1 || 2004 == var1;
   }

   public boolean othersDeletesAreVisible(int var1) throws SQLServerException {
      this.checkClosed();
      this.checkResultType(var1);
      return var1 == 1006 || 1003 == var1 || 1005 == var1 || 1005 == var1 || 2004 == var1;
   }

   public boolean othersInsertsAreVisible(int var1) throws SQLServerException {
      this.checkClosed();
      this.checkResultType(var1);
      return var1 == 1006 || 1003 == var1 || 2004 == var1;
   }

   public boolean updatesAreDetected(int var1) throws SQLServerException {
      this.checkClosed();
      this.checkResultType(var1);
      return false;
   }

   public boolean deletesAreDetected(int var1) throws SQLServerException {
      this.checkClosed();
      this.checkResultType(var1);
      return 1005 == var1;
   }

   private void checkResultType(int var1) throws SQLServerException {
      switch (var1) {
         case 1003:
         case 1004:
         case 1005:
         case 1006:
         case 2003:
         case 2004:
            return;
         default:
            MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidArgument"));
            Object[] var3 = new Object[]{new Integer(var1)};
            throw new SQLServerException(null, var2.format(var3), null, 0, true);
      }
   }

   private void checkConcurrencyType(int var1) throws SQLServerException {
      switch (var1) {
         case 1007:
         case 1008:
         case 1009:
         case 1010:
            return;
         default:
            MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidArgument"));
            Object[] var3 = new Object[]{new Integer(var1)};
            throw new SQLServerException(null, var2.format(var3), null, 0, true);
      }
   }

   public boolean insertsAreDetected(int var1) throws SQLServerException {
      this.checkClosed();
      this.checkResultType(var1);
      return false;
   }

   public boolean supportsBatchUpdates() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public ResultSet getUDTs(String var1, String var2, String var3, int[] var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      return this.getResultSetFromInternalQueries(
         var1,
         "SELECT cast(NULL as char(1)) as TYPE_CAT, cast(NULL as char(1)) as TYPE_SCHEM, cast(NULL as char(1)) as TYPE_NAME, cast(NULL as char(1)) as CLASS_NAME, cast(0 as int) as DATA_TYPE, cast(NULL as char(1)) as REMARKS, cast(0 as smallint) as BASE_TYPE where 0 = 1"
      );
   }

   public Connection getConnection() throws SQLServerException {
      this.checkClosed();
      return this.connection.getConnection();
   }

   public int getSQLStateType() throws SQLServerException {
      this.checkClosed();
      return this.connection != null && this.connection.xopenStates ? 1 : 2;
   }

   public int getDatabaseMajorVersion() throws SQLServerException {
      this.checkClosed();
      String var1 = this.connection.sqlServerVersion;
      int var2 = var1.indexOf(46);
      if (var2 > 0) {
         var1 = var1.substring(0, var2);
      }

      try {
         return new Integer(var1);
      } catch (NumberFormatException var4) {
         return 0;
      }
   }

   public int getDatabaseMinorVersion() throws SQLServerException {
      this.checkClosed();
      String var1 = this.connection.sqlServerVersion;
      int var2 = var1.indexOf(46);
      int var3 = var1.indexOf(46, var2 + 1);
      if (var2 > 0 && var3 > 0) {
         var1 = var1.substring(var2 + 1, var3);
      }

      try {
         return new Integer(var1);
      } catch (NumberFormatException var5) {
         return 0;
      }
   }

   public int getJDBCMajorVersion() throws SQLServerException {
      this.checkClosed();
      return 4;
   }

   public int getJDBCMinorVersion() throws SQLServerException {
      this.checkClosed();
      return 2;
   }

   public int getResultSetHoldability() throws SQLServerException {
      this.checkClosed();
      return 1;
   }

   public RowIdLifetime getRowIdLifetime() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return RowIdLifetime.ROWID_UNSUPPORTED;
   }

   public boolean supportsResultSetHoldability(int var1) throws SQLServerException {
      this.checkClosed();
      if (1 != var1 && 2 != var1) {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidArgument"));
         Object[] var3 = new Object[]{new Integer(var1)};
         throw new SQLServerException(null, var2.format(var3), null, 0, true);
      } else {
         return true;
      }
   }

   public ResultSet getAttributes(String var1, String var2, String var3, String var4) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      return this.getResultSetFromInternalQueries(
         var1,
         "SELECT cast(NULL as char(1)) as TYPE_CAT, cast(NULL as char(1)) as TYPE_SCHEM, cast(NULL as char(1)) as TYPE_NAME, cast(NULL as char(1)) as ATTR_NAME, cast(0 as int) as DATA_TYPE, cast(NULL as char(1)) as ATTR_TYPE_NAME, cast(0 as int) as ATTR_SIZE, cast(0 as int) as DECIMAL_DIGITS, cast(0 as int) as NUM_PREC_RADIX, cast(0 as int) as NULLABLE, cast(NULL as char(1)) as REMARKS, cast(NULL as char(1)) as ATTR_DEF, cast(0 as int) as SQL_DATA_TYPE, cast(0 as int) as SQL_DATETIME_SUB, cast(0 as int) as CHAR_OCTET_LENGTH, cast(0 as int) as ORDINAL_POSITION, cast(NULL as char(1)) as IS_NULLABLE, cast(NULL as char(1)) as SCOPE_CATALOG, cast(NULL as char(1)) as SCOPE_SCHEMA, cast(NULL as char(1)) as SCOPE_TABLE, cast(0 as smallint) as SOURCE_DATA_TYPE where 0 = 1"
      );
   }

   public ResultSet getSuperTables(String var1, String var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      return this.getResultSetFromInternalQueries(
         var1,
         "SELECT cast(NULL as char(1)) as TYPE_CAT, cast(NULL as char(1)) as TYPE_SCHEM, cast(NULL as char(1)) as TYPE_NAME, cast(NULL as char(1)) as SUPERTABLE_NAME where 0 = 1"
      );
   }

   public ResultSet getSuperTypes(String var1, String var2, String var3) throws SQLServerException {
      if (loggerExternal.isLoggable(Level.FINER) && Util.IsActivityTraceOn()) {
         loggerExternal.finer(this.toString() + " ActivityId: " + ActivityCorrelator.getNext().toString());
      }

      this.checkClosed();
      return this.getResultSetFromInternalQueries(
         var1,
         "SELECT cast(NULL as char(1)) as TYPE_CAT, cast(NULL as char(1)) as TYPE_SCHEM, cast(NULL as char(1)) as TYPE_NAME, cast(NULL as char(1)) as SUPERTYPE_CAT, cast(NULL as char(1)) as SUPERTYPE_SCHEM, cast(NULL as char(1)) as SUPERTYPE_NAME where 0 = 1"
      );
   }

   public boolean supportsGetGeneratedKeys() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsMultipleOpenResults() throws SQLServerException {
      this.checkClosed();
      return false;
   }

   public boolean supportsNamedParameters() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsSavepoints() throws SQLServerException {
      this.checkClosed();
      return true;
   }

   public boolean supportsStatementPooling() throws SQLException {
      this.checkClosed();
      return false;
   }

   public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
      DriverJDBCVersion.checkSupportsJDBC4();
      this.checkClosed();
      return true;
   }

   public boolean locatorsUpdateCopy() throws SQLException {
      this.checkClosed();
      return true;
   }

   enum CallableHandles {
      SP_COLUMNS("{ call sp_columns(?, ?, ?, ?, ?) }", "{ call sp_columns_100(?, ?, ?, ?, ?, ?) }"),
      SP_COLUMN_PRIVILEGES("{ call sp_column_privileges(?, ?, ?, ?)}", "{ call sp_column_privileges(?, ?, ?, ?)}"),
      SP_TABLES("{ call sp_tables(?, ?, ?, ?) }", "{ call sp_tables(?, ?, ?, ?) }"),
      SP_SPECIAL_COLUMNS("{ call sp_special_columns (?, ?, ?, ?, ?, ?, ?)}", "{ call sp_special_columns_100 (?, ?, ?, ?, ?, ?, ?)}"),
      SP_FKEYS("{ call sp_fkeys (?, ?, ?, ? , ? ,?)}", "{ call sp_fkeys (?, ?, ?, ? , ? ,?)}"),
      SP_STATISTICS("{ call sp_statistics(?,?,?,?,?, ?) }", "{ call sp_statistics_100(?,?,?,?,?, ?) }"),
      SP_SPROC_COLUMNS("{ call sp_sproc_columns(?, ?, ?,?,?) }", "{ call sp_sproc_columns_100(?, ?, ?,?,?) }"),
      SP_STORED_PROCEDURES("{call sp_stored_procedures(?, ?, ?) }", "{call sp_stored_procedures(?, ?, ?) }"),
      SP_TABLE_PRIVILEGES("{call sp_table_privileges(?,?,?) }", "{call sp_table_privileges(?,?,?) }"),
      SP_PKEYS("{ call sp_pkeys (?, ?, ?)}", "{ call sp_pkeys (?, ?, ?)}");

      private final String preKatProc;
      private final String katProc;

      CallableHandles(String var3, String var4) {
         this.preKatProc = var3;
         this.katProc = var4;
      }

      CallableStatement prepare(SQLServerConnection var1) throws SQLServerException {
         return var1.prepareCall(var1.isKatmaiOrLater() ? this.katProc : this.preKatProc);
      }
   }

   final class HandleAssociation {
      final String databaseName;
      final CallableStatement stmt;

      HandleAssociation(String var2, CallableStatement var3) {
         this.databaseName = var2;
         this.stmt = var3;
      }

      final void close() throws SQLServerException {
         ((SQLServerCallableStatement)this.stmt).close();
      }
   }
}
