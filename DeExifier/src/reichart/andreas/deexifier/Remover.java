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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;


/**
 * Removes metadata from jpg images by extending Swingworker.<br>
 * <br>
 * {@link #setParams(File[], File, int, boolean, String, JList, JProgressBar)}
 * 
 * @author Andreas Reichart
 */
class Remover extends SwingWorker<Void, String> {
    private File[] filelist;
    // private String suffix;
    // private boolean removeIptc;
    // private boolean removeExif;
    private File path;
    private String[] jpgSuffixes = { ".jpg", ".jpeg", ".JPG", ".JPEG" };
    private DefaultListModel<String> listModel;
    private JProgressBar progressBar;
    private int progressCounter = 0;
    private int compressionQuality;
    private boolean recompress;
    private String additionalSuffix;
//    private JList<String> list;
    private int width;
    private int scalingFactor;

    void setParams(File[] fileList, File path, int compressionQuality, boolean recompress, String addSuffix,
	    JList<String> list, JProgressBar progressBar) {
	this.filelist = fileList;
	this.path = path;
	this.listModel = (DefaultListModel<String>) list.getModel();
	this.progressBar = progressBar;
	this.recompress = recompress;
	this.compressionQuality = compressionQuality;
	this.additionalSuffix = addSuffix;
//	this.list = list;
	progressBar.setMinimum(0);
	progressBar.setMaximum(fileList.length);

    }

    void setWidth(int width) {
	this.width = width;
	scalingFactor = 0;
	recompress = true;
    }

    void setScale(int scalingFactor) {
	this.scalingFactor = scalingFactor;
	width = 0;
	recompress = true;
    }

    @Override
    protected Void doInBackground() throws Exception {
	int counter = 0;

	// iterate over the fileList
	for (File f : filelist) {

	    // rename correctly
	    String destFileName = "";
	    String fName = f.getName();
	    for (int i = 4; i <= 5; i++) {
		for (String s : jpgSuffixes) {
		    if (fName.substring(fName.length() - i).equals(s)) {
			destFileName = fName.substring(0, fName.length() - i) + additionalSuffix + ".jpg";
		    }
		}
	    }

	    FileOutputStream fileOutStream = new FileOutputStream(path + File.separator + destFileName);
	    BufferedOutputStream bOutputStream = new BufferedOutputStream(fileOutStream);
	    ExifRewriter eRewriter = new ExifRewriter();

	    if (recompress) {

		// recompress

		if (width == 0 && scalingFactor == 0)
		    scalingFactor = 100;
		Resizer resizer = new Resizer();
		byte[] resizedImageByte = resizer.resize(f, width, scalingFactor, (float) compressionQuality / 100);

		eRewriter.removeExifMetadata(resizedImageByte, bOutputStream);
	    } else {

		// if no recompression should be done, we use the standard Sanselan ExifRewriter
		// if (!recompress) {

		eRewriter.removeExifMetadata(f, bOutputStream);
	    }

	    eRewriter = null;
	    bOutputStream.flush();
	    fileOutStream.flush();

	    // }

	    publish(f.toString());

	    bOutputStream.close();
	    fileOutStream.close();

	    counter++;
	    setProgress(counter);
	}
	return null;
    }

    @Override
    protected void done() {
	filelist = null;
	listModel.removeAllElements();

    }

    @Override
    protected void process(List<String> chunks) {
	for (String f : chunks) {
	    listModel.removeElement(f);
	    progressBar.setValue(progressCounter++);
	}
    }
}
