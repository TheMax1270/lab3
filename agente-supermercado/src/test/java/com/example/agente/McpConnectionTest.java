package com.example.agente;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class McpConnectionTest {
    @Test void descubreSieteHerramientasPorStreamableHttp() throws Exception {
        try (var toolset = SupermercadoAgent.newToolset()) {
            assertEquals(7, SupermercadoAgent.discover(toolset).size());
        }
    }
    @Test void consultaProductosSinModificarDatos() throws Exception {
        try (var toolset = SupermercadoAgent.newToolset()) {
            var tool = SupermercadoAgent.discover(toolset).stream().filter(t -> t.name().equals("listarProductos")).findFirst().orElseThrow();
            var result = tool.runAsync(Map.<String,Object>of(), null).blockingGet();
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.toString().contains("Arroz 1 kg"));
            assertTrue(result.toString().contains("Huevos docena"));
        }
    }
}
