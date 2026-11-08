package org.tvheadend.htsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/**
 * Pure JVM tests for the typed accessors of [HtspMessage].
 *
 * [HtspMessageCodecTest] only covers what happens when a message travels over the wire.
 * These tests pin the in-memory behaviour of the accessors themselves: which types they
 * accept, how they coerce (or reject) other types, what happens for a missing key and what
 * happens for a key that is present but holds `null`.
 *
 * The `allowMissing`/coercion rules are important for the callers in `HtspUtils` because the
 * server only sends optional fields when they are set.
 */
class HtspMessageTypeTest {

    // ---- method ---------------------------------------------------------------------

    @Test
    fun methodDefaultsToAnEmptyStringWhenItIsNotSet() {
        assertEquals("", HtspMessage().getMethod())
    }

    @Test
    fun methodIsStoredUnderTheMethodKey() {
        val message = HtspMessage()
        message.setMethod("getEvents")
        assertEquals("getEvents", message.getMethod())
        // setMethod is a thin wrapper around put("method", name).
        assertEquals("getEvents", message.get("method"))
    }

    // ---- plain map operations -------------------------------------------------------

    @Test
    fun putReturnsThePreviousValueAndContainsKeyReflectsTheState() {
        val message = HtspMessage()
        assertNull("A fresh message must not contain the key", message.put("key", "first"))
        assertTrue(message.containsKey("key"))
        assertEquals("first", message.put("key", "second"))
        assertEquals("second", message.get("key"))
    }

    @Test
    fun removeDropsTheEntry() {
        val message = HtspMessage()
        message.put("key", 1)
        assertEquals(1, message.remove("key"))
        assertFalse(message.containsKey("key"))
        assertNull(message.remove("key"))
    }

    @Test
    fun keySetExposesAllKeysThatWerePut() {
        val message = HtspMessage()
        message.put("a", 1)
        message.put("b", 2)
        assertEquals(setOf("a", "b"), message.keys)
    }

    // ---- getString ------------------------------------------------------------------

    @Test
    fun getStringReturnsNullForAMissingKey() {
        assertNull(HtspMessage().getString("missing"))
    }

    @Test
    fun getStringReturnsNullForANullValue() {
        val message = HtspMessage()
        message.put("key", null)
        assertNull(message.getString("key"))
    }

    @Test
    fun getStringCoercesAnyValueWithToString() {
        val message = HtspMessage()
        message.put("number", 42)
        assertEquals("42", message.getString("number"))
    }

    @Test
    fun getStringWithFallbackReturnsTheFallbackForAMissingKey() {
        assertEquals("fallback", HtspMessage().getString("missing", "fallback"))
    }

    @Test
    fun getStringWithFallbackUsesTheFallbackForAPresentNullValue() {
        // A key that is present but holds null is now treated the same as an absent one. It used to
        // bypass the fallback and hand back null to a caller that had asked for a default.
        val message = HtspMessage()
        message.put("key", null)
        assertEquals("fallback", message.getString("key", "fallback"))
    }

    @Test
    fun getStringWithFallbackPrefersTheStoredValue() {
        val message = HtspMessage()
        message.put("key", "stored")
        assertEquals("stored", message.getString("key", "fallback"))
    }

    // ---- getInteger -----------------------------------------------------------------

    @Test
    fun getIntegerReturnsTheStoredInteger() {
        val message = HtspMessage()
        message.put("key", 7)
        assertEquals(7, message.getInteger("key"))
    }

    @Test
    fun getIntegerUnpacksABigInteger() {
        // Integers that arrive over the wire become BigIntegers (HTSP has no 32 bit type).
        val message = HtspMessage()
        message.put("key", BigInteger.valueOf(123))
        assertEquals(123, message.getInteger("key"))
    }

    @Test
    fun getIntegerThrowsForAMissingKey() {
        assertThrows(RuntimeException::class.java) { HtspMessage().getInteger("missing") }
    }

    @Test
    fun getIntegerThrowsForANullValue() {
        val message = HtspMessage()
        message.put("key", null)
        assertThrows(RuntimeException::class.java) { message.getInteger("key") }
    }

    @Test
    fun getIntegerNarrowsALong() {
        // Integers from the wire are BigInteger, but a value put in locally is a boxed Integer or
        // Long. Only BigInteger used to be recognised, so a Long threw ClassCastException.
        val message = HtspMessage()
        message.put("key", 7L)
        assertEquals(7, message.getInteger("key"))
    }

    @Test
    fun getIntegerRejectsAString() {
        val message = HtspMessage()
        message.put("key", "7")
        assertThrows(ClassCastException::class.java) { message.getInteger("key") }
    }

    @Test
    fun getIntegerWithFallbackReturnsTheFallbackForAMissingKey() {
        assertEquals(5, HtspMessage().getInteger("missing", 5))
    }

    // ---- getLong --------------------------------------------------------------------

    @Test
    fun getLongReturnsTheStoredLong() {
        val message = HtspMessage()
        message.put("key", 9_000_000_000L)
        assertEquals(9_000_000_000L, message.getLong("key"))
    }

