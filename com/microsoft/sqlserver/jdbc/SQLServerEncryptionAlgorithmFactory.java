package com.microsoft.sqlserver.jdbc;

abstract class SQLServerEncryptionAlgorithmFactory {
   abstract SQLServerEncryptionAlgorithm create(SQLServerSymmetricKey var1, SQLServerEncryptionType var2, String var3) throws SQLServerException;
}
