package com.microsoft.sqlserver.jdbc;

import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

public final class SQLServerXAConnection extends SQLServerPooledConnection implements XAConnection {
   private SQLServerXAResource XAResource;
   private SQLServerConnection physicalControlConnection;
   private Logger xaLogger;

   SQLServerXAConnection(SQLServerDataSource var1, String var2, String var3) throws SQLException {
      super(var1, var2, var3);
      this.xaLogger = SQLServerXADataSource.xaLogger;
      SQLServerConnection var4 = this.getPhysicalConnection();
      Properties var5 = (Properties)var4.activeConnectionProperties.clone();
      var5.setProperty(SQLServerDriverBooleanProperty.SEND_STRING_PARAMETERS_AS_UNICODE.toString(), "true");
      var5.remove(SQLServerDriverStringProperty.SELECT_METHOD.toString());
      if (this.xaLogger.isLoggable(Level.FINER)) {
         this.xaLogger.finer("Creating an internal control connection for" + this.toString());
      }

      this.physicalControlConnection = new SQLServerConnection(this.toString());
      this.physicalControlConnection.connect(var5, null);
      if (this.xaLogger.isLoggable(Level.FINER)) {
         this.xaLogger
            .finer(
               "Created an internal control connection"
                  + this.physicalControlConnection.toString()
                  + " for "
                  + this.toString()
                  + " Physical connection:"
                  + this.getPhysicalConnection().toString()
            );
      }

      if (this.xaLogger.isLoggable(Level.FINER)) {
         this.xaLogger.finer(var1.toString() + " user:" + var2);
      }
   }

   public synchronized XAResource getXAResource() throws SQLException {
      if (this.XAResource == null) {
         this.XAResource = new SQLServerXAResource(this.getPhysicalConnection(), this.physicalControlConnection, this.toString());
      }

      return this.XAResource;
   }

   public void close() throws SQLException {
      synchronized (this) {
         if (this.XAResource != null) {
            this.XAResource.close();
            this.XAResource = null;
         }

         if (null != this.physicalControlConnection) {
            this.physicalControlConnection.close();
            this.physicalControlConnection = null;
         }
      }

      super.close();
   }
}
