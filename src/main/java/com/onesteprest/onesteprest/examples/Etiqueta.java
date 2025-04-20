package com.onesteprest.onesteprest.examples;

import com.onesteprest.onesteprest.annotations.RestModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

/**
 * Example model class for tags that can be applied to products.
 */
@Entity
@RestModel(path = "/etiquetas")
public class Etiqueta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    private String color;
    
    @ManyToMany(mappedBy = "etiquetas")
    @JsonIgnoreProperties({"etiquetas"})
    private Set<Producto> productos = new HashSet<>();
    
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
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public Set<Producto> getProductos() {
        return productos;
    }
    
    public void setProductos(Set<Producto> productos) {
        this.productos = productos;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Etiqueta etiqueta = (Etiqueta) o;
        
        return id != null && id.equals(etiqueta.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}