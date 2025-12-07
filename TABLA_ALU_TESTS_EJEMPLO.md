# Ejemplo: Detección de Cambios en TableAluOperation

## Escenario: Bug introducido en RLA

Supongamos que alguien modifica accidentalmente la operación RLA:

```java
// ANTES (correcto)
A = (A << 1) | (F & FLAG_C);

// DESPUÉS (bug - bit shift incorrecto)
A = (A << 2) | (F & FLAG_C);  // ⚠️ INCORRECTO: shift de 2 en lugar de 1
```

## Ejecución del Test

```bash
mvn test -Dtest=RLATableCompatibilityTest#testRlaTableCompatibility
```

## Salida del Test

```
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.210 s
[ERROR] testRlaTableCompatibility  Time elapsed: 0.205 s  <<< FAILURE!
[ERROR]   RLA table has 256 differences from baseline:
Found 256 differences:
Mismatch at index 1: current=(A=0x04,F=0x00,val=0x00000400) vs saved=(A=0x02,F=0x00,val=0x00000200)
Mismatch at index 2: current=(A=0x08,F=0x00,val=0x00000800) vs saved=(A=0x04,F=0x00,val=0x00000400)
Mismatch at index 3: current=(A=0x0C,F=0x00,val=0x00000C00) vs saved=(A=0x06,F=0x00,val=0x00000600)
Mismatch at index 4: current=(A=0x10,F=0x08,val=0x00001008) vs saved=(A=0x08,F=0x08,val=0x00000808)
Mismatch at index 5: current=(A=0x14,F=0x08,val=0x00001408) vs saved=(A=0x0A,F=0x08,val=0x00000A08)
...y 251 más...
```

### Análisis del Error

El test detectó inmediatamente:
- Índice 1 con entrada A=0x01, Carry=0
  - Esperado: A << 1 = 0x02 (valor 0x00000200)
  - Obtenido: A << 2 = 0x04 (valor 0x00000400)

- Todos los valores a partir de índice 1 difieren (256 entradas afectadas)

## Restaurar a Correcto

```java
A = (A << 1) | (F & FLAG_C);  // ✅ CORRECTO
```

Ejecutar nuevamente:

```bash
mvn test -Dtest=RLATableCompatibilityTest#testRlaTableCompatibility
```

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.125 s - in com.fpetrola.z80.instructions.impl.RLATableCompatibilityTest
[INFO] BUILD SUCCESS
```

## Ventajas Demostradas

1. **Detección automática**: No necesita casos de prueba manuales
2. **Cobertura total**: Verifica todos los 512 valores (256 × 2)
3. **Mensaje claro**: Muestra exactamente qué cambió
4. **Regresión**: Previene que bugs similares se reintroduzcan

## Casos de Uso Reales

Este sistema es especialmente útil cuando:

- Se refactoriza código de operaciones ALU
- Se optimiza la generación de la tabla
- Se cambian constantes de flags
- Se integran cambios de otra rama
- Se detectan incompatibilidades con hardware real

## Modificación Intencional

Si el cambio es **intencional** (arreglar un bug real en RLA):

```bash
# 1. Hacer la corrección
# (editar RLA.java)

# 2. Regenerar el JSON
mvn test -Dtest=RLATableCompatibilityTest#exportTableOnDemand

# 3. Verificar nuevos valores
git diff src/test/resources/rla_table_export.json

# 4. Commit del cambio y JSON actualizado
git add -u
git add src/test/resources/rla_table_export.json
git commit -m "Fix RLA carry flag calculation"
```

El archivo JSON actualizado en Git documenta exactamente qué cambió.
