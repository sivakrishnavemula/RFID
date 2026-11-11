package com.microsoft.sqlserver.jdbc;

import java.io.UnsupportedEncodingException;
import java.sql.NClob;
import java.util.logging.Logger;

public final class SQLServerNClob extends SQLServerClobBase implements NClob {
   private static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.SQLServerNClob");

   SQLServerNClob(SQLServerConnection var1) {
      super(var1, "", var1.getDatabaseCollation(), logger);
   }

   SQLServerNClob(BaseInputStream var1, TypeInfo var2) throws SQLServerException, UnsupportedEncodingException {
      super(null, new String(var1.getBytes(), var2.getCharset()), var2.getSQLCollation(), logger);
   }

   final JDBCType getJdbcType() {
      return JDBCType.NCLOB;
   }
}
