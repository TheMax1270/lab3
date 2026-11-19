# Laboratorio 3: supermercado con MCP y Google ADK

El laboratorio conecta un agente de Google ADK con un servidor MCP para consultar productos y gestionar pedidos de supermercado con persistencia en MySQL.

## Arquitectura y tecnologias

`Google ADK / Gemini -> MCP Streamable HTTP -> Spring Boot / Spring AI -> MySQL`

Se utiliza Java 21, Maven Wrapper, Spring Boot 3.4.7, Spring AI 1.1.4, Spring Data JPA, MySQL 8.4 y Google ADK Java 1.15.0. El agente es un proyecto independiente y accede al supermercado exclusivamente mediante MCP.

```text
lab3/
├── supermercado-mcp/    # Servidor MCP, persistencia y pruebas
├── agente-supermercado/ # Agente Google ADK y verificaciones MCP
└── README.md
```

El servidor publica siete herramientas: `listarProductos`, `buscarProducto`, `crearPedido`, `agregarProductoPedido`, `consultarPedido`, `confirmarPedido` y `cancelarPedido`.

## Ejecutar el servidor MCP en Windows

Requisitos: JDK 21 y MySQL Server 8.4 instalado en `C:\Program Files\MySQL\MySQL Server 8.4`. Maven se descarga mediante el wrapper.

```powershell
cd supermercado-mcp
.\lab.cmd run
```

El lanzador prepara una instancia local de MySQL en `.local/mysql-data`, crea la base `labmcp` si falta y proporciona la contrasena mediante `PWD_UDEA` en el proceso. Comprueba la identidad de la instancia antes de modificarla; se detiene si el puerto 3306 pertenece a otra instalacion.

Endpoint MCP: **http://localhost:8088/mcp**. Con el servidor activo, `.\lab.cmd check` verifica MCP y consulta el catalogo. `.\lab.cmd test` ejecuta las pruebas del servidor.

## Ejecutar el agente Google ADK

Con el servidor MCP activo, abrir otra terminal:

```powershell
cd agente-supermercado
.\agent.cmd test       # Compilar y ejecutar las pruebas de integracion MCP
.\agent.cmd mcp        # Descubrir las siete herramientas y ejecutar listarProductos
.\agent.cmd web        # Interfaz ADK: http://localhost:8080/dev-ui/
```

Para usar Gemini, configurar **`GOOGLE_API_KEY`** mediante las variables de entorno de Windows y reiniciar VS Code o la terminal. No guardar la clave en archivos del repositorio. Despues, ejecutar `.\agent.cmd consulta` para una consulta conversacional de productos con herramientas de solo lectura.

El modelo predeterminado es `gemini-3.8-flash`; puede seleccionarse otro mediante `GEMINI_MODEL`. `MCP_ENDPOINT` permite cambiar la URL del servidor.

## Estado de la validacion

La compilacion del agente y sus dos pruebas de integracion MCP finalizaron correctamente. Se verificaron las siete herramientas y la consulta de ocho productos. La generacion con Gemini sigue pendiente de validacion: aunque la clave permite consultar los metadatos del modelo, una peticion minima de texto agoto un tiempo de espera de 90 segundos.

Se excluyen de Git las credenciales, configuraciones privadas, bases de datos locales, `.local/`, logs, archivos temporales y `target/`. Los scripts SQL de `src/main/resources` contienen el esquema y catalogo de ejemplo necesarios para el proyecto.
