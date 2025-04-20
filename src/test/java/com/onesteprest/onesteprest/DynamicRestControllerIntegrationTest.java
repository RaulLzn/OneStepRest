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

import java.util.HashMap;
import java.util.Map;

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
        Map<String, Object> categoriaData = new HashMap<>();
        categoriaData.put("nombre", "Electrónicos");
        categoriaData.put("descripcion", "Productos electrónicos y gadgets");
        
        MvcResult categoriaResult = mockMvc.perform(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoriaData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Electrónicos"))
                .andReturn();
        
        // Extraer el ID de la categoría creada
        String categoriaResponse = categoriaResult.getResponse().getContentAsString();
        Map<String, Object> createdCategoria = objectMapper.readValue(categoriaResponse, Map.class);
        Number categoriaId = (Number) createdCategoria.get("id");
        
        // 2. Crear un producto asociado a la categoría
        Map<String, Object> productoData = new HashMap<>();
        productoData.put("nombre", "Smartphone XYZ");
        productoData.put("precio", 799.99);
        productoData.put("categoriaId", categoriaId);

        MvcResult productoResult = mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Smartphone XYZ"))
                .andExpect(jsonPath("$.precio").value(799.99))
                .andExpect(jsonPath("$.categoria.id").value(categoriaId))
                .andReturn();
        
        // Extraer el ID del producto creado
        String productoResponse = productoResult.getResponse().getContentAsString();
        Map<String, Object> createdProducto = objectMapper.readValue(productoResponse, Map.class);
        Number productoId = (Number) createdProducto.get("id");
        
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
        Map<String, Object> updatedProductoData = new HashMap<>();
        updatedProductoData.put("id", productoId);
        updatedProductoData.put("nombre", "Smartphone XYZ Pro");
        updatedProductoData.put("precio", 899.99);
        updatedProductoData.put("categoriaId", categoriaId); // Usar ID para la relación
        
        mockMvc.perform(put("/api/productos/{id}", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedProductoData)))
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
        Map<String, Object> invalidProducto = new HashMap<>();
        invalidProducto.put("nombre", ""); // Nombre vacío
        invalidProducto.put("precio", -10.0); // Precio negativo
        
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
            Map<String, Object> productoData = new HashMap<>();
            productoData.put("nombre", "Producto Test " + i);
            productoData.put("precio", 100.0 + i);
            
            mockMvc.perform(post("/api/productos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productoData)))
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
        Map<String, Object> categoriaData = new HashMap<>();
        categoriaData.put("nombre", "Tecnología");
        categoriaData.put("descripcion", "Productos tecnológicos");
        
        MvcResult categoriaResult = mockMvc.perform(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoriaData)))
                .andExpect(status().isOk())
                .andReturn();
        
        String categoriaResponse = categoriaResult.getResponse().getContentAsString();
        Map<String, Object> createdCategoria = objectMapper.readValue(categoriaResponse, Map.class);
        Number categoriaId = (Number) createdCategoria.get("id");
        
        // 2. Agregar productos a través de la relación
        Map<String, Object> producto1Data = new HashMap<>();
        producto1Data.put("nombre", "Laptop");
        producto1Data.put("precio", 1299.99);
        producto1Data.put("categoriaId", categoriaId);
        
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(producto1Data)))
                .andExpect(status().isOk());
        
        Map<String, Object> producto2Data = new HashMap<>();
        producto2Data.put("nombre", "Tablet");
        producto2Data.put("precio", 499.99);
        producto2Data.put("categoriaId", categoriaId);
        
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(producto2Data)))
                .andExpect(status().isOk());
        
        // 3. Verificar que podemos obtener los productos por categoría
        mockMvc.perform(get("/api/categorias/{id}", categoriaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoriaId))
                .andExpect(jsonPath("$.productos").isArray())
                .andExpect(jsonPath("$.productos", hasSize(2)));
    }
    
    @Test
    @Order(5)
    @Transactional
    public void testRelationshipWithNestedObject() throws Exception {
        // 1. Crear una categoría
        Map<String, Object> categoriaData = new HashMap<>();
        categoriaData.put("nombre", "Electrodomésticos");
        categoriaData.put("descripcion", "Artículos para el hogar");
        
        MvcResult categoriaResult = mockMvc.perform(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoriaData)))
                .andExpect(status().isOk())
                .andReturn();
        
        String categoriaResponse = categoriaResult.getResponse().getContentAsString();
        Map<String, Object> createdCategoria = objectMapper.readValue(categoriaResponse, Map.class);
        Number categoriaId = (Number) createdCategoria.get("id");
        
        // 2. Crear un producto con la categoría como objeto anidado
        Map<String, Object> categoriaRef = new HashMap<>();
        categoriaRef.put("id", categoriaId);
        
        Map<String, Object> productoData = new HashMap<>();
        productoData.put("nombre", "Refrigerador");
        productoData.put("precio", 1099.99);
        productoData.put("categoria", categoriaRef); // Objeto anidado con ID
        
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(categoriaId))
                .andExpect(jsonPath("$.categoria.nombre").value("Electrodomésticos"));
    }
}