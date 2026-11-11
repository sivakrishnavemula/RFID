package org.apache.poi.ss.formula;

public interface IStabilityClassifier {
   IStabilityClassifier TOTALLY_IMMUTABLE = new IStabilityClassifier() {
      @Override
      public boolean isCellFinal(int sheetIndex, int rowIndex, int columnIndex) {
         return true;
      }
   };

   boolean isCellFinal(int var1, int var2, int var3);
}
