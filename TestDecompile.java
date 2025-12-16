import com.fpetrola.z80.bytecode.Decompiler;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestDecompile {
  public static void main(String[] args) throws Exception {
    byte[] bytecode = Files.readAllBytes(Paths.get("Test3.class"));
    Decompiler decompiler = new Decompiler();
    decompiler.addClass(bytecode, new java.io.File("Test3.class"));
    String result = decompiler.decompile();
    System.out.println(result);
  }
}
