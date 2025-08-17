package com.onesteprest.onesteprest.filters;

/**
 * Enum de operaciones de filtrado soportadas.
 */
public enum FilterOperation {
    EQUAL("eq"),
    NOT_EQUAL("neq"),
    GREATER_THAN("gt"),
    GREATER_THAN_OR_EQUAL("gte"),
    LESS_THAN("lt"),
    LESS_THAN_OR_EQUAL("lte"),
    LIKE("like"),
    IN("in"),
    BETWEEN("between");

    private final String code;

    FilterOperation(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * Obtiene la operación a partir del código.
     *
     * @param code Código de operación
     * @return FilterOperation correspondiente
     */
    public static FilterOperation fromCode(String code) {
        for (FilterOperation operation : values()) {
            if (operation.getCode().equalsIgnoreCase(code)) {
                return operation;
            }
        }
        return EQUAL; // Valor por defecto
    }
}
