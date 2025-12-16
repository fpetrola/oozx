import sootup.core.IdentifierFactory;
import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.stmt.*;
import sootup.core.model.*;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;

public class DebugCloner {
  public static void main(String[] args) {
    AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/bytecode/target/classes");
    JavaView view = new JavaView(inputLocation);
    IdentifierFactory factory = view.getIdentifierFactory();
    ClassType classType = factory.getClassType("com.fpetrola.oozx.t2.IndirectMemory8BitReference");
    JavaSootClass originalClass = (JavaSootClass) view.getClassOrThrow(classType);
    
    for (SootMethod sm : originalClass.getMethods()) {
      if (sm.getName().equals("getTarget")) {
        Body body = sm.getBody();
        StmtGraph<?> stmtGraph = body.getStmtGraph();
        for (Stmt stmt : stmtGraph.getStmts()) {
          System.out.println("Stmt: " + stmt.getClass().getSimpleName() + " -> " + stmt);
          if (stmt instanceof JReturnStmt) {
            JReturnStmt ret = (JReturnStmt) stmt;
            Object op = ret.getOp();
            System.out.println("  Return op class: " + op.getClass().getName());
            System.out.println("  Return op: " + op);
          }
        }
      }
    }
  }
}
