package com.recall.app.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.recall.app.core.database.RecallDatabase
import com.recall.app.core.database.entity.AppSettingEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppSettingDaoTest {
    private lateinit var db: RecallDatabase
    private lateinit var dao: AppSettingDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecallDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.appSettingDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun set_andGetValue() = runTest {
        dao.set(AppSettingEntity(key = "last_scan", value = "12345"))

        assertEquals("12345", dao.getValue("last_scan"))
    }

    @Test
    fun set_overwritesExistingValue() = runTest {
        dao.set(AppSettingEntity(key = "theme", value = "dark"))
        dao.set(AppSettingEntity(key = "theme", value = "light"))

        assertEquals("light", dao.getValue("theme"))
    }

    @Test
    fun delete_removesSetting() = runTest {
        dao.set(AppSettingEntity(key = "feature_flag", value = "on"))
        dao.delete("feature_flag")

        assertNull(dao.getValue("feature_flag"))
    }
}
