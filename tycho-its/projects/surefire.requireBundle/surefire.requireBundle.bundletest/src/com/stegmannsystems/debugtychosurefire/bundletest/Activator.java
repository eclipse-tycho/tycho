package com.stegmannsystems.debugtychosurefire.bundletest;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator
{

    private static BundleContext context;

    static BundleContext getContext()
    {
        return context;
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(final BundleContext bundleContext) throws Exception
    {
        Activator.context = bundleContext;
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(final BundleContext bundleContext) throws Exception
    {
        Activator.context = null;
    }

}
