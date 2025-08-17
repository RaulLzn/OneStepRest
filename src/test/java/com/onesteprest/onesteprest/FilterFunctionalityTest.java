package com.onesteprest.onesteprest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onesteprest.onesteprest.core.RestModelRegistry;
import com.onesteprest.onesteprest.filters.FilterOperation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class FilterFunctionalityTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestModelRegistry restModelRegistry;

    @Test
    @Transactional
    public void testFilteringByQueryParams() throws Exception {
        // 1. Registrar modelos
        restModelRegistry.registerModels();
        
        // 2. Crear algunas categorías
        Map<String, Object> categoria1Data = new HashMap<>();
        categoria1Data.put("nombre", "Electrónicos");
        categoria1Data.put("descripcion", "Productos electrónicos y gadgets");
        
        mockMvc.perform(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoria1Data)))
                .andExpect(status().isOk());
        
        Map<String, Object> categoria2Data = new HashMap<>();
        categoria2Data.put("nombre", "Hogar");
        categoria2Data.put("descripcion", "Artículos para el hogar");
        
        mockMvc.perform(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoria2Data)))
                .andExpect(status().isOk());
        
        // 3. Crear productos con diferentes precios y categorías
        // Crear productos en categoría Electrónicos
        createProducto("Smartphone", 799.99, 1L);
        createProducto("Laptop", 1299.99, 1L);
        createProducto("Auriculares", 99.99, 1L);
        
        // Crear productos en categoría Hogar
        createProducto("Mesa", 249.99, 2L);
        createProducto("Silla", 89.99, 2L);
        
        // 4. Probar filtrado por precio (mayor que)
        mockMvc.perform(get("/api/productos")
                .param("filter_precio_gt", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].nombre", hasItems("Smartphone", "Laptop", "Mesa")));
        
        // 5. Probar filtrado por precio (menor o igual a)
        mockMvc.perform(get("/api/productos")
                .param("filter_precio_lte", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nombre", hasItems("Auriculares", "Silla")));
        
        // 6. Probar filtrado por nombre (contiene)
        mockMvc.perform(get("/api/productos")
                .param("filter_nombre_like", "ar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nombre", hasItems("Auriculares", "Smartphone")));
        
        // 7. Probar filtrado por categoría (relación)
        mockMvc.perform(get("/api/productos")
                .param("filter_categoria.id_eq", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nombre", hasItems("Mesa", "Silla")));
                
        // 8. Probar filtrado combinado con OR
        mockMvc.perform(get("/api/productos")
                .param("filter_logic", "or")
                .param("filter_nombre_eq", "Laptop")
                .param("filter_nombre_eq", "Silla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nombre", hasItems("Laptop", "Silla")));
                
        // 9. Probar filtrado con paginación
        mockMvc.perform(get("/api/productos")
                .param("page", "0")
                .param("size", "2")
                .param("filter_precio_gt", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));
    }
    
    @Test
    @Transactional
    public void testFilteringWithJsonFilter() throws Exception {
        // 1. Registrar modelos
        restModelRegistry.registerModels();
        
        // 2. Crear algunas categorías y productos
        createCategoria("Electrónicos", "Productos electrónicos y gadgets");
        createCategoria("Hogar", "Artículos para el hogar");
        
        createProducto("Smartphone", 799.99, 1L);
        createProducto("Laptop", 1299.99, 1L);
        createProducto("Mesa", 249.99, 2L);
        
        // 3. Probar filtrado utilizando JSON para especificación de filtro
        String filterJson = "{\"filters\":[{\"field\":\"precio\",\"operation\":\"GREATER_THAN\",\"value\":500}],\"logic\":\"AND\"}";
        
        mockMvc.perform(get("/api/productos")
                .param("filter", filterJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nombre", hasItems("Smartphone", "Laptop")));
                
        // 4. Probar filtrado complejo con múltiples condiciones
        String complexFilterJson = "{\"filters\":[" +
            "{\"field\":\"precio\",\"operation\":\"GREATER_THAN\",\"value\":200}," +
            "{\"field\":\"categoria.nombre\",\"operation\":\"EQUAL\",\"value\":\"Hogar\"}" +
            "],\"logic\":\"AND\"}";
            
        mockMvc.perform(get("/api/productos")
                .param("filter", complexFilterJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre", equalTo("Mesa")));
    }
    
    // Helpers
    
    private void createCategoria(String nombre, String descripcion) throws Exception {
        Map<String, Object> categoriaData = new HashMap<>();
        categoriaData.put("nombre", nombre);
        categoriaData.put("descripcion", descripcion);
        
        mockMvc.perform(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoriaData)))
                .andExpect(status().isOk());
    }
    
    private void createProducto(String nombre, Double precio, Long categoriaId) throws Exception {
        Map<String, Object> productoData = new HashMap<>();
        productoData.put("nombre", nombre);
        productoData.put("precio", precio);
        productoData.put("categoriaId", categoriaId);
        
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoData)))
                .andExpect(status().isOk());
    }
}
