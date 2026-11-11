package org.apache.poi.ss.formula.function;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class FunctionDataBuilder {
   private int _maxFunctionIndex = -1;
   private final Map<String, FunctionMetadata> _functionDataByName;
   private final Map<Integer, FunctionMetadata> _functionDataByIndex;
   private final Set<Integer> _mutatingFunctionIndexes;

   public FunctionDataBuilder(int sizeEstimate) {
      this._functionDataByName = new HashMap(sizeEstimate * 3 / 2);
      this._functionDataByIndex = new HashMap(sizeEstimate * 3 / 2);
      this._mutatingFunctionIndexes = new HashSet();
   }

   public void add(int functionIndex, String functionName, int minParams, int maxParams, byte returnClassCode, byte[] parameterClassCodes, boolean hasFootnote) {
      FunctionMetadata fm = new FunctionMetadata(functionIndex, functionName, minParams, maxParams, returnClassCode, parameterClassCodes);
      Integer indexKey = functionIndex;
      if (functionIndex > this._maxFunctionIndex) {
         this._maxFunctionIndex = functionIndex;
      }

      FunctionMetadata prevFM = (FunctionMetadata)this._functionDataByName.get(functionName);
      if (prevFM != null) {
         if (!hasFootnote || !this._mutatingFunctionIndexes.contains(indexKey)) {
            throw new RuntimeException("Multiple entries for function name '" + functionName + "'");
         }

         this._functionDataByIndex.remove(prevFM.getIndex());
      }

      prevFM = (FunctionMetadata)this._functionDataByIndex.get(indexKey);
      if (prevFM != null) {
         if (!hasFootnote || !this._mutatingFunctionIndexes.contains(indexKey)) {
            throw new RuntimeException("Multiple entries for function index (" + functionIndex + ")");
         }

         this._functionDataByName.remove(prevFM.getName());
      }

      if (hasFootnote) {
         this._mutatingFunctionIndexes.add(indexKey);
      }

      this._functionDataByIndex.put(indexKey, fm);
      this._functionDataByName.put(functionName, fm);
   }

   public FunctionMetadataRegistry build() {
      FunctionMetadata[] jumbledArray = new FunctionMetadata[this._functionDataByName.size()];
      this._functionDataByName.values().toArray(jumbledArray);
      FunctionMetadata[] fdIndexArray = new FunctionMetadata[this._maxFunctionIndex + 1];

      for (int i = 0; i < jumbledArray.length; i++) {
         FunctionMetadata fd = jumbledArray[i];
         fdIndexArray[fd.getIndex()] = fd;
      }

      return new FunctionMetadataRegistry(fdIndexArray, this._functionDataByName);
   }
}
