package com.example.labmcp.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
    @NotBlank @Column(name = "direccion_entrega", nullable = false, length = 255)
    private String direccionEntrega;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;
    @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2)
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = new BigDecimal("0.00");
    @Version @Column(nullable = false)
    private Long version;
    @Valid
    @OneToMany(mappedBy = "pedido", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<DetallePedido> detalles = new ArrayList<>();

    protected Pedido() {}
    public Pedido(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public DetallePedido agregarDetalle(Producto producto, int cantidad) {
        DetallePedido detalle = new DetallePedido(this, producto, cantidad, producto.getPrecio());
        detalles.add(detalle);
        total = detalles.stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        return detalle;
    }
    public Long getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public EstadoPedido getEstado() { return estado; }
    public BigDecimal getTotal() { return total; }
    public Long getVersion() { return version; }
    public List<DetallePedido> getDetalles() { return Collections.unmodifiableList(detalles); }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }
}
