package com.example.labmcp;

import com.example.labmcp.model.*;
import com.example.labmcp.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class LabmcpApplicationTests {
    @Autowired ProductoRepository productos;
    @Autowired PedidoRepository pedidos;
    @Autowired DetallePedidoRepository detalles;
    @Autowired ToolCallbackProvider callbacks;
    @Autowired EntityManager em;
    @Autowired DataSource dataSource;

    @Test
    void contextLoads() {
        assertThat(callbacks.getToolCallbacks()).extracting(c -> c.getToolDefinition().name())
            .containsExactlyInAnyOrder("listarProductos", "buscarProducto", "crearPedido", "agregarProductoPedido", "consultarPedido", "confirmarPedido", "cancelarPedido");
    }

    @Test
    void catalogoInicialEsIdempotente() throws Exception {
        var nombres = java.util.List.of("Arroz 1 kg", "Leche 1 L", "Huevos docena",
                "Pan tajado 500 g", "Aceite vegetal 1 L", "Azucar 1 kg", "Sal 1 kg", "Pasta 500 g");
        var antes = productos.findAll();
        assertThat(antes).extracting(Producto::getNombre).containsAll(nombres);
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
        }
        assertThat(productos.count()).isEqualTo(antes.size());
        for (var producto : antes) {
            var despues = productos.findById(producto.getId()).orElseThrow();
            assertThat(despues.getPrecio()).isEqualByComparingTo(producto.getPrecio());
            assertThat(despues.getStock()).isEqualTo(producto.getStock());
        }
    }

    @Test
    @Transactional
    void persistePedidoConDetallesYPrecioHistorico() {
        var producto = productos.saveAndFlush(new Producto("Prueba-" + UUID.randomUUID(), new BigDecimal("4500.25"), 10));
        var otro = productos.saveAndFlush(new Producto("Prueba-" + UUID.randomUUID(), new BigDecimal("4200.10"), 20));
        var pedido = new Pedido("Calle 10 # 20-30");
        pedido.agregarDetalle(producto, 2);
        pedido.agregarDetalle(otro, 1);
        pedidos.saveAndFlush(pedido);
        Long id = pedido.getId();
        producto.setPrecio(new BigDecimal("9999.00"));
        productos.flush();
        em.clear();
        var guardado = pedidos.findById(id).orElseThrow();
        assertThat(guardado.getFecha()).isNotNull();
        assertThat(guardado.getDireccionEntrega()).isEqualTo("Calle 10 # 20-30");
        assertThat(guardado.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(guardado.getTotal()).isEqualByComparingTo("13200.60");
        assertThat(guardado.getDetalles()).hasSize(2);
        var detalle = guardado.getDetalles().stream().filter(d -> d.getProducto().getId().equals(producto.getId())).findFirst().orElseThrow();
        assertThat(detalles.findById(detalle.getId())).isPresent();
        assertThat(detalle.getPedido().getId()).isEqualTo(id);
        assertThat(detalle.getCantidad()).isEqualTo(2);
        assertThat(detalle.getPrecioUnitario()).isEqualByComparingTo("4500.25");
        assertThat(detalle.getProducto().getPrecio()).isEqualByComparingTo("9999.00");
        assertThat(detalle.getProducto().getStock()).isEqualTo(10);
    }

    @Test
    @Transactional
    void rechazaProductoConStockNegativo() {
        assertThatThrownBy(() -> productos.saveAndFlush(new Producto("Invalido-" + UUID.randomUUID(), new BigDecimal("1.00"), -1)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void rechazaCantidadNoPositiva() {
        var pedido = new Pedido("Calle 10");
        var producto = new Producto("Prueba", new BigDecimal("1.00"), 10);
        assertThatIllegalArgumentException().isThrownBy(() -> pedido.agregarDetalle(producto, 0));
        assertThat(pedido.getDetalles()).isEmpty();
        assertThat(pedido.getTotal()).isEqualByComparingTo("0.00");
    }
}
