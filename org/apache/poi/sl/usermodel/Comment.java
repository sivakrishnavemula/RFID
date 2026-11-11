package org.apache.poi.sl.usermodel;

import java.awt.geom.Point2D;
import java.util.Date;

public interface Comment {
   String getAuthor();

   void setAuthor(String var1);

   String getAuthorInitials();

   void setAuthorInitials(String var1);

   String getText();

   void setText(String var1);

   Date getDate();

   void setDate(Date var1);

   Point2D getOffset();

   void setOffset(Point2D var1);
}
