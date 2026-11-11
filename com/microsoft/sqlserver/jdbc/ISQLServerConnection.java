package com.microsoft.sqlserver.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public interface ISQLServerConnection extends Connection {
   int TRANSACTION_SNAPSHOT = 4096;

   UUID getClientConnectionId() throws SQLException;
}
