package org.apache.poi.sl.draw.geom;

import java.io.InputStream;
import java.util.LinkedHashMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.poi.sl.draw.binding.CTCustomGeometry2D;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StaxHelper;

public class PresetGeometries extends LinkedHashMap<String, CustomGeometry> {
   private static final POILogger LOG = POILogFactory.getLogger(PresetGeometries.class);
   protected static final String BINDING_PACKAGE = "org.apache.poi.sl.draw.binding";
   protected static PresetGeometries _inst;

   protected PresetGeometries() {
   }

   public void init(InputStream is) throws XMLStreamException, JAXBException {
      EventFilter startElementFilter = new EventFilter() {
         public boolean accept(XMLEvent event) {
            return event.isStartElement();
         }
      };
      XMLInputFactory staxFactory = StaxHelper.newXMLInputFactory();
      XMLEventReader staxReader = staxFactory.createXMLEventReader(is);
      XMLEventReader staxFiltRd = staxFactory.createFilteredReader(staxReader, startElementFilter);
      staxFiltRd.nextEvent();
      JAXBContext jaxbContext = JAXBContext.newInstance("org.apache.poi.sl.draw.binding");
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      long cntElem = 0L;

      while (staxFiltRd.peek() != null) {
         StartElement evRoot = (StartElement)staxFiltRd.peek();
         String name = evRoot.getName().getLocalPart();
         JAXBElement<CTCustomGeometry2D> el = unmarshaller.unmarshal(staxReader, CTCustomGeometry2D.class);
         CTCustomGeometry2D cus = (CTCustomGeometry2D)el.getValue();
         cntElem++;
         if (this.containsKey(name)) {
            LOG.log(5, new Object[]{"Duplicate definition of " + name});
         }

         this.put(name, new CustomGeometry(cus));
      }
   }

   public static CustomGeometry convertCustomGeometry(XMLStreamReader staxReader) {
      try {
         JAXBContext jaxbContext = JAXBContext.newInstance("org.apache.poi.sl.draw.binding");
         Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
         JAXBElement<CTCustomGeometry2D> el = unmarshaller.unmarshal(staxReader, CTCustomGeometry2D.class);
         return new CustomGeometry((CTCustomGeometry2D)el.getValue());
      } catch (JAXBException e) {
         LOG.log(7, new Object[]{"Unable to parse single custom geometry", e});
         return null;
      }
   }

   public static synchronized PresetGeometries getInstance() {
      if (_inst == null) {
         PresetGeometries lInst = new PresetGeometries();

         try {
            InputStream is = PresetGeometries.class.getResourceAsStream("presetShapeDefinitions.xml");

            try {
               lInst.init(is);
            } finally {
               is.close();
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }

         _inst = lInst;
      }

      return _inst;
   }
}
