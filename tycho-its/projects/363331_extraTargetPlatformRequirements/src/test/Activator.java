package test;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator
    implements BundleActivator
{

    public static BundleContext context;

    public void start( BundleContext context )
        throws Exception
    {
        Activator.context = context;

        // doesn't compile with osgi 4.3/eclipse 3.7
        context.getServiceReference(null);
    }

    public void stop( BundleContext context )
        throws Exception
    {
        Activator.context = null;
    }

}