    @Test
    fun getLongUnpacksABigInteger() {
        val message = HtspMessage()
        message.put("key", BigInteger.valueOf(9_000_000_000L))
        assertEquals(9_000_000_000L, message.getLong("key"))
    }

    @Test
    fun getLongWidensAnInteger() {
        val message = HtspMessage()
        message.put("key", 7)
        assertEquals(7L, message.getLong("key"))
    }

    @Test
    fun getLongThrowsForAMissingKey() {
        assertThrows(RuntimeException::class.java) { HtspMessage().getLong("missing") }
    }

    @Test
    fun getLongWithFallbackReturnsTheFallbackForAMissingKey() {
        assertEquals(99L, HtspMessage().getLong("missing", 99L))
    }

    // ---- getBoolean -----------------------------------------------------------------

    @Test
    fun getBooleanWithFallbackReturnsTheFallbackForAMissingKey() {
        assertTrue(HtspMessage().getBoolean("missing", true))
        assertFalse(HtspMessage().getBoolean("missing", false))
    }

    @Test
    fun getBooleanTreatsOneAsTrueAndZeroAsFalse() {
        val message = HtspMessage()
        message.put("yes", 1)
        message.put("no", 0)
        assertTrue(message.getBoolean("yes", false))
        assertFalse(message.getBoolean("no", true))
    }

    // ---- getList / getIntegerList / getArrayList ------------------------------------

    @Test
    fun getListReturnsNullForAMissingOrNullValue() {
        assertNull(HtspMessage().getList("missing"))

        val message = HtspMessage()
        message.put("key", null)
        assertNull(message.getList("key"))
    }

    @Test
    fun getListReturnsTheStoredList() {
        val list = arrayListOf("a", "b")
        val message = HtspMessage()
        message.put("key", list)
        assertSame(list, message.getList("key"))
    }

    @Test
    fun getListRejectsANonListValue() {
        val message = HtspMessage()
        message.put("key", "not a list")
        assertThrows(ClassCastException::class.java) { message.getList("key") }
    }

    @Test
    fun getArrayListReturnsNullForAMissingKey() {
        assertNull(HtspMessage().getArrayList("missing"))
    }

    @Test
    fun getArrayListReturnsTheStoredArrayList() {
        val list = arrayListOf("a", "b")
        val message = HtspMessage()
        message.put("key", list)
        assertSame(list, message.getArrayList("key"))
    }

    @Test
    fun getArrayListRejectsALinkedList() {
        // An ArrayList is the only accepted type: the cast is to ArrayList, not to List.
        val message = HtspMessage()
        message.put("key", LinkedListHolder.LIST)
        assertThrows(ClassCastException::class.java) { message.getArrayList("key") }
    }

    @Test
    fun getIntegerListReturnsAnEmptyListForAMissingKey() {
        assertTrue(HtspMessage().getIntegerList("missing").isEmpty())
    }

    @Test
    fun getIntegerListUnpacksBigIntegers() {
        val message = HtspMessage()
        message.put("key", arrayListOf(BigInteger.valueOf(1), BigInteger.valueOf(2)))
        assertEquals(listOf(1, 2), message.getIntegerList("key"))
    }

    @Test
    fun getIntegerListAcceptsAnyNumber() {
        // Entries that come off the wire are BigInteger, but a hand-built list holds Integers.
        // Those used to be silently dropped, so this returned an empty list.
        val message = HtspMessage()
        message.put("key", arrayListOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), message.getIntegerList("key"))
    }

    // ---- getByteArray ---------------------------------------------------------------

    @Test
    fun getByteArrayReturnsNullForAMissingOrNullValue() {
        assertNull(HtspMessage().getByteArray("missing"))

        val message = HtspMessage()
        message.put("key", null)
        assertNull(message.getByteArray("key"))
    }

    @Test
    fun getByteArrayReturnsTheStoredArray() {
        val bytes = byteArrayOf(1, 2, 3)
        val message = HtspMessage()
        message.put("key", bytes)
        assertSame(bytes, message.getByteArray("key"))
    }

    @Test
    fun getByteArrayRejectsANonByteArrayValue() {
        val message = HtspMessage()
        message.put("key", "not bytes")
        assertThrows(ClassCastException::class.java) { message.getByteArray("key") }
    }

    // ---- nested messages ------------------------------------------------------------

    @Test
    fun keepsANestedMessageAsAMapValue() {
        val nested = HtspMessage()
        nested.put("value", 7)

        val message = HtspMessage()
        message.put("child", nested)

        assertSame(nested, message.get("child"))
        assertEquals(7, (message.get("child") as HtspMessage).getInteger("value"))
    }

    @Test
    fun keepsNestedMessagesInsideAList() {
        val first = HtspMessage().apply { put("value", 1) }
        val second = HtspMessage().apply { put("value", 2) }

        val message = HtspMessage()
        message.put("children", arrayListOf(first, second))

        val children = message.getList("children")!!
        assertEquals(2, children.size)
        assertSame(first, children[0])
        assertSame(second, children[1])
    }

    private object LinkedListHolder {
        val LIST = java.util.LinkedList(listOf("a", "b"))
    }
}
