/*******************************************************************************
 * Copyright 2012 Andreas Reichart.
 * Distributed under the terms of the GNU General Public License.
 * 
 *     This file is part of DeExifier.
 * 
 *     DeExifier is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     DeExifier is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with DeExifier.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/



/**
 * 
 */
package reichart.andreas.deexifier;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.ImageIcon;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.common.ImageMetadata;

/**
 * Resizes and recompresses images.
 * 
 * @author Andreas Reichart
 * 
 */
class Resizer {

    private ImageWriter iWriter;
    private ImageWriteParam iWriterParam;

    /**
     * 
     */
    public Resizer() {
	Iterator<ImageWriter> iterator = ImageIO.getImageWritersByFormatName("jpg");
	iWriter = iterator.next();
	iWriterParam = iWriter.getDefaultWriteParam();

	iWriterParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

    }

    public byte[] resize(File file, int width, int scaleFactor, float compressionQuality) {

	ImageIcon iIcon = new ImageIcon(file.getAbsolutePath());
	Image image = iIcon.getImage();
	int height=0;
	double scalingFactor = 0;
	if (scaleFactor>0) {
	    scalingFactor = (double) scaleFactor/(double)100;
	    width = (int) (image.getWidth(null)*scalingFactor);
	    height = (int)(image.getHeight(null)*scalingFactor);
	} else {
	     height = width*image.getHeight(null)/image.getWidth(null);
	    scalingFactor = (((double) width) / ((double) image.getWidth(null)));
	}
	BufferedImage dImage =null;
	iWriterParam.setCompressionQuality(compressionQuality);

	AffineTransform transform = new AffineTransform();
	RenderingHints hints = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
		RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
	AffineTransformOp transformOp = new AffineTransformOp(transform, hints);
	transform.scale(scalingFactor, scalingFactor);
	int orientation = getOrientation(file);
	if (orientation == 0) {
	    dImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	} else if (orientation == 3) {
	    dImage = new BufferedImage(height, width, BufferedImage.TYPE_INT_RGB);
	    transform.translate(0, image.getWidth(null));
	    transform.quadrantRotate(-1);
	} else if (orientation == 6) {
	    dImage = new BufferedImage(height, width, BufferedImage.TYPE_INT_RGB);
	    transform.translate(image.getHeight(null), 0);
	    transform.rotate(1);
	} else {
	    dImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}
	Graphics2D g2 = (Graphics2D) dImage.createGraphics();

	g2.drawImage(image, transform, null);

	ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
	ImageOutputStream ioutputStream = new MemoryCacheImageOutputStream(baOutputStream);

	iWriter.setOutput(ioutputStream);
	IIOImage iioImage = new IIOImage(dImage, null, null);

	try {
	    iWriter.write(null, iioImage, iWriterParam);
	} catch (IOException e) {
	    e.printStackTrace();
	}

	byte[] outputByte = baOutputStream.toByteArray();
	try {
	    ioutputStream.flush();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	g2.dispose();
	try {
	    ioutputStream.close();
	    baOutputStream.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return outputByte;
    }

    private int getOrientation(File file) {
	IImageMetadata metadata = null;
	try {
	    metadata = Sanselan.getMetadata(file);
	} catch (ImageReadException | IOException e) {
	    e.printStackTrace();
	}

	ArrayList<ImageMetadata.Item> metadataItems = metadata.getItems();
	for (ImageMetadata.Item item : metadataItems) {
	    if (item.getKeyword().equals("Orientation")) {
		int orientation = Integer.parseInt(item.getText());
		switch (orientation) {
		case 3:
		    return 2;
		case 8:
		    return 3;
		case 6:
		    return 1;
		case 1:
		    return 0;
		}
	    }
	}
	return 0;
    }
}
