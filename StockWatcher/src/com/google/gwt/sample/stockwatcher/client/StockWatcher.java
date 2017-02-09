package com.google.gwt.sample.stockwatcher.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.sample.stockwatcher.shared.StockPrice;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class StockWatcher implements EntryPoint {
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final int REFRESH_INTERVAL = 5000;

	private VerticalPanel mainPanel = new VerticalPanel();
	private FlexTable stocksFlexTable = new FlexTable();
	private HorizontalPanel addPanel = new HorizontalPanel();
	private TextBox newSymbolTextBox = new TextBox();
	private Button addStockButton = new Button("Add");
	private Label lastUpdatedLabel = new Label();
	private Label errorMsgLabel = new Label();

	private List<String> stockItems = new ArrayList<String>();

	private StockPriceServiceAsync stockPriceService = GWT.create(StockPriceService.class);
	private LoginServiceAsync loginService = GWT.create(LoginService.class);
	private Anchor signInLink = new Anchor("Sign In");
	private Anchor signOutLink = new Anchor("Sign Out");

	private LoginInfo loginInfo = null;
	private VerticalPanel loginPanel = new VerticalPanel();
	private Label loginLabel = new Label(
			"Please sign in to your Google Account to access the StockWatcher application.");
	private final StockServiceAsync stockService = GWT.create(StockService.class);

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		loginService.login(GWT.getHostPageBaseURL(), new AsyncCallback<LoginInfo>() {
			@Override
			public void onFailure(Throwable caught) {
				handleError(caught);
			}

			@Override
			public void onSuccess(LoginInfo result) {
				loginInfo = result;
				if (loginInfo.isLoggedIn()) {
					loadStockWatcher();
				} else {
					loadLogin();
				}
			}
		});
	}

	private void loadLogin() {
		// Assemble login panel.
		signInLink.setHref(loginInfo.getLoginUrl());
		loginPanel.add(loginLabel);
		loginPanel.add(signInLink);
		RootPanel.get("stockList").add(loginPanel);
	}

	private void loadStockWatcher() {
		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel rootPanel = RootPanel.get("stockList");
		rootPanel.add(createMainPanel());
		loadStocks();

		Timer refreshTimer = new Timer() {
			@Override
			public void run() {
				 refreshWatchList();
			}
		};

		refreshTimer.scheduleRepeating(REFRESH_INTERVAL);

		// RootPanel.get("nameFieldContainer").add(nameField);
		// RootPanel.get("sendButtonContainer").add(sendButton);
		// RootPanel.get("errorLabelContainer").add(errorLabel);
	}

	private void loadStocks() {
		stockService.getStocks(new AsyncCallback<String[]>() {
			public void onFailure(Throwable error) {
				handleError(error);
			}

			public void onSuccess(String[] symbols) {
				displayStocks(symbols);
			}
		});
	}

	private void displayStocks(String[] symbols) {
		for (String symbol : symbols) {
			displayStock(symbol);
		}
	}

	private void refreshWatchList() {

		// Initialize the service proxy.
		if (stockPriceService == null) {
			stockPriceService = GWT.create(StockPriceService.class);
		}

		// Set up the callback object.
		AsyncCallback<StockPrice[]> callback = new AsyncCallback<StockPrice[]>() {
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				if (caught instanceof DelistedException) {
					details = "Company '" + ((DelistedException) caught).getSymbol() + "' was delisted";
				}

				errorMsgLabel.setText("Error: " + details);
				errorMsgLabel.setVisible(true);
			}

			public void onSuccess(StockPrice[] result) {
				updateTable(result);
			}
		};

		stockPriceService.getPrices(stockItems.toArray(new String[] {}), callback);
	}

	private void updateTable(StockPrice[] prices) {
		for (int i = 0; i < prices.length; i++) {
			updateTable(prices[i]);
		}
	}

	private void updateTable(StockPrice stockPrice) {
		// Make sure the stock is still in the stock table.
		if (!stockItems.contains(stockPrice.getSymbol())) {
			return;
		}

		int row = stockItems.indexOf(stockPrice.getSymbol()) + 1;

		// Format the data in the Price and Change fields.
		String priceText = NumberFormat.getFormat("#,##0.00").format(stockPrice.getPrice());

		double change = stockPrice.getChange();
		NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
		String changeText = changeFormat.format(change);
		String changePercentText = changeFormat.format(stockPrice.getChangePercent());

		// Populate the Price and Change fields with new data.
		stocksFlexTable.setText(row, 1, priceText);
		Label changeWidget = (Label) stocksFlexTable.getWidget(row, 2);
		changeWidget.setText(changeText + " (" + changePercentText + "%)");

		String styleName = "noChange";
		if (change > 0) {
			styleName = "positiveChange";
		} else if (change < 0) {
			styleName = "negativeChange";
		}

		changeWidget.setStyleName(styleName);

		// stocksFlexTable.setText(row, 2, changeText + " (" + changePercentText
		// + "%)");

		DateTimeFormat dateFormat = DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM);
		lastUpdatedLabel.setText("Last updated: " + dateFormat.format(new Date()));

		// Clear any errors.
		errorMsgLabel.setVisible(false);

	}

	private Widget createMainPanel() {
		stocksFlexTable.setText(0, 0, "Symbol");
		stocksFlexTable.setText(0, 1, "Price");
		stocksFlexTable.setText(0, 2, "Change");
		stocksFlexTable.setText(0, 3, "Remove");

		// Add styles to elements in the stock list table.
		stocksFlexTable.setCellPadding(6);

		stocksFlexTable.addStyleName("watchList");
		stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
		setStylesForRow(0);

		errorMsgLabel.setStyleName("errorMessage");
		errorMsgLabel.setVisible(false);

		signOutLink.setHref(loginInfo.getLogoutUrl());
		mainPanel.add(signOutLink);
		mainPanel.add(errorMsgLabel);
		mainPanel.add(stocksFlexTable);
		addPanel.add(newSymbolTextBox);
		addPanel.add(addStockButton);
		addPanel.setStyleName("addPanel");

		addPanel.addStyleName("addPanel");

		AddHandler addHandler = new AddHandler();
		addStockButton.addClickHandler(addHandler);
		newSymbolTextBox.addKeyUpHandler(addHandler);

		mainPanel.add(addPanel);
		mainPanel.add(lastUpdatedLabel);

		newSymbolTextBox.setFocus(true);
		newSymbolTextBox.selectAll();
		return mainPanel;
	}

	private void setStylesForRow(final int row) {
		stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");
	}

	private void displayStock(final String symbol) {
		// Add the stock to the table.
		int row = stocksFlexTable.getRowCount();
		stockItems.add(symbol);
		stocksFlexTable.setText(row, 0, symbol);
		stocksFlexTable.setWidget(row, 2, new Label());
		stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");

		// Add a button to remove this stock from the table.
		Button removeStockButton = new Button("x");
		removeStockButton.addStyleDependentName("remove");
		removeStockButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				removeStock(symbol);
			}
		});
		stocksFlexTable.setWidget(row, 3, removeStockButton);

		// Get the stock price.
		refreshWatchList();

	}

	private void removeStock(final String symbol) {
		stockService.removeStock(symbol, new AsyncCallback<Void>() {
			public void onFailure(Throwable error) {
				handleError(error);
			}

			public void onSuccess(Void ignore) {
				undisplayStock(symbol);
			}
		});
	}

	private void undisplayStock(String symbol) {
		int removedIndex = stockItems.indexOf(symbol);
		stockItems.remove(removedIndex);
		stocksFlexTable.removeRow(removedIndex + 1);
	}

	private void handleError(Throwable error) {
		Window.alert(error.getMessage());
		if (error instanceof NotLoggedInException) {
			Window.Location.replace(loginInfo.getLogoutUrl());
		}
	}

	private class AddHandler implements ClickHandler, KeyUpHandler {

		@Override
		public void onKeyUp(KeyUpEvent event) {
			if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
				doAdd();
			}
		}

		private void doAdd() {
			final String symbol = newSymbolTextBox.getText().toUpperCase().trim();
			newSymbolTextBox.setFocus(true);

			// Stock code must be between 1 and 10 chars that are numbers,
			// letters, or dots.
			if (!symbol.matches("^[0-9A-Z\\.]{1,10}$")) {
				Window.alert("'" + symbol + "' is not a valid symbol.");
				newSymbolTextBox.selectAll();
				return;
			}

			if (stockItems.contains(symbol)) {
				return;
			}
			addStock(symbol);
		}

		private void addStock(final String symbol) {
			stockService.addStock(symbol, new AsyncCallback<Void>() {
				public void onFailure(Throwable error) {
					handleError(error);
				}

				public void onSuccess(Void ignore) {
					displayStock(symbol);
				}
			});
		}

		@Override
		public void onClick(ClickEvent event) {
			doAdd();
		}
	}
}
