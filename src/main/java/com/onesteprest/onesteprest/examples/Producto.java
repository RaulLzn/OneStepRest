package com.onesteprest.onesteprest.examples;

import com.onesteprest.onesteprest.annotations.RestModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

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
    
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "producto_etiqueta",
        joinColumns = @JoinColumn(name = "producto_id"),
        inverseJoinColumns = @JoinColumn(name = "etiqueta_id")
    )
    @JsonIgnoreProperties({"productos"})
    private Set<Etiqueta> etiquetas = new HashSet<>();

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
        this.categoria = categoria;
    }
    
    public Set<Etiqueta> getEtiquetas() {
        return etiquetas;
    }
    
    public void setEtiquetas(Set<Etiqueta> etiquetas) {
        this.etiquetas = etiquetas;
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