// 平均是 22.028
// 計算公式是 (x/1000)^2 + (y/1000 - 2000)^2
package com.wade.libs;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CPDB extends SQLiteAssetHelper {
    final static String TAG = "MyLog";

    private static final String DATABASE_NAME = "cp.db";
    private static final int DATABASE_VERSION = 1;
    private static final String ID="id";
    private static final String T="t";
    private static final String Number="number";
    private static final String Name="name";
    private static final String X="x";
    private static final String Y="y";
    private static final String H="h";
    private static final String INFO="info";

    private static final String TABLE = "cp";
    private SQLiteDatabase db = null;

    public CPDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public List<CP> getCpByNumber(String number){
        if (db == null) db=getWritableDatabase();
        if (db == null) {
            Log.d(TAG, "Not connect cp.db");
            return null;
        }

        // 先找出完整比對的結果
        Cursor cursor = db.rawQuery("select * from " + TABLE + " where "+
                "number like '%"+number+"' or "+
                "number like '"+number+"%' or "+
                "number like '%"+number+"%' or "+
                "number = '"+number+"'",
                null);
        List<CP> cps = new ArrayList<>();
        while (cursor.moveToNext()) {
			int iId     = cursor.getColumnIndex(ID);
			int iT      = cursor.getColumnIndex(T);
			int iNumber = cursor.getColumnIndex(Number);
			int iName   = cursor.getColumnIndex(Name);
			int iX      = cursor.getColumnIndex(X);
			int iY      = cursor.getColumnIndex(Y);
			int iH      = cursor.getColumnIndex(H);
			int iInfo   = cursor.getColumnIndex(INFO);
			if (iId < 0 || iT < 0 || iNumber < 0 || iName < 0 ||
				iX < 0 || iY < 0 || iH < 0 || iInfo < 0) {
				continue;
			}
            CP cp = new CP(cursor.getInt(iId),
                    cursor.getInt(iT),
                    cursor.getString(iNumber),
                    cursor.getString(iName),
                    cursor.getDouble(iX),
                    cursor.getDouble(iY),
                    cursor.getDouble(iH),
                    cursor.getString(iInfo)
            );
            cps.add(cp);
        }
        return cps;
    }
    public List<CP> getCp(double x, double y, double l){
        if (db == null) db=getWritableDatabase();
        if (db == null) {
            Log.d(TAG, "Not connect cp.db");
            return null;
        }

        // 先找出完整比對的結果
        List<CP> cps = new ArrayList<>();
        double distance = l;
        while (cps.size() < 3 && distance <= 50000) {
            cps = new ArrayList<>();
            if (l == 0) distance += 1000;
            String q = "select * from " + TABLE + " where " +
                    (x - distance) + " <= x and x <= " + (x + distance) + " and " +
                    (y - distance) + " <= y and y <= " + (y + distance);
            Cursor cursor = db.rawQuery(q, null);
            while (cursor.moveToNext()) {
				int iId     = cursor.getColumnIndex(ID);
				int iT      = cursor.getColumnIndex(T);
				int iNumber = cursor.getColumnIndex(Number);
				int iName   = cursor.getColumnIndex(Name);
				int iX      = cursor.getColumnIndex(X);
				int iY      = cursor.getColumnIndex(Y);
				int iH      = cursor.getColumnIndex(H);
				int iInfo   = cursor.getColumnIndex(INFO);
				if (iId < 0 || iT < 0 || iNumber < 0 || iName < 0 ||
					iX < 0 || iY < 0 || iH < 0 || iInfo < 0) {
					continue;
				}
            	CP cp = new CP(cursor.getInt(iId),
            	        cursor.getInt(iT),
            	        cursor.getString(iNumber),
            	        cursor.getString(iName),
            	        cursor.getDouble(iX),
            	        cursor.getDouble(iY),
            	        cursor.getDouble(iH),
            	        cursor.getString(iInfo)
            	);
                if (len(cp.y - y, cp.x - x) <= distance) {
                    cps.add(cp);
                }
            }
            if (l != 0) break;
        }
        return cps;
    }
    private double len(double dx, double dy) { return Math.sqrt(dx*dx + dy*dy); }
    public class CP{
        public int id, t;
        public String number, name;
        public double x, y, h;
        public String info;
        CP() {
            id = t = -1;
            x = y = h = 0;
            info = "";
        }
        CP(int id, int t, String number, String name, double x, double y, double h, String info) {
            this.id = id; this.t = t;
            this.number = number; this.name = name;
            this.x = x; this.y = y; this.h = h;
            this.info = info;
        }
    }
}
