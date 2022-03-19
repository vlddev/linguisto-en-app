package com.vlad.linguisto.db;

import android.database.Cursor;

import com.vlad.linguisto.db.obj.Inf;
import com.vlad.linguisto.db.obj.Translation;
import com.vlad.linguisto.db.obj.WordType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReaderDAO {
	
	private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public ReaderDAO() {
	}

	public WordType readWordType(Cursor rs) {
		WordType ret = new WordType();
		ret.setId(rs.getInt(0)); // id
		ret.setDesc(rs.getString(1)); //desc
		ret.setComment(rs.getString(2)); //comment
		return ret;
	}

	public Inf readInf(Cursor rs) {
		Inf ret = new Inf();
		ret.setId(rs.getInt(0)); //id
		ret.setInf(rs.getString(1)); //inf
		ret.setType(rs.getInt(2)); //type
		ret.setTranscription(rs.getString(3)); //transcription
		ret.setRank(rs.getInt(4)); //rank
		ret.setKnown(rs.getInt(5) > 0); //known
		ret.setLearnDate(convertStrToDate(rs.getString(6))); //known
		if (rs.getColumnCount() > 7) {
			ret.setQuizScore(rs.getInt(7)); //quiz score
		}
		return ret;
	}

	private Date convertStrToDate(String date) {
		Date ret = null;
		if (date.length() == 10) {
			try {
				ret = simpleDateFormat.parse(date);
			} catch (ParseException e) {
			}
		}
		return ret;
	}
	
	public Translation readTranslation(Cursor rs) {
		Translation ret = new Translation();
		ret.setId(rs.getInt(0)); //id
		ret.setOrderNr(rs.getInt(1)); //order_nr
		ret.setTranslation(rs.getString(2)); //translation
		ret.setExample(rs.getString(3)); //example
		return ret;
	}
}
