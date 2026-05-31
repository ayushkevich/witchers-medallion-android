package by.alexy.witchersmedallion.repository

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GattOperationQueueTest {

    @Test
    fun `channel processes operations sequentially`() = runTest {
        val channel = Channel<String>(Channel.UNLIMITED)

        channel.send("op1")
        channel.send("op2")
        channel.send("op3")

        assertEquals("op1", channel.receive())
        assertEquals("op2", channel.receive())
        assertEquals("op3", channel.receive())
    }

    @Test
    fun `channel closes on cancel`() = runTest {
        val channel = Channel<String>(Channel.UNLIMITED)
        channel.send("op1")

        channel.close()
        assertFalse(channel.trySend("op2").isSuccess)
    }

    @Test
    fun `channel preserves FIFO order`() = runTest {
        val channel = Channel<Int>(Channel.UNLIMITED)

        for (i in 1..10) {
            channel.send(i)
        }

        for (i in 1..10) {
            assertEquals(i, channel.receive())
        }
    }
}
