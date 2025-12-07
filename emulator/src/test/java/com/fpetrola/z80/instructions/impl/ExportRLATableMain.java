package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.registers.flag.TableAluOperationExporter;
import java.io.IOException;

/**
 * Programa para exportar la tabla de RLA a JSON.
 * 
 * Uso: java -cp ... ExportRLATableMain
 * 
 * Esto genera rla_table_export.json con todos los valores de la tabla precalculada.
 */
public class ExportRLATableMain {
  public static void main(String[] args) throws IOException {
    System.out.println("Exporting RLA table...");

    String outputPath = "emulator/src/test/resources/rla_table_export.json";
    TableAluOperationExporter.exportToJson(RLA.rlaTableAluOperation, "RLA", outputPath);
  }
}
