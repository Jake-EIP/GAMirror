package com.neokii.androidautomirror;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.slashmax.aamirror.AppEntry;
import com.github.slashmax.aamirror.AppListLoader;

import java.util.List;


public class FavoritesFragment extends Fragment implements Loader.OnLoadCompleteListener<List<AppEntry>>, Loader.OnLoadCanceledListener<List<AppEntry>>
{
    GridView _gridView;
    ProgressBar _progBar;
    ListAdapter _adapter;
    AppListLoader _loader;
    List<AppEntry> _items;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        RelativeLayout layout = new RelativeLayout(getActivity());

        _gridView = new GridView(getActivity());
        _gridView.setNumColumns(4);
        _gridView.setHorizontalSpacing(Util.DP2PX(getActivity(), 8));
        _gridView.setVerticalSpacing(Util.DP2PX(getActivity(), 8));

        int p = Util.DP2PX(getActivity(), 5);
        _gridView.setPadding(p, p, p, p);
        _gridView.setClipToPadding(false);

        _gridView.setAdapter(_adapter = new ListAdapter());

        layout.addView(_gridView);

        _progBar = new ProgressBar(getActivity());

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);

        layout.addView(_progBar, params);

        _loader = new AppListLoader(getActivity());

        _loader.registerListener(0, this);
        _loader.registerOnLoadCanceledListener(this);

        _loader.startLoading();

        _progBar.setVisibility(View.VISIBLE);

        return layout;
    }

    @Override
    public void onLoadComplete(Loader<List<AppEntry>> loader, List<AppEntry> data)
    {
        _progBar.setVisibility(View.GONE);
        _items = data;
        _adapter.notifyDataSetChanged();
    }

    @Override
    public void onLoadCanceled(Loader<List<AppEntry>> loader)
    {
        _progBar.setVisibility(View.GONE);
        _items = null;
        _adapter.notifyDataSetChanged();
    }

    private class ListAdapter extends BaseAdapter
    {
        @Override
        public long getItemId(int position)
        {
            return 0;
        }

        @Override
        public Object getItem(int position)
        {
            return null;
        }

        @Override
        public int getCount()
        {
            return _items != null ? _items.size() : 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent)
        {
            View v = convertView;
            if(v == null)
            {
                v = LayoutInflater.from(getActivity()).inflate(R.layout.app_list_cell, parent, false);
            }

            AppEntry entry = _items.get(position);

            ImageView imageThumb = v.findViewById(R.id.imageIcon);
            TextView textTitle = v.findViewById(R.id.textTitle);

            imageThumb.setImageDrawable(entry.getIcon());
            textTitle.setText(entry.toString());

            if(FavoritesLoader.instance().contains(getActivity(), entry.getApplicationInfo().packageName))
            {
                v.setBackgroundColor(0xFFDDDDDD);
            }
            else
            {
                v.setBackgroundColor(0);
            }

            v.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    handleClick(position);
                }
            });

            return v;
        }
    }

    private void handleClick(int position)
    {
        AppEntry entry = _items.get(position);
        String packageName = entry.getApplicationInfo().packageName;

        Log.d("qqqqqq", packageName);

        if(FavoritesLoader.instance().contains(getActivity(), packageName))
        {
            FavoritesLoader.instance().remove(getActivity(), packageName);
        }
        else
        {
            FavoritesLoader.instance().add(getActivity(), packageName);
        }

        _adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.menu_favorites, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            startActivity(new Intent(getActivity(), MainActivity.class));
            return true;
        }
        else if(id == R.id.menu_action_deselect_all)
        {
            FavoritesLoader.instance().removeAll(getActivity());
            _adapter.notifyDataSetChanged();
        }

        return super.onOptionsItemSelected(item);
    }
}
