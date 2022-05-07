package org.eclipse.tycho.test.compiler;

import java.util.List;

public class Main {
  public static void main(String[] args) {
    // Stream.toList() is Java 16
    System.out.println(List.of("a", "b").stream().toList());
  }
  // records are Java 17
  record Person(String name, String address) {}
}
