package com.fpetrola.z80.registers.flag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
    public String className;
    public String fieldName;
    public String description;

    public OperationConfig() {}

    public OperationConfig(String name, String className, String fieldName, String description) {
      this.name = name;
      this.className = className;
      this.fieldName = fieldName;
      this.description = description;
    }

    @Override
    public String toString() {
      return String.format("%s (%s.%s)", name, className, fieldName);
    }
  }

  static class ConfigRoot {
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

  /**
   * Carga operaciones desde archivo JSON.
   */
  private static List<OperationConfig> loadFromJson(String path) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = new String(Files.readAllBytes(Paths.get(path)));
    ConfigRoot root = mapper.readValue(json, ConfigRoot.class);
    return root.operations;
  }

  /**
   * Obtiene una operación TableAluOperation por nombre de configuración.
   * Usa reflexión para acceder al campo estático.
   */
  public static TableAluOperation getOperation(OperationConfig config) throws Exception {
    try {
      Class<?> clazz = Class.forName(config.className);
      java.lang.reflect.Field field = clazz.getDeclaredField(config.fieldName);
      field.setAccessible(true);
      return (TableAluOperation) field.get(null);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to load operation " + config + ": " + e.getMessage(), e);
    }
  }

  /**
   * Obtiene la tabla int[] de una TableAluOperation.
   */
  public static int[] getTable(TableAluOperation operation) throws NoSuchFieldException,
      IllegalAccessException {
    java.lang.reflect.Field tableField = TableAluOperation.class.getDeclaredField("table");
    tableField.setAccessible(true);
    return (int[]) tableField.get(operation);
  }

  /**
   * Exporta las tablas de todas las operaciones a archivos JSON individuales.
   */
  public static void exportAllOperations(String outputDirectory) throws Exception {
    Files.createDirectories(Paths.get(outputDirectory));

    List<OperationConfig> configs = getOperations();
    int successCount = 0;
    int failCount = 0;

    System.out.println("Exporting " + configs.size() + " table ALU operations...");

    for (OperationConfig config : configs) {
      try {
        TableAluOperation operation = getOperation(config);
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
