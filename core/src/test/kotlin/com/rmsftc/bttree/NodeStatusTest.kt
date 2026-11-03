package com.rmsftc.bttree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NodeStatusTest {
    @Test
    fun `exposes all behavior tree outcomes`() {
        assertEquals(
            listOf(NodeStatus.SUCCESS, NodeStatus.FAILURE, NodeStatus.RUNNING),
            NodeStatus.entries
        )
    }
}
