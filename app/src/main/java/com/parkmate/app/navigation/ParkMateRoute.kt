package com.parkmate.app.navigation

import kotlinx.serialization.Serializable

sealed interface ParkMateRoute {

    @Serializable
    data object MapHome : ParkMateRoute

    @Serializable
    data object SpotsHistory : ParkMateRoute

    @Serializable
    data object Settings : ParkMateRoute

    @Serializable
    data class QuickSave(val spotId: Long) : ParkMateRoute

    @Serializable
    data class CameraPermission(
        val spotId: Long,
        val returnToRead: Boolean = false
    ) : ParkMateRoute

    @Serializable
    data class CameraCapture(
        val spotId: Long,
        val returnToRead: Boolean = false
    ) : ParkMateRoute

    @Serializable
    data class PhotoReview(
        val spotId: Long,
        val photoPath: String,
        val returnToRead: Boolean = false
    ) : ParkMateRoute

    @Serializable
    data class AddDetails(
        val spotId: Long,
        val fromPhotoReview: Boolean = false
    ) : ParkMateRoute

    @Serializable
    data class SpotRead(val spotId: Long) : ParkMateRoute

    @Serializable
    data class CurrentSpotDetails(val spotId: Long) : ParkMateRoute

    @Serializable
    data class PhotoViewer(
        val spotId: Long,
        val initialIndex: Int = 0
    ) : ParkMateRoute

    @Serializable
    data class GuideMe(val spotId: Long) : ParkMateRoute
}
