package org.scid.android;

import java.io.File;
import java.util.Vector;

import org.scid.database.ScidProviderMetaData;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import org.scid.database.DataBaseView;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class GameListActivity extends ListActivity {
	/**
	 * maximum number of games supported in list
	 */
	private static final int MAX_GAMES = 10000;

	private ArrayAdapter<GameInfo> listAdapter;
	final static int PROGRESS_DIALOG = 0;
	private static Vector<GameInfo> gamesInFile = new Vector<GameInfo>();
	private String fileName;
	private ProgressDialog progress = null;
	private static int defaultItem = 0;
	static private long lastModTime = -1;
	static private String lastFileName = "";
	static private DataBaseView lastDataBaseView = null;
	static private String title = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final boolean reloadGameList = Boolean.parseBoolean(this.getIntent()
				.getAction());
		fileName = ((ScidApplication) this.getApplicationContext())
				.getCurrentFileName();
		listAdapter = new GameListArrayAdapter(this, R.id.item_title);
		setListAdapter(listAdapter);
		showDialog(PROGRESS_DIALOG);

		final GameListActivity gameList = this;
		new Thread(new Runnable() {
			public void run() {
				readGameInformation(reloadGameList);
				runOnUiThread(new Runnable() {
					public void run() {
						gameList.showList();
					}
				});
			}
		}).start();
	}

	private final void showList() {
		progress.dismiss();
		ListView lv = getListView();
		lv.setSelectionFromTop(defaultItem, 0);
		lv.setFastScrollEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				defaultItem = pos;
				setResult(RESULT_OK, (new Intent()).setAction("" + defaultItem));
				finish();
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			progress = new ProgressDialog(this);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setTitle(R.string.please_wait);
			progress.setMessage(getString(R.string.gamelist_loading));
			progress.setCancelable(false);
			return progress;
		default:
			return null;
		}
	}

	private final void readGameInformation(boolean reloadGameList) {
		if (!fileName.equals(lastFileName)) {
			defaultItem = 0;
		}
		long modTime = new File(fileName).lastModified();
		if (!reloadGameList
				&& (modTime == lastModTime)
				&& fileName.equals(lastFileName)
				&& lastDataBaseView != null
				&& lastDataBaseView
						.equals(((ScidApplication) getApplicationContext())
								.getGamesDataBaseView())) {
			if (GameListActivity.title.length() > 0) {
				setTitle(GameListActivity.title);
			}
			runOnUiThread(new Runnable() {
				public void run() {
					for (GameInfo info : gamesInFile) {
						listAdapter.add(info);
					}
				}
			});
			return;
		}
		lastModTime = modTime;
		lastFileName = fileName;
		lastDataBaseView = ((ScidApplication) getApplicationContext())
				.getGamesDataBaseView();
		gamesInFile.clear();
		DataBaseView dbv = getDataBaseView();
		if (dbv != null) {
			int lastPosition = dbv.getPosition();
			// disable loading of PGN information in the dbv to speeding up
			// the list view
			setPgnLoading(dbv, false);
			int noGames = dbv.getCount();
			if (noGames > MAX_GAMES) {
				// limit games shown in list
				String title = getString(R.string.gamelist) + " - " + MAX_GAMES
						+ "/" + noGames;
				setTitle(title);
				GameListActivity.title = title;
				noGames = MAX_GAMES;
			} else {
				int allGames = ((ScidApplication) getApplicationContext())
						.getNoGames();
				final String title;
				if (allGames > noGames) {
					// there's currently a filter
					title = getString(R.string.gamelist_filter) + " " + noGames
							+ "/" + allGames;
				} else {
					title = getString(R.string.gamelist);
				}
				runOnUiThread(new Runnable() {
					public void run() {
						setTitle(title);
					}
				});
			}
			final int games = noGames;
			gamesInFile.ensureCapacity(games);
			progress.setMax(100);
			int percent = -1;
			if (dbv.moveToFirst()) {
				int gameNo = 0;
				addGameInfo(dbv);
				while (gameNo < noGames && dbv.moveToNext()) {
					gameNo++;
					addGameInfo(dbv);
					final int newPercent = (int) (gameNo * 100 / noGames);
					if (newPercent > percent) {
						percent = newPercent;
						if (progress != null) {
							runOnUiThread(new Runnable() {
								public void run() {
									progress.setProgress(newPercent);
								}
							});
						}
					}
				}
				// re-enable loading of PGN data in the dbv
				setPgnLoading(dbv, true);
				// move dbv to last known position before the list view was
				// called
				// because a return without selecting an entry would mess up the
				// dbv position of the
				// currently displayed game
				dbv.moveToPosition(lastPosition);
			}
		}
	}

	private void setPgnLoading(DataBaseView dbv, boolean value) {
		setDataBaseViewValue(dbv, "loadPGN", value);
	}

	private void setDataBaseViewValue(DataBaseView dbv, String key, boolean value) {
		Bundle bundle = new Bundle();
		bundle.putBoolean(key, value);
		dbv.respond(bundle);
	}

	private DataBaseView getDataBaseView() {
		DataBaseView dbv = ((ScidApplication) this.getApplicationContext())
				.getGamesDataBaseView();
		if (dbv != null) {
			setDataBaseViewValue(dbv, "reloadIndex", true);
		}
		return dbv;
	}

	private void addGameInfo(DataBaseView dbv) {
		final GameInfo info = new GameInfo();
		info.setDetails(dbv.getString(dbv
				.getColumnIndex(ScidProviderMetaData.ScidMetaData.DETAILS)));
		info.setTitle(dbv.getString(dbv
				.getColumnIndex(ScidProviderMetaData.ScidMetaData.WHITE))
				+ " - "
				+ dbv.getString(dbv
						.getColumnIndex(ScidProviderMetaData.ScidMetaData.BLACK)));
		boolean isFavorite = Boolean
				.parseBoolean(dbv.getString(dbv
						.getColumnIndex(ScidProviderMetaData.ScidMetaData.IS_FAVORITE)));
		info.setFavorite(isFavorite);
		boolean isDeleted = Boolean.parseBoolean(dbv.getString(dbv
				.getColumnIndex(ScidProviderMetaData.ScidMetaData.IS_DELETED)));
		info.setDeleted(isDeleted);
		gamesInFile.add(info);
		runOnUiThread(new Runnable() {
			public void run() {
				listAdapter.add(info);
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		// need to destroy progress dialog in case user turns device
		if (progress != null) {
			progress.dismiss();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (progress != null) {
			progress.dismiss();
		}
	}
}
