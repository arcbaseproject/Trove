package com.trove.app.ui.update

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteLinesTest {
    @Test
    fun `headings, bullets and inline markup are cleaned up`() {
        val lines = noteLines("## Fixes\n\n- Faster **search**\n* Uses `OCR` less\nPlain line")
        assertEquals(
            listOf(
                NoteLineUi("Fixes", heading = true),
                NoteLineUi("Faster search"),
                NoteLineUi("Uses OCR less"),
                NoteLineUi("Plain line"),
            ),
            lines,
        )
    }
}
