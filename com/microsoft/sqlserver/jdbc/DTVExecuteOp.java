package com.microsoft.sqlserver.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Calendar;
import microsoft.sql.DateTimeOffset;

abstract class DTVExecuteOp {
   abstract void execute(DTV var1, String var2) throws SQLServerException;

   abstract void execute(DTV var1, Clob var2) throws SQLServerException;

   abstract void execute(DTV var1, Byte var2) throws SQLServerException;

   abstract void execute(DTV var1, Integer var2) throws SQLServerException;

   abstract void execute(DTV var1, Time var2) throws SQLServerException;

   abstract void execute(DTV var1, Date var2) throws SQLServerException;

   abstract void execute(DTV var1, Timestamp var2) throws SQLServerException;

   abstract void execute(DTV var1, java.util.Date var2) throws SQLServerException;

   abstract void execute(DTV var1, Calendar var2) throws SQLServerException;

   abstract void execute(DTV var1, LocalDate var2) throws SQLServerException;

   abstract void execute(DTV var1, LocalTime var2) throws SQLServerException;

   abstract void execute(DTV var1, LocalDateTime var2) throws SQLServerException;

   abstract void execute(DTV var1, OffsetTime var2) throws SQLServerException;

   abstract void execute(DTV var1, OffsetDateTime var2) throws SQLServerException;

   abstract void execute(DTV var1, DateTimeOffset var2) throws SQLServerException;

   abstract void execute(DTV var1, Float var2) throws SQLServerException;

   abstract void execute(DTV var1, Double var2) throws SQLServerException;

   abstract void execute(DTV var1, BigDecimal var2) throws SQLServerException;

   abstract void execute(DTV var1, Long var2) throws SQLServerException;

   abstract void execute(DTV var1, BigInteger var2) throws SQLServerException;

   abstract void execute(DTV var1, Short var2) throws SQLServerException;

   abstract void execute(DTV var1, Boolean var2) throws SQLServerException;

   abstract void execute(DTV var1, byte[] var2) throws SQLServerException;

   abstract void execute(DTV var1, Blob var2) throws SQLServerException;

   abstract void execute(DTV var1, InputStream var2) throws SQLServerException;

   abstract void execute(DTV var1, Reader var2) throws SQLServerException;

   abstract void execute(DTV var1, SQLServerSQLXML var2) throws SQLServerException;

   abstract void execute(DTV var1, TVP var2) throws SQLServerException;
}
