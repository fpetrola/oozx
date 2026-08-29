# Tests de Compatibilidad para TableAluOperation

Sistema para exportar y verificar la compatibilidad de operaciones ALU que heredan de `TableAluOperation`.

**Concepto**: El `int[] table` de una `TableAluOperation` contiene todos los valores precalculados para combinaciones de entradas. Se serializa a JSON como baseline. Cuando corre el test, se regenera la tabla y se compara entrada por entrada. Cualquier cambio se detecta inmediatamente.

## Archivos Creados

### 1. `TableAluOperationExporter`
Clase utilitaria que:
- Extrae el campo `int[] table` de una `TableAluOperation` (usando reflexión)
- Convierte los valores a JSON (separando resultado y flags)
- Carga JSON previamente guardado
- Compara tablas para detectar cambios

**Estructura JSON exportada:**
```json
{
  "operationName": "RLA",
  "className": "com.fpetrola.z80.instructions.impl.RLA$1",
  "exportedAt": 1234567890000,
  "tableSize": 512,
  "entries": [
    {
      "index": 0,
      "value": 0x00000000,
      "resultA": 0x00,
      "resultFlags": 0x00
    },
    {
      "index": 1,
      "value": 0x00000002,
      "resultA": 0x00,
      "resultFlags": 0x02
    }
  ]
}
```

### 2. `ExportRLATableMain`
Programa que exporta la tabla de RLA a JSON:
```bash
java -cp target/classes com.fpetrola.z80.instructions.impl.ExportRLATableMain
```

Genera: `emulator/src/test/resources/rla_table_export.json`

### 3. `RLATableCompatibilityTest`
Test JUnit 5 que:
- Carga `rla_table_export.json`
- Regenera la tabla actual desde RLA
- Compara entrada por entrada
- Reporta todas las diferencias encontradas

## Cómo Usar

### Paso 1: Generar Baseline
```bash
# Compilar
mvn clean compile test-compile

# Exportar tabla de RLA
java -cp target/classes:target/test-classes com.fpetrola.z80.instructions.impl.ExportRLATableMain
```

Esto crea: `emulator/src/test/resources/rla_table_export.json`

### Paso 2: Verificar Compatibilidad
```bash
mvn test -Dtest=RLATableCompatibilityTest
```

Salida esperada:
```
✓ Exported RLA table (512 entries)
  Saved to: emulator/src/test/resources/rla_table_export.json
```

Si hay cambios en RLA, el test fallará mostrando exactamente qué entradas cambiaron:
```
Mismatch at index 128: current=(A=0x00,F=0x01,val=0x00000001) vs saved=(A=0x00,F=0x00,val=0x00000000)
```

## Cómo Extender a Otros TableAluOperation

Hay dos formas de crear tests para otras operaciones ALU:

### Opción A: Heredar del Template

```java
@DisplayName("SLA Table Compatibility Test")
class SLATableCompatibilityTest extends TableAluOperationTestTemplate {

  @Override
  protected String getTableExportPath() {
    return "src/test/resources/sla_table_export.json";
  }

  @Override
  protected TableAluOperation getTableOperation() {
    return SLA.slaTableAluOperation;
  }

  @Override
  protected String getOperationName() {
    return "SLA";
  }
}
```

### Opción B: Copiar de RLATableCompatibilityTest

Más control, cambiar:
- Nombre: `SLATableCompatibilityTest`
- `TABLE_EXPORT_PATH`: `"src/test/resources/sla_table_export.json"`
- `RLA.rlaTableAluOperation` → `SLA.slaTableAluOperation`
- `"RLA"` → `"SLA"`

### Paso 1: Generar Baseline

```bash
mvn test -Dtest=SLATableCompatibilityTest#exportTableOnDemand
```

Esto crea `src/test/resources/sla_table_export.json`

### Paso 2: Verificar Compatibilidad

```bash
mvn test -Dtest=SLATableCompatibilityTest
```

Todos los tests pasarán (la tabla match consigo misma la primera vez).

## Ventajas

