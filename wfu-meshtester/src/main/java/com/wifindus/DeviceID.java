package com.wifindus;

import android.content.SharedPreferences;

import com.wifindus.logs.LogSender;
import com.wifindus.logs.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by marzer on 19/03/2015.
 */
public class DeviceID
{
	private int id = -1;
	private String idHex = "";

	public DeviceID(LogSender logContext, File idFile, SharedPreferences prefs, SharedPreferences.Editor prefsEditor)
	{
		//reading
		id = readFromPrefs(logContext, prefs);
		if (id <= 0)
			id = readFromStorage(logContext, idFile);
		if (id <= 0)
			id = generateRandom(logContext);

		//writing
		writeToPrefs(logContext, prefsEditor);
		writeToStorage(logContext, idFile);

		//hex
		idHex = Integer.toHexString(id).toUpperCase();
	}

	private int readFromPrefs(LogSender logContext, SharedPreferences prefs)
	{
		if (prefs == null)
			return -1;

		int id = -1;
		Logger.i(logContext, "Reading device ID from prefs...");
		try
		{
			id = prefs.getInt("id",-1);
		}
		catch(ClassCastException e){ }
		if (id <= 0)
			Logger.w(logContext,"Could not read device ID from prefs.");
		return id;
	}

	private int readFromStorage(LogSender logContext, File idFile)
	{
		if (idFile == null)
			return -1;
		Logger.i(logContext, "Reading device ID from storage...");
		if (!Static.isExternalStorageReadable())
		{
			Logger.e(logContext, "Storage was not available for reading!");
			return -1;
		}
		if (!idFile.exists())
		{
			Logger.w(logContext, "Could not find device ID file in storage.");
			return -1;
		}

		int id = -1;
		try
		{
			Scanner scanner = new Scanner(idFile);
			while (id <= 0 && scanner.hasNextInt(16))
			{
				id = scanner.nextInt(16);
				break;
			}
		}
		catch (Exception e) { }
		if (id <= 0)
			Logger.w(logContext,"Could not read device ID from storage.");
		return id;
	}

	private int generateRandom(LogSender logContext)
	{
		Logger.w(logContext,"Generating new random device ID...");
		return 1 + (int) (2147483646 * new Random().nextDouble());
	}

	private void writeToStorage(LogSender logContext, File idFile)
	{
		if (idFile == null)
			return;
		if (id <= 0)
			throw new IllegalStateException("The device ID has not been initialized yet.");
		if (!Static.isExternalStorageWritable())
		{
			Logger.e(logContext, "Storage was not available for writing!");
			return;
		}

		Logger.i(logContext,"Writing device ID to storage...");
		try
		{
			FileOutputStream fOut = new FileOutputStream(idFile);
			OutputStreamWriter osw = new OutputStreamWriter(fOut);
			osw.write(Integer.toHexString(id) + "\n");
			osw.flush();
			osw.close();
			Logger.i(logContext,"Device ID written to storage OK");
		}
		catch (Exception e)
		{
			Logger.e(logContext, "Device ID could not be written to storage!");
		}
	}

	private void writeToPrefs(LogSender logContext, SharedPreferences.Editor prefsEditor)
	{
		if (prefsEditor == null)
			return;
		if (id <= 0)
			throw new IllegalStateException("The device ID has not been initialized yet.");
		try
		{
			prefsEditor.putInt("id", id);
			Logger.i(logContext,"Device ID written to prefs OK");
		}
		catch (Exception e)
		{
			Logger.e(logContext, "Device ID could not be written to prefs!");
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
