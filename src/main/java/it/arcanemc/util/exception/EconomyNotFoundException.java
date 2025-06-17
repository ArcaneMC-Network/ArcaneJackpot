package it.arcanemc.util.exception;

public class EconomyNotFoundException extends RuntimeException {
  public EconomyNotFoundException() {
    super("Economy does not found or did not install with Vault");
  }
  public EconomyNotFoundException(String message) {
    super(message);
  }
}
