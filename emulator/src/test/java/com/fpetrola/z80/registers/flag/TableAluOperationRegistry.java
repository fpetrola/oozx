package com.fpetrola.z80.registers.flag;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.beans.Transient;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/** Loads every TableAluOperation named in a JSON config, so all of them can be iterated and
 *  tested without hardcoding each one. */
public class TableAluOperationRegistry {

  public static class OperationConfig {
    public String name;
    public String description;
    public String md5;

    // Resolved via reflection, not read from the config file
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

  public static List<OperationConfig> getOperations() throws IOException {
    if (loadedConfigs == null) {
      loadedConfigs = loadFromJson("src/test/resources/table_alu_operations_config.json");
    }
    return loadedConfigs;
  }

  private static final String BASE_PACKAGE = "com.fpetrola.z80.instructions.impl";

  private static List<OperationConfig> loadFromJson(String path) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = new String(Files.readAllBytes(Paths.get(path)));
    ConfigRoot root = mapper.readValue(json, ConfigRoot.class);

    for (OperationConfig config : root.operations) {
      resolveClassAndFieldByReflection(config);
    }

    return root.operations;
  }

  private static void resolveClassAndFieldByReflection(OperationConfig config) throws IOException {
    String className = BASE_PACKAGE + "." + config.name;
    config.setClassName(className);

    try {
      Class<?> clazz = Class.forName(className);

      java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
      java.lang.reflect.Field tableAluField = null;

      for (java.lang.reflect.Field field : fields) {
        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
          try {
            field.setAccessible(true);
            Object fieldValue = field.get(null);
            if (fieldValue instanceof AluOperation) {
              // Prefer a field whose name matches the operation's name.
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
            // Field not accessible: skip it.
          }
        }
      }

      if (tableAluField != null) {
        config.setFieldName(tableAluField.getName());
      } else {
        // Try to find inner class that extends AluOperation
        Class<?>[] innerClasses = clazz.getDeclaredClasses();
        for (Class<?> innerClass : innerClasses) {
          if (AluOperation.class.isAssignableFrom(innerClass) &&
              java.lang.reflect.Modifier.isStatic(innerClass.getModifiers())) {
            // Found an inner class that extends AluOperation
            // We need to instantiate it to treat it as a field
            config.setFieldName(innerClass.getSimpleName());
            break;
          }
        }

        if (config.getFieldName() == null) {
          throw new IOException("No TableAluOperation field or inner class found in class " + className);
        }
      }
    } catch (ClassNotFoundException e) {
      throw new IOException("Class not found: " + className, e);
    }
  }

  /** Resolves a config entry's operation via reflection: a matching static field, or an inner
   *  class instantiated in its place. */
  public static AluOperation getOperation(OperationConfig config) throws Exception {
    try {
      Class<?> clazz = Class.forName(config.getClassName());
      
      // Try to get it as a static field first
      try {
        java.lang.reflect.Field field = clazz.getDeclaredField(config.getFieldName());
        field.setAccessible(true);
        return (AluOperation) field.get(null);
      } catch (NoSuchFieldException e) {
        // Field not found, try as inner class
        Class<?>[] innerClasses = clazz.getDeclaredClasses();
        for (Class<?> innerClass : innerClasses) {
          if (innerClass.getSimpleName().equals(config.getFieldName()) &&
              AluOperation.class.isAssignableFrom(innerClass)) {
            // Instantiate the inner class
            return (AluOperation) innerClass.getDeclaredConstructor().newInstance();
          }
        }
        throw new NoSuchFieldException("Could not find field or inner class: " + config.getFieldName());
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to load operation " + config + ": " + e.getMessage(), e);
    }
  }

  /** Returns a CachedTableAluOperation's precomputed table, or generates one on demand. */
  public static int[] getTable(AluOperation operation) {
    try {
      Class<?> cachedClass = Class.forName("com.fpetrola.z80.registers.flag.CachedTableAluOperation");
      if (cachedClass.isInstance(operation)) {
        Field tableField = cachedClass.getDeclaredField("table");
        tableField.setAccessible(true);
        return (int[]) tableField.get(operation);
      }
    } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
      // Not a cached operation: fall through to generating the table.
    }

    return generateTableForOperation(operation);
  }

  private static int[] generateTableForOperation(AluOperation operation) {
    int tableSize = 256 * 256 * 2; // Default size for 2-value operations
    int[] table = new int[tableSize];
    
    for (int a = 0; a < 256; a++) {
      for (int b = 0; b < 256; b++) {
        for (int c = 0; c < 2; c++) {
          operation.F = b;
          int result = operation.calculate2Values1Boolean(a, b, c);
          if (result != -1) {
            table[((a & 0xff)) | (b << 8) | (c << 16)] = ((result & 0xff) << 8) + operation.F;
          } else {
            // Try other calculation methods
            operation.F = b;
            result = operation.calculate1Value(a);
            if (result != -1) {
              table[((a & 0xff)) | (b << 8) | (c << 16)] = ((result & 0xff) << 8) + operation.F;
            } else {
              // Try 3-value calculation
              tableSize = 256 * 256 * 256;
              if (table.length < tableSize) {
                int[] newTable = new int[tableSize];
                System.arraycopy(table, 0, newTable, 0, table.length);
                table = newTable;
              }
              operation.F = b;
              result = operation.calculate3Values(a, b, c);
              if (result != -1) {
                table[((a & 0xff)) | (b << 8) | (c << 16)] = ((result & 0xff) << 8) + operation.F;
              }
            }
          }
        }
      }
    }
    
    return table;
  }



  /** Calculates and updates the MD5 of every operation in the config JSON. */
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

    saveConfigJson(configs);

    System.out.println(
        "\n✓ Updated " + successCount + " operations" + (failCount > 0 ? ", " + failCount + " failed" : ""));
    System.out.println("  Config saved to: src/test/resources/table_alu_operations_config.json");
  }

  private static void saveConfigJson(List<OperationConfig> configs) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ConfigRoot root = new ConfigRoot();
    root.operations = configs;

    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    Files.write(Paths.get("src/test/resources/table_alu_operations_config.json"), json.getBytes());
  }

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
