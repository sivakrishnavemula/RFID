package org.apache.poi.ss.formula.udf;

import org.apache.poi.ss.formula.functions.FreeRefFunction;

public interface UDFFinder {
   FreeRefFunction findFunction(String var1);
}
