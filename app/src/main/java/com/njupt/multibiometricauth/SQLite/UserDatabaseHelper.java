package com.njupt.multibiometricauth.SQLite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UserDatabaseHelper extends SQLiteOpenHelper {

    public static final String USER_TABLE_NAME = "db_user";
    public static final String PHONE = "phone";
    public static final String PASSWORD = "password";
    public static final String NAME = "name";
    public static final String CREATE_TIME = "create_time";
    public static final String UPDATE_TIME = "update_time";
    public static final String ID = "id";
    public static final String CREATE_USER_SQL = "create table IF NOT EXISTS db_user (" +
            "id integer primary key autoincrement," +
            "phone TEXT," +
            "password TEXT," +
            "name TEXT," +
            "create_time DATATIME," +
            "update_time DATATIME)";
    private SQLiteDatabase db;

    public UserDatabaseHelper(Context context) {
        super(context, USER_TABLE_NAME, null, 1);
        db = getWritableDatabase();
        db.execSQL(CREATE_USER_SQL);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS db_user");
        onCreate(db);
    }

    public void addUser(String phoneNumber, String name, String password) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PHONE, phoneNumber);
        contentValues.put(NAME, name);
        contentValues.put(PASSWORD, password);
        contentValues.put(CREATE_TIME, System.currentTimeMillis());
        contentValues.put(UPDATE_TIME, System.currentTimeMillis());
        db.insert(USER_TABLE_NAME, null, contentValues);
    }

    public List<User> queryUser(String phoneNumber) {
        List<User> userList = new ArrayList<>();
        Cursor cursor = db.query(USER_TABLE_NAME, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                if (phoneNumber.equals(cursor.getString(cursor.getColumnIndex(PHONE)))) {
                    userList.add(new User(cursor.getString(cursor.getColumnIndex(PHONE)), cursor.getString(cursor.getColumnIndex(NAME)), cursor.getString(cursor.getColumnIndex(PASSWORD))));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return userList;
    }

    @Nullable
    public String queryUserWithPhoneNumber(String phoneNumber) {
        List<User> userList = new ArrayList<>();
        Cursor cursor = db.query(USER_TABLE_NAME, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                if (phoneNumber.equals(cursor.getString(cursor.getColumnIndex(PHONE)))) {
                    return cursor.getString(cursor.getColumnIndex(NAME));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return null;
    }

    /**
     * 检查用户名是否注册
     * @param usrName
     * @return
     */
    public boolean isUserNameExist(String usrName) {
        Cursor cursor = db.query(USER_TABLE_NAME, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                if (usrName.equals(cursor.getString(cursor.getColumnIndex(NAME)))) {
                    return true;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return false;
    }

    /**
     * 根据用户名查询手机号
     * @param usrName
     * @return
     */
    @Nullable
    public String queryPhoneNumberWithName(String usrName) {
        Cursor cursor = db.query(USER_TABLE_NAME, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                if (usrName.equals(cursor.getString(cursor.getColumnIndex(NAME)))) {
                    return cursor.getString(cursor.getColumnIndex(PHONE));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return null;
    }

//
//    public void delete(String phone,String name,String password){
//        db.execSQL("DELETE FROM db_user WHERE phone AND name = AND password AND phone="+phone+name+password);
//    }
//    public void updata(String password){
//        db.execSQL("UPDATE db_user SET password = ?",new Object[]{password});
//    }
//
//

    public ArrayList<User> getAllData() {
        ArrayList<User> list = new ArrayList<User>();
        Cursor cursor = db.query(USER_TABLE_NAME, null, null, null, null, null, "null");
        while (cursor.moveToNext()) {
            String phone = cursor.getString(cursor.getColumnIndex(PHONE));
            String name = cursor.getString(cursor.getColumnIndex(NAME));
            String password = cursor.getString(cursor.getColumnIndex(PASSWORD));
            list.add(new User(phone, name, password));
        }
        cursor.close();
        return list;
    }
}
