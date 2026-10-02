package com.uvg.agroconecta.ui.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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

    @Test
    fun `blank search returns every faq section`() {
        assertSame(helpFaqSections, filterFaqSections(helpFaqSections, "   "))
    }

    @Test
    fun `search ignores case and accents and matches answers`() {
        val result = filterFaqSections(helpFaqSections, "CONTRASENA")

        assertEquals(listOf(HelpCategory.ACCOUNT), result.map { it.category })
        assertEquals(listOf("account-security"), result.flatMap { it.questions }.map { it.id })
    }

    @Test
    fun `search requires every word and keeps matching categories only`() {
        val result = filterFaqSections(helpFaqSections, "  pedido   urgente ")

        assertEquals(listOf("orders-urgent"), result.flatMap { it.questions }.map { it.id })
    }

    @Test
    fun `search matches category names`() {
        val result = filterFaqSections(helpFaqSections, "pagos")

        assertEquals(listOf(HelpCategory.PAYMENTS), result.map { it.category })
        assertEquals(3, result.single().questions.size)
    }

    @Test
    fun `search without matches returns no sections`() {
        assertTrue(filterFaqSections(helpFaqSections, "criptomonedas").isEmpty())
    }
}
