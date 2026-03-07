package com.fpetrola.z80.bytecode.tests.minimal;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.processing.AbstractProcessor;

import java.util.List;

public class InlineAndRemoveLocalsProcessorV3 extends AbstractProcessor<CtLocalVariable<?>> {

    @Override
    public void process(CtLocalVariable<?> localVar) {
        if (localVar.getDefaultExpression() == null) return;

        CtLocalVariableReference<?> ref = localVar.getReference();
        CtExecutable<?> method = localVar.getParent(CtExecutable.class);
        if (method == null) return;

        List<CtVariableAccess> accesses = method.getElements(new TypeFilter<>(CtVariableAccess.class))
                .stream()
                .filter(acc -> acc.getVariable().equals(ref))
                .toList();

        if (accesses.isEmpty()) {
            localVar.delete();           // no usada
            return;
        }
        if (accesses.size() != 1) return; // se usa más de una vez → no tocamos

        CtVariableAccess<?> access = accesses.get(0);
        CtExpression<?> init = localVar.getDefaultExpression();

        if (!isSafeToInline(init)) return;

        // === PATRÓN 1: uso normal como lectura ===
        if (access instanceof CtVariableRead) {
            access.replace(init.clone());
            localVar.delete();
            return;
        }

        // === PATRÓN 2: único uso en lado derecho de asignación (el caso que fallaba) ===
        if (access.getParent(CtAssignment.class) != null) {
            CtAssignment<?, ?> assignment = access.getParent(CtAssignment.class);

            // evitamos A = A
            if (assignment.getAssigned() instanceof CtVariableAccess &&
                ((CtVariableAccess<?>) assignment.getAssigned()).getVariable().equals(ref)) {
                return;
            }

            access.replace(init.clone());
            localVar.delete();

            // Bonus: x ^ x → 0 (como en el caso anterior)
            if (init instanceof CtBinaryOperator<?> bin && bin.getKind() == BinaryOperatorKind.BITXOR) {
                if (bin.getLeftHandOperand().equals(bin.getRightHandOperand())) {
                    assignment.getAssignment().replace(localVar.getFactory().createLiteral(0));
                }
            }
            return;
        }
    }

    /**
     * Lista ampliada de expresiones seguras para duplicar (inline)
     * Ahora incluye accesos a array → mem[IX + 0], mem[addr], etc.
     */
    private boolean isSafeToInline(CtExpression<?> expr) {
        if (expr == null) return false;

        return expr instanceof CtLiteral ||
               expr instanceof CtThisAccess ||
               expr instanceof CtSuperAccess ||
               expr instanceof CtVariableAccess ||
               expr instanceof CtFieldAccess ||
               expr instanceof CtArrayRead ||           // ← CLAVE para tu caso
               expr instanceof CtUnaryOperator ||
               expr instanceof CtBinaryOperator ||      // ya cubría A ^ A
               (expr instanceof CtNewClass && ((CtNewClass<?>) expr).getAnonymousClass() == null) ||
               (expr instanceof CtConstructorCall && ((CtConstructorCall<?>) expr).getArguments().isEmpty()) ||
               (expr instanceof CtInvocation && isSimpleInvocation((CtInvocation<?>) expr));
    }

    private boolean isSimpleInvocation(CtInvocation<?> inv) {
        // puedes poner aquí reglas más estrictas si querés (ej: solo getters)
        return true;
    }

    // ====================== MAIN ======================
    public static void main(String[] args) {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.getEnvironment().setAutoImports(true);
        launcher.addInputResource("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/minimal/JetSetWilly2Converted.java");   // ← cambia si tu código está en otra carpeta

        launcher.buildModel();
        CtModel model = launcher.getModel();

        model.getRootPackage()
             .filterChildren(CtLocalVariable.class::isInstance)
             .forEach(var -> new InlineAndRemoveLocalsProcessorV3().process((CtLocalVariable<?>) var));

        launcher.prettyprint();
        System.out.println("✅ Inline y limpieza V3 finalizados.");
    }
}