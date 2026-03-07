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

public class InlineAndRemoveLocalsProcessorMejorado extends AbstractProcessor<CtLocalVariable<?>> {

    @Override
    public void process(CtLocalVariable<?> localVar) {
        if (localVar.getDefaultExpression() == null) {
            return;
        }

        CtLocalVariableReference<?> ref = localVar.getReference();

        // Buscamos TODOS los accesos en el método (scope conservador)
        CtExecutable<?> enclosingExec = localVar.getParent(CtExecutable.class);
        if (enclosingExec == null) return;

        List<CtVariableAccess> accesses = enclosingExec.getElements(new TypeFilter<>(CtVariableAccess.class))
                .stream()
                .filter(access -> access.getVariable().equals(ref))
                .toList();

        // Caso 1: cero usos → eliminar
        if (accesses.isEmpty()) {
            localVar.delete();
            return;
        }

        // Caso 2: exactamente un uso
        if (accesses.size() != 1) {
            return;
        }

        CtVariableAccess<?> access = accesses.get(0);

        CtExpression<?> initializer = localVar.getDefaultExpression();

        // Solo inlineamos inicializadores que consideramos "seguros"
        if (!isSafeToInline(initializer)) {
            return;
        }

        // Patrón 1: lectura normal (System.out.println(var), return var, etc.)
        if (access instanceof CtVariableRead) {
            access.replace(initializer.clone());
            localVar.delete();
            return;
        }

        // Patrón 2: el único uso es en el RHS de una asignación simple (A = var;)
        if (access.getParent() instanceof CtAssignment) {
            CtAssignment<?, ?> assignment = (CtAssignment<?, ?>) access.getParent();

            // Verificamos que el lado izquierdo no sea la misma variable (evitar A = A)
            if (assignment.getAssigned() instanceof CtVariableAccess &&
                ((CtVariableAccess<?>) assignment.getAssigned()).getVariable().equals(ref)) {
                return;
            }

            // Reemplazamos el acceso (RHS) por el inicializador
            access.replace(initializer.clone());

            // Eliminamos la declaración
            localVar.delete();

            // Bonus opcional: si el inicializador es algo como x ^ x → podemos reemplazar por 0
            if (initializer instanceof CtBinaryOperator &&
                ((CtBinaryOperator<?>) initializer).getKind() == BinaryOperatorKind.BITXOR) {
                CtBinaryOperator<?> binOp = (CtBinaryOperator<?>) initializer;
                if (binOp.getLeftHandOperand().equals(binOp.getRightHandOperand())) {
                    // Simplificación agresiva: x ^ x → 0
                    CtLiteral<Integer> zero = localVar.getFactory().createLiteral(0);
                    assignment.getAssignment().replace(zero);
                }
            }
            return;
        }

        // Otros casos (por ahora no inlineamos)
    }

    private boolean isSafeToInline(CtExpression<?> expr) {
        if (expr == null) return false;

        return expr instanceof CtLiteral ||
               expr instanceof CtThisAccess ||
               expr instanceof CtVariableAccess ||
               expr instanceof CtFieldAccess ||
               (expr instanceof CtNewClass && ((CtNewClass<?>) expr).getAnonymousClass() == null) ||
               (expr instanceof CtConstructorCall && ((CtConstructorCall<?>) expr).getArguments().isEmpty()) ||
               (expr instanceof CtInvocation && isSimpleInvocation((CtInvocation<?>) expr)) ||
               expr instanceof CtBinaryOperator;  // ← agregamos binarios simples (como ^)
    }

    private boolean isSimpleInvocation(CtInvocation<?> inv) {
        // Podrías restringir más (ej: solo getters, métodos puros)
        return true;
    }

    // ------------------------------------------------------
    // Uso (main)
    // ------------------------------------------------------
    public static void main(String[] args) {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.getEnvironment().setAutoImports(true);
        launcher.addInputResource("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/minimal/JetSetWilly2Converted.java");  // ajusta a tu carpeta

        launcher.buildModel();
        CtModel model = launcher.getModel();

        model.getRootPackage()
             .filterChildren(CtLocalVariable.class::isInstance)
             .forEach(var -> new InlineAndRemoveLocalsProcessorMejorado().process((CtLocalVariable<?>) var));

        launcher.prettyprint();

        System.out.println("Inline y limpieza finalizados.");
    }
}