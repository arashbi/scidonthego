package org.scid.android;

import org.scid.android.gamelogic.Position;

import android.app.Application;
import android.database.Cursor;

public class ScidApplication extends Application {
	private Cursor gamesCursor = null;
	private String currentFileName = "";
	private Position position = null;
	private int currentGameNo;
	private int noGames;

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public String getCurrentFileName() {
		return currentFileName;
	}

	public void setCurrentFileName(String currentFileName) {
		this.currentFileName = currentFileName;
	}

	public Cursor getGamesCursor() {
		return gamesCursor;
	}

	public void setGamesCursor(Cursor gamesCursor) {
		this.gamesCursor = gamesCursor;
	}

	public int getCurrentGameNo() {
		return this.currentGameNo;
	}

	public int getNoGames() {
		return this.noGames;
	}

	public void setCurrentGameNo(int currentGameNo) {
		this.currentGameNo = currentGameNo;
	}

	public void setNoGames(int noGames) {
		this.noGames = noGames;
	}
}