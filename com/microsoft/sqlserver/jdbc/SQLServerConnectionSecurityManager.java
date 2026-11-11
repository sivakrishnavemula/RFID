package com.microsoft.sqlserver.jdbc;

final class SQLServerConnectionSecurityManager {
   static final String dllName = "sqljdbc_auth.dll";
   String serverName;
   int portNumber;

   SQLServerConnectionSecurityManager(String var1, int var2) {
      this.serverName = var1;
      this.portNumber = var2;
   }

   public void checkConnect() throws SecurityException {
      SecurityManager var1 = System.getSecurityManager();
      if (null != var1) {
         var1.checkConnect(this.serverName, this.portNumber);
      }
   }

   public void checkLink() throws SecurityException {
      SecurityManager var1 = System.getSecurityManager();
      if (null != var1) {
         var1.checkLink("sqljdbc_auth.dll");
      }
   }
}
