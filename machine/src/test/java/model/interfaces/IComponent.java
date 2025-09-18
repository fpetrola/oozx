package model.interfaces;

public interface IComponent {
  void connectToBus(ISpectrumBus bus);

  void disconnectFromBus();

  default void reset() {

  }
}
