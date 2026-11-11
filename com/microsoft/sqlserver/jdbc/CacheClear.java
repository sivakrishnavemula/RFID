package com.microsoft.sqlserver.jdbc;

import java.util.logging.Level;
import java.util.logging.Logger;

class CacheClear implements Runnable {
   private String keylookupValue;
   private static Logger aeLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.CacheClear");

   CacheClear(String var1) {
      this.keylookupValue = var1;
   }

   public void run() {
      synchronized (SQLServerSymmetricKeyCache.lock) {
         SQLServerSymmetricKeyCache var2 = SQLServerSymmetricKeyCache.getInstance();
         if (var2.getCache().containsKey(this.keylookupValue)) {
            ((SQLServerSymmetricKey)var2.getCache().get(this.keylookupValue)).zeroOutKey();
            var2.getCache().remove(this.keylookupValue);
            if (aeLogger.isLoggable(Level.FINE)) {
               aeLogger.fine("Removed encryption key from cache...");
            }
         }
      }
   }
}
