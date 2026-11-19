package com.example.agente;

import com.google.adk.runner.InMemoryRunner;
import com.google.adk.agents.RunConfig;
import com.google.genai.types.Content;
import com.google.genai.types.Part;

public final class ConsultaProductos {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("GOOGLE_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Falta GOOGLE_API_KEY en el entorno. No se invoco Gemini.");
        }
        try (var toolset = SupermercadoAgent.newToolset()) {
            SupermercadoAgent.discover(toolset);
            // El ensayo con Gemini solo expone lecturas: no puede modificar pedidos ni stock.
            var runner = new InMemoryRunner(SupermercadoAgent.buildAgent(toolset, true));
            var session = runner.sessionService().createSession(runner.appName(), "verificacion-local").blockingGet();
            runner.runAsync(session.userId(), session.id(),
                    Content.fromParts(Part.fromText("Muestra los productos disponibles. Consulta listarProductos y presenta nombre, precio y stock. No modifiques datos.")),
                    RunConfig.builder().build()).blockingForEach(event -> {
                        if (event.finalResponse()) { System.out.println(event.stringifyContent()); }
                    });
        } finally {
            reactor.core.scheduler.Schedulers.shutdownNow();
            io.reactivex.rxjava3.schedulers.Schedulers.shutdown();
        }
    }
}
