package com.microsoft.sqlserver.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.sql.StatementEventListener;

public class SQLServerPooledConnection implements PooledConnection {
   private final Vector<ConnectionEventListener> listeners = new Vector();
   private SQLServerDataSource factoryDataSource;
   private SQLServerConnection physicalConnection;
   private SQLServerConnectionPoolProxy lastProxyConnection;
   private String factoryUser;
   private String factoryPassword;
   private Logger pcLogger = SQLServerDataSource.dsLogger;
   private static int basePooledConnectionID = 0;
   private final String traceID;

   SQLServerPooledConnection(SQLServerDataSource var1, String var2, String var3) throws SQLException {
      this.factoryDataSource = var1;
      this.factoryUser = var2;
      this.factoryPassword = var3;
      if (this.pcLogger.isLoggable(Level.FINER)) {
         this.pcLogger.finer(this.toString() + " Start create new connection for pool.");
      }

      this.physicalConnection = this.createNewConnection();
      String var4 = this.getClass().getName();
      this.traceID = var4.substring(1 + var4.lastIndexOf(46)) + ":" + nextPooledConnectionID();
      if (this.pcLogger.isLoggable(Level.FINE)) {
         this.pcLogger
            .fine(this.toString() + " created by (" + var1.toString() + ")" + " Physical connection " + this.safeCID() + ", End create new connection for pool");
      }
   }

   public String toString() {
      return this.traceID;
   }

   private SQLServerConnection createNewConnection() throws SQLException {
      return this.factoryDataSource.getConnectionInternal(this.factoryUser, this.factoryPassword, this);
   }

   public Connection getConnection() throws SQLException {
      if (this.pcLogger.isLoggable(Level.FINER)) {
         this.pcLogger.finer(this.toString() + " user:(default).");
      }

      synchronized (this) {
         if (this.physicalConnection == null) {
            SQLServerException.makeFromDriverError(null, this, SQLServerException.getErrString("R_physicalConnectionIsClosed"), "", true);
         }

         this.physicalConnection.doSecurityCheck();
         if (this.pcLogger.isLoggable(Level.FINE)) {
            this.pcLogger.fine(this.toString() + " Physical connection, " + this.safeCID());
         }

         if (null != this.physicalConnection.getAuthenticationResult() && Util.checkIfNeedNewAccessToken(this.physicalConnection)) {
            this.physicalConnection = this.createNewConnection();
         }

         if (null != this.lastProxyConnection) {
            this.physicalConnection.resetPooledConnection();
            if (this.pcLogger.isLoggable(Level.FINE) && !this.lastProxyConnection.isClosed()) {
               this.pcLogger.fine(this.toString() + "proxy " + this.lastProxyConnection.toString() + " is not closed before getting the connection.");
            }

            this.lastProxyConnection.internalClose();
         }

         this.lastProxyConnection = new SQLServerConnectionPoolProxy(this.physicalConnection);
         if (this.pcLogger.isLoggable(Level.FINE) && !this.lastProxyConnection.isClosed()) {
            this.pcLogger.fine(this.toString() + " proxy " + this.lastProxyConnection.toString() + " is returned.");
         }

         return this.lastProxyConnection;
      }
   }

   void notifyEvent(SQLServerException var1) {
      if (this.pcLogger.isLoggable(Level.FINER)) {
         this.pcLogger.finer(this.toString() + " Exception:" + var1 + this.safeCID());
      }

      if (null != var1) {
         synchronized (this) {
            if (null != this.lastProxyConnection) {
               this.lastProxyConnection.internalClose();
               this.lastProxyConnection = null;
            }
         }
      }

      synchronized (this.listeners) {
         for (int var3 = 0; var3 < this.listeners.size(); var3++) {
            ConnectionEventListener var4 = (ConnectionEventListener)this.listeners.elementAt(var3);
            if (var4 != null) {
               ConnectionEvent var5 = new ConnectionEvent(this, var1);
               if (null == var1) {
                  if (this.pcLogger.isLoggable(Level.FINER)) {
                     this.pcLogger.finer(this.toString() + " notifyEvent:connectionClosed " + this.safeCID());
                  }

                  var4.connectionClosed(var5);
               } else {
                  if (this.pcLogger.isLoggable(Level.FINER)) {
                     this.pcLogger.finer(this.toString() + " notifyEvent:connectionErrorOccurred " + this.safeCID());
                  }

                  var4.connectionErrorOccurred(var5);
               }
            }
         }
      }
   }

   public void addConnectionEventListener(ConnectionEventListener var1) {
      if (this.pcLogger.isLoggable(Level.FINER)) {
         this.pcLogger.finer(this.toString() + this.safeCID());
      }

      synchronized (this.listeners) {
         this.listeners.add(var1);
      }
   }

   public void close() throws SQLException {
      if (this.pcLogger.isLoggable(Level.FINER)) {
         this.pcLogger.finer(this.toString() + " Closing physical connection, " + this.safeCID());
      }

      synchronized (this) {
         if (null != this.lastProxyConnection) {
            this.lastProxyConnection.internalClose();
         }

         if (null != this.physicalConnection) {
            this.physicalConnection.DetachFromPool();
            this.physicalConnection.close();
         }

         this.physicalConnection = null;
      }

      synchronized (this.listeners) {
         this.listeners.clear();
      }
   }

   public void removeConnectionEventListener(ConnectionEventListener var1) {
      if (this.pcLogger.isLoggable(Level.FINER)) {
         this.pcLogger.finer(this.toString() + this.safeCID());
      }

      synchronized (this.listeners) {
         this.listeners.remove(var1);
      }
   }

   public void addStatementEventListener(StatementEventListener var1) {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new UnsupportedOperationException(SQLServerException.getErrString("R_notSupported"));
   }

   public void removeStatementEventListener(StatementEventListener var1) {
      DriverJDBCVersion.checkSupportsJDBC4();
      throw new UnsupportedOperationException(SQLServerException.getErrString("R_notSupported"));
   }

   SQLServerConnection getPhysicalConnection() {
      return this.physicalConnection;
   }

   private static synchronized int nextPooledConnectionID() {
      basePooledConnectionID++;
      return basePooledConnectionID;
   }

   private String safeCID() {
      return null == this.physicalConnection ? " ConnectionID:(null)" : this.physicalConnection.toString();
   }
}
