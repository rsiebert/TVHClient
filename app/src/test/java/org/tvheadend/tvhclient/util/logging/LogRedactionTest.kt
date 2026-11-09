package org.tvheadend.tvhclient.util.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LogRedactionTest {

    @Test
    fun keepsTheSchemeButHidesTheServerAddress() {
        assertEquals(
                "Parsing url http://<redacted> to get required host and port information",
                LogRedaction.apply(
                        "Parsing url http://tvh.example.com:9981/ to get required host and port information"))
    }

    @Test
    fun hidesHttpsAddressesToo() {
        assertFalse(
                LogRedaction.apply("Connecting to server https://tv.example.org:9981").contains("tv.example.org"))
    }

    @Test
    fun hidesTicketsInTheTicketIsForm() {
        assertEquals(
                "Received response for ticket request, path is /stream/channel/1, ticket=<redacted>",
                LogRedaction.apply(
                        "Received response for ticket request, path is /stream/channel/1, ticket is 0123456789abcdef"))
    }

    @Test
    fun hidesTicketsInTheAssignmentForm() {
        assertFalse(
                LogRedaction.apply("Download from http://host.example.com:9981/stream?ticket=deadbeefcafe")
                        .contains("deadbeefcafe"))
    }

    @Test
    fun hidesTicketsInTheColonForm() {
        assertEquals(
                "Requesting stream, ticket=<redacted>",
                LogRedaction.apply("Requesting stream, ticket: a1b2c3d4e5"))
    }

    @Test
    fun leavesHarmlessMessagesUntouched() {
        val message = "View model returned 6 channels"
        assertEquals(message, LogRedaction.apply(message))
    }
}
