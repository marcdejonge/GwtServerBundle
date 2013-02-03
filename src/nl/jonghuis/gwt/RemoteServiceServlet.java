package nl.jonghuis.gwt;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.servlet.RemoteServiceCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStream;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.gwt.user.server.rpc.SerializationPolicy;

public class RemoteServiceServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(RemoteServiceHandler.class);

    private static final long serialVersionUID = -7477632927203510705L;

    private final RemoteService remoteService;

    public RemoteServiceServlet(RemoteService remoteService) {
        this.remoteService = remoteService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String requestPayLoad = RPCServletUtils.readContentAsGwtRpc(req);
            String responsePayload = processCall(req, requestPayLoad, remoteService);
            boolean gzip = RPCServletUtils.acceptsGzipEncoding(req);
            RPCServletUtils.writeResponse(getServletContext(), resp, responsePayload, gzip);
        } catch (Exception ex) {
            logger.error("Exception thrown", ex);
            RPCServletUtils.writeResponseForUnexpectedFailure(getServletContext(), resp, ex);
        }
    }

    private String
            processCall(HttpServletRequest request, String requestPayLoad, RemoteService remoteService) throws SerializationException {
        final ClassLoader serviceClassloader = remoteService.getClass().getClassLoader();
        final ClassLoader rpcClassloader = RPCRequest.class.getClassLoader();

        final ClassLoader threadClassLoader = new ClassLoader() {
            @Override
            public URL getResource(String name) {
                URL url = serviceClassloader.getResource(name);
                if (url == null) {
                    url = rpcClassloader.getResource(name);
                }
                return url;
            }

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                try {
                    return serviceClassloader.loadClass(name);
                } catch (ClassNotFoundException ex) {
                    return rpcClassloader.loadClass(name);
                }
            }
        };

        final ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(threadClassLoader);

        try {
            RPCRequest rpcRequest = RPC.decodeRequest(requestPayLoad, remoteService.getClass());
            return invokeAndEncodeResponse(request,
                                           remoteService,
                                           rpcRequest.getMethod(),
                                           rpcRequest.getParameters(),
                                           rpcRequest.getSerializationPolicy(),
                                           AbstractSerializationStream.DEFAULT_FLAGS);
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        }
    }

    // taken from com.google.gwt.user.server.rpc.RPC with added exception logging
    private static String invokeAndEncodeResponse(HttpServletRequest request,
                                                  final RemoteService remoteService,
                                                  final Method serviceMethod,
                                                  final Object[] args,
                                                  final SerializationPolicy serializationPolicy,
                                                  final int flags) throws SerializationException {
        try {
            Object result = null;
            if (remoteService instanceof RemoteServiceCallback) {
                result = ((RemoteServiceCallback) remoteService).handleCall(request, serviceMethod, args);
            } else {
                result = serviceMethod.invoke(remoteService, args);
            }
            return RPC.encodeResponseForSuccess(serviceMethod, result, serializationPolicy, flags);
        } catch (final IllegalAccessException e) {
            throw new SecurityException("Blocked attempt to access inaccessible method " + serviceMethod
                                        + " on target "
                                        + remoteService
                                        + "; this is either misconfiguration or a hack attempt", e);
        } catch (final IllegalArgumentException e) {
            throw new SecurityException("Blocked attempt to invoke method " + serviceMethod
                                        + " on target "
                                        + remoteService
                                        + " with invalid arguments "
                                        + Arrays.toString(args), e);
        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            logger.error("Error invoking method " + serviceMethod
                         + " on target "
                         + remoteService
                         + "; we will try to encode the caught exception", cause);
            return RPC.encodeResponseForFailure(serviceMethod, cause, serializationPolicy, flags);
        }
    }

    public RemoteService getRemoteService() {
        return remoteService;
    }
}
