package org.apache.poi.ss.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public final class CellUtil {
   private static final POILogger log = POILogFactory.getLogger(CellUtil.class);
   public static final String ALIGNMENT = "alignment";
   public static final String BORDER_BOTTOM = "borderBottom";
   public static final String BORDER_LEFT = "borderLeft";
   public static final String BORDER_RIGHT = "borderRight";
   public static final String BORDER_TOP = "borderTop";
   public static final String BOTTOM_BORDER_COLOR = "bottomBorderColor";
   public static final String LEFT_BORDER_COLOR = "leftBorderColor";
   public static final String RIGHT_BORDER_COLOR = "rightBorderColor";
   public static final String TOP_BORDER_COLOR = "topBorderColor";
   public static final String DATA_FORMAT = "dataFormat";
   public static final String FILL_BACKGROUND_COLOR = "fillBackgroundColor";
   public static final String FILL_FOREGROUND_COLOR = "fillForegroundColor";
   public static final String FILL_PATTERN = "fillPattern";
   public static final String FONT = "font";
   public static final String HIDDEN = "hidden";
   public static final String INDENTION = "indention";
   public static final String LOCKED = "locked";
   public static final String ROTATION = "rotation";
   public static final String VERTICAL_ALIGNMENT = "verticalAlignment";
   public static final String WRAP_TEXT = "wrapText";
   private static final Set<String> shortValues = Collections.unmodifiableSet(
      new HashSet(
         Arrays.asList(
            new String[]{
               "bottomBorderColor",
               "leftBorderColor",
               "rightBorderColor",
               "topBorderColor",
               "fillForegroundColor",
               "fillBackgroundColor",
               "indention",
               "dataFormat",
               "font",
               "rotation"
            }
         )
      )
   );
   private static final Set<String> booleanValues = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[]{"locked", "hidden", "wrapText"})));
   private static final Set<String> borderTypeValues = Collections.unmodifiableSet(
      new HashSet(Arrays.asList(new String[]{"borderBottom", "borderLeft", "borderRight", "borderTop"}))
   );
   private static CellUtil.UnicodeMapping[] unicodeMappings = new CellUtil.UnicodeMapping[]{
      um("alpha", "α"),
      um("beta", "β"),
      um("gamma", "γ"),
      um("delta", "δ"),
      um("epsilon", "ε"),
      um("zeta", "ζ"),
      um("eta", "η"),
      um("theta", "θ"),
      um("iota", "ι"),
      um("kappa", "κ"),
      um("lambda", "λ"),
      um("mu", "μ"),
      um("nu", "ν"),
      um("xi", "ξ"),
      um("omicron", "ο")
   };

   private CellUtil() {
   }

   public static Row getRow(int rowIndex, Sheet sheet) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
         row = sheet.createRow(rowIndex);
      }

      return row;
   }

   public static Cell getCell(Row row, int columnIndex) {
      Cell cell = row.getCell(columnIndex);
      if (cell == null) {
         cell = row.createCell(columnIndex);
      }

      return cell;
   }

   public static Cell createCell(Row row, int column, String value, CellStyle style) {
      Cell cell = getCell(row, column);
      cell.setCellValue(cell.getRow().getSheet().getWorkbook().getCreationHelper().createRichTextString(value));
      if (style != null) {
         cell.setCellStyle(style);
      }

      return cell;
   }

   public static Cell createCell(Row row, int column, String value) {
      return createCell(row, column, value, null);
   }

   public static void setAlignment(Cell cell, HorizontalAlignment align) {
      setCellStyleProperty(cell, "alignment", align);
   }

   public static void setVerticalAlignment(Cell cell, VerticalAlignment align) {
      setCellStyleProperty(cell, "verticalAlignment", align);
   }

   public static void setFont(Cell cell, Font font) {
      Workbook wb = cell.getSheet().getWorkbook();
      int fontIndex = font.getIndexAsInt();
      if (!wb.getFontAt(fontIndex).equals(font)) {
         throw new IllegalArgumentException("Font does not belong to this workbook");
      }

      setCellStyleProperty(cell, "font", fontIndex);
   }

   public static void setCellStyleProperties(Cell cell, Map<String, Object> properties) {
      Workbook workbook = cell.getSheet().getWorkbook();
      CellStyle originalStyle = cell.getCellStyle();
      CellStyle newStyle = null;
      Map<String, Object> values = getFormatProperties(originalStyle);
      putAll(properties, values);
      int numberCellStyles = workbook.getNumCellStyles();

      for (int i = 0; i < numberCellStyles; i++) {
         CellStyle wbStyle = workbook.getCellStyleAt(i);
         Map<String, Object> wbStyleMap = getFormatProperties(wbStyle);
         if (wbStyleMap.equals(values)) {
            newStyle = wbStyle;
            break;
         }
      }

      if (newStyle == null) {
         newStyle = workbook.createCellStyle();
         setFormatProperties(newStyle, workbook, values);
      }

      cell.setCellStyle(newStyle);
   }

   public static void setCellStyleProperty(Cell cell, String propertyName, Object propertyValue) {
      Map<String, Object> property = Collections.singletonMap(propertyName, propertyValue);
      setCellStyleProperties(cell, property);
   }

   private static Map<String, Object> getFormatProperties(CellStyle style) {
      Map<String, Object> properties = new HashMap();
      put(properties, "alignment", style.getAlignment());
      put(properties, "verticalAlignment", style.getVerticalAlignment());
      put(properties, "borderBottom", style.getBorderBottom());
      put(properties, "borderLeft", style.getBorderLeft());
      put(properties, "borderRight", style.getBorderRight());
      put(properties, "borderTop", style.getBorderTop());
      put(properties, "bottomBorderColor", style.getBottomBorderColor());
      put(properties, "dataFormat", style.getDataFormat());
      put(properties, "fillPattern", style.getFillPattern());
      put(properties, "fillForegroundColor", style.getFillForegroundColor());
      put(properties, "fillBackgroundColor", style.getFillBackgroundColor());
      put(properties, "font", style.getFontIndexAsInt());
      put(properties, "hidden", style.getHidden());
      put(properties, "indention", style.getIndention());
      put(properties, "leftBorderColor", style.getLeftBorderColor());
      put(properties, "locked", style.getLocked());
      put(properties, "rightBorderColor", style.getRightBorderColor());
      put(properties, "rotation", style.getRotation());
      put(properties, "topBorderColor", style.getTopBorderColor());
      put(properties, "wrapText", style.getWrapText());
      return properties;
   }

   private static void putAll(Map<String, Object> src, Map<String, Object> dest) {
      for (String key : src.keySet()) {
         if (shortValues.contains(key)) {
            dest.put(key, getShort(src, key));
         } else if (booleanValues.contains(key)) {
            dest.put(key, getBoolean(src, key));
         } else if (borderTypeValues.contains(key)) {
            dest.put(key, getBorderStyle(src, key));
         } else if ("alignment".equals(key)) {
            dest.put(key, getHorizontalAlignment(src, key));
         } else if ("verticalAlignment".equals(key)) {
            dest.put(key, getVerticalAlignment(src, key));
         } else if ("fillPattern".equals(key)) {
            dest.put(key, getFillPattern(src, key));
         } else if (log.check(3)) {
            log.log(3, new Object[]{"Ignoring unrecognized CellUtil format properties key: " + key});
         }
      }
   }

   private static void setFormatProperties(CellStyle style, Workbook workbook, Map<String, Object> properties) {
      style.setAlignment(getHorizontalAlignment(properties, "alignment"));
      style.setVerticalAlignment(getVerticalAlignment(properties, "verticalAlignment"));
      style.setBorderBottom(getBorderStyle(properties, "borderBottom"));
      style.setBorderLeft(getBorderStyle(properties, "borderLeft"));
      style.setBorderRight(getBorderStyle(properties, "borderRight"));
      style.setBorderTop(getBorderStyle(properties, "borderTop"));
      style.setBottomBorderColor(getShort(properties, "bottomBorderColor"));
      style.setDataFormat(getShort(properties, "dataFormat"));
      style.setFillPattern(getFillPattern(properties, "fillPattern"));
      style.setFillForegroundColor(getShort(properties, "fillForegroundColor"));
      style.setFillBackgroundColor(getShort(properties, "fillBackgroundColor"));
      style.setFont(workbook.getFontAt(getInt(properties, "font")));
      style.setHidden(getBoolean(properties, "hidden"));
      style.setIndention(getShort(properties, "indention"));
      style.setLeftBorderColor(getShort(properties, "leftBorderColor"));
      style.setLocked(getBoolean(properties, "locked"));
      style.setRightBorderColor(getShort(properties, "rightBorderColor"));
      style.setRotation(getShort(properties, "rotation"));
      style.setTopBorderColor(getShort(properties, "topBorderColor"));
      style.setWrapText(getBoolean(properties, "wrapText"));
   }

   private static short getShort(Map<String, Object> properties, String name) {
      Object value = properties.get(name);
      return value instanceof Number ? ((Number)value).shortValue() : 0;
   }

   private static int getInt(Map<String, Object> properties, String name) {
      Object value = properties.get(name);
      return value instanceof Number ? ((Number)value).intValue() : 0;
   }

   private static BorderStyle getBorderStyle(Map<String, Object> properties, String name) {
      Object value = properties.get(name);
      BorderStyle border;
      if (value instanceof BorderStyle) {
         border = (BorderStyle)value;
      } else if (value instanceof Short) {
         if (log.check(5)) {
            log.log(5, new Object[]{"Deprecation warning: CellUtil properties map uses Short values for " + name + ". Should use BorderStyle enums instead."});
         }

         short code = (Short)value;
         border = BorderStyle.valueOf(code);
      } else {
         if (value != null) {
            throw new RuntimeException("Unexpected border style class. Must be BorderStyle or Short (deprecated).");
         }

         border = BorderStyle.NONE;
      }

      return border;
   }

   private static FillPatternType getFillPattern(Map<String, Object> properties, String name) {
      Object value = properties.get(name);
      FillPatternType pattern;
      if (value instanceof FillPatternType) {
         pattern = (FillPatternType)value;
      } else if (value instanceof Short) {
         if (log.check(5)) {
            log.log(
               5, new Object[]{"Deprecation warning: CellUtil properties map uses Short values for " + name + ". Should use FillPatternType enums instead."}
            );
         }

         short code = (Short)value;
         pattern = FillPatternType.forInt(code);
      } else {
         if (value != null) {
            throw new RuntimeException("Unexpected fill pattern style class. Must be FillPatternType or Short (deprecated).");
         }

         pattern = FillPatternType.NO_FILL;
      }

      return pattern;
   }

   private static HorizontalAlignment getHorizontalAlignment(Map<String, Object> properties, String name) {
      Object value = properties.get(name);
      HorizontalAlignment align;
      if (value instanceof HorizontalAlignment) {
         align = (HorizontalAlignment)value;
      } else if (value instanceof Short) {
         if (log.check(5)) {
            log.log(
               5,
               new Object[]{"Deprecation warning: CellUtil properties map used a Short value for " + name + ". Should use HorizontalAlignment enums instead."}
            );
         }

         short code = (Short)value;
         align = HorizontalAlignment.forInt(code);
      } else {
         if (value != null) {
            throw new RuntimeException("Unexpected horizontal alignment style class. Must be HorizontalAlignment or Short (deprecated).");
         }

         align = HorizontalAlignment.GENERAL;
      }

      return align;
   }

   private static VerticalAlignment getVerticalAlignment(Map<String, Object> properties, String name) {
      Object value = properties.get(name);
      VerticalAlignment align;
      if (value instanceof VerticalAlignment) {
         align = (VerticalAlignment)value;
      } else if (value instanceof Short) {
         if (log.check(5)) {
            log.log(
               5, new Object[]{"Deprecation warning: CellUtil properties map used a Short value for " + name + ". Should use VerticalAlignment enums instead."}
            );
         }

         short code = (Short)value;
         align = VerticalAlignment.forInt(code);
      } else {
         if (value != null) {
            throw new RuntimeException("Unexpected vertical alignment style class. Must be VerticalAlignment or Short (deprecated).");
         }

         align = VerticalAlignment.BOTTOM;
      }

      return align;
   }

   private static boolean getBoolean(Map<String, Object> properties, String name) {
      Object value = properties.get(name);
      return value instanceof Boolean ? (Boolean)value : false;
   }

   private static void put(Map<String, Object> properties, String name, Object value) {
      properties.put(name, value);
   }

   public static Cell translateUnicodeValues(Cell cell) {
      String s = cell.getRichStringCellValue().getString();
      boolean foundUnicode = false;
      String lowerCaseStr = s.toLowerCase(Locale.ROOT);

      for (CellUtil.UnicodeMapping entry : unicodeMappings) {
         String key = entry.entityName;
         if (lowerCaseStr.contains(key)) {
            s = s.replaceAll(key, entry.resolvedValue);
            foundUnicode = true;
         }
      }

      if (foundUnicode) {
         cell.setCellValue(cell.getRow().getSheet().getWorkbook().getCreationHelper().createRichTextString(s));
      }

      return cell;
   }

   private static CellUtil.UnicodeMapping um(String entityName, String resolvedValue) {
      return new CellUtil.UnicodeMapping(entityName, resolvedValue);
   }

   private static final class UnicodeMapping {
      public final String entityName;
      public final String resolvedValue;

      public UnicodeMapping(String pEntityName, String pResolvedValue) {
         this.entityName = "&" + pEntityName + ";";
         this.resolvedValue = pResolvedValue;
      }
   }
}
