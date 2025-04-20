package com.onesteprest.onesteprest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onesteprest.onesteprest.core.RestModelRegistry;
import com.onesteprest.onesteprest.examples.Categoria;
import com.onesteprest.onesteprest.examples.Producto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DynamicRestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestModelRegistry restModelRegistry;

    @BeforeEach
    public void setup() {
        restModelRegistry.registerModels();
    }
    
    @Test
    @Order(1)
    @Transactional
    public void testCrudOperations() throws Exception {
        // 1. Crear una categoría
        Categoria categoria = new Categoria();
        categoria.setNombre("Electrónicos");
        categoria.setDescripcion("Productos electrónicos y gadgets");
        
        MvcResult categoriaResult = mockMvc.perform(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Electrónicos"))
                .andReturn();
        
        // Extraer el ID de la categoría creada
        String categoriaResponse = categoriaResult.getResponse().getContentAsString();
        Categoria createdCategoria = objectMapper.readValue(categoriaResponse, Categoria.class);
        Long categoriaId = createdCategoria.getId();
        
        // 2. Crear un producto asociado a la categoría
        Producto producto = new Producto();
        producto.setNombre("Smartphone XYZ");
        producto.setPrecio(799.99);
        
        // Asociar el producto con la categoría - simplemente usando el ID
        // No usamos el objeto completo para evitar ciclos
        producto.setCategoriaId(categoriaId);
        
        MvcResult productoResult = mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Smartphone XYZ"))
                .andExpect(jsonPath("$.precio").value(799.99))
                .andExpect(jsonPath("$.categoria.id").value(categoriaId))
                .andReturn();
        
        // Extraer el ID del producto creado
        String productoResponse = productoResult.getResponse().getContentAsString();
        Producto createdProducto = objectMapper.readValue(productoResponse, Producto.class);
        Long productoId = createdProducto.getId();
        
        // 3. Obtener todos los productos
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].nombre").exists());
        
        // 4. Verificar que se puede obtener un solo producto
        mockMvc.perform(get("/api/productos/{id}", productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productoId))
                .andExpect(jsonPath("$.nombre").value("Smartphone XYZ"));
        
        // 5. Actualizar un producto
        Producto updatedProducto = new Producto();
        updatedProducto.setId(productoId);
        updatedProducto.setNombre("Smartphone XYZ Pro");
        updatedProducto.setPrecio(899.99);
        updatedProducto.setCategoriaId(categoriaId); // Usar ID en lugar del objeto completo
        
        mockMvc.perform(put("/api/productos/{id}", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedProducto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Smartphone XYZ Pro"))
                .andExpect(jsonPath("$.precio").value(899.99));
        
        // 6. Verificar la actualización
        mockMvc.perform(get("/api/productos/{id}", productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Smartphone XYZ Pro"));
        
        // 7. Eliminar el producto
        mockMvc.perform(delete("/api/productos/{id}", productoId))
                .andExpect(status().isNoContent());
        
        // 8. Verificar que el producto fue eliminado
        mockMvc.perform(get("/api/productos/{id}", productoId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @Order(2)
    public void testValidation() throws Exception {
        // Producto con datos inválidos (sin nombre y precio negativo)
        Producto invalidProducto = new Producto();
        invalidProducto.setNombre(""); // Nombre vacío
        invalidProducto.setPrecio(-10.0); // Precio negativo
        
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProducto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.nombre").exists())
                .andExpect(jsonPath("$.errors.precio").exists());
    }
    
    @Test
    @Order(3)
    @Transactional
    public void testPagination() throws Exception {
        // Crear múltiples productos para probar la paginación
        for (int i = 1; i <= 15; i++) {
            Producto producto = new Producto();
            producto.setNombre("Producto Test " + i);
            producto.setPrecio(100.0 + i);
            
            mockMvc.perform(post("/api/productos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(producto)))
                    .andExpect(status().isOk());
        }
        
        // Probar paginación - página 0, tamaño 5
        mockMvc.perform(get("/api/productos")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(15)))
                .andExpect(jsonPath("$.totalPages").value(greaterThanOrEqualTo(3)));
        
        // Probar paginación - página 1, tamaño 5
        mockMvc.perform(get("/api/productos")
                .param("page", "1")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(5)));
    }
    
    @Test
    @Order(4)
    @Transactional
    public void testRelationships() throws Exception {
        // 1. Crear una categoría
        Categoria categoria = new Categoria();
        categoria.setNombre("Tecnología");
        categoria.setDescripcion("Productos tecnológicos");
        
        MvcResult categoriaResult = mockMvc.perform(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoria)))
                .andExpect(status().isOk())
                .andReturn();
        
        String categoriaResponse = categoriaResult.getResponse().getContentAsString();
        Categoria createdCategoria = objectMapper.readValue(categoriaResponse, Categoria.class);
        Long categoriaId = createdCategoria.getId();
        
        // 2. Agregar productos a través de la relación
        Producto producto1 = new Producto();
        producto1.setNombre("Laptop");
        producto1.setPrecio(1299.99);
        producto1.setCategoriaId(categoriaId);
        
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(producto1)))
                .andExpect(status().isOk());
        
        Producto producto2 = new Producto();
        producto2.setNombre("Tablet");
        producto2.setPrecio(499.99);
        producto2.setCategoriaId(categoriaId);
        
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(producto2)))
                .andExpect(status().isOk());
        
        // 3. Verificar que podemos obtener los productos por categoría
        mockMvc.perform(get("/api/categorias/{id}", categoriaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoriaId))
                .andExpect(jsonPath("$.productos").isArray())
                .andExpect(jsonPath("$.productos", hasSize(2)));
    }
}