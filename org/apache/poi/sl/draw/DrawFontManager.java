package org.apache.poi.sl.draw;

import java.awt.Font;
import java.awt.Graphics2D;
import org.apache.poi.common.usermodel.fonts.FontInfo;

public interface DrawFontManager {
   FontInfo getMappedFont(Graphics2D var1, FontInfo var2);

   FontInfo getFallbackFont(Graphics2D var1, FontInfo var2);

   String mapFontCharset(Graphics2D var1, FontInfo var2, String var3);

   Font createAWTFont(Graphics2D var1, FontInfo var2, double var3, boolean var5, boolean var6);
}
