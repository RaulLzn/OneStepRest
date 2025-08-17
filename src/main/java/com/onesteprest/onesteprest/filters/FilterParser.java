package com.onesteprest.onesteprest.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onesteprest.onesteprest.utils.TypeConverter;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Componente para analizar parámetros de consulta y convertirlos en especificaciones de filtro.
 */
@Component
public class FilterParser {
    
    private final ObjectMapper objectMapper;
    
    public FilterParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Analiza los parámetros de consulta para crear filtros.
     * 
     * @param queryParams Los parámetros de consulta (normalmente del request)
     * @return Especificación del filtro basada en los parámetros
     */
    public FilterSpecification parseFromQueryParams(Map<String, String[]> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return new FilterSpecification();
        }

        List<Filter> filters = new ArrayList<>();
        FilterSpecification.FilterLogic logic = FilterSpecification.FilterLogic.AND;

        // Procesar la lógica del filtro (AND/OR)
        String[] logicValues = queryParams.get("filter_logic");
        if (logicValues != null && logicValues.length > 0) {
            String logicValue = logicValues[0];
            if ("or".equalsIgnoreCase(logicValue)) {
                logic = FilterSpecification.FilterLogic.OR;
            }
        }

        System.out.println("Processing filter parameters: " + queryParams);

        // Procesar campos individuales
        for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("filter_") && !key.equals("filter_logic")) {
                try {
                    String[] values = entry.getValue();
                    if (values != null && values.length > 0) {
                        processFilterParam(key, values, filters);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing filter parameter: " + key + " - " + e.getMessage());
                    e.printStackTrace();
                    // Continue processing other filters even if one fails
                }
            }
        }

        return new FilterSpecification(filters, logic);
    }

    /**
     * Procesa un parámetro de filtro individual.
     */
    private void processFilterParam(String key, String[] values, List<Filter> filters) {
        if (values == null || values.length == 0) {
            return;
        }

        // Formato esperado: filter_field_operation
        String[] parts = key.split("_", 3);
        if (parts.length < 3) {
            return;
        }

        String field = parts[1];
        String operationCode = parts[2];
        FilterOperation operation = FilterOperation.fromCode(operationCode);
        
        System.out.println("Processing filter: field=" + field + ", operation=" + operation + 
                         ", value=" + (values.length > 0 ? values[0] : "null"));

        // Manejar tipos de operación especiales
        try {
            switch (operation) {
                case BETWEEN:
                    if (values.length >= 2) {
                        filters.add(new Filter(field, operation, values[0], values[1]));
                    } else if (values.length == 1 && values[0].contains(",")) {
                        String[] betweenValues = values[0].split(",", 2);
                        filters.add(new Filter(field, operation, betweenValues[0], betweenValues[1]));
                    }
                    break;
                    
                case IN:
                    if (values.length > 1) {
                        filters.add(new Filter(field, operation, Arrays.asList(values)));
                    } else if (values.length == 1) {
                        if (values[0].contains(",")) {
                            // Split the comma-separated values
                            String[] inValues = values[0].split(",");
                            filters.add(new Filter(field, operation, Arrays.asList(inValues)));
                        } else {
                            // Single value, treat as a regular IN operation with one element
                            List<String> singleValueList = new ArrayList<>();
                            singleValueList.add(values[0]);
                            filters.add(new Filter(field, operation, singleValueList));
                        }
                    }
                    break;
                    
                default:
                    // Operaciones regulares
                    if (values.length > 0 && values[0] != null) {
                        filters.add(new Filter(field, operation, values[0].trim()));
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error creating filter for field: " + field + 
                               ", operation: " + operation + 
                               ", value: " + (values.length > 0 ? values[0] : "null") +
                               " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Convierte una cadena JSON a una especificación de filtro.
     */
    public FilterSpecification parseFromJson(String jsonFilter) {
        try {
            if (jsonFilter == null || jsonFilter.isEmpty()) {
                return new FilterSpecification();
            }
            return objectMapper.readValue(jsonFilter, FilterSpecification.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing filter JSON: " + e.getMessage(), e);
        }
    }
}
