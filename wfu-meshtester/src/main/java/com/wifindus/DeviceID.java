package com.wifindus;

import android.content.SharedPreferences;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by marzer on 19/03/2015.
 */
public class DeviceID
{
	private int id = -1;
	private String idHex = "";
	private static final String TAG = "DeviceID";

	public DeviceID(SharedPreferences prefs, String subdir)
	{
		//deal with storage locations first
		String storages[] = Static.getStorageDirectories();
		List<File> validFiles = new ArrayList<File>();
		for (String storage : storages)
		{
			File f = new File(storage + "/" + subdir);
			Log.i(TAG,"Checking paths in " + f.getAbsolutePath());
			if ((f.exists() && !f.isDirectory()) || (!f.exists() && !f.mkdirs()))
				Log.e(TAG,"Invalid paths in " + f.getAbsolutePath() + "!");
			else
				validFiles.add(new File(f, ".id"));
		}

		//reading
		Log.i(TAG,"Reading ID from preferences...");
		id = readFromPrefs(prefs);
		for (int i = 0; i < validFiles.size() && id <= 0; i++)
		{
			File f = validFiles.get(i);
			Log.i(TAG,"Reading ID from "+f.getAbsolutePath()+"...");
			id = readFromStorage(f);
		}
		if (id <= 0)
		{
			Log.i(TAG, "Generating random ID.");
			id = generateRandom();
		}

		//writing
		Log.i(TAG,"Writing ID to preferences...");
		writeToPrefs(prefs);
		for (int i = 0; i < validFiles.size(); i++)
		{
			File f = validFiles.get(i);
			Log.i(TAG,"Writing ID to "+f.getAbsolutePath()+"...");
			writeToStorage(validFiles.get(i));
		}

		//hex
		idHex = Integer.toHexString(id).toUpperCase();
		Log.i(TAG,"Final ID: " + idHex);
	}

	private int readFromPrefs(SharedPreferences prefs)
	{
		if (prefs == null)
			return -1;

		int id = -1;
		try
		{
			id = prefs.getInt("id",-1);
		}
		catch(ClassCastException e)
		{
			Log.e(TAG,"ClassCastException thrown reading ID from preferences!");
		}
		return id;
	}

	private int readFromStorage(File idFile)
	{
		if (idFile == null || !idFile.exists())
			return -1;

		int id = -1;
		try
		{
			Scanner scanner = new Scanner(idFile);
			while (id <= 0 && scanner.hasNextInt(16))
				id = scanner.nextInt(16);
		}
		catch (Exception e)
		{
			Log.e(TAG,e.getClass().getSimpleName() + " thrown reading ID from "+idFile.getAbsolutePath()+"!");
		}
		return id;
	}

	private int generateRandom()
	{
		return 1 + (int) (2147483646 * new Random().nextDouble());
	}

	private void writeToStorage(File idFile)
	{
		if (idFile == null)
			return;
		if (id <= 0)
			throw new IllegalStateException("The device ID has not been initialized yet.");

		try
		{
			FileOutputStream fOut = new FileOutputStream(idFile);
			OutputStreamWriter osw = new OutputStreamWriter(fOut);
			osw.write(Integer.toHexString(id) + "\n");
			osw.flush();
			osw.close();
		}
		catch (Exception e)
		{
			Log.e(TAG,e.getClass().getSimpleName() + " thrown writing ID to "+idFile.getAbsolutePath()+"!");
		}
	}

	private void writeToPrefs(SharedPreferences prefs)
	{
		if (prefs == null)
			return;
		if (id <= 0)
			throw new IllegalStateException("The device ID has not been initialized yet.");
		SharedPreferences.Editor prefsEditor = prefs.edit();
		if (prefsEditor == null)
			return;
		try
		{
			prefsEditor.putInt("id", id);
			prefsEditor.apply();
		}
		catch (Exception e)
		{
			Log.e(TAG,e.getClass().getSimpleName() + " thrown writing ID to preferences!");
		}
	}

	public int integer()
	{
		if (id <= 0)
			throw new IllegalStateException("The device ID has not been initialized yet.");
		return id;
	}

	public String hex()
	{
		if (id <= 0)
			throw new IllegalStateException("The device ID has not been initialized yet.");
		return idHex;
	}
}
