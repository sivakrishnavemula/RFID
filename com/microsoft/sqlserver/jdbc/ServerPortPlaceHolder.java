package com.microsoft.sqlserver.jdbc;

final class ServerPortPlaceHolder {
   private final String serverName;
   private final int port;
   private final String instanceName;
   private final boolean checkLink;
   private final SQLServerConnectionSecurityManager securityManager;

   ServerPortPlaceHolder(String var1, int var2, String var3, boolean var4) {
      this.serverName = var1;
      this.port = var2;
      this.instanceName = var3;
      this.checkLink = var4;
      this.securityManager = new SQLServerConnectionSecurityManager(this.serverName, this.port);
      this.doSecurityCheck();
   }

   int getPortNumber() {
      return this.port;
   }

   String getServerName() {
      return this.serverName;
   }

   String getInstanceName() {
      return this.instanceName;
   }

   void doSecurityCheck() {
      this.securityManager.checkConnect();
      if (this.checkLink) {
         this.securityManager.checkLink();
      }
   }
}
