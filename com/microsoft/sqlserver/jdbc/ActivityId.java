package com.microsoft.sqlserver.jdbc;

import java.util.UUID;

class ActivityId {
   private final UUID Id = UUID.randomUUID();
   private long Sequence = 0L;
   private boolean isSentToServer = false;

   UUID getId() {
      return this.Id;
   }

   long getSequence() {
      return this.Sequence;
   }

   void Increment() {
      if (this.Sequence < 4294967295L) {
         this.Sequence++;
      } else {
         this.Sequence = 0L;
      }

      this.isSentToServer = false;
   }

   void setSentFlag() {
      this.isSentToServer = true;
   }

   boolean IsSentToServer() {
      return this.isSentToServer;
   }

   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append(this.Id.toString());
      var1.append("-");
      var1.append(this.Sequence);
      return var1.toString();
   }
}
