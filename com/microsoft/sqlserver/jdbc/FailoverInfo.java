package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;
import java.util.logging.Level;

final class FailoverInfo {
   private String failoverPartner;
   private int portNumber;
   private String failoverInstance;
   private boolean setUpInfocalled;
   private boolean useFailoverPartner;

   boolean getUseFailoverPartner() {
      return this.useFailoverPartner;
   }

   FailoverInfo(String var1, SQLServerConnection var2, boolean var3) {
      this.failoverPartner = var1;
      this.useFailoverPartner = var3;
      this.portNumber = -1;
   }

   void log(SQLServerConnection var1) {
      if (var1.getConnectionLogger().isLoggable(Level.FINE)) {
         var1.getConnectionLogger()
            .fine(var1.toString() + " Failover server :" + this.failoverPartner + " Failover partner is primary : " + this.useFailoverPartner);
      }
   }

   private void setupInfo(SQLServerConnection var1) throws SQLServerException {
      if (!this.setUpInfocalled) {
         if (0 == this.failoverPartner.length()) {
            this.portNumber = SQLServerConnection.DEFAULTPORT;
         } else {
            int var2 = this.failoverPartner.indexOf(92);
            String var3 = null;
            String var4 = null;
            if (var2 >= 0) {
               if (var1.getConnectionLogger().isLoggable(Level.FINE)) {
                  var1.getConnectionLogger().fine(var1.toString() + " Failover server :" + this.failoverPartner);
               }

               var4 = this.failoverPartner.substring(var2 + 1, this.failoverPartner.length());
               this.failoverPartner = this.failoverPartner.substring(0, var2);
               var1.ValidateMaxSQLLoginName(SQLServerDriverStringProperty.INSTANCE_NAME.toString(), var4);
               this.failoverInstance = var4;
               var3 = var1.getInstancePort(this.failoverPartner, var4);

               try {
                  this.portNumber = new Integer(var3);
               } catch (NumberFormatException var8) {
                  MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_invalidPortNumber"));
                  Object[] var7 = new Object[]{var3};
                  SQLServerException.makeFromDriverError(var1, null, var6.format(var7), null, false);
               }
            } else {
               this.portNumber = SQLServerConnection.DEFAULTPORT;
            }
         }

         this.setUpInfocalled = true;
      }
   }

   synchronized ServerPortPlaceHolder failoverPermissionCheck(SQLServerConnection var1, boolean var2) throws SQLServerException {
      this.setupInfo(var1);
      return new ServerPortPlaceHolder(this.failoverPartner, this.portNumber, this.failoverInstance, var2);
   }

   synchronized void failoverAdd(SQLServerConnection var1, boolean var2, String var3) throws SQLServerException {
      if (this.useFailoverPartner != var2) {
         if (var1.getConnectionLogger().isLoggable(Level.FINE)) {
            var1.getConnectionLogger().fine(var1.toString() + " Failover detected. failover partner=" + var3);
         }

         this.useFailoverPartner = var2;
      }

      if (!var2 && !this.failoverPartner.equals(var3)) {
         this.failoverPartner = var3;
         this.setUpInfocalled = false;
      }
   }
}
