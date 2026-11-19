package com.example.labmcp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "producto", uniqueConstraints = @UniqueConstraint(name = "uk_producto_nombre", columnNames = "nombre"))
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 120)
    private String nombre;
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;
    @NotNull @Min(0) @Column(nullable = false)
    private Integer stock;
    @Version @Column(nullable = false)
    private Long version;

    protected Producto() {}
    public Producto(String nombre, BigDecimal precio, Integer stock) {
        this.nombre = nombre; this.precio = precio; this.stock = stock;
    }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
    public Integer getStock() { return stock; }
    public Long getVersion() { return version; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public void setStock(Integer stock) { this.stock = stock; }
}
