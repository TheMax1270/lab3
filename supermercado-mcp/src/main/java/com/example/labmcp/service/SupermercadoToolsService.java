package com.example.labmcp.service;

import com.example.labmcp.model.*;
import com.example.labmcp.repository.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class SupermercadoToolsService {
    private final ProductoRepository productos;
    private final PedidoRepository pedidos;

    public SupermercadoToolsService(ProductoRepository productos, PedidoRepository pedidos) {
        this.productos = productos;
        this.pedidos = pedidos;
    }

    // DTOs evitan referencias circulares y acceso a proxies JPA fuera de la transaccion.
    public record ProductoVista(Long id, String nombre, BigDecimal precio, Integer stock) {}
    public record DetalleVista(Long id, Long productoId, String nombre, int cantidad,
                               BigDecimal precioUnitario, BigDecimal subtotal) {}
    public record PedidoVista(Long id, LocalDateTime fecha, String direccionEntrega,
                              EstadoPedido estado, BigDecimal total, List<DetalleVista> productos) {}

    @Tool(name = "listarProductos", description = "Lista todos los productos del supermercado con id, nombre, precio y stock disponible.")
    public List<ProductoVista> listarProductos() {
        return productos.findAll(Sort.by("id")).stream().map(this::vistaProducto).toList();
    }

    @Tool(name = "buscarProducto", description = "Busca productos por nombre o fragmento, sin distinguir mayusculas. Devuelve id, nombre, precio y stock; lista vacia si no hay coincidencias.")
    public List<ProductoVista> buscarProducto(@ToolParam(description = "Nombre o fragmento no vacio del producto") String nombre) {
        if (nombre == null || nombre.isBlank()) { throw new IllegalArgumentException("Indique un nombre o fragmento"); }
        return productos.findByNombreContainingIgnoreCaseOrderByNombreAsc(nombre.trim()).stream().map(this::vistaProducto).toList();
    }

    @Tool(name = "crearPedido", description = "Crea un pedido vacio con direccion de entrega, estado PENDIENTE y total cero. Devuelve su id para agregar productos.")
    @Transactional
    public PedidoVista crearPedido(@ToolParam(description = "Direccion de entrega no vacia, maximo 255 caracteres") String direccionEntrega) {
        if (direccionEntrega == null || direccionEntrega.isBlank() || direccionEntrega.trim().length() > 255) {
            throw new IllegalArgumentException("La direccion debe tener entre 1 y 255 caracteres");
        }
        return vistaPedido(pedidos.saveAndFlush(new Pedido(direccionEntrega.trim())));
    }

    @Tool(name = "agregarProductoPedido", description = "Agrega una cantidad positiva de un producto a un pedido PENDIENTE. Reserva stock descontandolo, conserva el precio actual y actualiza el total. Cada llamada agrega unidades adicionales.")
    @Transactional
    public PedidoVista agregarProductoPedido(
            @ToolParam(description = "Id del pedido PENDIENTE") Long pedidoId,
            @ToolParam(description = "Id del producto del catalogo") Long productoId,
            @ToolParam(description = "Cantidad entera mayor que cero y no superior al stock") int cantidad) {
        if (cantidad <= 0) { throw new IllegalArgumentException("La cantidad debe ser mayor que cero"); }
        Pedido pedido = pedidoBloqueado(pedidoId);
        exigirPendiente(pedido);
        Producto producto = productoBloqueado(productoId);
        if (producto.getStock() < cantidad) { throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre()); }
        pedido.agregarDetalle(producto, cantidad);
        producto.setStock(producto.getStock() - cantidad);
        pedidos.saveAndFlush(pedido);
        return vistaPedido(pedido);
    }

    @Tool(name = "consultarPedido", description = "Consulta un pedido por id: fecha, direccion de entrega, estado, total y productos con cantidades, precio historico y subtotal.")
    public PedidoVista consultarPedido(@ToolParam(description = "Id del pedido que se desea consultar") Long pedidoId) {
        validarId(pedidoId);
        return vistaPedido(pedidos.findById(pedidoId).orElseThrow(() -> new IllegalArgumentException("No existe el pedido " + pedidoId)));
    }

    @Tool(name = "confirmarPedido", description = "Confirma un pedido PENDIENTE con al menos un producto. No vuelve a descontar stock. Rechaza pedidos vacios o en otro estado.")
    @Transactional
    public PedidoVista confirmarPedido(@ToolParam(description = "Id del pedido PENDIENTE no vacio") Long pedidoId) {
        Pedido pedido = pedidoBloqueado(pedidoId);
        exigirPendiente(pedido);
        if (pedido.getDetalles().isEmpty()) { throw new IllegalArgumentException("No se puede confirmar un pedido vacio"); }
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        pedidos.flush();
        return vistaPedido(pedido);
    }

    @Tool(name = "cancelarPedido", description = "Cancela un pedido PENDIENTE o CONFIRMADO y devuelve al stock sus cantidades reservadas. Conserva detalles y total como historial. Rechaza pedidos CANCELADOS, EN_CAMINO o ENTREGADOS.")
    @Transactional
    public PedidoVista cancelarPedido(@ToolParam(description = "Id del pedido PENDIENTE o CONFIRMADO que se desea cancelar") Long pedidoId) {
        Pedido pedido = pedidoBloqueado(pedidoId);
        if (pedido.getEstado() != EstadoPedido.PENDIENTE && pedido.getEstado() != EstadoPedido.CONFIRMADO) {
            throw new IllegalArgumentException("No se puede cancelar un pedido en estado " + pedido.getEstado());
        }
        // Orden estable de locks evita ciclos entre cancelaciones de pedidos distintos.
        Map<Long, Integer> cantidades = new TreeMap<>();
        pedido.getDetalles().forEach(d -> cantidades.merge(d.getProducto().getId(), d.getCantidad(), Math::addExact));
        cantidades.forEach((id, cantidad) -> {
            Producto producto = productoBloqueado(id);
            producto.setStock(Math.addExact(producto.getStock(), cantidad));
        });
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedidos.flush();
        return vistaPedido(pedido);
    }

    private Pedido pedidoBloqueado(Long id) {
        validarId(id);
        return pedidos.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("No existe el pedido " + id));
    }
    private Producto productoBloqueado(Long id) {
        validarId(id);
        return productos.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("No existe el producto " + id));
    }
    private void validarId(Long id) {
        if (id == null || id <= 0) { throw new IllegalArgumentException("El id debe ser positivo"); }
    }
    private void exigirPendiente(Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) { throw new IllegalArgumentException("El pedido debe estar PENDIENTE"); }
    }
    private ProductoVista vistaProducto(Producto p) { return new ProductoVista(p.getId(), p.getNombre(), p.getPrecio(), p.getStock()); }
    private PedidoVista vistaPedido(Pedido p) {
        var detalles = p.getDetalles().stream().map(d -> new DetalleVista(d.getId(), d.getProducto().getId(),
                d.getProducto().getNombre(), d.getCantidad(), d.getPrecioUnitario(),
                d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))).toList();
        return new PedidoVista(p.getId(), p.getFecha(), p.getDireccionEntrega(), p.getEstado(), p.getTotal(), detalles);
    }
}
