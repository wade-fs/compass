// 平均是 22.028
// 計算公式是 (x/1000)^2 + (y/1000 - 2000)^2
package com.wade.libs;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/* 0 在 app/build.gradle 中要加 dependence
 *dependencies {
 *        compile 'com.readystatesoftware.sqliteasset:sqliteassethelper:+'
 *        }
 */
// 1. extends /home/wade/src/均利/APK/wade_libs/app/src/main/java/com/wade/libs/XYZ.java
public class XYZ extends SQLiteAssetHelper {
    final static String TAG = "MyLog";

    private static final String DATABASE_NAME = "Data.db";
    private static final int DATABASE_VERSION = 2;
    private static final String ID="id";
    private static final String X="x";
    private static final String Y="y";
    private static final String H="z";
    private static final String L="l";

    private static final String TABLE = "xyzl";
    private SQLiteDatabase db = null;
// 2. constructor super(....)
    public XYZ(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public double getHeight(double x, double y, int l){
// 3. getWritableDatabase()
        if (db == null) db=getWritableDatabase();
        if (db == null) {
            Log.d(TAG, "Not connect Data.db");
            return -10000;
        }

        // 先找出完整比對的結果
        B bru=new B(), blu=new B(), bld=new B(), brd=new B();

        Cursor clu = db.rawQuery("select * from "+TABLE+" where X <= "+x+" and Y >= "+y+" and L="+l+" order by x desc, y asc limit 1;", null);
        if(clu.moveToFirst()){
			int iX = clu.getColumnIndex(X);
			int iY = clu.getColumnIndex(Y);
			int iH = clu.getColumnIndex(H);
			if (iX >= 0 && iY >= 0 && iH >= 0) {
            	blu.x  = clu.getDouble(iX);
            	blu.y  = clu.getDouble(iY);
            	blu.z  = clu.getDouble(iH);
			}
        }
        Cursor cru = db.rawQuery("select * from "+TABLE+" where X >= "+x+" and Y >= "+y+" and L="+l+" order by x asc, y asc limit 1;", null);
        if(cru.moveToFirst()){
			int iX = cru.getColumnIndex(X);
			int iY = cru.getColumnIndex(Y);
			int iH = cru.getColumnIndex(H);
			if (iX >= 0 && iY >= 0 && iH >= 0) {
            	bru.x  = cru.getDouble(iX);
            	bru.y  = cru.getDouble(iY);
            	bru.z  = cru.getDouble(iH);
			}
        }

        Cursor cld = db.rawQuery("select * from "+TABLE+" where X <= "+x+" and Y <= "+y+" and L="+l+" order by x desc, y desc limit 1;", null);
        if(cld.moveToFirst()){
			int iX = cld.getColumnIndex(X);
			int iY = cld.getColumnIndex(Y);
			int iH = cld.getColumnIndex(H);
			if (iX >= 0 && iY >= 0 && iH >= 0) {
            	bld.x  = cld.getDouble(iX);
            	bld.y  = cld.getDouble(iY);
            	bld.z  = cld.getDouble(iH);
			}
        }

        Cursor crd = db.rawQuery("select * from "+TABLE+" where X >= "+x+" and Y <= "+y+" and L="+l+" order by x asc, y desc limit 1;", null);
        if(crd.moveToFirst()){
			int iX = crd.getColumnIndex(X);
			int iY = crd.getColumnIndex(Y);
			int iH = crd.getColumnIndex(H);
			if (iX >= 0 && iY >= 0 && iH >= 0) {
         	   brd.x  = crd.getDouble(iX);
         	   brd.y  = crd.getDouble(iY);
         	   brd.z  = crd.getDouble(iH);
			}
        }

        double h=0, r=100000000;
        if (blu.x > 0) {
            r = sqr(x-blu.x, y-blu.y); h = blu.z;
            Log.d(TAG, String.format("*LU(%.2f,%.2f) : %.3f", blu.x, blu.y, blu.z));
        }
        if (bru.x > 0) {
            double r1 = sqr(x-bru.x, y-bru.y);
            if (r1 < r) {
                h = bru.z;
                r = r1;
                Log.d(TAG, String.format("*RU(%.2f,%.2f) : %.3f", bru.x, bru.y, bru.z));
            }
        }
        if (bld.x > 0) {
            double r1 = sqr(x - bld.x, y - bld.y);
            if (r1 < r) {
                h = bld.z;
                r = r1;
                Log.d(TAG, String.format("*LD(%.2f,%.2f) : %.3f", bld.x, bld.y, bld.z));
            }
        }
        if (brd.x > 0) {
            if (sqr(x-brd.x, y-brd.y) < r) {
                h = brd.z;
                Log.d(TAG, String.format("*RD(%.2f,%.2f) : %.3f", brd.x, brd.y, brd.z));
            }
        }
        return h;
    }
    private double sqr(double x, double y) { return Math.sqrt(x*x + y*y); }

// x. define class to match database table definition
    public class B{
        public int id;
        public double x, y, z, l;
        B() {
            id = -1;
            x = y = z = l = 0;
        }
    }
}
