package com.fpetrola.z80.bytecode.tests.aa;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.fasterxml.jackson.databind.*;

public class RegisterAccessConverter {

    public static void main(String[] args) throws Exception {
        String jsonPath = "field-dependency-analysis.json";
        String inputFile = "translator/src/main/java/com/fpetrola/z80/bytecode/tests/JetSetWilly2.java";
        String outputFile = "translator/src/main/java/com/fpetrola/z80/bytecode/tests/JetSetWilly2Converted.java";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(jsonPath));

        String content = new String(Files.readAllBytes(Paths.get(inputFile)));
        String[] lines = content.split("\n");
        
        Map<Integer, String> conversions = new HashMap<>();
        JsonNode convReq = root.get("conversionRequirements");

        // Build conversions
        convReq.fields().forEachRemaining(entry -> {
            String convType = entry.getKey();
            entry.getValue().forEach(pcNode -> {
                int pc = pcNode.asInt();
                conversions.put(pc, convType);
            });
        });

        System.out.println("Total conversions to apply: " + conversions.size());
        
        // Find lines with these PCs and apply conversions
        Pattern pcPattern = Pattern.compile("pc\\((\\d+),");
        int applied = 0;
        List<String> examples = new ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            Matcher m = pcPattern.matcher(lines[i]);
            if (m.find()) {
                int pc = Integer.parseInt(m.group(1));
                if (conversions.containsKey(pc)) {
                    String convType = conversions.get(pc);
                    String register = convType.substring(convType.indexOf(":") + 1);
                    
                    String before = lines[i];
                    if (convType.startsWith("16bit_to_8bit:")) {
                        applyConversion(lines, i, register, "_16");
                    } else if (convType.startsWith("8bit_to_16bit:")) {
                        applyConversion(lines, i, register, "_8");
                    }
                    applied++;
                    
                    if (examples.size() < 5) {
                        examples.add("PC:" + pc + " " + convType + " line:" + (i+1));
                        if (i + 1 < lines.length) {
                            examples.add("  Was: " + before.trim());
                            examples.add("  Now: " + lines[i].trim());
                        }
                    }
                }
            }
        }

        String output = String.join("\n", lines);
        Files.write(Paths.get(outputFile), output.getBytes());
        System.out.println("Conversions applied: " + applied);
        System.out.println("\nExamples:");
        examples.forEach(System.out::println);
    }

    private static void applyConversion(String[] lines, int pcLine, String register, String suffix) {
        Pattern regPattern = Pattern.compile("(?<![_a-zA-Z])" + Pattern.quote(register) + "\\(");
        
        // Apply to current line and next few lines until another pc() call
        Pattern pcPattern = Pattern.compile("pc\\(\\d+,");
        
        for (int i = pcLine; i < lines.length && i < pcLine + 10; i++) {
            if (i > pcLine && pcPattern.matcher(lines[i]).find()) {
                break;
            }
            lines[i] = regPattern.matcher(lines[i]).replaceAll(register + suffix + "(");
        }
    }
}
