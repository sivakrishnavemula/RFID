package com.microsoft.sqlserver.jdbc;

import java.util.HashMap;
import java.util.logging.Level;

final class FailoverMapSingleton {
   private static int INITIALHASHMAPSIZE = 5;
   private static HashMap<String, FailoverInfo> failoverMap = new HashMap(INITIALHASHMAPSIZE);

   private FailoverMapSingleton() {
   }

   private static String concatPrimaryDatabase(String var0, String var1, String var2) {
      StringBuilder var3 = new StringBuilder();
      var3.append(var0);
      if (null != var1) {
         var3.append("\\");
         var3.append(var1);
      }

      var3.append(";");
      var3.append(var2);
      return var3.toString();
   }

   static FailoverInfo getFailoverInfo(SQLServerConnection var0, String var1, String var2, String var3) {
      synchronized (FailoverMapSingleton.class) {
         if (failoverMap.isEmpty()) {
            return null;
         }

         String var5 = concatPrimaryDatabase(var1, var2, var3);
         if (var0.getConnectionLogger().isLoggable(Level.FINER)) {
            var0.getConnectionLogger().finer(var0.toString() + " Looking up info in the map using key: " + var5);
         }

         FailoverInfo var6 = (FailoverInfo)failoverMap.get(var5);
         if (null != var6) {
            var6.log(var0);
         }

         return var6;
      }
   }

   static void putFailoverInfo(SQLServerConnection var0, String var1, String var2, String var3, FailoverInfo var4, boolean var5, String var6) throws SQLServerException {
      synchronized (FailoverMapSingleton.class) {
         FailoverInfo var7;
         if (null == (var7 = getFailoverInfo(var0, var1, var2, var3))) {
            if (var0.getConnectionLogger().isLoggable(Level.FINE)) {
               var0.getConnectionLogger().fine(var0.toString() + " Failover map add server: " + var1 + "; database:" + var3 + "; Mirror:" + var6);
            }

            failoverMap.put(concatPrimaryDatabase(var1, var2, var3), var4);
         } else {
            var7.failoverAdd(var0, var5, var6);
         }
      }
   }
}
