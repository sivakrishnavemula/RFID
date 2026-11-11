package com.microsoft.sqlserver.jdbc;

abstract class SQLServerEncryptionAlgorithm {
   abstract byte[] encryptData(byte[] var1) throws SQLServerException;

   abstract byte[] decryptData(byte[] var1) throws SQLServerException;
}
