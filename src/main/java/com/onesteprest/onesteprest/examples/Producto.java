package com.onesteprest.onesteprest.examples;

import com.onesteprest.onesteprest.annotations.RestModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Example model class annotated with @RestModel.
 * This will generate CRUD endpoints for the "Producto" type.
 */
@Entity
@RestModel(path = "/productos")
public class Producto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotNull(message = "El precio es obligatorio")
    @Min(value = 0, message = "El precio debe ser mayor o igual a cero")
    private Double precio;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    @JsonIgnoreProperties({"productos"})
    private Categoria categoria;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }
    
    public Categoria getCategoria() {
        return categoria;
    }
    
    public void setCategoria(Categoria categoria) {
        // Remove from old categoria if exists
        if (this.categoria != null && !this.categoria.equals(categoria)) {
            this.categoria.getProductos().remove(this);
        }
        
        this.categoria = categoria;
        
        // Add to new categoria if not null
        if (categoria != null && !categoria.getProductos().contains(this)) {
            categoria.getProductos().add(this);
        }
    }
    
    // Helper method for tests to set categoria by ID
    public void setCategoriaId(Long categoriaId) {
        if (categoriaId != null) {
            Categoria cat = new Categoria();
            cat.setId(categoriaId);
            this.categoria = cat;
        } else {
            this.categoria = null;
        }
    }
    
    // Helper method to get categoriaId
    public Long getCategoriaId() {
        return categoria != null ? categoria.getId() : null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Producto producto = (Producto) o;
        
        return id != null && id.equals(producto.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}