package com.parkmate.app.feature

import com.parkmate.app.data.model.ParkingSpot
import com.parkmate.app.data.repository.ParkingRepository
import com.parkmate.app.feature.history.HistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeParkingRepository : ParkingRepository {
    private val spotsFlow = MutableStateFlow<List<ParkingSpot>>(emptyList())
    private var nextId = 1L

    override fun observeActiveSpot(): Flow<ParkingSpot?> =
        spotsFlow.map { list -> list.firstOrNull { it.isActive } }

    override suspend fun getActiveSpotOnce(): ParkingSpot? =
        spotsFlow.value.firstOrNull { it.isActive }

    override fun observeAllSpots(): Flow<List<ParkingSpot>> = spotsFlow

    override fun observeSpotById(spotId: Long): Flow<ParkingSpot?> =
        spotsFlow.map { list -> list.firstOrNull { it.id == spotId } }

    override suspend fun getSpotByIdOnce(spotId: Long): ParkingSpot? =
        spotsFlow.value.firstOrNull { it.id == spotId }

    override suspend fun saveNewSpot(spot: ParkingSpot): Long {
        val assignedId = if (spot.id == 0L) nextId++ else spot.id
        val deactivated = spotsFlow.value.map { it.copy(isActive = false) }
        spotsFlow.value = listOf(spot.copy(id = assignedId, isActive = true)) + deactivated
        return assignedId
    }

    override suspend fun updateSpot(spot: ParkingSpot) {
        spotsFlow.value = spotsFlow.value.map { if (it.id == spot.id) spot else it }
    }

    override suspend fun deleteSpot(spotId: Long) {
        spotsFlow.value = spotsFlow.value.filterNot { it.id == spotId }
    }

    override suspend fun clearAllSpots() {
        spotsFlow.value = emptyList()
    }

    override suspend fun seedSampleSpots() {
        saveNewSpot(
            ParkingSpot(
                latitude = 44.4412,
                longitude = 26.0979,
                addressLine = "Strada Exemplu 12",
                cityDistrict = "Bucharest, Sector 1",
                savedAtMillis = 1_700_000_000_000L,
                isActive = true,
                tags = listOf("Level -2", "Pillar C4"),
                note = "Near the elevator"
            )
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeParkingRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeParkingRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `HistoryViewModel loads example history, toggles empty preview, and clears history`() = runTest {
        val viewModel = HistoryViewModel(fakeRepository)
        assertTrue(viewModel.uiState.value.spots.isEmpty())

        viewModel.loadExampleHistory()
        assertEquals(1, viewModel.uiState.value.spots.size)
        assertEquals("Strada Exemplu 12", viewModel.uiState.value.spots.first().addressLine)

        viewModel.togglePreviewEmptyState()
        assertTrue(viewModel.uiState.value.previewEmptyState)

        viewModel.togglePreviewEmptyState()
        assertFalse(viewModel.uiState.value.previewEmptyState)

        viewModel.clearAllHistory()
        assertTrue(viewModel.uiState.value.spots.isEmpty())
    }

    @Test
    fun `FakeRepository saveNewSpot deactivates previous active spot`() = runTest {
        val firstId = fakeRepository.saveNewSpot(
            ParkingSpot(
                latitude = 44.43,
                longitude = 26.09,
                addressLine = "Calea Victoriei 102",
                cityDistrict = "Bucharest",
                savedAtMillis = 1000L,
                isActive = true
            )
        )
        val secondId = fakeRepository.saveNewSpot(
            ParkingSpot(
                latitude = 44.44,
                longitude = 26.10,
                addressLine = "Strada Exemplu 12",
                cityDistrict = "Bucharest",
                savedAtMillis = 2000L,
                isActive = true
            )
        )

        val firstSpot = fakeRepository.getSpotByIdOnce(firstId)
        val activeSpot = fakeRepository.getActiveSpotOnce()

        assertFalse(firstSpot!!.isActive)
        assertEquals(secondId, activeSpot!!.id)
        assertTrue(activeSpot.isActive)
    }
}
