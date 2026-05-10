package com.p_nsk.replicated_integration.api

import com.p_nsk.replicated_integration.api.command.MatterCommandMutation
import com.p_nsk.replicated_integration.api.command.MatterCommandSupport
import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatterCommandMutationTest {
    @Test
    fun addsSingleMatterWithoutDroppingExistingValues() {
        val current = LiteMatterCompound(
            mapOf(
                MatterCommandSupport.EARTH to 1.0,
                MatterCommandSupport.NETHER to 2.0,
            )
        )

        val updated = MatterCommandMutation.setSingleMatter(
            current = current,
            matterType = MatterCommandSupport.ORGANIC,
            amount = 3.0,
        )

        assertEquals(
            mapOf(
                MatterCommandSupport.EARTH to 1.0,
                MatterCommandSupport.NETHER to 2.0,
                MatterCommandSupport.ORGANIC to 3.0,
            ),
            updated.values,
        )
    }

    @Test
    fun zeroRemovesOnlySelectedMatter() {
        val current = LiteMatterCompound(
            mapOf(
                MatterCommandSupport.EARTH to 1.0,
                MatterCommandSupport.NETHER to 2.0,
            )
        )

        val updated = MatterCommandMutation.setSingleMatter(
            current = current,
            matterType = MatterCommandSupport.EARTH,
            amount = 0.0,
        )

        assertEquals(mapOf(MatterCommandSupport.NETHER to 2.0), updated.values)
    }

    @Test
    fun zeroCanRemoveTheLastMatterValue() {
        val current = LiteMatterCompound(mapOf(MatterCommandSupport.EARTH to 1.0))

        val updated = MatterCommandMutation.setSingleMatter(
            current = current,
            matterType = MatterCommandSupport.EARTH,
            amount = 0.0,
        )

        assertTrue(updated.values.isEmpty())
    }
}
