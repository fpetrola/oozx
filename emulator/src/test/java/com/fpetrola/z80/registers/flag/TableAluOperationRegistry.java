package com.fpetrola.z80.registers.flag;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.beans.Transient;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Registry que carga todas las operaciones TableAluOperation desde JSON de configuración.
 * Permite iterar y probar todas las operaciones sin hardcodear cada una.
 */
public class TableAluOperationRegistry {

  public static class OperationConfig {
    public String name;
    public String description;
    public String md5;

    // Campos que se resuelven por reflection
    private transient String className;
    private transient String fieldName;

    public OperationConfig() {
    }

    public OperationConfig(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public OperationConfig(String name, String description, String md5) {
      this.name = name;
      this.description = description;
      this.md5 = md5;
    }

    @Transient
    public String getClassName() {
      return className;
    }

    @Transient
    public void setClassName(String className) {
      this.className = className;
    }

    @Transient
    public String getFieldName() {
      return fieldName;
    }

    @Transient
    public void setFieldName(String fieldName) {
      this.fieldName = fieldName;
    }

    @Override
    public String toString() {
      return String.format("%s (%s.%s)", name, className, fieldName);
    }
  }

  public static class ConfigRoot {
    public List<OperationConfig> operations;

    public ConfigRoot() {
      this.operations = new ArrayList<>();
    }
  }

  private static List<OperationConfig> loadedConfigs = null;

  /**
   * Carga la configuración desde JSON.
   */
  public static List<OperationConfig> getOperations() throws IOException {
    if (loadedConfigs == null) {
      loadedConfigs = loadFromJson("src/test/resources/table_alu_operations_config.json");
    }
    return loadedConfigs;
  }

  private static final String BASE_PACKAGE = "com.fpetrola.z80.instructions.impl";

  /**
   * Carga operaciones desde archivo JSON.
   */
  private static List<OperationConfig> loadFromJson(String path) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = new String(Files.readAllBytes(Paths.get(path)));
    ConfigRoot root = mapper.readValue(json, ConfigRoot.class);

    // Resolver className y fieldName por reflection
    for (OperationConfig config : root.operations) {
      resolveClassAndFieldByReflection(config);
    }

    return root.operations;
  }

  /**
   * Resuelve el className y fieldName por reflection basado en el nombre de la operación.
   */
  private static void resolveClassAndFieldByReflection(OperationConfig config) throws IOException {
    String className = BASE_PACKAGE + "." + config.name;
    config.setClassName(className);

    try {
      Class<?> clazz = Class.forName(className);

      // Buscar campos static de tipo TableAluOperation o AluOperation que sea TableAluOperation
      java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
      java.lang.reflect.Field tableAluField = null;

      for (java.lang.reflect.Field field : fields) {
        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
          // Verificar si es TableAluOperation (puede ser directo o anónimo)
          try {
            field.setAccessible(true);
            Object fieldValue = field.get(null);
            if (fieldValue instanceof AluOperation) {
              // Prefiere campos que coincidan con el patrón de nombre
              String fieldNameLower = field.getName().toLowerCase();
              String nameLower = config.name.toLowerCase();

              if (fieldNameLower.contains(nameLower) || fieldNameLower.contains("table")) {
                tableAluField = field;
                break;
              } else if (tableAluField == null) {
                tableAluField = field;
              }
            }
          } catch (Exception e) {
            // Ignorar si no se puede acceder
          }
        }
      }

      if (tableAluField != null) {
        config.setFieldName(tableAluField.getName());
      } else {
        throw new IOException("No TableAluOperation field found in class " + className);
      }
    } catch (ClassNotFoundException e) {
      throw new IOException("Class not found: " + className, e);
    }
  }

  /**
   * Obtiene una operación TableAluOperation por nombre de configuración.
   * Usa reflexión para acceder al campo estático.
   */
  public static AluOperation getOperation(OperationConfig config) throws Exception {
    try {
      Class<?> clazz = Class.forName(config.getClassName());
      java.lang.reflect.Field field = clazz.getDeclaredField(config.getFieldName());
      field.setAccessible(true);
      return (AluOperation) field.get(null);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to load operation " + config + ": " + e.getMessage(), e);
    }
  }

  /**
   * Obtiene la tabla int[] de una TableAluOperation.
   */
  public static int[] getTable(AluOperation operation) {
    try {
      Field tableField = getTableField(operation);
      tableField.setAccessible(true);
      return (int[]) tableField.get(operation);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static Field getTableField(AluOperation operation) {
    try {
      Field tableField = CachedTableAluOperation.class.getDeclaredField("table");
      return tableField;
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Calcula y actualiza los MD5 de todas las operaciones en el config JSON.
   */
  public static void updateAllMd5Hashes() throws Exception {
    List<OperationConfig> configs = getOperations();
    int successCount = 0;
    int failCount = 0;

    System.out.println("Calculating MD5 hashes for " + configs.size() + " table ALU operations...\n");

    for (OperationConfig config : configs) {
      try {
        AluOperation operation = getOperation(config);
        int[] table = getTable(operation);
        config.md5 = TableAluOperationExporter.calculateTableMd5(table);
        System.out.println("✓ " + config.name + ": " + config.md5);
        successCount++;
      } catch (Exception e) {
        System.err.println("✗ Failed to calculate MD5 for " + config.name + ": " + e.getMessage());
        failCount++;
      }
    }

    // Guardar el config actualizado
    saveConfigJson(configs);

    System.out.println(
        "\n✓ Updated " + successCount + " operations" + (failCount > 0 ? ", " + failCount + " failed" : ""));
    System.out.println("  Config saved to: src/test/resources/table_alu_operations_config.json");
  }

  /**
   * Guarda la configuración actualizada al JSON.
   */
  private static void saveConfigJson(List<OperationConfig> configs) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ConfigRoot root = new ConfigRoot();
    root.operations = configs;

    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    Files.write(Paths.get("src/test/resources/table_alu_operations_config.json"), json.getBytes());
  }

  /**
   * Exporta las tablas a JSON (deprecated, usar binario).
   */
  public static void exportAllOperations(String outputDirectory) throws Exception {
    System.out.println(
        "Warning: JSON export is deprecated due to large file sizes. Use exportAllOperationsBinary() instead.");
    Files.createDirectories(Paths.get(outputDirectory));

    List<OperationConfig> configs = getOperations();
    int successCount = 0;
    int failCount = 0;

    System.out.println("Exporting " + configs.size() + " table ALU operations...");

    for (OperationConfig config : configs) {
      try {
        AluOperation operation = getOperation(config);
        String outputPath = outputDirectory + "/" + config.name.toLowerCase() + "_table.json";
        TableAluOperationExporter.exportToJson(operation, config.name, outputPath);
        successCount++;
      } catch (Exception e) {
        System.err.println("✗ Failed to export " + config.name + ": " + e.getMessage());
        failCount++;
      }
    }

    System.out.println(
        "✓ Exported " + successCount + " operations" + (failCount > 0 ? ", " + failCount + " failed" : ""));
  }
}
