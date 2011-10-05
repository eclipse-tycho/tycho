package org.eclipse.tycho.core.osgitools;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.eclipse.core.runtime.internal.adaptor.PluginConverterImpl;
import org.osgi.framework.BundleContext;

/**
 * {@link PluginConverterImpl} which can be used without a running OSGi framework.
 */
public class StandalonePluginConverter extends PluginConverterImpl {

    public StandalonePluginConverter() {
        super(null, createDummyContext());
    }

    /**
     * create a dummy BundleContext. This workaround allows us to reuse {@link PluginConverterImpl}
     * outside a running OSGi framework
     */
    private static BundleContext createDummyContext() {
        return (BundleContext) Proxy.newProxyInstance(BundleContext.class.getClassLoader(),
                new Class[] { BundleContext.class }, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return null;
                    }
                });
    }

}
