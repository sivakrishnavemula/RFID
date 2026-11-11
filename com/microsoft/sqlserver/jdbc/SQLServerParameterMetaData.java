package com.microsoft.sqlserver.jdbc;

import com.microsoft.sqlserver.jdbc.SSType.Category;
import java.sql.ParameterMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SQLServerParameterMetaData implements ParameterMetaData {
   private static final int SQL_SERVER_2012_VERSION = 11;
   private final SQLServerStatement stmtParent;
   private SQLServerConnection con;
   private Statement stmtCall;
   private SQLServerResultSet rsProcedureMeta;
   private static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.SQLServerParameterMetaData");
   private static int baseID = 0;
   private final String traceID = " SQLServerParameterMetaData:" + nextInstanceID();
   Map<Integer, SQLServerParameterMetaData.QueryMeta> queryMetaMap = null;

   private static synchronized int nextInstanceID() {
      baseID++;
      return baseID;
   }

   public final String toString() {
      return this.traceID;
   }

   private String parseColumns(String var1, String var2) {
      StringTokenizer var3 = new StringTokenizer(var1, " =?<>!", true);
      byte var7 = 0;
      String var8 = null;
      StringBuilder var9 = new StringBuilder();

      while (var3.hasMoreTokens()) {
         String var10 = var3.nextToken();
         if (var10.equalsIgnoreCase(var2)) {
            var7 = 1;
         } else if (var7 != 0) {
            if (var10.charAt(0) == '='
               || var10.equalsIgnoreCase("is")
               || var10.charAt(0) == '<'
               || var10.charAt(0) == '>'
               || var10.equalsIgnoreCase("like")
               || var10.equalsIgnoreCase("not")
               || var10.equalsIgnoreCase("in")
               || var10.charAt(0) == '!') {
               var7 = 2;
            } else if (var10.charAt(0) == '?' && var8 != null) {
               if (var9.length() != 0) {
                  var9.append(", ");
               }

               var9.append(var8);
               var7 = 1;
               var8 = null;
            } else if (var7 == 1 && !var10.equals(" ")) {
               String var11 = this.escapeParse(var3, var10);
               if (var11.length() > 0) {
                  var8 = var11;
               }
            }
         }
      }

      return var9.toString();
   }

   private String parseInsertColumns(String var1, String var2) {
      StringTokenizer var3 = new StringTokenizer(var1, " (),", true);
      byte var4 = 0;
      String var5 = null;
      StringBuilder var6 = new StringBuilder();

      while (var3.hasMoreTokens()) {
         String var7 = var3.nextToken();
         if (var7.equalsIgnoreCase(var2)) {
            var4 = 1;
         } else if (var4 != 0) {
            if (var7.charAt(0) == '=') {
               var4 = 2;
            } else {
               if ((var7.charAt(0) == ',' || var7.charAt(0) == ')' || var7.charAt(0) == ' ') && var5 != null) {
                  if (var6.length() != 0) {
                     var6.append(", ");
                  }

                  var6.append(var5);
                  var4 = 1;
                  var5 = null;
               }

               if (var7.charAt(0) == ')') {
                  boolean var8 = false;
                  break;
               }

               if (var4 == 1 && var7.trim().length() > 0 && var7.charAt(0) != ',') {
                  var5 = this.escapeParse(var3, var7);
               }
            }
         }
      }

      return var6.toString();
   }

   private void parseQueryMeta(ResultSet var1) throws SQLServerException {
      Pattern var2 = Pattern.compile("(.*)\\((.*)(\\)|,(.*)\\))");

      try {
         while (var1.next()) {
            SQLServerParameterMetaData.QueryMeta var3 = new SQLServerParameterMetaData.QueryMeta();
            SSType var4 = null;
            int var5 = var1.getInt("parameter_ordinal");
            String var6 = var1.getString("suggested_system_type_name");
            if (null == var6) {
               var6 = var1.getString("suggested_user_type_name");
               SQLServerPreparedStatement var7 = (SQLServerPreparedStatement)this.con
                  .prepareCall("select max_length, precision, scale, is_nullable from sys.assembly_types where name = ?");
               var7.setNString(1, var6);
               ResultSet var8 = var7.executeQuery();
               if (var8.next()) {
                  var3.parameterTypeName = var6;
                  var3.precision = var8.getInt("max_length");
                  var3.scale = var8.getInt("scale");
                  var4 = SSType.UDT;
               }
            } else {
               var3.precision = var1.getInt("suggested_precision");
               var3.scale = var1.getInt("suggested_scale");
               Matcher var14 = var2.matcher(var6);
               if (var14.matches()) {
                  var4 = SSType.of(var14.group(1));
                  if (var6.equalsIgnoreCase("varchar(max)") || var6.equalsIgnoreCase("varbinary(max)")) {
                     var3.precision = Integer.MAX_VALUE;
                  } else if (var6.equalsIgnoreCase("nvarchar(max)")) {
                     var3.precision = 1073741823;
                  } else if (Category.CHARACTER == var4.category || Category.BINARY == var4.category || Category.NCHARACTER == var4.category) {
                     try {
                        var3.precision = Integer.parseInt(var14.group(2));
                     } catch (NumberFormatException var11) {
                        MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_metaDataErrorForParameter"));
                        Object[] var10 = new Object[]{new Integer(var5)};
                        SQLServerException.makeFromDriverError(this.con, this.stmtParent, var9.format(var10) + " " + var11.toString(), null, false);
                     }
                  }
               } else {
                  var4 = SSType.of(var6);
               }

               if (SSType.FLOAT == var4) {
                  var3.precision = 15;
               } else if (SSType.REAL == var4) {
                  var3.precision = 7;
               } else if (SSType.TEXT == var4) {
                  var3.precision = Integer.MAX_VALUE;
               } else if (SSType.NTEXT == var4) {
                  var3.precision = 1073741823;
               } else if (SSType.IMAGE == var4) {
                  var3.precision = Integer.MAX_VALUE;
               } else if (SSType.GUID == var4) {
                  var3.precision = 36;
               } else if (SSType.TIMESTAMP == var4) {
                  var3.precision = 8;
               } else if (SSType.XML == var4) {
                  var3.precision = 1073741823;
               }

               var3.parameterTypeName = var4.toString();
            }

            if (null == var4) {
               throw new SQLServerException(SQLServerException.getErrString("R_metaDataErrorForParameter"), null);
            }

            JDBCType var15 = var4.getJDBCType();
            var3.parameterClassName = var15.className();
            var3.parameterType = var15.getIntValue();
            var3.isSigned = Category.NUMERIC == var4.category && SSType.BIT != var4 && SSType.TINYINT != var4;
            this.queryMetaMap.put(var5, var3);
         }
      } catch (SQLException var12) {
         throw new SQLServerException(SQLServerException.getErrString("R_metaDataErrorForParameter"), var12);
      }
   }

   private void parseQueryMetaFor2008(ResultSet var1) throws SQLServerException {
      try {
         ResultSetMetaData var2 = var1.getMetaData();

         for (int var3 = 1; var3 <= var2.getColumnCount(); var3++) {
            SQLServerParameterMetaData.QueryMeta var4 = new SQLServerParameterMetaData.QueryMeta();
            var4.parameterClassName = var2.getColumnClassName(var3);
            var4.parameterType = var2.getColumnType(var3);
            var4.parameterTypeName = var2.getColumnTypeName(var3);
            var4.precision = var2.getPrecision(var3);
            var4.scale = var2.getScale(var3);
            var4.isNullable = var2.isNullable(var3);
            var4.isSigned = var2.isSigned(var3);
            this.queryMetaMap.put(var3, var4);
         }
      } catch (SQLException var5) {
         throw new SQLServerException(SQLServerException.getErrString("R_metaDataErrorForParameter"), var5);
      }
   }

   private String escapeParse(StringTokenizer var1, String var2) {
      String var3 = var2;

      while (var3.equals(" ") && var1.hasMoreTokens()) {
         var3 = var1.nextToken();
      }

      String var4 = var3;
      if (var3.charAt(0) == '[' && var3.charAt(var3.length() - 1) != ']') {
         while (var1.hasMoreTokens()) {
            var3 = var1.nextToken();
            var4 = var4.concat(var3);
            if (var3.charAt(var3.length() - 1) == ']') {
               break;
            }
         }
      }

      return var4.trim();
   }

   private SQLServerParameterMetaData.MetaInfo parseStatement(String var1, String var2) {
      StringTokenizer var3 = new StringTokenizer(var1, " ,", true);
      String var4 = null;
      String var5 = "";

      while (var3.hasMoreTokens()) {
         String var6 = var3.nextToken().trim();
         if (var6.equalsIgnoreCase(var2) && var3.hasMoreTokens()) {
            var4 = this.escapeParse(var3, var3.nextToken());
            break;
         }
      }

      if (null != var4) {
         if (var2.equalsIgnoreCase("UPDATE")) {
            var5 = this.parseColumns(var1, "SET");
         } else if (var2.equalsIgnoreCase("INTO")) {
            var5 = this.parseInsertColumns(var1, "(");
         } else {
            var5 = this.parseColumns(var1, "WHERE");
         }

         return new SQLServerParameterMetaData.MetaInfo(var4, var5);
      } else {
         return null;
      }
   }

   private SQLServerParameterMetaData.MetaInfo parseStatement(String var1) throws SQLServerException {
      StringTokenizer var2 = new StringTokenizer(var1, " ");
      if (var2.hasMoreTokens()) {
         String var3 = var2.nextToken().trim();
         if (var3.equalsIgnoreCase("INSERT")) {
            return this.parseStatement(var1, "INTO");
         }

         if (var3.equalsIgnoreCase("UPDATE")) {
            return this.parseStatement(var1, "UPDATE");
         }

         if (var3.equalsIgnoreCase("SELECT")) {
            return this.parseStatement(var1, "FROM");
         }

         if (var3.equalsIgnoreCase("DELETE")) {
            return this.parseStatement(var1, "FROM");
         }
      }

      return null;
   }

   String parseThreePartNames(String var1) throws SQLServerException {
      int var2 = 0;
      String var3 = null;
      String var4 = null;
      String var5 = null;
      StringTokenizer var6 = new StringTokenizer(var1, ".", true);

      while (var6.hasMoreTokens()) {
         String var7 = var6.nextToken();
         String var8 = this.escapeParse(var6, var7);
         if (!var8.equals(".")) {
            switch (var2) {
               case 0:
                  var3 = var8;
                  var2++;
                  break;
               case 1:
                  var4 = var3;
                  var3 = var8;
                  var2++;
                  break;
               case 2:
                  var5 = var4;
                  var4 = var3;
                  var3 = var8;
                  var2++;
                  break;
               default:
                  var2++;
            }
         }
      }

      StringBuilder var9 = new StringBuilder(100);
      if (var2 > 3 && 1 < var2) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, SQLServerException.getErrString("R_noMetadata"), null, false);
      }

      switch (var2) {
         case 1:
            var9.append("@procedure_name =");
            var9.append(var3);
            var9.append(", ");
            break;
         case 2:
            var9.append("@procedure_owner =");
            var9.append(var4);
            var9.append(", ");
            var9.append("@procedure_name =");
            var9.append(var3);
            var9.append(", ");
            break;
         case 3:
            var9.append("@procedure_qualifier =");
            var9.append(var5);
            var9.append(", ");
            var9.append("@procedure_owner =");
            var9.append(var4);
            var9.append(", ");
            var9.append("@procedure_name =");
            var9.append(var3);
            var9.append(", ");
      }

      return var9.toString();
   }

   private void checkClosed() throws SQLServerException {
      this.stmtParent.checkClosed();
   }

   SQLServerParameterMetaData(SQLServerStatement var1, String var2) throws SQLServerException {
      assert null != var1;
      this.stmtParent = var1;
      this.con = var1.connection;
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(this.toString() + " created by (" + var1.toString() + ")");
      }

      try {
         if (null != var1.procedureName) {
            SQLServerStatement var3 = (SQLServerStatement)this.con.createStatement(1004, 1007);
            String var4 = this.parseThreePartNames(var1.procedureName);
            if (this.con.isKatmaiOrLater()) {
               this.rsProcedureMeta = var3.executeQueryInternal("exec sp_sproc_columns_100 " + var4 + " @ODBCVer=3");
            } else {
               this.rsProcedureMeta = var3.executeQueryInternal("exec sp_sproc_columns " + var4 + " @ODBCVer=3");
            }

            this.rsProcedureMeta.getColumn(6).setFilter(new DataTypeFilter());
            if (this.con.isKatmaiOrLater()) {
               this.rsProcedureMeta.getColumn(8).setFilter(new ZeroFixupFilter());
               this.rsProcedureMeta.getColumn(9).setFilter(new ZeroFixupFilter());
               this.rsProcedureMeta.getColumn(17).setFilter(new ZeroFixupFilter());
            }
         } else {
            this.queryMetaMap = new HashMap();
            if (this.con.getServerMajorVersion() >= 11) {
               String var8 = this.con
                  .replaceParameterMarkers(
                     ((SQLServerPreparedStatement)this.stmtParent).userSQL,
                     ((SQLServerPreparedStatement)this.stmtParent).inOutParam,
                     ((SQLServerPreparedStatement)this.stmtParent).bReturnValueSyntax
                  );
               SQLServerCallableStatement var10 = (SQLServerCallableStatement)this.con.prepareCall("exec sp_describe_undeclared_parameters ?");
               var10.setNString(1, var8);
               this.parseQueryMeta(var10.executeQueryInternal());
               var10.close();
            } else {
               SQLServerParameterMetaData.MetaInfo var9 = this.parseStatement(var2);
               if (null == var9) {
                  MessageFormat var11 = new MessageFormat(SQLServerException.getErrString("R_cantIdentifyTableMetadata"));
                  Object[] var5 = new Object[]{new String(var2)};
                  SQLServerException.makeFromDriverError(this.con, this.stmtParent, var11.format(var5), null, false);
               }

               if (var9.fields.length() <= 0) {
                  return;
               }

               Statement var12 = this.con.createStatement();
               String var13 = "sp_executesql N'SET FMTONLY ON SELECT " + var9.fields + " FROM " + var9.table + " WHERE 1 = 2'";
               ResultSet var6 = var12.executeQuery(var13);
               this.parseQueryMetaFor2008(var6);
               var12.close();
               var6.close();
            }
         }
      } catch (SQLException var7) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var7.toString(), null, false);
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

   private void verifyParameterPosition(int var1) throws SQLServerException {
      boolean var2 = false;

      try {
         var2 = this.rsProcedureMeta.absolute(var1 + 1);
      } catch (SQLException var6) {
         MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_metaDataErrorForParameter"));
         Object[] var5 = new Object[]{new Integer(var1)};
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var4.format(var5) + " " + var6.toString(), null, false);
      }

      if (!var2) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_invalidParameterNumber"));
         Object[] var7 = new Object[]{new Integer(var1)};
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var3.format(var7), null, false);
      }
   }

   private void checkParam(int var1) throws SQLServerException {
      if (!this.queryMetaMap.containsKey(var1)) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, SQLServerException.getErrString("R_noMetadata"), null, false);
      }
   }

   public String getParameterClassName(int var1) throws SQLServerException {
      this.checkClosed();

      try {
         if (this.rsProcedureMeta == null) {
            this.checkParam(var1);
            return ((SQLServerParameterMetaData.QueryMeta)this.queryMetaMap.get(var1)).parameterClassName;
         } else {
            this.verifyParameterPosition(var1);
            JDBCType var2 = JDBCType.of(this.rsProcedureMeta.getShort("DATA_TYPE"));
            return var2.className();
         }
      } catch (SQLException var3) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var3.toString(), null, false);
         return null;
      }
   }

   public int getParameterCount() throws SQLServerException {
      this.checkClosed();

      try {
         if (this.rsProcedureMeta == null) {
            return this.queryMetaMap.size();
         }

         this.rsProcedureMeta.last();
         int var1 = this.rsProcedureMeta.getRow() - 1;
         if (var1 < 0) {
            var1 = 0;
         }

         return var1;
      } catch (SQLException var2) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var2.toString(), null, false);
         return 0;
      }
   }

   public int getParameterMode(int var1) throws SQLServerException {
      this.checkClosed();

      try {
         if (this.rsProcedureMeta == null) {
            this.checkParam(var1);
            return 1;
         }

         this.verifyParameterPosition(var1);
         int var2 = this.rsProcedureMeta.getInt("COLUMN_TYPE");
         switch (var2) {
            case 1:
               return 1;
            case 2:
               return 4;
            default:
               return 0;
         }
      } catch (SQLException var3) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var3.toString(), null, false);
         return 0;
      }
   }

   public int getParameterType(int var1) throws SQLServerException {
      this.checkClosed();

      try {
         int var2;
         if (this.rsProcedureMeta == null) {
            this.checkParam(var1);
            var2 = ((SQLServerParameterMetaData.QueryMeta)this.queryMetaMap.get(var1)).parameterType;
         } else {
            this.verifyParameterPosition(var1);
            var2 = this.rsProcedureMeta.getShort("DATA_TYPE");
         }

         switch (var2) {
            case -151:
            case -150:
               var2 = SSType.DATETIME2.getJDBCType().asJavaSqlType();
            case -149:
            case -147:
            default:
               break;
            case -148:
            case -146:
               var2 = SSType.DECIMAL.getJDBCType().asJavaSqlType();
               break;
            case -145:
               var2 = SSType.CHAR.getJDBCType().asJavaSqlType();
         }

         return var2;
      } catch (SQLException var4) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var4.toString(), null, false);
         return 0;
      }
   }

   public String getParameterTypeName(int var1) throws SQLServerException {
      this.checkClosed();

      try {
         if (this.rsProcedureMeta == null) {
            this.checkParam(var1);
            return ((SQLServerParameterMetaData.QueryMeta)this.queryMetaMap.get(var1)).parameterTypeName;
         } else {
            this.verifyParameterPosition(var1);
            return this.rsProcedureMeta.getString("TYPE_NAME");
         }
      } catch (SQLException var3) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var3.toString(), null, false);
         return null;
      }
   }

   public int getPrecision(int var1) throws SQLServerException {
      this.checkClosed();

      try {
         if (this.rsProcedureMeta == null) {
            this.checkParam(var1);
            return ((SQLServerParameterMetaData.QueryMeta)this.queryMetaMap.get(var1)).precision;
         } else {
            this.verifyParameterPosition(var1);
            return this.rsProcedureMeta.getInt("PRECISION");
         }
      } catch (SQLException var3) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var3.toString(), null, false);
         return 0;
      }
   }

   public int getScale(int var1) throws SQLServerException {
      this.checkClosed();

      try {
         if (this.rsProcedureMeta == null) {
            this.checkParam(var1);
            return ((SQLServerParameterMetaData.QueryMeta)this.queryMetaMap.get(var1)).scale;
         } else {
            this.verifyParameterPosition(var1);
            return this.rsProcedureMeta.getInt("SCALE");
         }
      } catch (SQLException var3) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var3.toString(), null, false);
         return 0;
      }
   }

   public int isNullable(int var1) throws SQLServerException {
      this.checkClosed();

      try {
         if (this.rsProcedureMeta == null) {
            this.checkParam(var1);
            return ((SQLServerParameterMetaData.QueryMeta)this.queryMetaMap.get(var1)).isNullable;
         } else {
            this.verifyParameterPosition(var1);
            int var2 = this.rsProcedureMeta.getInt("NULLABLE");
            if (var2 == 1) {
               return 1;
            } else {
               return var2 == 0 ? 0 : 2;
            }
         }
      } catch (SQLException var3) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var3.toString(), null, false);
         return 0;
      }
   }

   public boolean isSigned(int var1) throws SQLServerException {
      this.checkClosed();

      try {
         if (this.rsProcedureMeta == null) {
            this.checkParam(var1);
            return ((SQLServerParameterMetaData.QueryMeta)this.queryMetaMap.get(var1)).isSigned;
         } else {
            this.verifyParameterPosition(var1);
            return JDBCType.of(this.rsProcedureMeta.getShort("DATA_TYPE")).isSigned();
         }
      } catch (SQLException var3) {
         SQLServerException.makeFromDriverError(this.con, this.stmtParent, var3.toString(), null, false);
         return false;
      }
   }

   private class MetaInfo {
      String table;
      String fields;

      MetaInfo(String var2, String var3) {
         this.table = var2;
         this.fields = var3;
      }
   }

   class QueryMeta {
      String parameterClassName = null;
      int parameterType = 0;
      String parameterTypeName = null;
      int precision = 0;
      int scale = 0;
      int isNullable = 2;
      boolean isSigned = false;
   }
}
