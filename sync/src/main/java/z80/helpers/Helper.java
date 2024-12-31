package z80.helpers;

import com.fpetrola.z80.opcodes.references.WordNumber;

import java.lang.reflect.Constructor;

public class Helper {
  public static String convertToHex(int routineAddress) {
    return Long.toHexString(routineAddress).toUpperCase();
  }

  public static <T extends WordNumber> String convertToHex(T value) {
    return value.toString();
  }

  public static <T> T createInstance(Class<T> type) {
    T instance;
    try {
      Constructor<T> constructor = type.getConstructor(null);
      instance = constructor.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return (T) instance;
  }

}
