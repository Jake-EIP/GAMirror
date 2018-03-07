package com.neokii.androidautomirror;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FavoritesLoader implements Serializable
{
    private static final String FILE_NAME = "favorites.dat";

    private static FavoritesLoader _instance = new FavoritesLoader();
    public static FavoritesLoader instance()
    {
        return _instance;
    }

    private FavoritesLoader(){}

    private List<String> _items;

    public void add(Context context, String packageName)
    {
        if(_items == null)
            load(context);

        if(!_items.contains(packageName))
        {
            _items.add(packageName);
            save(context);
        }
    }

    public void remove(Context context, String packageName)
    {
        if(_items == null)
            load(context);

        if(_items.contains(packageName))
        {
            _items.remove(packageName);
            save(context);
        }
    }

    public void removeAll(Context context)
    {
        if(_items == null)
            load(context);

        if(_items.size() > 0)
        {
            _items.clear();
            save(context);
        }
    }

    public List<String> getItems(Context context)
    {
        if(_items == null)
            load(context);

        return _items;
    }

    public boolean contains(Context context, String packageName)
    {
        if(_items == null)
            load(context);

        return _items.contains(packageName);
    }

    public void check(Context context)
    {
        if(_items == null)
            load(context);

        PackageManager packageManager = context.getPackageManager();

        List<String> items = new ArrayList<>();

        boolean edited = false;

        for(String packageName : _items)
        {
            if(packageManager.getLaunchIntentForPackage(packageName) != null)
                items.add(packageName);
            else
                edited = true;
        }

        _items = items;

        if(edited)
            save(context);
    }

    private void load(Context context)
    {
        try
        {
            if(_items == null || _items.size() == 0)
                _items = (ArrayList<String>)readObjectFromFile(context, FILE_NAME);
        }
        catch(Exception e){}

        if(_items == null)
            _items = new ArrayList<>();
    }


    private void save(Context context)
    {
        try
        {
            witeObjectToFile(context, _items, FILE_NAME);
        }
        catch(Exception e){}
    }




    public static void witeObjectToFile(Context context, Object object, String filename) {

        ObjectOutputStream objectOut = null;
        try {

            if(object == null)
            {
                context.deleteFile(filename);
                return;
            }

            FileOutputStream fileOut = context.openFileOutput(filename, Activity.MODE_PRIVATE);
            objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(object);
            fileOut.getFD().sync();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (objectOut != null) {
                try {
                    objectOut.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static Object readObjectFromFile(Context context, String filename) {

        ObjectInputStream objectIn = null;
        Object object = null;
        try {

            FileInputStream fileIn = context.getApplicationContext().openFileInput(filename);
            objectIn = new ObjectInputStream(fileIn);
            object = objectIn.readObject();

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e) {
                }
            }
        }

        return object;
    }


}
