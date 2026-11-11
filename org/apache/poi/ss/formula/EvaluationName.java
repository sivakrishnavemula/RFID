package org.apache.poi.ss.formula;

import org.apache.poi.ss.formula.ptg.NamePtg;
import org.apache.poi.ss.formula.ptg.Ptg;

public interface EvaluationName {
   String getNameText();

   boolean isFunctionName();

   boolean hasFormula();

   Ptg[] getNameDefinition();

   boolean isRange();

   NamePtg createPtg();
}
