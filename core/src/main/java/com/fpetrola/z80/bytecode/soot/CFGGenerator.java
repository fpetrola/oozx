/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.fpetrola.z80.bytecode.soot;

import com.fpetrola.z80.bytecode.tests.JetSetWilly;
import soot.*;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class CFGGenerator {
    public static void main(String[] args) {
        // Configuración de Soot para procesar clases y bytecode
        Options.v().set_prepend_classpath(true);
        Options.v().set_whole_program(true);
        String s = "/home/fernando/.sdkman/candidates/java/8.0.282-open/jre/lib/rt.jar:/home/fernando/.sdkman/candidates/java/8.0.282-open/jre/lib/jce.jar:target/classes";
        Options.v().set_soot_classpath(s);

        Scene scene = Scene.v();
        scene.setSootClassPath(s);
        scene.loadBasicClasses();
        scene.loadNecessaryClasses();


//        Options.scene().set_soot_classpath(Scene.scene().getSootClassPath());

        // Establecer la clase a analizar (puede ser cualquier clase compilada en bytecode)
        String className = JetSetWilly.class.getName();  // Reemplaza con tu clase
        scene.addBasicClass(className, SootClass.SIGNATURES);

        // Añadir la clase a la escena de Soot
        SootClass sootClass = scene.loadClass(className, SootClass.SIGNATURES);
        sootClass.setApplicationClass();

        // Convertir la clase a formato intermedio (Jimple)
        for (SootMethod method : sootClass.getMethods()) {
            if (!method.isConcrete()) continue;

            // Obtener la representación en Jimple del método
            Body body = method.retrieveActiveBody();

            // Generar el CFG usando UnitGraph
            UnitGraph cfg = new BriefUnitGraph(body);

            // Mostrar información sobre el CFG
            System.out.println("CFG for method: " + method.getName());
            for (Unit unit : cfg) {
                System.out.println(unit);
            }
        }
    }
}
