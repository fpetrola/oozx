package com.fpetrola.oozx.specializer;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import static net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default.IMITATE_SUPER_CLASS_PUBLIC;

public class Devirtualizer {

    /**
     * Crea una versión "especializada" de la instancia pasada.
     * La nueva instancia es de una subclase final donde todos los métodos virtuales
     * delegan directamente al método concreto del tipo real (usando invokespecial).
     * Esto ayuda enormemente al JIT a devirtualizar e inlinear.
     *
     * @param original instancia original (no null)
     * @return nueva instancia especializada con el mismo estado
     */
    public static <T> T specialize(T original) {
        if (original == null) throw new IllegalArgumentException("Instance cannot be null");

        Class<?> concreteClass = original.getClass();

        // Crear subclase final que imita los constructores públicos del tipo concreto
        Class<? extends T> specializedClass = (Class<? extends T>) new ByteBuddy()
                .subclass(concreteClass, IMITATE_SUPER_CLASS_PUBLIC)
                .modifiers(Modifier.PUBLIC | Modifier.FINAL) // clase final
                .method(ElementMatchers.any().and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class))))                   // todos los métodos
                .intercept(MethodDelegation.to(DirectDelegator.class))
                .make()
                .load(concreteClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        try {
            // Instanciar usando un constructor que coincida con el original (imitado)
            // Si el tipo concreto tiene constructor público sin params, usa ese.
            // Si no, Byte Buddy crea uno que llama al super con los mismos params.
            T specialized = specializedClass.getDeclaredConstructor().newInstance();

            // Copiar todos los campos (incluyendo privados) del original al nuevo
            copyFields(original, specialized);

            return specialized;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create specialized instance", e);
        }
    }

    /**
     * Copia el estado (todos los campos) de source a target vía reflexión.
     */
    private static <T> void copyFields(T source, T target) throws IllegalAccessException {
        Class<?> clazz = source.getClass();
        while (clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    field.set(target, field.get(source));
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Interceptor que delega directamente al método concreto del supertipo.
     */
    public static class DirectDelegator {

        @RuntimeType
        public static Object intercept(@SuperMethod(nullIfImpossible = true) java.lang.reflect.Method superMethod,
                                       @AllArguments Object[] args, @This Object _this) throws Throwable {
            if (superMethod == null) {
                // Si no hay super method (ej. método abstract implementado), lanzar excepción
                throw new AbstractMethodError("No super method found");
            }
            // Llama directamente al método concreto del supertipo
            return superMethod.invoke(_this, args); // null receiver porque @SuperMethod ya bindea el this
            // En realidad Byte Buddy maneja el receiver internamente con invokespecial
        }
    }
}