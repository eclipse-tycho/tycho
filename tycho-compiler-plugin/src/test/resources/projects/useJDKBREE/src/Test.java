public class Test {

    // autoboxing => warning
    private Integer autoBoxed = 1;
    // unused private => error
    private String unused;

    void foo() {
        System.out.println(autoBoxed);
    }
}
