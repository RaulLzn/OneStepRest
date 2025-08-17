package com.onesteprest.onesteprest.filters;

/**
 * Clase que representa un filtro individual con campo, operación y valor.
 */
public class Filter {
    private String field;
    private FilterOperation operation;
    private Object value;
    private Object secondValue; // Para operaciones como BETWEEN

    public Filter() {
    }

    public Filter(String field, FilterOperation operation, Object value) {
        this.field = field;
        this.operation = operation;
        this.value = value;
    }

    public Filter(String field, FilterOperation operation, Object value, Object secondValue) {
        this.field = field;
        this.operation = operation;
        this.value = value;
        this.secondValue = secondValue;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public FilterOperation getOperation() {
        return operation;
    }

    public void setOperation(FilterOperation operation) {
        this.operation = operation;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getSecondValue() {
        return secondValue;
    }

    public void setSecondValue(Object secondValue) {
        this.secondValue = secondValue;
    }

    @Override
    public String toString() {
        return "Filter{" +
                "field='" + field + '\'' +
                ", operation=" + operation +
                ", value=" + value +
                (secondValue != null ? ", secondValue=" + secondValue : "") +
                '}';
    }
}
