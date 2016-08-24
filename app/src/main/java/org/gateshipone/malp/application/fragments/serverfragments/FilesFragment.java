package org.gateshipone.malp.application.fragments.serverfragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.FileAdapter;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;
import org.gateshipone.malp.application.callbacks.PlaylistCallback;
import org.gateshipone.malp.application.loaders.FilesLoader;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDDirectory;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;

public class FilesFragment extends GenericMPDFragment<List<MPDFileEntry>> implements AbsListView.OnItemClickListener {
    public static final String EXTRA_FILENAME = "filename";
    public static final String TAG = FilesFragment.class.getSimpleName();

    /**
     * Main ListView of this fragment
     */
    private ListView mListView;

    private FABFragmentCallback mFABCallback = null;

    private FilesCallback mCallback;

    private PlaylistCallback mPlaylistCallback;

    private String mPath;

    /**
     * Save the last position here. Gets reused when the user returns to this view after selecting sme
     * albums.
     */
    private int mLastPosition;

    /**
     * Adapter used by the ListView
     */
    private FileAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.listview_layout_refreshable, container, false);

        // Get the main ListView of this fragment
        mListView = (ListView) rootView.findViewById(R.id.main_listview);

        Bundle args = getArguments();
        if (null != args) {
            mPath = args.getString(EXTRA_FILENAME);
        } else {
            mPath = "";
        }

        // Create the needed adapter for the ListView
        mAdapter = new FileAdapter(getActivity(), true);

        // Combine the two to a happy couple
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);

        // get swipe layout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(getContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                refreshContent();
            }
        });

        setHasOptionsMenu(true);

        // Return the ready inflated and configured fragment view.
        return rootView;
    }

    /**
     * Starts the loader to make sure the data is up-to-date after resuming the fragment (from background)
     */
    @Override
    public void onResume() {
        super.onResume();

        if (null != mFABCallback) {
            mFABCallback.setupFAB(true, new FABListener());
            if (mPath.equals("")) {
                mFABCallback.setupToolbar(getString(R.string.menu_files), false, true, false);
            } else {
                String[] pathSplit = mPath.split("/");
                if ( pathSplit.length > 0 ) {
                    mFABCallback.setupToolbar(pathSplit[pathSplit.length-1], false, false, false);
                } else {
                    mFABCallback.setupToolbar(mPath, false, false, false);
                }
            }
        }
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (FilesCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mPlaylistCallback = (PlaylistCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mFABCallback = (FABFragmentCallback) context;
        } catch (ClassCastException e) {
            mFABCallback = null;
        }
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getActivity().getMenuInflater();
        int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;

        MPDFileEntry file = (MPDFileEntry)mAdapter.getItem(position);

        if (file instanceof MPDFile) {
            inflater.inflate(R.menu.context_menu_track, menu);
        } else if ( file instanceof  MPDDirectory) {
            inflater.inflate(R.menu.context_menu_directory, menu);
        } else if ( file instanceof MPDPlaylist) {
            inflater.inflate(R.menu.context_menu_playlist, menu);
        }
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.action_song_enqueue:
                MPDQueryHandler.addSong(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_song_play:
                MPDQueryHandler.playSong(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_song_play_next:
                MPDQueryHandler.playSongNext(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_play_playlist:
                MPDQueryHandler.playPlaylist(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_add_playlist:
                MPDQueryHandler.loadPlaylist(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_add_directory:
                MPDQueryHandler.addDirectory(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_play_directory:
                MPDQueryHandler.playDirectory(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.fragment_menu_files, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), android.R.attr.textColor);

        Drawable drawable = menu.findItem(R.id.action_add_playlist).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_add_playlist).setIcon(drawable);

        drawable = menu.findItem(R.id.action_search).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_search).setIcon(drawable);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        searchView.setOnQueryTextListener(new SearchTextObserver());

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_directory:
                MPDQueryHandler.addDirectory(mPath);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates a new Loader that retrieves the list of playlists
     *
     * @param id
     * @param args
     * @return Newly created loader
     */
    @Override
    public Loader<List<MPDFileEntry>> onCreateLoader(int id, Bundle args) {
        return new FilesLoader(getActivity(), mPath);
    }

    /**
     * When the loader finished its loading of the data it is transferred to the adapter.
     *
     * @param loader Loader that finished its loading
     * @param data   Data that was retrieved by the laoder
     */
    @Override
    public void onLoadFinished(Loader<List<MPDFileEntry>> loader, List<MPDFileEntry> data) {
        mAdapter.swapModel(data);
        finishedLoading();

        // Reset old scroll position
        if (mLastPosition >= 0) {
            mListView.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    /**
     * Resets the loader and clears the model data set
     *
     * @param loader The loader that gets cleared.
     */
    @Override
    public void onLoaderReset(Loader<List<MPDFileEntry>> loader) {
        // Clear the model data of the used adapter
        mAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mLastPosition = i;

        MPDFileEntry file = (MPDFileEntry) mAdapter.getItem(i);

        if (file instanceof MPDDirectory) {
            mCallback.openPath(file.getPath());
        } else if (file instanceof MPDPlaylist) {
            mPlaylistCallback.openPlaylist(file.getPath());
        }
    }

    public interface FilesCallback {
        void openPath(String path);
    }

    private class FABListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MPDQueryHandler.playDirectory(mPath);
        }
    }

    public void applyFilter(String name) {
        mAdapter.applyFilter(name);
    }

    public void removeFilter() {
        mAdapter.removeFilter();
    }

    private class SearchTextObserver implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            if (!query.isEmpty()) {
                applyFilter(query);
            } else {
                removeFilter();
            }
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (!newText.isEmpty()) {
                applyFilter(newText);
            } else {
                removeFilter();
            }

            return true;
        }

    }

}