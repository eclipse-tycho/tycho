package phf.host;

public class HostInterfaceFactory {

    public static HostInterface newInstance() throws Exception {
        Class<?> implClassFromFragment = Class.forName("phf.fragment.FragmentImpl", true,
                HostInterface.class.getClassLoader());
        return (HostInterface) implClassFromFragment.getConstructor().newInstance();
    }

}
