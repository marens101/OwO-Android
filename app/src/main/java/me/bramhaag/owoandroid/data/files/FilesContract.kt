package me.bramhaag.owoandroid.data.files

import android.provider.BaseColumns

class FilesContract {

    companion object {
        internal val SQL_CREATE_ENTRIES = "CREATE TABLE ${FileEntry.TABLE_NAME} (" +
                "${FileEntry._ID} INTEGER PRIMARY KEY," +
                "${FileEntry.COLUMN_NAME_NAME} TEXT," +
                "${FileEntry.COLUMN_NAME_URL} TEXT," +
                "${FileEntry.COLUMN_NAME_DATE} INTEGER)"

        //TODO delete files history
        internal val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${FileEntry.TABLE_NAME}"

        internal val SQL_GET_FILES = "SELECT * FROM ${FileEntry.TABLE_NAME}"
    }

    class FileEntry : BaseColumns {
        companion object {
            val _ID = BaseColumns._ID
            val TABLE_NAME = "files"
            val COLUMN_NAME_NAME = "name"
            val COLUMN_NAME_URL = "url"
            val COLUMN_NAME_DATE = "date"
        }
    }
}