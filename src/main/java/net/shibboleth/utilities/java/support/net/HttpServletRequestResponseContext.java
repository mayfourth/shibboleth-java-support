package net.shibboleth.utilities.java.support.net;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.utilities.java.support.logic.Constraint;


/**
 * Class which holds and makes available the current HTTP servlet request and response via ThreadLocal storage.
 * 
 * <p>
 * See also {@link RequestResponseContextFilter}, which is a Java Servlet {@link Filter}-based way to populate
 * and clean up this context in a servlet container.
 * </p>
 */
public class HttpServletRequestResponseContext {
	
	/** ThreadLocal storage for request and response. */
	private static ThreadLocal<HttpServletRequestResponseContext> curr = new ThreadLocal<HttpServletRequestResponseContext>();
	
	/** The stored HTTP servlet request. */
	private HttpServletRequest req;

	/** The stored HTTP servlet response. */
	private HttpServletResponse resp;

	/** Constructor. Only allow local and subclass instantiation */
	protected HttpServletRequestResponseContext() {};

	/**
	 * Load the thread-local storage with the current request and response.
	 * 
	 * @param request the current {@link HttpServletRequest}
	 * @param response the current {@link HttpServletResponse}
	 */
	public static void loadCurrent(@Nonnull final HttpServletRequest request, @Nonnull final HttpServletResponse response) {
	    Constraint.isNotNull(request, "HttpServletRequest may not be null");
	    Constraint.isNotNull(response, "HttpServletResponse may not be null");
	    
		HttpServletRequestResponseContext current = new HttpServletRequestResponseContext();
		
		current.req = request;
		current.resp = response;
		
		curr.set(current);
	}
	
	/**
	 * Get the thread-local context instance holding the current request and response.
	 * 
	 * @return the current thread-local context instance
	 */
	@Nullable public static HttpServletRequestResponseContext getCurrent() {
		return curr.get();
	}

	/**
	 *  Clear the current thread-local context instance.
	 */
	public static void clearCurrent() {
		curr.remove();
	}

	/**
	 * Get the current {@link HttpServletRequest} being serviced by the current thread.
	 * 
	 * @return the current request 
	 */
	@Nonnull public HttpServletRequest getRequest() {
		return req;
	}

	/**
	 * Get the current {@link HttpServletResponse} being serviced by the current thread.
	 * 
	 * @return the current response 
	 */
	@Nonnull public HttpServletResponse getResponse() {
		return resp;
	}
	
}
