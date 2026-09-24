package com.uvg.agroconecta.ui.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpContentTest {

    @Test
    fun `faq content includes every category with valid unique questions`() {
        assertEquals(HelpCategory.entries.toSet(), helpFaqSections.map { it.category }.toSet())
        assertTrue(helpFaqSections.all { it.questions.isNotEmpty() })

        val questions = helpFaqSections.flatMap(FaqSection::questions)
        assertEquals(questions.size, questions.map(FrequentlyAskedQuestion::id).distinct().size)
        assertTrue(questions.all { it.question.isNotBlank() && it.answer.isNotBlank() })
    }
}
