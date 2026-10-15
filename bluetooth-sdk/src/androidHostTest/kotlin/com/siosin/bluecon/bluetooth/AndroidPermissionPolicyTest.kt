package com.siosin.bluecon.bluetooth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidPermissionPolicyTest {
    @Test
    fun legacyAndroidRequiresForegroundLocationWithoutNearbyPermissions() {
        for (sdk in listOf(24, 28, 29, 30)) {
            assertEquals(listOf(AndroidPermissionPolicy.FINE_LOCATION), AndroidPermissionPolicy.requiredPermissions(sdk, false))
            assertTrue(AndroidPermissionPolicy.requiresLocationServices(sdk, false))
        }
    }

    @Test
    fun nearbyOnlyModeDoesNotAskForLocationOnModernAndroid() {
        for (sdk in listOf(31, 33, 34, 35, 36, 37)) {
            assertEquals(
                listOf(AndroidPermissionPolicy.SCAN, AndroidPermissionPolicy.CONNECT),
                AndroidPermissionPolicy.requiredPermissions(sdk, false),
            )
            assertFalse(AndroidPermissionPolicy.requiresLocationServices(sdk, false))
        }
    }

    @Test
    fun proximityModeRequestsCoarseTogetherWithFineOnAndroid12AndAbove() {
        val required = AndroidPermissionPolicy.requiredPermissions(31, true)
        assertTrue(AndroidPermissionPolicy.COARSE_LOCATION in required)
        assertTrue(AndroidPermissionPolicy.FINE_LOCATION in required)
        assertTrue(AndroidPermissionPolicy.requiresLocationServices(31, true))
    }
}
