package nl.knaw.huc.resussun.configuration;

import org.glassfish.jersey.message.MessageUtils;
import org.glassfish.jersey.server.ContainerRequest;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.List;

/**
 * This interceptor wraps a JSON stream obtained by a underlying JSON provider into a callback function.
 * As org.glassfish.jersey.server.internal.JsonWithPaddingInterceptor requires a JavaScript Accepts header,
 * this implementation provides an alternative that only looks at the callback parameter.
 */
public class JsonWithPaddingInterceptor implements WriterInterceptor {
  private static final String CALLBACK_PARAM = "callback";

  @Inject
  private Provider<ContainerRequest> containerRequestProvider;

  @Override
  public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
    final ContainerRequest containerRequest = containerRequestProvider.get();
    final UriInfo uriInfo = containerRequest.getUriInfo();
    final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
    final List<String> queryParameter = queryParameters.get(CALLBACK_PARAM);
    final String callback = (queryParameter != null && !queryParameter.isEmpty() && !queryParameter.get(0).isEmpty())
        ? queryParameter.get(0) : null;

    if (callback != null) {
      context.setMediaType(MediaType.APPLICATION_JSON_TYPE);
      context.getOutputStream().write(callback.getBytes(MessageUtils.getCharset(context.getMediaType())));
      context.getOutputStream().write('(');
    }

    context.proceed();

    if (callback != null) {
      context.getOutputStream().write(')');
      context.getOutputStream().write(';');
    }
  }
}
