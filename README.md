com.google.gwt.servlet
======================

This project creates an OSGi bundle for running GWT servlets in OSGi

The goal of this project is to minimize the size of the JAR that is being produced, since the 2 library files are 8.2 MB together. Almost all the client packages are no needed at the server-side though. Therefor the current version creates an JAR file of 2.2 MB.

OSGi integration
----------------

The way this bundle works is by listening for RemoteService registrations and to create a Servlet and register this as a Servlet service. This means that it needs a functionality such as the felix.http.whiteboard.

As an example, lets change the example from [Google developers][1] to OSGi:

[1]: https://developers.google.com/web-toolkit/doc/latest/tutorial/RPC#services

	package com.google.gwt.sample.stockwatcher.server;

	import aQute.bnd.annotation.component.Component;
	import com.google.gwt.sample.stockwatcher.client.StockPrice;
	import com.google.gwt.sample.stockwatcher.client.StockPriceService;
	import com.google.gwt.user.server.rpc.RemoteService;

	@Component(provide = RemoteService.class, properties = "contextId=stockwatcher")
	public class StockPriceServiceImpl implements StockPriceService {
		public StockPrice[] getPrices(String[] symbols) {
			// TODO Auto-generated method stub
			return null;
		}
	}

As you can see, this implementation is no direct implementation of the RemoteServiceServlet. This is replaced by the custom Servlet implementation of this bundle. The contextId property is used to create the path on which the servlet can be reached. In this case the 'stockwatcher' is the first part and the @RemoteServiceRelativePath of the StockPriceService interface is used for the second part. This means that this servlet is reachable at '/stockwatcher/stockPrices'.

If it is needed to handle a HttpRequest more directly (e.g. you want to read some of the request headers), you can implement the RemoteServiceCallback interface.
