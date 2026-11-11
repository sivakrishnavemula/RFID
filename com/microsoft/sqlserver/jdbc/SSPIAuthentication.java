package com.microsoft.sqlserver.jdbc;

abstract class SSPIAuthentication {
   abstract byte[] GenerateClientContext(byte[] var1, boolean[] var2) throws SQLServerException;

   abstract int ReleaseClientContext() throws SQLServerException;
}
