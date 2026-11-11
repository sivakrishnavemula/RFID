package org.apache.poi.sl.draw.binding;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CT_ColorMRU", propOrder = "egColorChoice")
public class CTColorMRU {
   @XmlElements(
      {
            @XmlElement(name = "scrgbClr", type = CTScRgbColor.class),
            @XmlElement(name = "srgbClr", type = CTSRgbColor.class),
            @XmlElement(name = "hslClr", type = CTHslColor.class),
            @XmlElement(name = "sysClr", type = CTSystemColor.class),
            @XmlElement(name = "schemeClr", type = CTSchemeColor.class),
            @XmlElement(name = "prstClr", type = CTPresetColor.class)
      }
   )
   protected List<Object> egColorChoice;

   public List<Object> getEGColorChoice() {
      if (this.egColorChoice == null) {
         this.egColorChoice = new ArrayList();
      }

      return this.egColorChoice;
   }

   public boolean isSetEGColorChoice() {
      return this.egColorChoice != null && !this.egColorChoice.isEmpty();
   }

   public void unsetEGColorChoice() {
      this.egColorChoice = null;
   }
}
