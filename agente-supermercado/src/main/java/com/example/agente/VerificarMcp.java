package com.example.agente;

import com.google.adk.JsonBaseModel;
import java.util.Map;

public final class VerificarMcp {
    public static void main(String[] args) throws Exception {
        try (var toolset = SupermercadoAgent.newToolset()) {
            var tools = SupermercadoAgent.discover(toolset);
            var listar = tools.stream().filter(t -> t.name().equals("listarProductos")).findFirst().orElseThrow();
            var result = listar.runAsync(Map.<String, Object>of(), null).blockingGet();
            System.out.println(JsonBaseModel.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
        } finally {
            reactor.core.scheduler.Schedulers.shutdownNow();
            io.reactivex.rxjava3.schedulers.Schedulers.shutdown();
        }
    }
}
