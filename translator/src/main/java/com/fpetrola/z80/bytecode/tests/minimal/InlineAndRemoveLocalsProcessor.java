package com.fpetrola.z80.bytecode.tests.minimal;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.processing.AbstractProcessor;

import java.util.List;

public class InlineAndRemoveLocalsProcessor extends AbstractProcessor<CtLocalVariable<?>> {

    @Override
    public void process(CtLocalVariable<?> localVar) {
        // Ignoramos variables sin inicializador o con modificadores raros
        if (localVar.getDefaultExpression() == null) {
            return;
        }

        CtLocalVariableReference<?> ref = localVar.getReference();

        // Obtenemos todos los accesos a esta variable en el scope donde está definida
        List<CtVariableAccess> accesses = localVar.getParent(CtExecutable.class)
                .getElements(new TypeFilter<>(CtVariableAccess.class))
                .stream()
                .filter(access -> access.getVariable().equals(ref))
                .toList();

        // Caso 1: No se usa → eliminar declaración
        if (accesses.isEmpty()) {
            localVar.delete();
            return;
        }

        // Caso 2: Se usa exactamente una vez → intentar inline
        if (accesses.size() != 1) {
            return;
        }

        CtVariableAccess<?> singleAccess = accesses.get(0);

        // Solo inlineamos si es lectura (no asignación)
        if (!(singleAccess instanceof CtVariableRead)) {
            return;
        }

        CtExpression<?> initializer = localVar.getDefaultExpression();

        // Filtro conservador: solo inlineamos inicializadores "simples"
        if (!isSafeToInline(initializer)) {
            return;
        }

        // Reemplazamos el acceso por el inicializador
        singleAccess.replace(initializer.clone());

        // Eliminamos la declaración de la variable
        localVar.delete();
    }

    /**
     * Decide si es seguro/trivial inlinear esta expresión
     */
    private boolean isSafeToInline(CtExpression<?> expr) {
        if (expr == null) return false;

        // Literales, this, super, variables, invocaciones simples, new sin argumentos
        return expr instanceof CtLiteral ||
               expr instanceof CtThisAccess ||
               expr instanceof CtVariableAccess ||
               expr instanceof CtFieldAccess ||
               (expr instanceof CtNewClass && ((CtNewClass<?>) expr).getAnonymousClass() == null) ||
               (expr instanceof CtConstructorCall && ((CtConstructorCall<?>) expr).getArguments().isEmpty()) ||
               (expr instanceof CtInvocation && isSimpleInvocation((CtInvocation<?>) expr));
    }

    private boolean isSimpleInvocation(CtInvocation<?> inv) {
        // Podrías hacer más restrictivo: solo métodos sin side-effects conocidos
        // Por ahora aceptamos casi todo (puedes agregar blacklist de métodos)
        return true; // Conservador: true → inlinea más
        // return inv.getExecutable().getSimpleName().startsWith("get") || ...;
    }

    // ------------------------------------------------------
    // Uso principal (puedes ponerlo en main o en un test)
    // ------------------------------------------------------
    public static void main(String[] args) {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setCommentEnabled(true);           // mantiene comentarios
        launcher.getEnvironment().setAutoImports(true);               // agrega imports si hace falta
        launcher.addInputResource("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/minimal/JetSetWilly2Converted.java");                    // o la carpeta/ruta de tu proyecto

        // Opcional: solo procesar una clase de prueba
        // launcher.addInputResource("src/main/java/com/example/MyClass.java");

        launcher.buildModel();
        CtModel model = launcher.getModel();

        // Ejecuta el processor
        model.getRootPackage()
             .filterChildren(CtLocalVariable.class::isInstance)
             .forEach(localVar -> new InlineAndRemoveLocalsProcessor().process((CtLocalVariable<?>) localVar));

        // Guarda los cambios (sobrescribe los .java originales)
        launcher.prettyprint();

        System.out.println("Procesamiento finalizado. Archivos modificados.");
    }
}