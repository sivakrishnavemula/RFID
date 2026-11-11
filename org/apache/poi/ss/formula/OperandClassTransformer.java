package org.apache.poi.ss.formula;

import org.apache.poi.ss.formula.ptg.AbstractFunctionPtg;
import org.apache.poi.ss.formula.ptg.AttrPtg;
import org.apache.poi.ss.formula.ptg.ControlPtg;
import org.apache.poi.ss.formula.ptg.FuncVarPtg;
import org.apache.poi.ss.formula.ptg.IntersectionPtg;
import org.apache.poi.ss.formula.ptg.MemAreaPtg;
import org.apache.poi.ss.formula.ptg.MemFuncPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RangePtg;
import org.apache.poi.ss.formula.ptg.UnionPtg;
import org.apache.poi.ss.formula.ptg.ValueOperatorPtg;

final class OperandClassTransformer {
   private final FormulaType _formulaType;

   public OperandClassTransformer(FormulaType formulaType) {
      this._formulaType = formulaType;
   }

   public void transformFormula(ParseNode rootNode) {
      byte rootNodeOperandClass;
      switch (this._formulaType) {
         case CELL:
            rootNodeOperandClass = 32;
            break;
         case ARRAY:
            rootNodeOperandClass = 64;
            break;
         case NAMEDRANGE:
         case DATAVALIDATION_LIST:
            rootNodeOperandClass = 0;
            break;
         default:
            throw new RuntimeException("Incomplete code - formula type (" + this._formulaType + ") not supported yet");
      }

      this.transformNode(rootNode, rootNodeOperandClass, false);
   }

   private void transformNode(ParseNode node, byte desiredOperandClass, boolean callerForceArrayFlag) {
      Ptg token = node.getToken();
      ParseNode[] children = node.getChildren();
      boolean isSimpleValueFunc = isSimpleValueFunction(token);
      if (isSimpleValueFunc) {
         boolean localForceArray = desiredOperandClass == 64;

         for (int i = 0; i < children.length; i++) {
            this.transformNode(children[i], desiredOperandClass, localForceArray);
         }

         this.setSimpleValueFuncClass((AbstractFunctionPtg)token, desiredOperandClass, callerForceArrayFlag);
      } else {
         if (isSingleArgSum(token)) {
            token = FuncVarPtg.SUM;
         }

         if (token instanceof ValueOperatorPtg
            || token instanceof ControlPtg
            || token instanceof MemFuncPtg
            || token instanceof MemAreaPtg
            || token instanceof UnionPtg
            || token instanceof IntersectionPtg) {
            byte localDesiredOperandClass = desiredOperandClass == 0 ? 32 : desiredOperandClass;

            for (int i = 0; i < children.length; i++) {
               this.transformNode(children[i], localDesiredOperandClass, callerForceArrayFlag);
            }
         } else if (token instanceof AbstractFunctionPtg) {
            this.transformFunctionNode((AbstractFunctionPtg)token, children, desiredOperandClass, callerForceArrayFlag);
         } else if (children.length > 0) {
            if (token != RangePtg.instance) {
               throw new IllegalStateException("Node should not have any children");
            }
         } else if (!token.isBaseToken()) {
            token.setClass(this.transformClass(token.getPtgClass(), desiredOperandClass, callerForceArrayFlag));
         }
      }
   }

   private static boolean isSingleArgSum(Ptg token) {
      if (token instanceof AttrPtg) {
         AttrPtg attrPtg = (AttrPtg)token;
         return attrPtg.isSum();
      } else {
         return false;
      }
   }

   private static boolean isSimpleValueFunction(Ptg token) {
      if (token instanceof AbstractFunctionPtg) {
         AbstractFunctionPtg aptg = (AbstractFunctionPtg)token;
         if (aptg.getDefaultOperandClass() != 32) {
            return false;
         }

         int numberOfOperands = aptg.getNumberOfOperands();

         for (int i = numberOfOperands - 1; i >= 0; i--) {
            if (aptg.getParameterClass(i) != 32) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private byte transformClass(byte currentOperandClass, byte desiredOperandClass, boolean callerForceArrayFlag) {
      switch (desiredOperandClass) {
         case 0:
            if (!callerForceArrayFlag) {
               return currentOperandClass;
            }

            return 0;
         case 32:
            if (!callerForceArrayFlag) {
               return 32;
            }
         case 64:
            return 64;
         default:
            throw new IllegalStateException("Unexpected operand class (" + desiredOperandClass + ")");
      }
   }

   private void transformFunctionNode(AbstractFunctionPtg afp, ParseNode[] children, byte desiredOperandClass, boolean callerForceArrayFlag) {
      byte defaultReturnOperandClass = afp.getDefaultOperandClass();
      boolean localForceArrayFlag;
      if (callerForceArrayFlag) {
         switch (defaultReturnOperandClass) {
            case 0:
               if (desiredOperandClass == 0) {
                  afp.setClass((byte)0);
               } else {
                  afp.setClass((byte)64);
               }

               localForceArrayFlag = false;
               break;
            case 32:
               afp.setClass((byte)64);
               localForceArrayFlag = true;
               break;
            case 64:
               afp.setClass((byte)64);
               localForceArrayFlag = false;
               break;
            default:
               throw new IllegalStateException("Unexpected operand class (" + defaultReturnOperandClass + ")");
         }
      } else if (defaultReturnOperandClass == desiredOperandClass) {
         localForceArrayFlag = false;
         afp.setClass(defaultReturnOperandClass);
      } else {
         switch (desiredOperandClass) {
            case 0:
               switch (defaultReturnOperandClass) {
                  case 32:
                     afp.setClass((byte)32);
                     break;
                  case 64:
                     afp.setClass((byte)64);
                     break;
                  default:
                     throw new IllegalStateException("Unexpected operand class (" + defaultReturnOperandClass + ")");
               }

               localForceArrayFlag = false;
               break;
            case 32:
               afp.setClass((byte)32);
               localForceArrayFlag = false;
               break;
            case 64:
               switch (defaultReturnOperandClass) {
                  case 0:
                     afp.setClass((byte)0);
                     break;
                  case 32:
                     afp.setClass((byte)64);
                     break;
                  default:
                     throw new IllegalStateException("Unexpected operand class (" + defaultReturnOperandClass + ")");
               }

               localForceArrayFlag = defaultReturnOperandClass == 32;
               break;
            default:
               throw new IllegalStateException("Unexpected operand class (" + desiredOperandClass + ")");
         }
      }

      for (int i = 0; i < children.length; i++) {
         ParseNode child = children[i];
         byte paramOperandClass = afp.getParameterClass(i);
         this.transformNode(child, paramOperandClass, localForceArrayFlag);
      }
   }

   private void setSimpleValueFuncClass(AbstractFunctionPtg afp, byte desiredOperandClass, boolean callerForceArrayFlag) {
      if (!callerForceArrayFlag && desiredOperandClass != 64) {
         afp.setClass((byte)32);
      } else {
         afp.setClass((byte)64);
      }
   }
}
