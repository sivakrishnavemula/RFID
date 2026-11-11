package com.microsoft.sqlserver.jdbc;

class FedAuthDllInfo {
   byte[] accessTokenBytes = null;
   long expiresIn = 0L;

   FedAuthDllInfo(byte[] var1, long var2) {
      this.accessTokenBytes = var1;
      this.expiresIn = var2;
   }
}
