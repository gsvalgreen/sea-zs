package br.com.fateczs.seazs.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ToDo: Configurar base de testes.

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AgendamentoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void deveConsultarAgendamentoUsandoIdNoGet() throws Exception {
        Integer id = 1;

        mockMvc.perform(get("/agendamento/buscar/" + id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evento.nome", is("TESTE_AGENDAMENTO")));
    }

    @Test
    public void deveRetornar500QuandoAgendamentoNaoExiste() throws Exception {
        Integer id = 2;

        mockMvc.perform(get("/agendamento/buscar/" + id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

}