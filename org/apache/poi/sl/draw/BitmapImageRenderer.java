package org.apache.poi.sl.draw;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public class BitmapImageRenderer implements ImageRenderer {
   private static final POILogger LOG = POILogFactory.getLogger(ImageRenderer.class);
   protected BufferedImage img;

   public void loadImage(InputStream data, String contentType) throws IOException {
      this.img = readImage(data, contentType);
   }

   public void loadImage(byte[] data, String contentType) throws IOException {
      this.img = readImage(new ByteArrayInputStream(data), contentType);
   }

   private static BufferedImage readImage(InputStream data, String contentType) throws IOException {
      IOException lastException = null;
      BufferedImage img = null;
      ByteArrayInputStream bis;
      if (data instanceof ByteArrayInputStream) {
         bis = (ByteArrayInputStream)data;
      } else {
         ByteArrayOutputStream bos = new ByteArrayOutputStream(262143);
         IOUtils.copy(data, bos);
         bis = new ByteArrayInputStream(bos.toByteArray());
      }

      ImageInputStream iis = new MemoryCacheImageInputStream(bis);

      try {
         Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);

         while (img == null && iter.hasNext()) {
            ImageReader reader = (ImageReader)iter.next();
            ImageReadParam param = reader.getDefaultReadParam();

            for (int mode = 0; img == null && mode < 3; mode++) {
               lastException = null;
               if (mode > 0) {
                  bis.reset();
                  iis.close();
                  iis = new MemoryCacheImageInputStream(bis);
               }

               try {
                  switch (mode) {
                     case 0:
                        reader.setInput(iis, false, true);
                        img = reader.read(0, param);
                        break;
                     case 1:
                        Iterator<ImageTypeSpecifier> imageTypesx = reader.getImageTypes(0);

                        while (imageTypesx.hasNext()) {
                           ImageTypeSpecifier imageTypeSpecifier = (ImageTypeSpecifier)imageTypesx.next();
                           int bufferedImageType = imageTypeSpecifier.getBufferedImageType();
                           if (bufferedImageType == 10) {
                              param.setDestinationType(imageTypeSpecifier);
                              break;
                           }
                        }

                        reader.setInput(iis, false, true);
                        img = reader.read(0, param);
                        break;
                     case 2:
                        reader.setInput(iis, false, true);
                        int height = reader.getHeight(0);
                        int width = reader.getWidth(0);
                        Iterator<ImageTypeSpecifier> imageTypes = reader.getImageTypes(0);
                        if (imageTypes.hasNext()) {
                           ImageTypeSpecifier imageTypeSpecifier = (ImageTypeSpecifier)imageTypes.next();
                           img = imageTypeSpecifier.createBufferedImage(width, height);
                           param.setDestination(img);

                           try {
                              reader.read(0, param);
                           } finally {
                              if (img.getType() != 2) {
                                 int y = findTruncatedBlackBox(img, width, height);
                                 if (y < height) {
                                    BufferedImage argbImg = new BufferedImage(width, height, 2);
                                    Graphics2D g = argbImg.createGraphics();
                                    g.clipRect(0, 0, width, y);
                                    g.drawImage(img, 0, 0, null);
                                    g.dispose();
                                    img.flush();
                                    img = argbImg;
                                 }
                              }
                           }
                        } else {
                           lastException = new IOException("unable to load even a truncated version of the image.");
                        }
                  }
               } catch (IOException e) {
                  if (mode < 2) {
                     lastException = e;
                  }
               } catch (RuntimeException e) {
                  if (mode < 2) {
                     lastException = new IOException("ImageIO runtime exception - " + (mode == 0 ? "normal" : "fallback"), e);
                  }
               }
            }

            reader.dispose();
         }
      } finally {
         iis.close();
      }

      if (img == null) {
         if (lastException != null) {
            throw lastException;
         }

         LOG.log(5, new Object[]{"Content-type: " + contentType + " is not support. Image ignored."});
         return null;
      } else if (img.getType() != 2) {
         BufferedImage argbImg = new BufferedImage(img.getWidth(), img.getHeight(), 2);
         Graphics g = argbImg.getGraphics();
         g.drawImage(img, 0, 0, null);
         g.dispose();
         return argbImg;
      } else {
         return img;
      }
   }

   private static int findTruncatedBlackBox(BufferedImage img, int width, int height) {
      for (int h = height - 1; h > 0; h--) {
         for (int w = width - 1; w > 0; w -= width / 10) {
            int p = img.getRGB(w, h);
            if (p != -16777216) {
               return h + 1;
            }
         }
      }

      return 0;
   }

   public BufferedImage getImage() {
      return this.img;
   }

   public BufferedImage getImage(Dimension dim) {
      double w_old = this.img.getWidth();
      double h_old = this.img.getHeight();
      BufferedImage scaled = new BufferedImage((int)w_old, (int)h_old, 2);
      double w_new = dim.getWidth();
      double h_new = dim.getHeight();
      AffineTransform at = new AffineTransform();
      at.scale(w_new / w_old, h_new / h_old);
      AffineTransformOp scaleOp = new AffineTransformOp(at, 2);
      scaleOp.filter(this.img, scaled);
      return scaled;
   }

   public Dimension getDimension() {
      return this.img == null ? new Dimension(0, 0) : new Dimension(this.img.getWidth(), this.img.getHeight());
   }

   public void setAlpha(double alpha) {
      if (this.img != null) {
         Dimension dim = this.getDimension();
         BufferedImage newImg = new BufferedImage((int)dim.getWidth(), (int)dim.getHeight(), 2);
         Graphics2D g = newImg.createGraphics();
         RescaleOp op = new RescaleOp(new float[]{1.0F, 1.0F, 1.0F, (float)alpha}, new float[]{0.0F, 0.0F, 0.0F, 0.0F}, null);
         g.drawImage(this.img, op, 0, 0);
         g.dispose();
         this.img = newImg;
      }
   }

   public boolean drawImage(Graphics2D graphics, Rectangle2D anchor) {
      return this.drawImage(graphics, anchor, null);
   }

   public boolean drawImage(Graphics2D graphics, Rectangle2D anchor, Insets clip) {
      if (this.img == null) {
         return false;
      }

      boolean isClipped = true;
      if (clip == null) {
         isClipped = false;
         clip = new Insets(0, 0, 0, 0);
      }

      int iw = this.img.getWidth();
      int ih = this.img.getHeight();
      double cw = (100000 - clip.left - clip.right) / 100000.0;
      double ch = (100000 - clip.top - clip.bottom) / 100000.0;
      double sx = anchor.getWidth() / (iw * cw);
      double sy = anchor.getHeight() / (ih * ch);
      double tx = anchor.getX() - iw * sx * clip.left / 100000.0;
      double ty = anchor.getY() - ih * sy * clip.top / 100000.0;
      AffineTransform at = new AffineTransform(sx, 0.0, 0.0, sy, tx, ty);
      Shape clipOld = graphics.getClip();
      if (isClipped) {
         graphics.clip(anchor.getBounds2D());
      }

      graphics.drawRenderedImage(this.img, at);
      graphics.setClip(clipOld);
      return true;
   }
}
