// src/main/java/agent/IndyClassLoader.java
package com.fpetrola.oozx.indy2;

import java.io.*;

public class IndyClassLoader extends ClassLoader {

    private final IndyVirtualTransformer transformer = new IndyVirtualTransformer();
    private final ClassLoader parent;

    public IndyClassLoader(ClassLoader parent) {
        this.parent = parent;
    }

    public IndyClassLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Primero intentamos con el parent (para clases del sistema)
        try {
            return parent.loadClass(name);
        } catch (ClassNotFoundException e) {
            // No está en parent, buscamos .class en classpath
        }

        String resourcePath = name.replace('.', '/') + ".class";
        try (InputStream is = parent.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new ClassNotFoundException("No se encontró " + resourcePath);
            }

            byte[] bytes = is.readAllBytes();
            byte[] transformed = transformer.transform(name, bytes);

            return defineClass(name, transformed, 0, transformed.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("Error leyendo clase " + name, e);
        }
    }
}