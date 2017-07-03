package com.zaclimon.aceiptv.catchup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;

import com.zaclimon.aceiptv.R;
import com.zaclimon.aceiptv.data.AvContent;
import com.zaclimon.aceiptv.playback.PlaybackActivity;
import com.zaclimon.aceiptv.presenter.CardViewPresenter;
import com.zaclimon.aceiptv.util.AvContentUtil;
import com.zaclimon.aceiptv.util.Constants;
import com.zaclimon.aceiptv.util.RichFeedUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by isaac on 17-07-01.
 */

public class CatchupTvFragment extends RowsFragment {

    public static final String AV_CONTENT_TITLE_BUNDLE = "av_content_title";
    public static final String AV_CONTENT_LOGO_BUNDLE = "av_content_logo";
    public static final String AV_CONTENT_LINK_BUNDLE = "av_content_link";
    public static final String AV_CONTENT_GROUP_BUNDLE = "av_content_group";

    private final String LOG_TAG = getClass().getSimpleName();

    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        new AsyncProcessCatchup().execute();

    }

    private class AsyncProcessCatchup extends AsyncTask<Void, Void, Boolean> {

        @Override
        public Boolean doInBackground(Void... params) {
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Constants.ACE_IPTV_PREFERENCES, Context.MODE_PRIVATE);
            String username = sharedPreferences.getString(Constants.USERNAME_PREFERENCE, "");
            String password = sharedPreferences.getString(Constants.PASSWORD_PREFERENCE, "");
            String catchupUrl = getString(R.string.ace_catchup_url, username, password);

            try {
                InputStream catchupInputStream = RichFeedUtil.getInputStream(catchupUrl);
                List<AvContent> avContents = AvContentUtil.getAvContentsList(catchupInputStream);
                List<String> avGroups = AvContentUtil.getAvContentsGroup(avContents);
                List<ArrayObjectAdapter> avAdapters = getObjectAdapters(avContents, avGroups);

                for (int i = 0; i < avAdapters.size(); i++) {
                    HeaderItem catchupItem = new HeaderItem(avGroups.get(i));
                    mRowsAdapter.add(new ListRow(catchupItem, avAdapters.get(i)));
                }

                return (true);
            } catch (IOException io) {
                // Nothing to be done...
                return (false);
            }

        }

        @Override
        public void onPostExecute(Boolean result) {
            if (result) {
                setAdapter(mRowsAdapter);
                setOnItemViewClickedListener(new CatchupTvItemClickListener());
                getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
            } else {
                Log.e(LOG_TAG, "Couldn't parse TV catchup!");
            }
        }

        private List<ArrayObjectAdapter> getObjectAdapters(List<AvContent> avContents, List<String> avGroups) {

            List<ArrayObjectAdapter> tempAdapters = new ArrayList<>();

            for (String group : avGroups) {
                ArrayObjectAdapter arrayObjectAdapter = new ArrayObjectAdapter(new CardViewPresenter());

                for (AvContent content : avContents) {
                    if (content.getGroup().equals(group)) {
                        arrayObjectAdapter.add(content);
                    }
                }
                tempAdapters.add(arrayObjectAdapter);
            }
            return (tempAdapters);
        }

    }

    private class CatchupTvItemClickListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof AvContent) {
                // The item comes from an AvContent element.
                AvContent avContent = (AvContent) item;
                Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(AV_CONTENT_TITLE_BUNDLE, avContent.getTitle());
                bundle.putString(AV_CONTENT_LOGO_BUNDLE, avContent.getLogo());
                bundle.putString(AV_CONTENT_LINK_BUNDLE, avContent.getContentLink());
                bundle.putString(AV_CONTENT_GROUP_BUNDLE, avContent.getGroup());
                intent.putExtras(bundle);
                startActivity(intent);
            }
        }
    }

}