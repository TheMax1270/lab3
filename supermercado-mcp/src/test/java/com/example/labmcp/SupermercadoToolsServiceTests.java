package com.example.labmcp;

import com.example.labmcp.model.*;
import com.example.labmcp.repository.*;
import com.example.labmcp.service.SupermercadoToolsService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class SupermercadoToolsServiceTests {
    @Autowired SupermercadoToolsService tools;
    @Autowired ProductoRepository productos;
    @Autowired PedidoRepository pedidos;
    @Autowired EntityManager em;

    private Producto producto(String precio, int stock) {
        return productos.saveAndFlush(new Producto("TestMcp-" + UUID.randomUUID(), new BigDecimal(precio), stock));
    }

    @Test void listaYBuscaProductosSinDistinguirMayusculas() {
        var p = producto("12.25", 10);
        assertThat(tools.listarProductos()).anySatisfy(v -> {
            assertThat(v.id()).isEqualTo(p.getId());
            assertThat(v.nombre()).isEqualTo(p.getNombre());
            assertThat(v.precio()).isEqualByComparingTo("12.25");
            assertThat(v.stock()).isEqualTo(10);
        });
        assertThat(tools.buscarProducto(p.getNombre().substring(4).toUpperCase()))
                .extracting(SupermercadoToolsService.ProductoVista::id).containsExactly(p.getId());
    }

    @Test void creaPedidoPendienteVacio() {
        var creado = tools.crearPedido("Calle 123");
        em.clear();
        var p = tools.consultarPedido(creado.id());
        assertThat(p.direccionEntrega()).isEqualTo("Calle 123");
        assertThat(p.estado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(p.total()).isEqualByComparingTo("0.00");
        assertThat(p.productos()).isEmpty();
    }

    @Test void agregaCalculaTotalDescuentaStockYConservaPrecio() {
        var a = producto("10.25", 10);
        var b = producto("3.10", 8);
        var pedido = tools.crearPedido("Calle 123");
        tools.agregarProductoPedido(pedido.id(), a.getId(), 2);
        a.setPrecio(new BigDecimal("20.50"));
        productos.flush();
        tools.agregarProductoPedido(pedido.id(), a.getId(), 1);
        tools.agregarProductoPedido(pedido.id(), b.getId(), 3);
        em.clear();
        var resultado = tools.consultarPedido(pedido.id());
        assertThat(resultado.total()).isEqualByComparingTo("50.30");
        assertThat(resultado.productos()).hasSize(3);
        assertThat(resultado.productos()).extracting(SupermercadoToolsService.DetalleVista::precioUnitario)
                .contains(new BigDecimal("10.25"), new BigDecimal("20.50"), new BigDecimal("3.10"));
        assertThat(productos.findById(a.getId()).orElseThrow().getStock()).isEqualTo(7);
        assertThat(productos.findById(b.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test void rechazaStockInsuficienteSinCambios() {
        var p = producto("10.00", 1);
        var pedido = tools.crearPedido("Calle 123");
        assertThatIllegalArgumentException().isThrownBy(() -> tools.agregarProductoPedido(pedido.id(), p.getId(), 2))
                .withMessageContaining("Stock insuficiente");
        assertThat(p.getStock()).isEqualTo(1);
        assertThat(pedidos.findById(pedido.id()).orElseThrow().getDetalles()).isEmpty();
    }

    @Test void rechazaCantidadCero() {
        var p = producto("10.00", 1);
        var pedido = tools.crearPedido("Calle 123");
        assertThatIllegalArgumentException().isThrownBy(() -> tools.agregarProductoPedido(pedido.id(), p.getId(), 0));
        assertThat(p.getStock()).isEqualTo(1);
    }

    @Test void rechazaConfirmarVacio() {
        var pedido = tools.crearPedido("Calle 123");
        assertThatIllegalArgumentException().isThrownBy(() -> tools.confirmarPedido(pedido.id())).withMessageContaining("vacio");
        assertThat(pedidos.findById(pedido.id()).orElseThrow().getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
    }

    @Test void confirmaSinDescontarStockNuevamente() {
        var p = producto("10.00", 10);
        var pedido = tools.crearPedido("Calle 123");
        tools.agregarProductoPedido(pedido.id(), p.getId(), 2);
        assertThat(tools.confirmarPedido(pedido.id()).estado()).isEqualTo(EstadoPedido.CONFIRMADO);
        em.clear();
        assertThat(productos.findById(p.getId()).orElseThrow().getStock()).isEqualTo(8);
        assertThatIllegalArgumentException().isThrownBy(() -> tools.agregarProductoPedido(pedido.id(), p.getId(), 1));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPedido.class, names = {"PENDIENTE", "CONFIRMADO"})
    void cancelaRestauraStockYRechazaSegundaCancelacion(EstadoPedido estado) {
        var p = producto("10.00", 10);
        var pedido = tools.crearPedido("Calle 123");
        tools.agregarProductoPedido(pedido.id(), p.getId(), 2);
        tools.agregarProductoPedido(pedido.id(), p.getId(), 3);
        if (estado == EstadoPedido.CONFIRMADO) { tools.confirmarPedido(pedido.id()); }
        var cancelado = tools.cancelarPedido(pedido.id());
        em.clear();
        assertThat(cancelado.estado()).isEqualTo(EstadoPedido.CANCELADO);
        assertThat(cancelado.productos()).hasSize(2);
        assertThat(cancelado.total()).isEqualByComparingTo("50.00");
        assertThat(productos.findById(p.getId()).orElseThrow().getStock()).isEqualTo(10);
        assertThatIllegalArgumentException().isThrownBy(() -> tools.cancelarPedido(pedido.id()));
        assertThat(productos.findById(p.getId()).orElseThrow().getStock()).isEqualTo(10);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPedido.class, names = {"EN_CAMINO", "ENTREGADO"})
    void noCancelaPedidosDespachados(EstadoPedido estado) {
        var p = producto("10.00", 10);
        var pedido = tools.crearPedido("Calle 123");
        tools.agregarProductoPedido(pedido.id(), p.getId(), 2);
        pedidos.findById(pedido.id()).orElseThrow().setEstado(estado);
        pedidos.flush();
        assertThatIllegalArgumentException().isThrownBy(() -> tools.cancelarPedido(pedido.id()));
        assertThat(p.getStock()).isEqualTo(8);
    }

    @Test void rechazaPedidoYProductoInexistentes() {
        assertThatIllegalArgumentException().isThrownBy(() -> tools.consultarPedido(Long.MAX_VALUE));
    }

    @Test void rechazaProductoInexistente() {
        var pedido = tools.crearPedido("Calle 123");
        assertThatIllegalArgumentException().isThrownBy(() -> tools.agregarProductoPedido(pedido.id(), Long.MAX_VALUE, 1));
        assertThat(pedidos.findById(pedido.id()).orElseThrow().getDetalles()).isEmpty();
    }
}
