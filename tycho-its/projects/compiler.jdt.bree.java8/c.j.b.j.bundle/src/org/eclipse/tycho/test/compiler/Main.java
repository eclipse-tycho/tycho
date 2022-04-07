package org.eclipse.tycho.test.compiler;
import javax.xml.bind.annotation.XmlEnum;

public class Main {
  public static void main(String[] args) {
    // String join is Java 8
    System.out.println(String.join("-", "Tycho", "is", "cool"));
    //this is to ensure javax.xml.bind.annotation is not only resolved by P2 but also by compiler!
	XmlEnum xmlEnum = null;
  }
}
