# Agente Supermercado (Google ADK Java)

Proyecto independiente del servidor `../supermercado-mcp`: no importa sus
clases, no accede a JDBC ni modifica su configuracion. Usa exclusivamente
Streamable HTTP en `http://localhost:8088/mcp` mediante McpToolset.

## Ejecutar en Windows

Desde esta carpeta, con el servidor MCP activo:

```powershell
.\agent.cmd test       # Dos pruebas reales MCP, solo lectura
.\agent.cmd mcp        # Descubre siete tools y ejecuta listarProductos
.\agent.cmd web        # ADK Web: http://localhost:8080/dev-ui/
.\agent.cmd consulta   # Gemini: pide los productos; solo expone tools de lectura
```

Para Gemini se requiere `GOOGLE_API_KEY` en el entorno del proceso o en las
variables de entorno de usuario de Windows. El lanzador la hereda sin imprimirla
ni escribirla. Configurarla mediante Variables de entorno de Windows y reiniciar
ADK es suficiente. No crear .env, env.bat ni archivos de propiedades con claves.
La UI y el descubrimiento MCP pueden arrancar sin clave; la invocacion del
modelo queda bloqueada explicitamente hasta configurarla. No hay fallback a
credenciales de archivos o Vertex AI. No pegar claves en el chat ni en Git.

Opcionales (variables sin secretos): `MCP_ENDPOINT` y `GEMINI_MODEL`.
El modelo predeterminado es `gemini-3.8-flash`. El servidor web escucha solo
en 127.0.0.1:8080. Las conversaciones ADK son en memoria y se pierden al
reiniciar el cliente; los pedidos reales permanecen en el servidor MCP.

## Versiones y compatibilidad

- Java 21 (verificado con Temurin 21.0.13).
- Maven Wrapper 3.3.4 / Maven 3.10.0 (copia independiente del wrapper ya probado).
- `google-adk` y `google-adk-dev`: ambos 1.15.0, publicados en Maven Central.
- Sus POM oficiales resuelven Google GenAI 1.77.0, MCP SDK 2.0.0 y Spring
  Boot 4.0.2 para ADK Web; no se mezclan con Boot 3.4.7 / Spring AI del servidor.
- JUnit Jupiter 5.11.4; compiler plugin 3.13.0, surefire 3.5.3, exec 3.5.0.

Se usaron los fuentes JAR publicados de 1.15.0 para verificar los constructores
StreamableHttpServerParameters/McpToolset y AdkWebServer.start, en vez de copiar
firmas de ejemplos 0.3.0 con APIs antiguas. Se utiliza una sola propiedad ADK
para mantener alineados ambos artefactos. Gemini 2.5 Flash de los ejemplos
antiguos tiene acceso limitado a usuarios previos segun Google; se selecciono
3.8 Flash estable, configurable mediante GEMINI_MODEL.

Se corrigio la configuracion exec.mainClass para permitir los tres lanzadores:
una clase literal en el plugin hacia que los diagnosticos arrancaran otra UI
y chocaran con 8080. No se cambiaron versiones para resolver ese problema.
Los diagnosticos CLI cierran toolset y schedulers Reactor/RxJava al terminar.

## Comportamiento del agente

SupermercadoAgent descubre y valida exactamente siete herramientas al arrancar:
listarProductos, buscarProducto, crearPedido, agregarProductoPedido,
consultarPedido, confirmarPedido y cancelarPedido. El agente instruye consultar
MCP y no inventar productos, precios o resultados, y solicitar direccion y
confirmacion antes de gestionar el pedido.

Antes de cancelar pide confirmacion explicita; ademas un callback ADK bloquea
la llamada hasta recibir ToolConfirmation.confirmed, usando la solicitud de
confirmacion nativa de ADK. Una denegacion no llega al servidor MCP. Esta ruta
no se ejecuto contra pedidos reales durante la validacion.

ConsultaProductos usa Gemini real (sin mocks) y filtra todas las herramientas
de escritura durante la prueba. VerificarMcp consulta directamente mediante
ADK y no equivale a una respuesta de Gemini.

## Validacion y limitacion

- Compilacion y tests de integracion: BUILD SUCCESS, 2 tests, 0 errores/fallos.
- Negociacion MCP 2025-06-18 y descubrimiento de siete herramientas reales.
- listarProductos devolvio los ocho productos existentes, incluido stock
  Arroz=48, Leche=39 y Huevos=29, sin modificar inventario ni pedidos.
- ADK Web: GET /dev-ui/ 200 y GET /list-apps 200, con SupermercadoAgent.
- Falta GOOGLE_API_KEY en el entorno; por ello la prueba conversacional Gemini
  se detuvo explicitamente. No se afirma haber obtenido respuesta del modelo.
  Tras configurar la variable, reiniciar `agent.cmd web` y ejecutar
  `agent.cmd consulta`. Acceso al modelo/cuota dependen de la cuenta Gemini.

Logs y fuentes consultados se guardan en `.local/`, fuera de Git. El informe,
Claude Desktop, MySQL y el codigo del servidor principal no se modificaron.

## Fuentes oficiales

- https://repo.maven.apache.org/maven2/com/google/adk/google-adk/maven-metadata.xml
- https://repo.maven.apache.org/maven2/com/google/adk/google-adk/1.15.0/
- https://repo.maven.apache.org/maven2/com/google/adk/google-adk-dev/1.15.0/
- https://github.com/google/adk-java
- https://github.com/google/adk-docs/blob/main/docs/tools-custom/mcp-tools.md
- https://aistudio.google.com/docs/models
- https://aistudio.google.com/docs/deprecations
