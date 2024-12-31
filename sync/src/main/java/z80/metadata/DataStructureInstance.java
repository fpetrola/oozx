package z80.metadata;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DataStructureInstance {
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataStructureInstance that = (DataStructureInstance) o;
    return Objects.equals(addresses, that.addresses);
  }

  @Override
  public int hashCode() {
    return Objects.hash(addresses);
  }

  public Set<Integer> addresses = new HashSet<>();

  public void addAddress(int address) {
  if (addresses.contains(33024) && address == 33032)
    System.out.println("oh!!");

    if (!addresses.isEmpty() && address - addresses.iterator().next().intValue() > 100)
      System.out.println("mucha distancia!");
    addresses.add(address);
  }
}
