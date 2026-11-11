package com.microsoft.sqlserver.jdbc;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Reference;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

public final class SQLServerXADataSource extends SQLServerConnectionPoolDataSource implements XADataSource {
   static Logger xaLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.XA");

   public XAConnection getXAConnection(String var1, String var2) throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getXAConnection", new Object[]{var1, "Password not traced"});
      }

      SQLServerXAConnection var3 = new SQLServerXAConnection(this, var1, var2);
      if (xaLogger.isLoggable(Level.FINER)) {
         xaLogger.finer(this.toString() + " user:" + var1 + var3.toString());
      }

      if (xaLogger.isLoggable(Level.FINER)) {
         xaLogger.finer(this.toString() + " Start get physical connection.");
      }

      SQLServerConnection var4 = var3.getPhysicalConnection();
      if (xaLogger.isLoggable(Level.FINE)) {
         xaLogger.fine(this.toString() + " End get physical connection, " + var4.toString());
      }

      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.exiting(this.getClassNameLogging(), "getXAConnection", var3);
      }

      return var3;
   }

   public XAConnection getXAConnection() throws SQLException {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getXAConnection");
      }

      return this.getXAConnection(this.getUser(), this.getPassword());
   }

   public Reference getReference() {
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.entering(this.getClassNameLogging(), "getReference");
      }

      Reference var1 = this.getReferenceInternal("com.microsoft.sqlserver.jdbc.SQLServerXADataSource");
      if (loggerExternal.isLoggable(Level.FINER)) {
         loggerExternal.exiting(this.getClassNameLogging(), "getReference", var1);
      }

      return var1;
   }

   private Object writeReplace() throws ObjectStreamException {
      return new SQLServerXADataSource.SerializationProxy(this);
   }

   private void readObject(ObjectInputStream var1) throws InvalidObjectException {
      throw new InvalidObjectException("");
   }

   private static class SerializationProxy implements Serializable {
      private final Reference ref;
      private static final long serialVersionUID = 454661379842314126L;

      SerializationProxy(SQLServerXADataSource var1) {
         this.ref = var1.getReferenceInternal(null);
      }

      private Object readResolve() {
         SQLServerXADataSource var1 = new SQLServerXADataSource();
         var1.initializeFromReference(this.ref);
         return var1;
      }
   }
}
