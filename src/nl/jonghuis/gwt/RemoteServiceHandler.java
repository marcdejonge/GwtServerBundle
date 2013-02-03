package nl.jonghuis.gwt;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Servlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@Component(immediate = true, provide = {})
public class RemoteServiceHandler {
    private static final Logger logger = LoggerFactory.getLogger(RemoteServiceHandler.class);

    private final Map<RemoteService, ServiceRegistration<Servlet>> serviceRegistrations;

    public RemoteServiceHandler() {
        serviceRegistrations = new HashMap<RemoteService, ServiceRegistration<Servlet>>();
    }

    @Deactivate
    public synchronized void destroy() {
        Iterator<ServiceRegistration<Servlet>> it = serviceRegistrations.values().iterator();
        while (it.hasNext()) {
            ServiceRegistration<Servlet> registration = it.next();
            registration.unregister();
            it.remove();
        }
    }

    @Reference(dynamic = true, optional = true, multiple = true)
    public void addRemoteService(RemoteService remoteService, Map<String, Object> properties) {
        BundleContext bundleContext = FrameworkUtil.getBundle(remoteService.getClass()).getBundleContext();

        String path = getPathFromAnnotation(remoteService.getClass());
        if (path == null) {
            throw new RuntimeException("No path defined for RemoteService: " + remoteService.getClass().getName());
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String contextId = null;
        if (properties.containsKey("contextId")) {
            contextId = (String) properties.get("contextId");
        }

        path = (contextId != null ? ("/" + contextId) : "") + path;

        synchronized (this) {
            logger.info("Registering remote service " + remoteService.getClass().getName() + " to " + path);
            RemoteServiceServlet rss = new RemoteServiceServlet(remoteService);

            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("alias", path);
            if (contextId != null) {
                props.put("contextId", contextId);
            }

            ServiceRegistration<Servlet> registration = bundleContext.registerService(Servlet.class, rss, props);
            serviceRegistrations.put(remoteService, registration);
        }
    }

    public synchronized void removeRemoteService(RemoteService remoteService) {
        ServiceRegistration<Servlet> registration = serviceRegistrations.remove(remoteService);
        if (registration != null) {
            registration.unregister();
        }
    }

    private String getPathFromAnnotation(final Class<?> clazz) {
        RemoteServiceRelativePath pathAnnotation = clazz.getAnnotation(RemoteServiceRelativePath.class);
        if (pathAnnotation != null) {
            return pathAnnotation.value();
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            String path = getPathFromAnnotation(iface);
            if (path != null) {
                return path;
            }
        }
        return clazz.getSuperclass() != null ? getPathFromAnnotation(clazz.getSuperclass()) : null;
    }
}
