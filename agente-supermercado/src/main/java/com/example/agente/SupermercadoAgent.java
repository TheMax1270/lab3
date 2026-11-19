package com.example.agente;

import com.google.adk.agents.LlmAgent;
import com.google.adk.models.Gemini;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StreamableHttpServerParameters;
import com.google.adk.web.AdkWebServer;
import java.util.*;

public final class SupermercadoAgent {
    public static final Set<String> EXPECTED_TOOLS = Set.of("listarProductos", "buscarProducto",
            "crearPedido", "agregarProductoPedido", "consultarPedido", "confirmarPedido", "cancelarPedido");
    public static final String INSTRUCTION = """
        Eres SupermercadoAgent, asistente de pedidos a domicilio. Responde en espanol.
        Usa exclusivamente las herramientas MCP para consultar productos, precios, stock
        y gestionar pedidos. No inventes ids, resultados ni disponibilidad.
        Consulta el catalogo para identificar los productos. Solicita la direccion antes
        de crear un pedido. Muestra cantidades, precios y total antes de confirmarlo.
        Antes de cancelar un pedido, explica que pedido vas a cancelar y pide confirmacion
        explicita al usuario. Nunca canceles sin esa confirmacion; la interfaz tambien
        requiere aprobar la llamada cancelarPedido. Si se rechaza, conserva el pedido.
        Informa los errores de stock o estado y no afirmes que una accion tuvo exito
        sin la respuesta de la herramienta. No repitas escrituras automaticamente.
        """;

    public static McpToolset newToolset() {
        return new McpToolset(StreamableHttpServerParameters.builder()
                .url(System.getenv().getOrDefault("MCP_ENDPOINT", "http://localhost:8088/mcp")).build());
    }

    public static List<BaseTool> discover(McpToolset toolset) {
        var tools = toolset.getTools(null).toList().blockingGet();
        Set<String> names = new TreeSet<>();
        tools.forEach(t -> names.add(t.name()));
        if (tools.size() != 7 || !names.equals(EXPECTED_TOOLS)) {
            throw new IllegalStateException("Se esperaban las siete herramientas de supermercado: " + names);
        }
        System.out.println("MCP: 7 herramientas descubiertas: " + names);
        return tools;
    }

    public static LlmAgent buildAgent(McpToolset toolset, boolean readOnly) {
        var builder = LlmAgent.builder().name("SupermercadoAgent")
                .description("Productos y pedidos del supermercado mediante MCP")
                .instruction(INSTRUCTION);
        String model = System.getenv().getOrDefault("GEMINI_MODEL", "gemini-3.8-flash");
        String key = System.getenv("GOOGLE_API_KEY");
        // Sin clave permitimos iniciar la UI y descubrir MCP, pero no invocar Gemini.
        if (key != null && !key.isBlank()) { builder.model(new Gemini(model, key)); }
        else { builder.model(model); }
        if (readOnly) {
            var tools = discover(toolset).stream().filter(t -> Set.of("listarProductos", "buscarProducto", "consultarPedido").contains(t.name())).toList();
            builder.tools(tools);
        } else { builder.tools(toolset); }
        return builder.beforeModelCallbackSync((context, request) -> {
            String currentKey = System.getenv("GOOGLE_API_KEY");
            if (currentKey == null || currentKey.isBlank()) {
                throw new IllegalStateException("Configure GOOGLE_API_KEY en el entorno y reinicie ADK; no se leen credenciales de archivos.");
            }
            return Optional.empty();
        }).beforeToolCallbackSync((invocation, tool, args, context) -> {
            if (!tool.name().equals("cancelarPedido")) { return Optional.empty(); }
            if (context.toolConfirmation().isEmpty()) {
                context.requestConfirmation("Confirma la cancelacion del pedido " + args.get("pedidoId"), args);
                return Optional.of(Map.of("estado", "PENDIENTE_CONFIRMACION", "mensaje", "Se requiere aprobacion del usuario; no se ha cancelado."));
            }
            if (!context.toolConfirmation().get().confirmed()) {
                return Optional.of(Map.of("estado", "CANCELACION_RECHAZADA", "mensaje", "El pedido no se modifico."));
            }
            return Optional.empty();
        }).build();
    }

    public static void main(String[] args) {
        var toolset = newToolset();
        discover(toolset);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { toolset.close(); } catch (Exception ignored) {} }));
        if (System.getenv("GOOGLE_API_KEY") == null) {
            System.out.println("Gemini pendiente: configure GOOGLE_API_KEY en el entorno. MCP y UI pueden verificarse sin clave.");
        }
        System.setProperty("server.port", "8080");
        System.setProperty("server.address", "127.0.0.1");
        AdkWebServer.start(buildAgent(toolset, false));
    }
}
