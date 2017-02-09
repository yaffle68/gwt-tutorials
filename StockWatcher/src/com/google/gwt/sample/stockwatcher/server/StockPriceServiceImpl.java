package com.google.gwt.sample.stockwatcher.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gwt.sample.stockwatcher.client.DelistedException;
import com.google.gwt.sample.stockwatcher.client.StockPriceService;
import com.google.gwt.sample.stockwatcher.shared.StockPrice;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server-side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class StockPriceServiceImpl extends RemoteServiceServlet implements StockPriceService {

	private static final double MAX_PRICE = 100.0; // $100.00
	private static final double MAX_PRICE_CHANGE = 0.02; // +/- 2%

	/**
	 * Escape an html string. Escaping data received from the client helps to
	 * prevent cross-site script vulnerabilities.
	 * 
	 * @param html the html string to escape
	 * @return the escaped string
	 */
	private String escapeHtml(String html) {
		if (html == null) {
			return null;
		}
		return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

	@Override
	public StockPrice[] getPrices(String[] symbols) throws DelistedException {
		List<StockPrice> prices = new ArrayList<StockPrice>(symbols.length);
				
		Random rnd = new Random();
		for (String nextSymbol : symbols) {
			if ("ERR".equals(nextSymbol)) {
				throw new DelistedException(nextSymbol);
			}
			double price = rnd.nextDouble() * MAX_PRICE;
			double change = price * MAX_PRICE_CHANGE * (rnd.nextDouble() * 2.0 - 1.0);

			prices.add(new StockPrice(nextSymbol, price, change));
		}
		
		return prices.toArray(new StockPrice[]{});
	}	
}
