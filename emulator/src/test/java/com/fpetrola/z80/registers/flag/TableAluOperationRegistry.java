package com.fpetrola.z80.registers.flag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
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
    public String md5;

    public OperationConfig() {}

    public OperationConfig(String name, String className, String fieldName, String description) {
      this.name = name;
      this.className = className;
      this.fieldName = fieldName;
      this.description = description;
    }

    public OperationConfig(String name, String className, String fieldName, String description, String md5) {
      this.name = name;
      this.className = className;
      this.fieldName = fieldName;
      this.description = description;
      this.md5 = md5;
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
   * Calcula y actualiza los MD5 de todas las operaciones en el config JSON.
   */
  public static void updateAllMd5Hashes() throws Exception {
    List<OperationConfig> configs = getOperations();
    int successCount = 0;
    int failCount = 0;

    System.out.println("Calculating MD5 hashes for " + configs.size() + " table ALU operations...\n");

    for (OperationConfig config : configs) {
      try {
        TableAluOperation operation = getOperation(config);
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
