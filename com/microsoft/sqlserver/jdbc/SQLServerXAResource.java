package com.microsoft.sqlserver.jdbc;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public final class SQLServerXAResource implements XAResource {
   private int timeoutSeconds;
   static final int XA_START = 0;
   static final int XA_END = 1;
   static final int XA_PREPARE = 2;
   static final int XA_COMMIT = 3;
   static final int XA_ROLLBACK = 4;
   static final int XA_FORGET = 5;
   static final int XA_RECOVER = 6;
   static final int XA_PREPARE_EX = 7;
   static final int XA_ROLLBACK_EX = 8;
   static final int XA_FORGET_EX = 9;
   static final int XA_INIT = 10;
   private SQLServerConnection controlConnection;
   private SQLServerConnection con;
   private boolean serverInfoRetrieved;
   private String version;
   private String instanceName;
   private int ArchitectureMSSQL;
   private int ArchitectureOS;
   private static boolean xaInitDone;
   private static Integer xaInitLock = new Integer(0);
   private String sResourceManagerId;
   private int enlistedTransactionCount;
   private final Logger xaLogger;
   private static int baseResourceID = 0;
   private int tightlyCoupled = 0;
   private int isTransacrionTimeoutSet = 0;
   public static final int SSTRANSTIGHTLYCPLD = 32768;
   private SQLServerCallableStatement[] xaStatements = new SQLServerCallableStatement[]{null, null, null, null, null, null, null, null, null, null};
   private final String traceID = " XAResourceID:" + nextResourceID();

   public String toString() {
      return this.traceID;
   }

   SQLServerXAResource(SQLServerConnection var1, SQLServerConnection var2, String var3) {
      this.xaLogger = SQLServerXADataSource.xaLogger;
      this.controlConnection = var2;
      this.con = var1;
      Properties var4 = var1.activeConnectionProperties;
      if (var4 == null) {
         this.sResourceManagerId = "";
      } else {
         this.sResourceManagerId = var4.getProperty(SQLServerDriverStringProperty.SERVER_NAME.toString())
            + "."
            + var4.getProperty(SQLServerDriverStringProperty.DATABASE_NAME.toString())
            + "."
            + var4.getProperty(SQLServerDriverIntProperty.PORT_NUMBER.toString());
      }

      if (this.xaLogger.isLoggable(Level.FINE)) {
         this.xaLogger.fine(this.toString() + " created by (" + var3 + ")");
      }

      this.serverInfoRetrieved = false;
      this.version = "0";
      this.instanceName = "";
      this.ArchitectureMSSQL = 0;
      this.ArchitectureOS = 0;
   }

   private synchronized SQLServerCallableStatement getXACallableStatementHandle(int var1) throws SQLServerException {
      assert var1 >= 0 && var1 <= 9;
      assert var1 < this.xaStatements.length;
      if (null != this.xaStatements[var1]) {
         return this.xaStatements[var1];
      }

      CallableStatement var2 = null;
      switch (var1) {
         case 0:
            var2 = this.controlConnection.prepareCall("{call master..xp_sqljdbc_xa_start(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            break;
         case 1:
            var2 = this.controlConnection.prepareCall("{call master..xp_sqljdbc_xa_end(?, ?, ?, ?, ?, ?, ?)}");
            break;
         case 2:
            var2 = this.controlConnection.prepareCall("{call master..xp_sqljdbc_xa_prepare(?, ?, ?, ?, ?)}");
            break;
         case 3:
            var2 = this.controlConnection.prepareCall("{call master..xp_sqljdbc_xa_commit(?, ?, ?, ?, ?, ?)}");
            break;
         case 4:
            var2 = this.controlConnection.prepareCall("{call master..xp_sqljdbc_xa_rollback(?, ?, ?, ?, ?)}");
            break;
         case 5:
            var2 = this.controlConnection.prepareCall("{call master..xp_sqljdbc_xa_forget(?, ?, ?, ?, ?)}");
            break;
         case 6:
            var2 = this.controlConnection.prepareCall("{call master..xp_sqljdbc_xa_recover(?, ?, ?, ?)}");
            break;
         case 7:
            var2 = this.controlConnection.prepareCall("{call master..xp_sqljdbc_xa_prepare_ex(?, ?, ?, ?, ?, ?)}");
            break;
         case 8:
            var2 = this.controlConnection.prepareCall("{call master..xp_sqljdbc_xa_rollback_ex(?, ?, ?, ?, ?, ?)}");
            break;
         case 9:
            var2 = this.controlConnection.prepareCall("{call master..xp_sqljdbc_xa_forget_ex(?, ?, ?, ?, ?, ?)}");
            break;
         default:
            assert false : "Bad handle request:" + var1;
      }

      this.xaStatements[var1] = (SQLServerCallableStatement)var2;
      return this.xaStatements[var1];
   }

   private synchronized void closeXAStatements() throws SQLServerException {
      for (int var1 = 0; var1 < this.xaStatements.length; var1++) {
         if (null != this.xaStatements[var1]) {
            this.xaStatements[var1].close();
            this.xaStatements[var1] = null;
         }
      }
   }

   final synchronized void close() throws SQLServerException {
      try {
         this.closeXAStatements();
      } catch (Exception var2) {
         if (this.xaLogger.isLoggable(Level.WARNING)) {
            this.xaLogger.warning(this.toString() + "Closing exception ignored: " + var2);
         }
      }

      if (null != this.controlConnection) {
         this.controlConnection.close();
      }
   }

   private String flagsDisplay(int var1) {
      if (0 == var1) {
         return "TMNOFLAGS";
      }

      StringBuilder var2 = new StringBuilder(100);
      if (0 != (8388608 & var1)) {
         var2.append("TMENDRSCAN");
      }

      if (0 != (536870912 & var1)) {
         if (var2.length() > 0) {
            var2.append("|");
         }

         var2.append("TMFAIL");
      }

      if (0 != (2097152 & var1)) {
         if (var2.length() > 0) {
            var2.append("|");
         }

         var2.append("TMJOIN");
      }

      if (0 != (1073741824 & var1)) {
         if (var2.length() > 0) {
            var2.append("|");
         }

         var2.append("TMONEPHASE");
      }

      if (0 != (134217728 & var1)) {
         if (var2.length() > 0) {
            var2.append("|");
         }

         var2.append("TMRESUME");
      }

      if (0 != (16777216 & var1)) {
         if (var2.length() > 0) {
            var2.append("|");
         }

         var2.append("TMSTARTRSCAN");
      }

      if (0 != (67108864 & var1)) {
         if (var2.length() > 0) {
            var2.append("|");
         }

         var2.append("TMSUCCESS");
      }

      if (0 != (33554432 & var1)) {
         if (var2.length() > 0) {
            var2.append("|");
         }

         var2.append("TMSUSPEND");
      }

      if (0 != (32768 & var1)) {
         if (var2.length() > 0) {
            var2.append("|");
         }

         var2.append("SSTRANSTIGHTLYCPLD");
      }

      return var2.toString();
   }

   private String cookieDisplay(byte[] var1) {
      return Util.byteToHexDisplayString(var1);
   }

   private String typeDisplay(int var1) {
      switch (var1) {
         case 0:
            return "XA_START";
         case 1:
            return "XA_END";
         case 2:
            return "XA_PREPARE";
         case 3:
            return "XA_COMMIT";
         case 4:
            return "XA_ROLLBACK";
         case 5:
            return "XA_FORGET";
         case 6:
            return "XA_RECOVER";
         default:
            return "UNKNOWN" + var1;
      }
   }

   // $VF: Could not resugar all assert statements!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private final XAReturnValue DTC_XA_Interface(int var1, Xid var2, int var3) throws XAException {
      if (this.xaLogger.isLoggable(Level.FINER)) {
         this.xaLogger
            .finer(
               this.toString()
                  + " Calling XA function for type:"
                  + this.typeDisplay(var1)
                  + " flags:"
                  + this.flagsDisplay(var3)
                  + " xid:"
                  + XidImpl.xidDisplay(var2)
            );
      }

      int var4 = 0;
      byte[] var5 = null;
      byte[] var6 = null;
      if (var2 != null) {
         var4 = var2.getFormatId();
         var5 = var2.getGlobalTransactionId();
         var6 = var2.getBranchQualifier();
      }

      String var7 = "DTC_XA_";
      int var8 = 1;
      int var9 = 0;
      XAReturnValue var10 = new XAReturnValue();
      SQLServerCallableStatement var11 = null;

      try {
         synchronized (this) {
            if (this.controlConnection == null) {
               try {
                  synchronized (xaInitLock) {
                     if (!xaInitDone) {
                        SQLServerCallableStatement var90 = null;
                        var90 = (SQLServerCallableStatement)this.controlConnection.prepareCall("{call master..xp_sqljdbc_xa_init_ex(?, ?,?)}");
                        var90.registerOutParameter(1, 4);
                        var90.registerOutParameter(2, 1);
                        var90.registerOutParameter(3, 1);

                        try {
                           var90.execute();
                        } catch (SQLServerException var28) {
                           try {
                              var90.close();
                              this.controlConnection.close();
                           } catch (SQLException var27) {
                              if (this.xaLogger.isLoggable(Level.FINER)) {
                                 this.xaLogger.finer(this.toString() + " Ignoring exception when closing failed execution. exception:" + var27);
                              }
                           }

                           if (this.xaLogger.isLoggable(Level.FINER)) {
                              this.xaLogger.finer(this.toString() + " exception:" + var28);
                           }

                           throw var28;
                        }

                        int var98 = var90.getInt(1);
                        String var16 = var90.getString(2);
                        String var17 = var90.getString(3);
                        if (this.xaLogger.isLoggable(Level.FINE)) {
                           this.xaLogger.fine(this.toString() + " Server XA DLL version:" + var17);
                        }

                        var90.close();
                        if (0 != var98) {
                           if ($assertionsDisabled || null != var16 && var16.length() > 1) {
                              this.controlConnection.close();
                              MessageFormat var18 = new MessageFormat(SQLServerException.getErrString("R_failedToInitializeXA"));
                              Object[] var19 = new Object[]{String.valueOf(var98), var16};
                              XAException var20 = new XAException(var18.format(var19));
                              var20.errorCode = var98;
                              if (this.xaLogger.isLoggable(Level.FINER)) {
                                 this.xaLogger.finer(this.toString() + " exception:" + var20);
                              }

                              throw var20;
                           }

                           throw new AssertionError();
                        }

                        xaInitDone = true;
                     }
                  }
               } catch (SQLServerException var30) {
                  MessageFormat var14 = new MessageFormat(SQLServerException.getErrString("R_failedToCreateXAConnection"));
                  Object[] var15 = new Object[]{new String(var30.getMessage())};
                  if (this.xaLogger.isLoggable(Level.FINER)) {
                     this.xaLogger.finer(this.toString() + " exception:" + var14.format(var15));
                  }

                  SQLServerException.makeFromDriverError(null, null, var14.format(var15), null, true);
               }
            }
         }

         switch (var1) {
            case 0:
               if (!this.serverInfoRetrieved) {
                  try {
                     this.serverInfoRetrieved = true;
                     String var12 = "select convert(varchar(100), SERVERPROPERTY('Edition'))as edition,  convert(varchar(100), SERVERPROPERTY('InstanceName'))as instance, convert(varchar(100), SERVERPROPERTY('ProductVersion')) as version, SUBSTRING(@@VERSION, CHARINDEX('<', @@VERSION)+2, 2)";
                     Statement var85 = this.controlConnection.createStatement();
                     ResultSet var92 = var85.executeQuery(var12);
                     var92.next();
                     String var99 = var92.getString(1);
                     this.ArchitectureMSSQL = null != var99 && var99.contains("(64-bit)") ? 64 : 32;
                     this.instanceName = var92.getString(2) == null ? "MSSQLSERVER" : var92.getString(2);
                     this.version = var92.getString(3);
                     if (null == this.version) {
                        this.version = "0";
                     } else if (-1 != this.version.indexOf(46)) {
                        this.version = this.version.substring(0, this.version.indexOf(46));
                     }

                     this.ArchitectureOS = Integer.parseInt(var92.getString(4));
                     var92.close();
                     var85.close();
                  } catch (Exception var32) {
                     if (this.xaLogger.isLoggable(Level.WARNING)) {
                        this.xaLogger.warning(this.toString() + " Cannot retrieve server information: :" + var32.getMessage());
                     }
                  }
               }

               var7 = "START:";
               var11 = this.getXACallableStatementHandle(0);
               var11.registerOutParameter(var8++, 4);
               var11.registerOutParameter(var8++, 1);
               var11.setBytes(var8++, var5);
               var11.setBytes(var8++, var6);
               var11.setInt(var8++, var3);
               var11.registerOutParameter(var8++, -2);
               var11.setInt(var8++, this.timeoutSeconds);
               var11.setInt(var8++, var4);
               var11.registerOutParameter(var8++, 1);
               var11.setInt(var8++, Integer.parseInt(this.version));
               var11.setInt(var8++, this.instanceName.length());
               var11.setBytes(var8++, this.instanceName.getBytes());
               var11.setInt(var8++, this.ArchitectureMSSQL);
               var11.setInt(var8++, this.ArchitectureOS);
               var11.setInt(var8++, this.isTransacrionTimeoutSet);
               var11.registerOutParameter(var8++, -2);
               break;
            case 1:
               var7 = "END:";
               var11 = this.getXACallableStatementHandle(1);
               var11.registerOutParameter(var8++, 4);
               var11.registerOutParameter(var8++, 1);
               var11.setBytes(var8++, var5);
               var11.setBytes(var8++, var6);
               var11.setInt(var8++, var3);
               var11.setInt(var8++, var4);
               var11.registerOutParameter(var8++, -2);
               break;
            case 2:
               var7 = "PREPARE:";
               if ((32768 & var3) == 32768) {
                  var11 = this.getXACallableStatementHandle(7);
               } else {
                  var11 = this.getXACallableStatementHandle(2);
               }

               var11.registerOutParameter(var8++, 4);
               var11.registerOutParameter(var8++, 1);
               var11.setBytes(var8++, var5);
               var11.setBytes(var8++, var6);
               if ((32768 & var3) == 32768) {
                  var11.setInt(var8++, var3);
               }

               var11.setInt(var8++, var4);
               break;
            case 3:
               var7 = "COMMIT:";
               var11 = this.getXACallableStatementHandle(3);
               var11.registerOutParameter(var8++, 4);
               var11.registerOutParameter(var8++, 1);
               var11.setBytes(var8++, var5);
               var11.setBytes(var8++, var6);
               var11.setInt(var8++, var3);
               var11.setInt(var8++, var4);
               break;
            case 4:
               var7 = "ROLLBACK:";
               if ((32768 & var3) == 32768) {
                  var11 = this.getXACallableStatementHandle(8);
               } else {
                  var11 = this.getXACallableStatementHandle(4);
               }

               var11.registerOutParameter(var8++, 4);
               var11.registerOutParameter(var8++, 1);
               var11.setBytes(var8++, var5);
               var11.setBytes(var8++, var6);
               if ((32768 & var3) == 32768) {
                  var11.setInt(var8++, var3);
               }

               var11.setInt(var8++, var4);
               break;
            case 5:
               var7 = "FORGET:";
               if ((32768 & var3) == 32768) {
                  var11 = this.getXACallableStatementHandle(9);
               } else {
                  var11 = this.getXACallableStatementHandle(5);
               }

               var11.registerOutParameter(var8++, 4);
               var11.registerOutParameter(var8++, 1);
               var11.setBytes(var8++, var5);
               var11.setBytes(var8++, var6);
               if ((32768 & var3) == 32768) {
                  var11.setInt(var8++, var3);
               }

               var11.setInt(var8++, var4);
               break;
            case 6:
               var7 = "RECOVER:";
               var11 = this.getXACallableStatementHandle(6);
               var11.registerOutParameter(var8++, 4);
               var11.registerOutParameter(var8++, 1);
               var11.setInt(var8++, var3);
               var11.registerOutParameter(var8++, -2);
               break;
            default:
               assert false : "Unknown execution type:" + var1;
         }

         var11.execute();
         var9 = var11.getInt(1);
         String var83 = var11.getString(2);
         if (var1 == 0) {
            String var86 = var11.getString(9);
            if (this.xaLogger.isLoggable(Level.FINE)) {
               this.xaLogger.fine(this.toString() + " Server XA DLL version:" + var86);
               if (null != var11.getString(16)) {
                  StringBuffer var93 = new StringBuffer(var11.getString(16));
                  var93.insert(20, '-');
                  var93.insert(16, '-');
                  var93.insert(12, '-');
                  var93.insert(8, '-');
                  this.xaLogger
                     .fine(this.toString() + " XID to UoW mapping for XA type:XA_START XID: " + XidImpl.xidDisplay(var2) + " UoW: " + var93.toString());
               }
            }
         }

         if (var1 == 1 && this.xaLogger.isLoggable(Level.FINE) && null != var11.getString(7)) {
            StringBuffer var87 = new StringBuffer(var11.getString(7));
            var87.insert(20, '-');
            var87.insert(16, '-');
            var87.insert(12, '-');
            var87.insert(8, '-');
            this.xaLogger.fine(this.toString() + " XID to UoW mapping for XA type:XA_END XID: " + XidImpl.xidDisplay(var2) + " UoW: " + var87.toString());
         }

         if (3 == var9 && 1 != var1 && 2 != var1 || 0 != var9 && 3 != var9) {
            if ($assertionsDisabled || null != var83 && var83.length() > 1) {
               MessageFormat var89 = new MessageFormat(SQLServerException.getErrString("R_failedFunctionXA"));
               Object[] var97 = new Object[]{var7, String.valueOf(var9), var83};
               XAException var104 = new XAException(var89.format(var97));
               var104.errorCode = var9;
               if (var1 == 1 && -7 == var9) {
                  try {
                     if (this.xaLogger.isLoggable(Level.FINER)) {
                        this.xaLogger.finer(this.toString() + " Begin un-enlist, enlisted count:" + this.enlistedTransactionCount);
                     }

                     this.con.JTAUnenlistConnection();
                     this.enlistedTransactionCount--;
                     if (this.xaLogger.isLoggable(Level.FINER)) {
                        this.xaLogger.finer(this.toString() + " End un-enlist, enlisted count:" + this.enlistedTransactionCount);
                     }
                  } catch (SQLServerException var26) {
                     if (this.xaLogger.isLoggable(Level.FINER)) {
                        this.xaLogger.finer(this.toString() + " Ignoring exception:" + var26);
                     }
                  }
               }

               throw var104;
            }

            throw new AssertionError();
         }

         if (var1 == 0) {
            byte[] var88 = var11.getBytes(6);
            if (var88 == null) {
               MessageFormat var94 = new MessageFormat(SQLServerException.getErrString("R_noTransactionCookie"));
               Object[] var100 = new Object[]{var7};
               SQLServerException.makeFromDriverError(null, null, var94.format(var100), null, true);
            } else {
               try {
                  if (this.xaLogger.isLoggable(Level.FINER)) {
                     this.xaLogger
                        .finer(this.toString() + " Begin enlisting, cookie:" + this.cookieDisplay(var88) + " enlisted count:" + this.enlistedTransactionCount);
                  }

                  this.con.JTAEnlistConnection(var88);
                  this.enlistedTransactionCount++;
                  if (this.xaLogger.isLoggable(Level.FINER)) {
                     this.xaLogger
                        .finer(this.toString() + " End enlisting, cookie:" + this.cookieDisplay(var88) + " enlisted count:" + this.enlistedTransactionCount);
                  }
               } catch (SQLServerException var25) {
                  MessageFormat var101 = new MessageFormat(SQLServerException.getErrString("R_failedToEnlist"));
                  Object[] var105 = new Object[]{var25.getMessage()};
                  SQLServerException.makeFromDriverError(null, null, var101.format(var105), null, true);
               }
            }
         }

         if (var1 == 1) {
            try {
               if (this.xaLogger.isLoggable(Level.FINER)) {
                  this.xaLogger.finer(this.toString() + " Begin un-enlist, enlisted count:" + this.enlistedTransactionCount);
               }

               this.con.JTAUnenlistConnection();
               this.enlistedTransactionCount--;
               if (this.xaLogger.isLoggable(Level.FINER)) {
                  this.xaLogger.finer(this.toString() + " End un-enlist, enlisted count:" + this.enlistedTransactionCount);
               }
            } catch (SQLServerException var24) {
               MessageFormat var95 = new MessageFormat(SQLServerException.getErrString("R_failedToUnEnlist"));
               Object[] var102 = new Object[]{var24.getMessage()};
               SQLServerException.makeFromDriverError(null, null, var95.format(var102), null, true);
            }
         }

         if (var1 == 6) {
            try {
               var10.bData = var11.getBytes(4);
            } catch (SQLServerException var23) {
               MessageFormat var96 = new MessageFormat(SQLServerException.getErrString("R_failedToReadRecoveryXIDs"));
               Object[] var103 = new Object[]{var23.getMessage()};
               SQLServerException.makeFromDriverError(null, null, var96.format(var103), null, true);
            }
         }
      } catch (SQLServerException var33) {
         if (this.xaLogger.isLoggable(Level.FINER)) {
            this.xaLogger.finer(this.toString() + " exception:" + var33);
         }

         XAException var13 = new XAException(var33.toString());
         var13.errorCode = -3;
         throw var13;
      }

      if (this.xaLogger.isLoggable(Level.FINER)) {
         this.xaLogger.finer(this.toString() + " Status:" + var9);
      }

      var10.nStatus = var9;
      return var10;
   }

   public void start(Xid var1, int var2) throws XAException {
      this.tightlyCoupled = var2 & 32768;
      this.DTC_XA_Interface(0, var1, var2);
   }

   public void end(Xid var1, int var2) throws XAException {
      this.DTC_XA_Interface(1, var1, var2 | this.tightlyCoupled);
   }

   public int prepare(Xid var1) throws XAException {
      boolean var2 = false;
      XAReturnValue var3 = this.DTC_XA_Interface(2, var1, this.tightlyCoupled);
      return var3.nStatus;
   }

   public void commit(Xid var1, boolean var2) throws XAException {
      this.DTC_XA_Interface(3, var1, (var2 ? 1073741824 : 0) | this.tightlyCoupled);
   }

   public void rollback(Xid var1) throws XAException {
      this.DTC_XA_Interface(4, var1, this.tightlyCoupled);
   }

   public void forget(Xid var1) throws XAException {
      this.DTC_XA_Interface(5, var1, this.tightlyCoupled);
   }

   public Xid[] recover(int var1) throws XAException {
      XAReturnValue var2 = this.DTC_XA_Interface(6, null, var1 | this.tightlyCoupled);
      int var3 = 0;
      Vector var4 = new Vector();
      if (null == var2.bData) {
         return new XidImpl[0];
      }

      while (var3 < var2.bData.length) {
         short var5 = 1;
         int var6 = 0;

         for (int var7 = 0; var7 < 4; var7++) {
            int var8 = var2.bData[var3 + var7] & 255;
            var8 *= var5;
            var6 += var8;
            var5 *= 256;
         }

         var3 += 4;
         int var18 = var2.bData[var3++] & 255;
         int var20 = var2.bData[var3++] & 255;
         byte[] var9 = new byte[var18];
         byte[] var10 = new byte[var20];
         System.arraycopy(var2.bData, var3, var9, 0, var18);
         var3 += var18;
         System.arraycopy(var2.bData, var3, var10, 0, var20);
         var3 += var20;
         XidImpl var11 = new XidImpl(var6, var9, var10);
         var4.add(var11);
      }

      XidImpl[] var16 = new XidImpl[var4.size()];

      for (int var17 = 0; var17 < var4.size(); var17++) {
         var16[var17] = (XidImpl)var4.elementAt(var17);
         if (this.xaLogger.isLoggable(Level.FINER)) {
            this.xaLogger.finer(this.toString() + var16[var17].toString());
         }
      }

      return var16;
   }

   public boolean isSameRM(XAResource var1) throws XAException {
      if (this.xaLogger.isLoggable(Level.FINER)) {
         this.xaLogger.finer(this.toString() + " xares:" + var1);
      }

      if (!(var1 instanceof SQLServerXAResource)) {
         return false;
      }

      SQLServerXAResource var2 = (SQLServerXAResource)var1;
      return var2.sResourceManagerId.equals(this.sResourceManagerId);
   }

   public boolean setTransactionTimeout(int var1) throws XAException {
      this.isTransacrionTimeoutSet = 1;
      this.timeoutSeconds = var1;
      if (this.xaLogger.isLoggable(Level.FINER)) {
         this.xaLogger.finer(this.toString() + " TransactionTimeout:" + var1);
      }

      return true;
   }

   public int getTransactionTimeout() throws XAException {
      return this.timeoutSeconds;
   }

   private static synchronized int nextResourceID() {
      baseResourceID++;
      return baseResourceID;
   }
}
