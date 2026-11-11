package com.microsoft.sqlserver.jdbc;

import java.util.HashMap;
import java.util.logging.Level;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

class KerbAuthentication$1SQLJDBCDriverConfig extends Configuration {
   Configuration current = null;
   AppConfigurationEntry[] driverConf;

   KerbAuthentication$1SQLJDBCDriverConfig() {
      try {
         this.current = Configuration.getConfiguration();
      } catch (SecurityException var4) {
         KerbAuthentication.access$000().finer(this.toString() + " No configurations provided, setting driver default");
      }

      AppConfigurationEntry[] var1 = null;
      if (null != this.current) {
         var1 = this.current.getAppConfigurationEntry("SQLJDBCDriver");
      }

      if (null == var1) {
         if (KerbAuthentication.access$000().isLoggable(Level.FINER)) {
            KerbAuthentication.access$000().finer(this.toString() + " SQLJDBCDriver configuration entry is not provided, setting driver default");
         }

         AppConfigurationEntry var2;
         if (Util.isIBM()) {
            HashMap var3 = new HashMap();
            var3.put("useDefaultCcache", "true");
            var3.put("moduleBanner", "false");
            var2 = new AppConfigurationEntry("com.ibm.security.auth.module.Krb5LoginModule", LoginModuleControlFlag.REQUIRED, var3);
            if (KerbAuthentication.access$000().isLoggable(Level.FINER)) {
               KerbAuthentication.access$000().finer(this.toString() + " Setting IBM Krb5LoginModule");
            }
         } else {
            HashMap var5 = new HashMap();
            var5.put("useTicketCache", "true");
            var5.put("doNotPrompt", "true");
            var2 = new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule", LoginModuleControlFlag.REQUIRED, var5);
            if (KerbAuthentication.access$000().isLoggable(Level.FINER)) {
               KerbAuthentication.access$000().finer(this.toString() + " Setting Sun Krb5LoginModule");
            }
         }

         this.driverConf = new AppConfigurationEntry[1];
         this.driverConf[0] = var2;
         Configuration.setConfiguration(this);
      }
   }

   public AppConfigurationEntry[] getAppConfigurationEntry(String var1) {
      if (var1.equals("SQLJDBCDriver")) {
         return this.driverConf;
      } else {
         return null != this.current ? this.current.getAppConfigurationEntry(var1) : null;
      }
   }

   public void refresh() {
      if (null != this.current) {
         this.current.refresh();
      }
   }
}
