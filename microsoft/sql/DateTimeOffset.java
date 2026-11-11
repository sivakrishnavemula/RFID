package microsoft.sql;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class DateTimeOffset implements Serializable, Comparable<DateTimeOffset> {
   private static final long serialVersionUID = 541973748553014280L;
   private final long utcMillis;
   private final int nanos;
   private final int minutesOffset;
   private static final int NANOS_MIN = 0;
   private static final int NANOS_MAX = 999999999;
   private static final int SS_NANOS_MAX = 9999999;
   private static final int MINUTES_OFFSET_MIN = -840;
   private static final int MINUTES_OFFSET_MAX = 840;
   private static final int HUNDRED_NANOS_PER_SECOND = 10000000;
   private String formattedValue = null;

   private DateTimeOffset(Timestamp var1, int var2) {
      if (var2 >= -840 && var2 <= 840) {
         this.minutesOffset = var2;
         int var3 = var1.getNanos();
         if (var3 >= 0 && var3 <= 999999999) {
            int var4 = (var3 + 50) / 100;
            this.nanos = 100 * (var4 % 10000000);
            this.utcMillis = var1.getTime() - var1.getNanos() / 1000000 + 1000 * (var4 / 10000000);
            assert this.minutesOffset >= -840 && this.minutesOffset <= 840 : "minutesOffset: " + this.minutesOffset;
            assert this.nanos >= 0 && this.nanos <= 999999999 : "nanos: " + this.nanos;
            assert 0 == this.nanos % 100 : "nanos: " + this.nanos;
            assert 0L == this.utcMillis % 1000L : "utcMillis: " + this.utcMillis;
         } else {
            throw new IllegalArgumentException();
         }
      } else {
         throw new IllegalArgumentException();
      }
   }

   public static DateTimeOffset valueOf(Timestamp var0, int var1) {
      return new DateTimeOffset(var0, var1);
   }

   public static DateTimeOffset valueOf(Timestamp var0, Calendar var1) {
      var1.setTimeInMillis(var0.getTime());
      return new DateTimeOffset(var0, (var1.get(15) + var1.get(16)) / 60000);
   }

   public String toString() {
      String var1 = this.formattedValue;
      if (null == var1) {
         String var2 = this.minutesOffset < 0
            ? String.format(Locale.US, "-%1$02d:%2$02d", new Object[]{-this.minutesOffset / 60, -this.minutesOffset % 60})
            : String.format(Locale.US, "+%1$02d:%2$02d", new Object[]{this.minutesOffset / 60, this.minutesOffset % 60});
         Calendar var3 = Calendar.getInstance(TimeZone.getTimeZone("GMT" + var2), Locale.US);
         var3.setTimeInMillis(this.utcMillis);
         assert this.nanos >= 0 && this.nanos <= 999999999;
         this.formattedValue = var1 = 0 == this.nanos
            ? String.format(Locale.US, "%1$tF %1$tT %2$s", new Object[]{var3, var2})
            : String.format(
               Locale.US,
               "%1$tF %1$tT.%2$s %3$s",
               new Object[]{var3, BigDecimal.valueOf(this.nanos, 9).stripTrailingZeros().toPlainString().substring(2), var2}
            );
      }

      return var1;

      throw new AssertionError();
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      }

      if (!(var1 instanceof DateTimeOffset)) {
         return false;
      }

      DateTimeOffset var2 = (DateTimeOffset)var1;
      return this.utcMillis == var2.utcMillis && this.nanos == var2.nanos && this.minutesOffset == var2.minutesOffset;
   }

   public int hashCode() {
      assert 0L == this.utcMillis % 1000L;
      long var1 = this.utcMillis / 1000L;
      int var3 = 571;
      var3 = 2011 * var3 + (int)var1;
      var3 = 3217 * var3 + (int)(var1 / 60L * 60L * 24L * 365L);
      var3 = 3919 * var3 + this.nanos / 100000;
      var3 = 4463 * var3 + this.nanos / 1000;
      var3 = 5227 * var3 + this.nanos;
      var3 = 6689 * var3 + this.minutesOffset;
      return 7577 * var3 + this.minutesOffset / 60;
   }

   public Timestamp getTimestamp() {
      Timestamp var1 = new Timestamp(this.utcMillis);
      var1.setNanos(this.nanos);
      return var1;
   }

   public int getMinutesOffset() {
      return this.minutesOffset;
   }

   public int compareTo(DateTimeOffset var1) {
      assert this.nanos >= 0;
      assert var1.nanos >= 0;
      return this.utcMillis > var1.utcMillis ? 1 : (this.utcMillis < var1.utcMillis ? -1 : this.nanos - var1.nanos);
   }

   private Object writeReplace() {
      return new DateTimeOffset.SerializationProxy(this);
   }

   private void readObject(ObjectInputStream var1) throws InvalidObjectException {
      throw new InvalidObjectException("");
   }

   private static class SerializationProxy implements Serializable {
      private final long utcMillis;
      private final int nanos;
      private final int minutesOffset;
      private static final long serialVersionUID = 664661379547314226L;

      SerializationProxy(DateTimeOffset var1) {
         this.utcMillis = var1.utcMillis;
         this.nanos = var1.nanos;
         this.minutesOffset = var1.minutesOffset;
      }

      private Object readResolve() {
         Timestamp var1 = new Timestamp(this.utcMillis);
         var1.setNanos(this.nanos);
         return new DateTimeOffset(var1, this.minutesOffset);
      }
   }
}
