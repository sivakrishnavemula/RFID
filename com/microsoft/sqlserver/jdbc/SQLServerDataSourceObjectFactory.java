package com.microsoft.sqlserver.jdbc;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

public final class SQLServerDataSourceObjectFactory implements ObjectFactory {
   public Object getObjectInstance(Object var1, Name var2, Context var3, Hashtable<?, ?> var4) throws SQLServerException {
      try {
         Reference var5 = (Reference)var1;
         RefAddr var6 = var5.get("class");
         if (null == var6) {
            SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_invalidDataSourceReference"), null, true);
         }

         String var7 = (String)var6.getContent();
         if (null == var7) {
            SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_invalidDataSourceReference"), null, true);
         }

         if (var7.equals("com.microsoft.sqlserver.jdbc.SQLServerDataSource")
            || var7.equals("com.microsoft.sqlserver.jdbc.SQLServerConnectionPoolDataSource")
            || var7.equals("com.microsoft.sqlserver.jdbc.SQLServerXADataSource")) {
            Class var8 = Class.forName(var7);
            Object var9 = var8.newInstance();
            SQLServerDataSource var10 = (SQLServerDataSource)var9;
            var10.initializeFromReference(var5);
            return var9;
         }

         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_invalidDataSourceReference"), null, true);
      } catch (ClassNotFoundException var11) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_invalidDataSourceReference"), null, true);
      } catch (InstantiationException var12) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_invalidDataSourceReference"), null, true);
      } catch (IllegalAccessException var13) {
         SQLServerException.makeFromDriverError(null, null, SQLServerException.getErrString("R_invalidDataSourceReference"), null, true);
      }

      return null;
   }
}
