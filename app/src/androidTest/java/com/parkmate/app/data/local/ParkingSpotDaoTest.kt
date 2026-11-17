package com.parkmate.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parkmate.app.data.model.ParkingSpot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParkingSpotDaoTest {

    private lateinit var database: ParkMateDatabase
    private lateinit var dao: ParkingSpotDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ParkMateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.parkingSpotDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveNewActiveSpot_deactivatesPreviousActiveSpotAndPersistsTagsAndPhotos() = runTest {
        val spot1 = ParkingSpot(
            latitude = 44.4396,
            longitude = 26.0963,
            addressLine = "Piața Amzei 4",
            cityDistrict = "Bucharest, Sector 1",
            savedAtMillis = 1_000L,
            isActive = true,
            tags = listOf("Zone A"),
            note = "First spot"
        )
        val id1 = dao.saveNewActiveSpot(spot1.toEntity())

        val spot2 = ParkingSpot(
            latitude = 44.4412,
            longitude = 26.0979,
            addressLine = "Strada Exemplu 12",
            cityDistrict = "Bucharest, Sector 1",
            savedAtMillis = 2_000L,
            isActive = true,
            tags = listOf("Level -2", "Pillar C4"),
            note = "Near the elevator, row 4",
            photoPaths = listOf("/data/user/0/com.parkmate.app/files/spot_photos/p1.jpg")
        )
        val id2 = dao.saveNewActiveSpot(spot2.toEntity())

        val active = dao.observeActiveSpot().first()?.toDomain()
        assertNotNull(active)
        assertEquals(id2, active?.id)
        assertEquals("Strada Exemplu 12", active?.addressLine)
        assertEquals(listOf("Level -2", "Pillar C4"), active?.tags)
        assertEquals(1, active?.photoPaths?.size)

        val previous = dao.getSpotByIdOnce(id1)?.toDomain()
        assertNotNull(previous)
        assertFalse(previous!!.isActive)

        dao.deleteSpotById(id2)
        assertNull(dao.getSpotByIdOnce(id2))
        assertTrue(dao.observeAllSpots().first().size == 1)
    }
}
