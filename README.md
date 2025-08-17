# OneStepRest
OneStepRest es un framework innovador para la creación de APIs RESTful en Java con configuración mínima y máxima productividad. ¡Define tus modelos y deja que OneStepRest haga el resto!

## Características

- Generación automática de endpoints CRUD para entidades JPA
- Soporte completo para relaciones JPA (OneToMany, ManyToOne, ManyToMany, OneToOne)
- Validación integrada con Jakarta Bean Validation
- Gestión automática de relaciones bidireccionales
- Paginación y ordenamiento
- Filtrado dinámico de entidades
- Soporte para serialización y deserialización JSON

## Filtrado de Entidades

OneStepRest proporciona un potente sistema de filtrado que permite a los consumidores de la API realizar consultas complejas a través de parámetros de consulta o especificaciones JSON.

### Método 1: Filtrado por parámetros de consulta

Utiliza parámetros con el formato `filter_campo_operacion` para aplicar filtros:

```
GET /api/productos?filter_precio_gt=500
```

Las operaciones de filtrado disponibles son:

- `eq` - Igual a (=)
- `neq` - No igual a (!=)
- `gt` - Mayor que (>)
- `gte` - Mayor o igual que (>=)
- `lt` - Menor que (<)
- `lte` - Menor o igual que (<=)
- `like` - Contiene (LIKE %valor%)
- `in` - Está en lista de valores
- `between` - Entre dos valores

Para combinar varios filtros, simplemente añade más parámetros:

```
GET /api/productos?filter_precio_gt=500&filter_categoria.nombre_eq=Electrónicos
```

Por defecto, los filtros se combinan con lógica AND. Para usar lógica OR:

```
GET /api/productos?filter_logic=or&filter_nombre_eq=Laptop&filter_nombre_eq=Smartphone
```

### Método 2: Filtrado con JSON

Para consultas más complejas, puedes proporcionar una especificación de filtro en formato JSON:

```
GET /api/productos?filter={"filters":[{"field":"precio","operation":"GREATER_THAN","value":500},{"field":"categoria.nombre","operation":"EQUAL","value":"Electrónicos"}],"logic":"AND"}
```

### Filtrado en campos de relaciones

Puedes filtrar en campos de relaciones usando notación de punto:

```
GET /api/productos?filter_categoria.id_eq=1
```

### Combinación con Paginación y Ordenamiento

Los filtros se pueden combinar con la paginación y el ordenamiento:

```
GET /api/productos?filter_precio_gt=500&page=0&size=10&sortBy=precio&direction=desc
```
