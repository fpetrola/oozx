// src/main/java/agent/OfflineTransformer.java
package com.fpetrola.oozx.indy2;

import org.objectweb.asm.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.*;

public class OfflineTransformer {

  private static final String BOOTSTRAP_CLASS = "com/fpetrola/oozx/indy2/IndyBootstrap";
  private static final Handle BOOTSTRAP_HANDLE = new Handle(
      Opcodes.H_INVOKESTATIC,
      BOOTSTRAP_CLASS,
      "bootstrapVirtual",
      "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/invoke/CallSite;",
      false);

  public static void main(String[] args) throws IOException {
    args = new String[]{"/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/target/classes/com/fpetrola/oozx/indy2/IndyClassLoaderTest.class", "/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator"};
    if (args.length < 1 || args.length > 2) {
      System.out.println("Uso: java OfflineTransformer <input> [output]");
      System.out.println("  input:  archivo .class, directorio o JAR");
      System.out.println("  output: directorio destino (opcional, si no se da sobrescribe en lugar)");
      return;
    }

    Path input = Paths.get(args[0]);
    Path output = args.length == 2 ? Paths.get(args[1]) : null;

    if (Files.isDirectory(input)) {
      transformDirectory(input, output != null ? output : input);
    } else if (input.toString().endsWith(".jar")) {
      transformJar(input, output != null ? output : input.getParent());
    } else if (input.toString().endsWith(".class")) {
      transformSingleClass(input, output != null ? output.resolve(input.getFileName()) : input);
    } else {
      System.err.println("Entrada no soportada: debe ser .class, directorio o .jar");
    }
  }

  private static void transformDirectory(Path sourceDir, Path targetDir) throws IOException {
    Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.toString().endsWith(".class")) {
          Path relative = sourceDir.relativize(file);
          Path target = targetDir.resolve(relative);
          Files.createDirectories(target.getParent());
          transformSingleClass(file, target);
        } else {
          // Copiar recursos no .class
          Path target = targetDir.resolve(sourceDir.relativize(file));
          Files.createDirectories(target.getParent());
          Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    System.out.println("Directorio transformado: " + sourceDir + " → " + targetDir);
  }

  private static void transformJar(Path jarPath, Path outputDir) throws IOException {
    String jarName = jarPath.getFileName().toString();
    Path outputJar = outputDir.resolve(jarName.replace(".jar", "-indy.jar"));

    try (JarInputStream jis = new JarInputStream(Files.newInputStream(jarPath));
         JarOutputStream jos = new JarOutputStream(Files.newOutputStream(outputJar))) {

      JarEntry entry;
      byte[] buffer = new byte[8192];

      while ((entry = jis.getNextJarEntry()) != null) {
        if (entry.isDirectory()) {
          jos.putNextEntry(new JarEntry(entry));
          continue;
        }

        byte[] originalBytes = jis.readAllBytes();
        byte[] transformedBytes = originalBytes;

        if (entry.getName().endsWith(".class") && !entry.getName().startsWith("agent/")) {
          transformedBytes = transformClassBytes(originalBytes);
          System.out.println("Transformado: " + entry.getName());
        }

        JarEntry newEntry = new JarEntry(entry.getName());
        jos.putNextEntry(newEntry);
        jos.write(transformedBytes);
      }
    }
    System.out.println("JAR transformado: " + outputJar);
  }

  private static void transformSingleClass(Path input, Path output) throws IOException {
    byte[] bytes = Files.readAllBytes(input);
    byte[] transformed = transformClassBytes(bytes);
    Files.write(output, transformed, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    System.out.println("Transformado: " + input + " → " + output);
  }

  private static byte[] transformClassBytes(byte[] original) {
    try {
      ClassReader reader = new ClassReader(original);
      ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

      ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
          MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
          // Usar rastreador de tipos concretos del receiver
          return new TypeTracker(Opcodes.ASM9, mv);
        }
      };

      reader.accept(cv, ClassReader.EXPAND_FRAMES);
      return writer.toByteArray();
    } catch (Exception e) {
      System.err.println("Error transformando clase: " + e.getMessage());
      return original; // Devuelve original si falla
    }
  }

  /**
   * Rastreador de tipos concretos en el stack para devirtualizar invokevirtual.
   * Determina el tipo real del receiver y lo pasa como parámetro bootstrap.
   */
  private static class TypeTracker extends MethodVisitor {
    private final Map<Integer, String> localVarTypes = new HashMap<>();
    private final Deque<String> stack = new ArrayDeque<>();

    public TypeTracker(int api, MethodVisitor mv) {
      super(api, mv);
    }

    @Override
    public void visitCode() {
      super.visitCode();
      // Local 0 es siempre 'this'
      localVarTypes.put(0, "java/lang/Object");
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
      super.visitVarInsn(opcode, var);

      switch (opcode) {
        case Opcodes.ALOAD:
          String varType = localVarTypes.getOrDefault(var, "java/lang/Object");
          stack.push(varType);
          break;
        case Opcodes.ASTORE:
          if (!stack.isEmpty()) {
            String type = stack.pop();
            localVarTypes.put(var, type);
          }
          break;
      }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDesc) {
      super.visitFieldInsn(opcode, owner, fieldName, fieldDesc);

      switch (opcode) {
        case Opcodes.GETFIELD:
          if (!stack.isEmpty()) stack.pop();
          String fieldType = extractClassName(fieldDesc);
          stack.push(fieldType);
          break;
        case Opcodes.PUTFIELD:
          if (!stack.isEmpty()) stack.pop();
          if (!stack.isEmpty()) stack.pop();
          break;
        case Opcodes.GETSTATIC:
          stack.push(extractClassName(fieldDesc));
          break;
        case Opcodes.PUTSTATIC:
          if (!stack.isEmpty()) stack.pop();
          break;
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
                                String descriptor, boolean isInterface) {
      if (opcode == Opcodes.INVOKEVIRTUAL && !isInterface && !owner.startsWith("agent/")) {
        // Obtener tipo concreto del receiver desde el stack
        String concreteReceiver = "java/lang/Object";
        if (!stack.isEmpty()) {
          String potentialType = stack.peek();
          if (potentialType != null && !potentialType.equals("java/lang/Object")) {
            concreteReceiver = potentialType;
          }
        }

        // Transformar a invokedynamic con el tipo concreto
        mv.visitInvokeDynamicInsn(
            name,
            descriptor,
            BOOTSTRAP_HANDLE,
            concreteReceiver, name, descriptor
        );

        // Limpiar stack
        Type[] args = Type.getArgumentTypes(descriptor);
        if (!stack.isEmpty()) stack.pop(); // receiver
        for (Type arg : args) {
          if (!stack.isEmpty()) stack.pop();
        }

        // Pushear return type
        Type retType = Type.getReturnType(descriptor);
        if (retType != Type.VOID_TYPE) {
          stack.push(retType.getInternalName());
        }
      } else {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

        // Actualizar stack también para métodos no transformados
        Type[] args = Type.getArgumentTypes(descriptor);
        if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) {
          if (!stack.isEmpty()) stack.pop(); // receiver
        }
        for (Type arg : args) {
          if (!stack.isEmpty()) stack.pop();
        }
        Type retType = Type.getReturnType(descriptor);
        if (retType != Type.VOID_TYPE) {
          stack.push(retType.getInternalName());
        }
      }
    }

    private String extractClassName(String desc) {
      // "Ljava/lang/String;" -> "java/lang/String"
      if (desc.startsWith("L") && desc.endsWith(";")) {
        return desc.substring(1, desc.length() - 1);
      }
      return "java/lang/Object";
    }
  }
}