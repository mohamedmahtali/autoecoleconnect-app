package app.autoeecoleconnect.controllers;

import app.autoeecoleconnect.services.ContexteAutoEcole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Slice web sans les filtres Spring Security : la matrice d'accès est testée
// dans AuthControllerIntegrationTest.
@WebMvcTest(PingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * {@code FiltrePerimetreAutoEcole} est un filtre web : @WebMvcTest le
     * charge, et il tire {@link ContexteAutoEcole} donc le repository derrière.
     * Un mock suffit — cette tranche ne teste pas le périmètre.
     */
    @MockitoBean
    private ContexteAutoEcole contexteAutoEcole;

    @Test
    void ping_repond_ok() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("autoeecoleconnect-backend"));
    }
}
