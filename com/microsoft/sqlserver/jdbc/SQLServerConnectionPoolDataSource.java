package com.microsoft.sqlserver.jdbc;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.logging.Level;
import javax.naming.Reference;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

public class SQLServerConnectionPoolDataSource extends SQLServerDataSource implements ConnectionPoolDataSource {
   public PooledConnection getPooledConnection() throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getPooledConnection");
      }

      PooledConnection var1 = this.getPooledConnection(this.getUser(), this.getPassword());
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.exiting(this.getClassNameLogging(), "getPooledConnection", var1);
      }

      return var1;
   }

   public PooledConnection getPooledConnection(String var1, String var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getPooledConnection", new Object[]{var1, "Password not traced"});
      }

      SQLServerPooledConnection var3 = new SQLServerPooledConnection(this, var1, var2);
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.exiting(this.getClassNameLogging(), "getPooledConnection", var3);
      }

      return var3;
   }

   public Reference getReference() {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getReference");
      }

      Reference var1 = this.getReferenceInternal("com.microsoft.sqlserver.jdbc.SQLServerConnectionPoolDataSource");
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.exiting(this.getClassNameLogging(), "getReference", var1);
      }

      return var1;
   }

   private Object writeReplace() throws ObjectStreamException {
      return new SQLServerConnectionPoolDataSource.SerializationProxy(this);
   }

   private void readObject(ObjectInputStream var1) throws InvalidObjectException {
      throw new InvalidObjectException("");
   }

   private static class SerializationProxy implements Serializable {
      private final Reference ref;
      private static final long serialVersionUID = 654661379842314126L;

      SerializationProxy(SQLServerConnectionPoolDataSource var1) {
         this.ref = var1.getReferenceInternal(null);
      }

      private Object readResolve() {
         SQLServerConnectionPoolDataSource var1 = new SQLServerConnectionPoolDataSource();
         var1.initializeFromReference(this.ref);
         return var1;
      }
   }
}
