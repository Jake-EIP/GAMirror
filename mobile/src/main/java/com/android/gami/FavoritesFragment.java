package com.android.gami;

import android.content.Intent;

import android.util.Log;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;


import com.github.slashmax.aamirror.AppEntry;


public class FavoritesFragment extends BaseAppSelectFragment
{
    @Override
    protected void applyCell(View view, AppEntry entry)
    {
        if(FavoritesLoader.instance().contains(getActivity(), entry.getApplicationInfo().packageName))
        {
            view.setBackgroundColor(0xFFDDDDDD);
        }
        else
        {
            view.setBackgroundColor(0);
        }
    }

    protected void handleClick(AppEntry entry)
    {
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
