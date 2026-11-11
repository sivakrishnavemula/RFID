package com.microsoft.sqlserver.jdbc;

final class ActivityCorrelator {
   private static ThreadLocal<ActivityId> ActivityIdTls = new ThreadLocal<ActivityId>() {
      protected ActivityId initialValue() {
         return new ActivityId();
      }
   };

   static ActivityId getCurrent() {
      return (ActivityId)ActivityIdTls.get();
   }

   static ActivityId getNext() {
      ActivityId var0 = getCurrent();
      var0.Increment();
      return var0;
   }

   static void setCurrentActivityIdSentFlag() {
      ActivityId var0 = getCurrent();
      var0.setSentFlag();
   }
}
