package com.vlad.linguisto.semantic;

public interface SChunk {

	int TEXT = 0;
	int LINK = 1;
	int COMMENT = 2;

	boolean isLink();
	String getText();
	String getStyleBegin();
	String getStyleEnd();
	int getType();
	String getLinkId();
}
