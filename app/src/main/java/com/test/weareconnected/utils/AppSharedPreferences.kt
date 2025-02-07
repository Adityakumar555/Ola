package com.test.weareconnected.utils

import android.content.Context

class AppSharedPreferences private constructor(context: Context){


    private var sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)


    companion object{

        private var INSTANCE: AppSharedPreferences? = null

        fun getInstance(context: Context): AppSharedPreferences? {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null)
                        INSTANCE = AppSharedPreferences(context)
                }
            }
            return INSTANCE
        }
    }

    fun saveUserName(userName: String) {
        sharedPreferences.edit().putString(PrefKeys.USER_NAME, userName).apply()
    }

    fun getUserName(): String? {
        return sharedPreferences.getString(PrefKeys.USER_NAME, null)
    }

    fun saveDriverId(driverId: String) {
        sharedPreferences.edit().putString(PrefKeys.DRIVER_ID, driverId).apply()
    }

    fun saveUserType(value: String) {
        sharedPreferences.edit().putString(PrefKeys.USER_TYPE, value).apply()
    }

    fun saveUserAddress(value: String) {
        sharedPreferences.edit().putString(PrefKeys.USER_LOCATION, value).apply()
    }

    fun saveMobileNumber(value: String) {
        sharedPreferences.edit().putString(PrefKeys.USER_NUMBER, value).apply()
    }

    fun setMainActivityVisited(value: Boolean) {
        sharedPreferences.edit().putBoolean(PrefKeys.VISITED_MAIN_ACTIVITY, value).apply()
    }

    fun getMobileNumber(): String? {
        return sharedPreferences.getString(PrefKeys.USER_NUMBER, null)
    }

    fun getUserType(): String? {
        return sharedPreferences.getString(PrefKeys.USER_TYPE, null)
    }


    fun isMainActivityVisited(): Boolean {
        return sharedPreferences.getBoolean(PrefKeys.VISITED_MAIN_ACTIVITY, false)
    }


    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }

}
