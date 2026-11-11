package org.apache.poi.ss.formula.functions;

public class Countifs extends Baseifs {
   public static final FreeRefFunction instance = new Countifs();

   protected boolean hasInitialRange() {
      return false;
   }
}
