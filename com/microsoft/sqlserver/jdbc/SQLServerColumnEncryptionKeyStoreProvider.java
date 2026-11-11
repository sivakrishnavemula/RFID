package com.microsoft.sqlserver.jdbc;

public abstract class SQLServerColumnEncryptionKeyStoreProvider {
   public abstract void setName(String var1);

   public abstract String getName();

   public abstract byte[] decryptColumnEncryptionKey(String var1, String var2, byte[] var3) throws SQLServerException;

   public abstract byte[] encryptColumnEncryptionKey(String var1, String var2, byte[] var3) throws SQLServerException;
}
