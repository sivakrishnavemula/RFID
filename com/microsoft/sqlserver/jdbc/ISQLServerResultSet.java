package com.microsoft.sqlserver.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import microsoft.sql.DateTimeOffset;

public interface ISQLServerResultSet extends ResultSet {
   int TYPE_SS_DIRECT_FORWARD_ONLY = 2003;
   int TYPE_SS_SERVER_CURSOR_FORWARD_ONLY = 2004;
   int TYPE_SS_SCROLL_STATIC = 1004;
   int TYPE_SS_SCROLL_KEYSET = 1005;
   int TYPE_SS_SCROLL_DYNAMIC = 1006;
   int CONCUR_SS_OPTIMISTIC_CC = 1008;
   int CONCUR_SS_SCROLL_LOCKS = 1009;
   int CONCUR_SS_OPTIMISTIC_CCVAL = 1010;

   DateTimeOffset getDateTimeOffset(int var1) throws SQLException;

   DateTimeOffset getDateTimeOffset(String var1) throws SQLException;

   void updateDateTimeOffset(int var1, DateTimeOffset var2) throws SQLException;

   void updateDateTimeOffset(String var1, DateTimeOffset var2) throws SQLException;
}
