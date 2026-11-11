package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;

public class SQLServerBulkCopyOptions {
   private int batchSize = 0;
   private int bulkCopyTimeout = 60;
   private boolean checkConstraints = false;
   private boolean fireTriggers = false;
   private boolean keepIdentity = false;
   private boolean keepNulls = false;
   private boolean tableLock = false;
   private boolean useInternalTransaction = false;
   private boolean allowEncryptedValueModifications = false;

   public int getBatchSize() {
      return this.batchSize;
   }

   public void setBatchSize(int var1) throws SQLServerException {
      if (var1 >= 0) {
         this.batchSize = var1;
      } else {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidNegativeArg"));
         Object[] var3 = new Object[]{"batchSize"};
         SQLServerException.makeFromDriverError(null, null, var2.format(var3), null, false);
      }
   }

   public int getBulkCopyTimeout() {
      return this.bulkCopyTimeout;
   }

   public void setBulkCopyTimeout(int var1) throws SQLServerException {
      if (var1 >= 0) {
         this.bulkCopyTimeout = var1;
      } else {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_invalidNegativeArg"));
         Object[] var3 = new Object[]{"timeout"};
         SQLServerException.makeFromDriverError(null, null, var2.format(var3), null, false);
      }
   }

   public boolean isKeepIdentity() {
      return this.keepIdentity;
   }

   public void setKeepIdentity(boolean var1) {
      this.keepIdentity = var1;
   }

   public boolean isKeepNulls() {
      return this.keepNulls;
   }

   public void setKeepNulls(boolean var1) {
      this.keepNulls = var1;
   }

   public boolean isTableLock() {
      return this.tableLock;
   }

   public void setTableLock(boolean var1) {
      this.tableLock = var1;
   }

   public boolean isUseInternalTransaction() {
      return this.useInternalTransaction;
   }

   public void setUseInternalTransaction(boolean var1) {
      this.useInternalTransaction = var1;
   }

   public boolean isCheckConstraints() {
      return this.checkConstraints;
   }

   public void setCheckConstraints(boolean var1) {
      this.checkConstraints = var1;
   }

   public boolean isFireTriggers() {
      return this.fireTriggers;
   }

   public void setFireTriggers(boolean var1) {
      this.fireTriggers = var1;
   }

   public boolean isAllowEncryptedValueModifications() {
      return this.allowEncryptedValueModifications;
   }

   public void setAllowEncryptedValueModifications(boolean var1) {
      this.allowEncryptedValueModifications = var1;
   }
}
