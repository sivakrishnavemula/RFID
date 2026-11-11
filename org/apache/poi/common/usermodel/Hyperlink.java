package org.apache.poi.common.usermodel;

import org.apache.poi.util.Removal;

public interface Hyperlink {
   String getAddress();

   void setAddress(String var1);

   String getLabel();

   void setLabel(String var1);

   HyperlinkType getType();

   @Deprecated
   @Removal(version = "4.2")
   HyperlinkType getTypeEnum();
}
