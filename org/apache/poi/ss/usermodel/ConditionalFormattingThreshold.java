package org.apache.poi.ss.usermodel;

public interface ConditionalFormattingThreshold {
   ConditionalFormattingThreshold.RangeType getRangeType();

   void setRangeType(ConditionalFormattingThreshold.RangeType var1);

   String getFormula();

   void setFormula(String var1);

   Double getValue();

   void setValue(Double var1);

   enum RangeType {
      NUMBER(1, "num"),
      MIN(2, "min"),
      MAX(3, "max"),
      PERCENT(4, "percent"),
      PERCENTILE(5, "percentile"),
      UNALLOCATED(6, null),
      FORMULA(7, "formula");

      public final int id;
      public final String name;

      public String toString() {
         return this.id + " - " + this.name;
      }

      public static ConditionalFormattingThreshold.RangeType byId(int id) {
         return values()[id - 1];
      }

      public static ConditionalFormattingThreshold.RangeType byName(String name) {
         for (ConditionalFormattingThreshold.RangeType t : values()) {
            if (t.name.equals(name)) {
               return t;
            }
         }

         return null;
      }

      RangeType(int id, String name) {
         this.id = id;
         this.name = name;
      }
   }
}
