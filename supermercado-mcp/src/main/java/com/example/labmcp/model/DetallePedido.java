package com.example.labmcp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "detalle_pedido")
public class DetallePedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_pedido"))
    private Pedido pedido;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_producto"))
    private Producto producto;
    @Min(1) @Column(nullable = false)
    private int cantidad;
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    protected DetallePedido() {}
    DetallePedido(Pedido pedido, Producto producto, int cantidad, BigDecimal precioUnitario) {
        if (cantidad < 1) { throw new IllegalArgumentException("La cantidad debe ser positiva"); }
        this.pedido = Objects.requireNonNull(pedido);
        this.producto = Objects.requireNonNull(producto);
        this.cantidad = cantidad;
        this.precioUnitario = Objects.requireNonNull(precioUnitario);
    }
    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public Producto getProducto() { return producto; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
}
