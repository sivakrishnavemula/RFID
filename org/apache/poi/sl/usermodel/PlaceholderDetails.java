package org.apache.poi.sl.usermodel;

public interface PlaceholderDetails {
   Placeholder getPlaceholder();

   void setPlaceholder(Placeholder var1);

   boolean isVisible();

   void setVisible(boolean var1);

   PlaceholderDetails.PlaceholderSize getSize();

   void setSize(PlaceholderDetails.PlaceholderSize var1);

   String getText();

   void setText(String var1);

   enum PlaceholderSize {
      quarter,
      half,
      full;
   }
}
