package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testBoletoParsing_identifiesEducationAndMoradia() {
    val collegeBoleto = com.example.util.BoletoPdfParser.identifyBoletoFromText("Boleto_Faculdade_Anhembi_Morumbi_R$750,00.pdf")
    assertEquals("Educação", collegeBoleto.category)
    assertEquals("Mensalidade Faculdade", collegeBoleto.title)
    assertTrue(collegeBoleto.isFixed)

    val energyBoleto = com.example.util.BoletoPdfParser.identifyBoletoFromText("Fatura_Enel_Luz_2026.pdf")
    assertEquals("Moradia", energyBoleto.category)
    assertEquals("Conta de Energia Elétrica", energyBoleto.title)
    assertTrue(energyBoleto.isFixed)
  }

  @Test
  fun testInstallmentGeneration_generatesCorrectDatesAndCount() {
    val installments = com.example.util.BoletoPdfParser.generateInstallmentTransactions(
      userId = "user_123",
      baseTitle = "Curso de Idiomas",
      category = "Educação",
      amountPerInstallment = 250.0,
      totalInstallments = 6,
      firstDueDate = "2026-11-10",
      barCode = "123456789",
      isFixed = false,
      workspaceId = "ws_alm",
      authorName = "Carlos Almeida",
      authorEmail = "carlos@almeida.com"
    )

    assertEquals(6, installments.size)
    assertEquals("Curso de Idiomas (Parcela 01/06)", installments[0].description)
    assertEquals("Curso de Idiomas (Parcela 06/06)", installments[5].description)
    assertEquals("2026-11-10", installments[0].dueDate)
    assertEquals(250.0, installments[0].amount, 0.001)
    assertEquals("ws_alm", installments[0].workspaceId)
    assertEquals("Carlos Almeida", installments[0].authorName)
  }
}
