package com.microsoft.sqlserver.jdbc;

import javax.transaction.xa.Xid;

final class XidImpl implements Xid {
   private final int formatId;
   private final byte[] gtrid;
   private final byte[] bqual;
   private final String traceID;

   public XidImpl(int var1, byte[] var2, byte[] var3) {
      this.formatId = var1;
      this.gtrid = var2;
      this.bqual = var3;
      this.traceID = " XID:" + xidDisplay(this);
   }

   public byte[] getGlobalTransactionId() {
      return this.gtrid;
   }

   public byte[] getBranchQualifier() {
      return this.bqual;
   }

   public int getFormatId() {
      return this.formatId;
   }

   public String toString() {
      return this.traceID;
   }

   static String xidDisplay(Xid var0) {
      if (null == var0) {
         return "(null)";
      }

      StringBuilder var1 = new StringBuilder(300);
      var1.append("formatId=");
      var1.append(var0.getFormatId());
      var1.append(" gtrid=");
      var1.append(Util.byteToHexDisplayString(var0.getGlobalTransactionId()));
      var1.append(" bqual=");
      var1.append(Util.byteToHexDisplayString(var0.getBranchQualifier()));
      return var1.toString();
   }
}