- ✅ **Captura el estado actual**: La tabla se serializa tal como está en memoria
- ✅ **Detección de cambios**: Cualquier modificación se detecta inmediatamente
- ✅ **Trazabilidad**: JSON con timestamp permite auditar cuándo cambió cada operación
- ✅ **Diferencias claras**: Reporte detallado de índice, valor anterior y nuevo
- ✅ **Reutilizable**: Una clase base para todas las operaciones ALU
- ✅ **Sin duplicación**: Usa reflexión para acceder a `table` sin modificar la clase original

## Consideraciones

### Tabla Inicial (Baseline)
La primera vez se genera con `ExportRLATableMain` y se commitea en Git.

Cuando se actualiza el comportamiento intencional de una operación:
```bash
# Regenerar archivo
java -cp target/classes:target/test-classes com.fpetrola.z80.instructions.impl.ExportRLATableMain

# Git registra el cambio
git add emulator/src/test/resources/rla_table_export.json
git commit -m "Update RLA table after fixing flag calculation"
```

### Diferentes Tipos de TableAluOperation
Este sistema funciona para:
- **BiFunction** (256 × 2 = 512 entradas) - RLA, RLC, etc.
- **BiAndBooleanFunction** (256 × 256 × 2 = 131,072 entradas) - Operaciones con valor extra
- **TriFunction** (256 × 256 × 256 = 16,777,216 entradas) - Operaciones complejas

Solo necesitas cambiar la ruta JSON y nombre de la operación.

## Test en Ejecución
El test de compatibilidad tiene tres métodos:

1. `testRlaTableCompatibility()` - Verifica valores contra JSON
2. `testRlaTableInitialized()` - Valida que la tabla no esté vacía
3. `exportRlaTableOnDemand()` - Permite regenerar el JSON desde el test mismo

## Ejemplo de Salida

```
Exported current RLA table...
✓ Exported RLA table (512 entries)
  Saved to: emulator/src/test/resources/rla_table_export.json
Exported RLA with 512 entries
First 5 entries:
  idx=  0: val=0x00000000 (A=0x00, F=0x00)
  idx=  1: val=0x00000002 (A=0x00, F=0x02)
  idx=  2: val=0x00000004 (A=0x00, F=0x04)
  idx=  3: val=0x00000006 (A=0x00, F=0x06)
  idx=  4: val=0x00000008 (A=0x00, F=0x08)
```

## Lo que un sello dorado no dice

Un MD5 detecta que una tabla **cambió**; no dice que sea **correcta**. Si una operación nació mal,
su sello congela el error y el test queda en verde para siempre. Eso fue exactamente lo que pasó
con cuatro instrucciones acá.

Por eso al lado de esta suite vive `AluReferenceTest`, que no compara contra un baseline propio
sino contra el comportamiento publicado del Z80, recorriendo **todo** el espacio de entrada de
cada instrucción (131072 combinaciones: cada acumulador contra cada operando contra los dos
acarreos), bits no documentados 3 y 5 incluidos, y ejecutando el opcode a través del procesador
en lugar de llamar a la tabla directamente.

Las dos se necesitan:

| | `AllTableAluOperationsCompatibilityTest` | `AluReferenceTest` |
|---|---|---|
| Contra qué compara | el sello de la última vez | el Z80 documentado |
| Qué detecta | cualquier cambio, incluso uno accidental | que el valor esté mal |
| Qué no detecta | que el valor esté mal desde el principio | un cambio que siga siendo correcto |

Flujo cuando `AluReferenceTest` encuentra un error: se arregla la instrucción, la suite de sellos
falla porque la tabla cambió, y recién ahí se repone el MD5 en
`emulator/src/test/resources/table_alu_operations_config.json`. Reponer un sello sin que la
suite de referencia esté verde es tapar el problema.

También conviene tener presente que la suite de Fuse, con 1356 casos, tiene **un vector por
opcode**: alcanza para encontrar una instrucción rota de arriba abajo, no una que acierta en ese
vector y falla en el resto. Las cuatro fallas encontradas acá pasaban Fuse.
