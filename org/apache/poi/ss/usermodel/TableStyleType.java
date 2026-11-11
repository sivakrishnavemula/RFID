package org.apache.poi.ss.usermodel;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressBase;
import org.apache.poi.ss.util.CellReference;

public enum TableStyleType {
   wholeTable {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         return new CellRangeAddress(table.getStartRowIndex(), table.getEndRowIndex(), table.getStartColIndex(), table.getEndColIndex());
      }
   },
   pageFieldLabels,
   pageFieldValues,
   firstColumnStripe {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         TableStyleInfo info = table.getStyle();
         if (!info.isShowColumnStripes()) {
            return null;
         }

         DifferentialStyleProvider c1Style = info.getStyle().getStyle(firstColumnStripe);
         DifferentialStyleProvider c2Style = info.getStyle().getStyle(secondColumnStripe);
         int c1Stripe = c1Style == null ? 1 : Math.max(1, c1Style.getStripeSize());
         int c2Stripe = c2Style == null ? 1 : Math.max(1, c2Style.getStripeSize());
         int firstStart = table.getStartColIndex();
         int secondStart = firstStart + c1Stripe;

         for (int c = cell.getCol(); firstStart <= c; secondStart = firstStart + c1Stripe) {
            if (c <= secondStart - 1) {
               return new CellRangeAddress(table.getStartRowIndex(), table.getEndRowIndex(), firstStart, secondStart - 1);
            }

            firstStart = secondStart + c2Stripe;
         }

         return null;
      }
   },
   secondColumnStripe {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         TableStyleInfo info = table.getStyle();
         if (!info.isShowColumnStripes()) {
            return null;
         }

         DifferentialStyleProvider c1Style = info.getStyle().getStyle(firstColumnStripe);
         DifferentialStyleProvider c2Style = info.getStyle().getStyle(secondColumnStripe);
         int c1Stripe = c1Style == null ? 1 : Math.max(1, c1Style.getStripeSize());
         int c2Stripe = c2Style == null ? 1 : Math.max(1, c2Style.getStripeSize());
         int firstStart = table.getStartColIndex();
         int secondStart = firstStart + c1Stripe;

         for (int c = cell.getCol(); firstStart <= c; secondStart = firstStart + c1Stripe) {
            if (c >= secondStart && c <= secondStart + c2Stripe - 1) {
               return new CellRangeAddress(table.getStartRowIndex(), table.getEndRowIndex(), secondStart, secondStart + c2Stripe - 1);
            }

            firstStart = secondStart + c2Stripe;
         }

         return null;
      }
   },
   firstRowStripe {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         TableStyleInfo info = table.getStyle();
         if (!info.isShowRowStripes()) {
            return null;
         }

         DifferentialStyleProvider c1Style = info.getStyle().getStyle(firstRowStripe);
         DifferentialStyleProvider c2Style = info.getStyle().getStyle(secondRowStripe);
         int c1Stripe = c1Style == null ? 1 : Math.max(1, c1Style.getStripeSize());
         int c2Stripe = c2Style == null ? 1 : Math.max(1, c2Style.getStripeSize());
         int firstStart = table.getStartRowIndex() + table.getHeaderRowCount();
         int secondStart = firstStart + c1Stripe;

         for (int c = cell.getRow(); firstStart <= c; secondStart = firstStart + c1Stripe) {
            if (c <= secondStart - 1) {
               return new CellRangeAddress(firstStart, secondStart - 1, table.getStartColIndex(), table.getEndColIndex());
            }

            firstStart = secondStart + c2Stripe;
         }

         return null;
      }
   },
   secondRowStripe {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         TableStyleInfo info = table.getStyle();
         if (!info.isShowRowStripes()) {
            return null;
         }

         DifferentialStyleProvider c1Style = info.getStyle().getStyle(firstRowStripe);
         DifferentialStyleProvider c2Style = info.getStyle().getStyle(secondRowStripe);
         int c1Stripe = c1Style == null ? 1 : Math.max(1, c1Style.getStripeSize());
         int c2Stripe = c2Style == null ? 1 : Math.max(1, c2Style.getStripeSize());
         int firstStart = table.getStartRowIndex() + table.getHeaderRowCount();
         int secondStart = firstStart + c1Stripe;

         for (int c = cell.getRow(); firstStart <= c; secondStart = firstStart + c1Stripe) {
            if (c >= secondStart && c <= secondStart + c2Stripe - 1) {
               return new CellRangeAddress(secondStart, secondStart + c2Stripe - 1, table.getStartColIndex(), table.getEndColIndex());
            }

            firstStart = secondStart + c2Stripe;
         }

         return null;
      }
   },
   lastColumn {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         return !table.getStyle().isShowLastColumn()
            ? null
            : new CellRangeAddress(table.getStartRowIndex(), table.getEndRowIndex(), table.getEndColIndex(), table.getEndColIndex());
      }
   },
   firstColumn {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         return !table.getStyle().isShowFirstColumn()
            ? null
            : new CellRangeAddress(table.getStartRowIndex(), table.getEndRowIndex(), table.getStartColIndex(), table.getStartColIndex());
      }
   },
   headerRow {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         return table.getHeaderRowCount() < 1
            ? null
            : new CellRangeAddress(
               table.getStartRowIndex(), table.getStartRowIndex() + table.getHeaderRowCount() - 1, table.getStartColIndex(), table.getEndColIndex()
            );
      }
   },
   totalRow {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         return table.getTotalsRowCount() < 1
            ? null
            : new CellRangeAddress(
               table.getEndRowIndex() - table.getTotalsRowCount() + 1, table.getEndRowIndex(), table.getStartColIndex(), table.getEndColIndex()
            );
      }
   },
   firstHeaderCell {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         return table.getHeaderRowCount() < 1
            ? null
            : new CellRangeAddress(table.getStartRowIndex(), table.getStartRowIndex(), table.getStartColIndex(), table.getStartColIndex());
      }
   },
   lastHeaderCell {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         return table.getHeaderRowCount() < 1
            ? null
            : new CellRangeAddress(table.getStartRowIndex(), table.getStartRowIndex(), table.getEndColIndex(), table.getEndColIndex());
      }
   },
   firstTotalCell {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         return table.getTotalsRowCount() < 1
            ? null
            : new CellRangeAddress(
               table.getEndRowIndex() - table.getTotalsRowCount() + 1, table.getEndRowIndex(), table.getStartColIndex(), table.getStartColIndex()
            );
      }
   },
   lastTotalCell {
      @Override
      public CellRangeAddressBase getRange(Table table, CellReference cell) {
         return table.getTotalsRowCount() < 1
            ? null
            : new CellRangeAddress(
               table.getEndRowIndex() - table.getTotalsRowCount() + 1, table.getEndRowIndex(), table.getEndColIndex(), table.getEndColIndex()
            );
      }
   },
   firstSubtotalColumn,
   secondSubtotalColumn,
   thirdSubtotalColumn,
   blankRow,
   firstSubtotalRow,
   secondSubtotalRow,
   thirdSubtotalRow,
   firstColumnSubheading,
   secondColumnSubheading,
   thirdColumnSubheading,
   firstRowSubheading,
   secondRowSubheading,
   thirdRowSubheading;

   TableStyleType() {
   }

   public CellRangeAddressBase appliesTo(Table table, Cell cell) {
      return cell == null
         ? null
         : this.appliesTo(table, new CellReference(cell.getSheet().getSheetName(), cell.getRowIndex(), cell.getColumnIndex(), true, true));
   }

   public CellRangeAddressBase appliesTo(Table table, CellReference cell) {
      if (table != null && cell != null) {
         if (!cell.getSheetName().equals(table.getSheetName())) {
            return null;
         }

         if (!table.contains(cell)) {
            return null;
         }

         CellRangeAddressBase range = this.getRange(table, cell);
         return range != null && range.isInRange(cell.getRow(), cell.getCol()) ? range : null;
      } else {
         return null;
      }
   }

   public final CellRangeAddressBase getRange(Table table, Cell cell) {
      return cell == null
         ? null
         : this.getRange(table, new CellReference(cell.getSheet().getSheetName(), cell.getRowIndex(), cell.getColumnIndex(), true, true));
   }

   public CellRangeAddressBase getRange(Table table, CellReference cell) {
      return null;
   }
}
