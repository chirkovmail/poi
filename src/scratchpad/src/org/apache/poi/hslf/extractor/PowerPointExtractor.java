
/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
        


package org.apache.poi.hslf.extractor;

import java.io.*;
import java.util.HashSet;

import org.apache.poi.POITextExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.hslf.*;
import org.apache.poi.hslf.model.*;
import org.apache.poi.hslf.usermodel.*;

/**
 * This class can be used to extract text from a PowerPoint file.
 *  Can optionally also get the notes from one.
 *
 * @author Nick Burch
 */

public class PowerPointExtractor extends POITextExtractor
{
	private HSLFSlideShow _hslfshow;
	private SlideShow _show;
	private Slide[] _slides;
	
	private boolean slidesByDefault = true;
	private boolean notesByDefault = false;

  /**
   * Basic extractor. Returns all the text, and optionally all the notes
   */
  public static void main(String args[]) throws IOException
  {
	if(args.length < 1) {
		System.err.println("Useage:");
		System.err.println("\tPowerPointExtractor [-notes] <file>");
		System.exit(1);
	}

	boolean notes = false;
	String file;
	if(args.length > 1) {
		notes = true;
		file = args[1];
	} else {
		file = args[0];
	}

	PowerPointExtractor ppe = new PowerPointExtractor(file);
	System.out.println(ppe.getText(true,notes));
	ppe.close();
  }

	/**
	 * Creates a PowerPointExtractor, from a file
	 * @param fileName The name of the file to extract from
	 */
	public PowerPointExtractor(String fileName) throws IOException {
		this(new FileInputStream(fileName));
	}
	/**
	 * Creates a PowerPointExtractor, from an Input Stream
	 * @param iStream The input stream containing the PowerPoint document
	 */
	public PowerPointExtractor(InputStream iStream) throws IOException {
		this(new POIFSFileSystem(iStream));
	}
	/**
	 * Creates a PowerPointExtractor, from an open POIFSFileSystem
	 * @param fs the POIFSFileSystem containing the PowerPoint document
	 */
	public PowerPointExtractor(POIFSFileSystem fs) throws IOException {
		this(new HSLFSlideShow(fs));
	}

	/**
	 * Creates a PowerPointExtractor, from a HSLFSlideShow
	 * @param ss the HSLFSlideShow to extract text from
	 */
	public PowerPointExtractor(HSLFSlideShow ss) throws IOException {
		super(ss);
		_hslfshow = ss;
		_show = new SlideShow(_hslfshow);
		_slides = _show.getSlides();
	}

	/**
	 * Shuts down the underlying streams
	 */
	public void close() throws IOException {
		_hslfshow.close();
		_hslfshow = null;
		_show = null;
		_slides = null;
	}

	/**
	 * Should a call to getText() return slide text?
	 * Default is yes
	 */
	public void setSlidesByDefault(boolean slidesByDefault) {
		this.slidesByDefault = slidesByDefault;
	}
	/**
	 * Should a call to getText() return notes text?
	 * Default is no
	 */
	public void setNotesByDefault(boolean notesByDefault) {
		this.notesByDefault = notesByDefault;
	}

	/**
	 * Fetches all the slide text from the slideshow, 
	 *  but not the notes, unless you've called
	 *  setSlidesByDefault() and setNotesByDefault()
	 *  to change this
	 */
	public String getText() {
		return getText(slidesByDefault,notesByDefault);
	}

	/**
	 * Fetches all the notes text from the slideshow, but not the slide text
	 */
	public String getNotes() {
		return getText(false,true);
	}

  /**
   * Fetches text from the slideshow, be it slide text or note text.
   * Because the final block of text in a TextRun normally have their
   *  last \n stripped, we add it back
   * @param getSlideText fetch slide text
   * @param getNoteText fetch note text
   */
  public String getText(boolean getSlideText, boolean getNoteText) {
	StringBuffer ret = new StringBuffer(); 

	if(getSlideText) {
		for(int i=0; i<_slides.length; i++) {
			Slide slide = _slides[i];
			TextRun[] runs = slide.getTextRuns();
			for(int j=0; j<runs.length; j++) {
				TextRun run = runs[j];
				if(run != null) {
					String text = run.getText();
					ret.append(text);
					if(! text.endsWith("\n")) {
						ret.append("\n");
					}
				}
			}
		}
		if(getNoteText) {
			ret.append("\n");
		}
	}

	if(getNoteText) {
		// Not currently using _notes, as that can have the notes of
		//  master sheets in. Grab Slide list, then work from there,
		//  but ensure no duplicates
		HashSet seenNotes = new HashSet();
		for(int i=0; i<_slides.length; i++) {
			Notes notes = _slides[i].getNotesSheet();
			if(notes == null) { continue; }
			Integer id = new Integer(notes._getSheetNumber());
			if(seenNotes.contains(id)) { continue; }
			seenNotes.add(id);

			TextRun[] runs = notes.getTextRuns();
			if(runs != null && runs.length > 0) {
				for(int j=0; j<runs.length; j++) {
					TextRun run = runs[j];
					String text = run.getText();
					ret.append(text);
					if(! text.endsWith("\n")) {
						ret.append("\n");
					}
				}
			}
		}
	}

	return ret.toString();
  }
}