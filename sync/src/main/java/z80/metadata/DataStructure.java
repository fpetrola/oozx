package z80.metadata;

import com.fpetrola.z80.metadata.DataStructureInstance;

import java.util.HashMap;
import java.util.Map;

public class DataStructure {
  public Map<Integer, DataStructureInstance> instances = new HashMap<>();

  public DataStructure() {
  }

  public DataStructureInstance getInstance(int instance) {
    DataStructureInstance dataStructureInstance = instances.get(instance);
    if (dataStructureInstance == null)
      instances.put(instance, dataStructureInstance = new DataStructureInstance());
    return dataStructureInstance;
  }
}
