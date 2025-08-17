package com.onesteprest.onesteprest.filters;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase que representa un conjunto de filtros con su lógica de combinación.
 * Permite construir consultas complejas con múltiples condiciones.
 */
public class FilterSpecification {
    private List<Filter> filters = new ArrayList<>();
    private FilterLogic logic = FilterLogic.AND;

    public enum FilterLogic {
        AND, OR
    }

    public FilterSpecification() {
    }

    public FilterSpecification(List<Filter> filters) {
        this.filters = filters;
    }

    public FilterSpecification(List<Filter> filters, FilterLogic logic) {
        this.filters = filters;
        this.logic = logic;
    }

    /**
     * Añade un filtro individual a la especificación.
     *
     * @param filter Filtro a añadir
     * @return Esta especificación para encadenar llamadas
     */
    public FilterSpecification addFilter(Filter filter) {
        this.filters.add(filter);
        return this;
    }

    /**
     * Añade un filtro con los parámetros especificados.
     *
     * @param field Campo a filtrar
     * @param operation Operación de filtrado
     * @param value Valor de comparación
     * @return Esta especificación para encadenar llamadas
     */
    public FilterSpecification addFilter(String field, FilterOperation operation, Object value) {
        this.filters.add(new Filter(field, operation, value));
        return this;
    }

    /**
     * Añade un filtro con operación BETWEEN.
     *
     * @param field Campo a filtrar
     * @param value1 Valor inferior del rango
     * @param value2 Valor superior del rango
     * @return Esta especificación para encadenar llamadas
     */
    public FilterSpecification addBetweenFilter(String field, Object value1, Object value2) {
        this.filters.add(new Filter(field, FilterOperation.BETWEEN, value1, value2));
        return this;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public FilterLogic getLogic() {
        return logic;
    }

    public void setLogic(FilterLogic logic) {
        this.logic = logic;
    }

    /**
     * Verifica si hay filtros en la especificación.
     *
     * @return true si hay al menos un filtro, false en caso contrario
     */
    public boolean hasFilters() {
        return filters != null && !filters.isEmpty();
    }
}
