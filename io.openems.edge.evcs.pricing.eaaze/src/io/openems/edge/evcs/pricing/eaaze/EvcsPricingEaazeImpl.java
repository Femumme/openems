package io.openems.edge.evcs.pricing.eaaze;


import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.pricing.EvcsPricing;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "EvcsPricing.Eaaze", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EvcsPricingEaazeImpl extends AbstractOpenemsComponent implements EvcsPricingEaaze, OpenemsComponent {

	/** Connect timeout in milliseconds (5 minutes). */
	private static final int CONNECT_TIMEOUT_MS = 300_000;

	/** Read timeout in milliseconds (5 minutes). */
	private static final int READ_TIMEOUT_MS = 300_000;

	private final Logger log = LoggerFactory.getLogger(EvcsPricingEaazeImpl.class);

	@Reference(target = "(id=" + EvcsPricing.SINGLETON_COMPONENT_ID + ")")
	private EvcsPricing pricing;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	private BridgeHttp httpBridge;

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private Config config;
	private volatile Double lastExportedPrice = null;

	public EvcsPricingEaazeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EvcsPricingEaaze.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.httpBridge = this.httpBridgeFactory.get();
		this.applyConfig(config);
		this.subscribeToPriceChanges();
		this.schedulePeriodicExport();
		// Log current price and trigger immediate export
		var currentPrice = this.pricing.getPrice().get();
		var nextPrice = this.pricing.getPriceChannel().getNextValue().get();
		this.log.info("Eaaze exporter activated. Price value={} €/kWh, nextValue={} €/kWh", currentPrice, nextPrice);
		this.tryExport();
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
		// Force re-export on config change
		this.lastExportedPrice = null;
		this.tryExport();
	}

	private synchronized void applyConfig(Config config) {
		this.config = config;
	}

	private void subscribeToPriceChanges() {
		this.pricing.getPriceChannel().onSetNextValue(ignore -> this.tryExport());
	}

	private void schedulePeriodicExport() {
		this.executor.scheduleAtFixedRate(this::tryExport, this.config.exportIntervalSeconds(),
				this.config.exportIntervalSeconds(), TimeUnit.SECONDS);
	}

	private void tryExport() {
		if (!this.config.enabled()) {
			return;
		}
		// Use getNextValue() as fallback if value() is not yet set
		var priceValue = this.pricing.getPrice();
		var price = priceValue.get();
		if (price == null) {
			// Try nextValue if current value is null
			price = this.pricing.getPriceChannel().getNextValue().get();
		}
		if (price == null) {
			this.log.debug("Price is null, skipping export");
			return;
		}
		// Avoid duplicate exports on unchanged price
		if (Objects.equals(this.lastExportedPrice, price)) {
			return;
		}
		this.exportPriceToEaaze(price);
		this.lastExportedPrice = price;
	}

	private void exportPriceToEaaze(Double priceEurPerKwh) {
		if (priceEurPerKwh == null || !Double.isFinite(priceEurPerKwh)) {
			this.log.warn("Cannot export invalid price: {}", priceEurPerKwh);
			this._setHttpStatusCode(0);
			return;
		}

		// Validate configuration
		if (this.config.apiToken().isBlank()) {
			this.log.warn("Cannot export price: API token is not configured");
			this._setHttpStatusCode(0);
			return;
		}
		if (this.config.tariffId().isBlank()) {
			this.log.warn("Cannot export price: Tariff ID is not configured");
			this._setHttpStatusCode(0);
			return;
		}
		if (this.config.tenantId().isBlank()) {
			this.log.warn("Cannot export price: Tenant ID is not configured");
			this._setHttpStatusCode(0);
			return;
		}

		// Convert brutto (gross) price to netto (net) price
		// Formula: netto = brutto / (1 + taxRate)
		var taxRate = this.config.taxRate();
		var nettoPrice = priceEurPerKwh / (1 + taxRate);

		this.log.info("Exporting EVCS price to Eaaze: {} €/kWh brutto -> {} €/kWh netto (tax rate: {}%)", 
				priceEurPerKwh, String.format("%.4f", nettoPrice), taxRate * 100);

		// Build GraphQL mutation
		var graphqlQuery = buildUpdateCpoTariffMutation(//
				this.config.tariffId(), //
				this.config.tenantId(), //
				this.config.tariffName(), //
				nettoPrice, //
				taxRate //
		);

		this.log.debug("GraphQL mutation:\n{}", graphqlQuery);

		// Build request body
		var requestBody = JsonUtils.buildJsonObject() //
				.addProperty("query", graphqlQuery) //
				.build();

		this.log.debug("Request body: {}", requestBody);

		// Build endpoint with headers
		var endpoint = new Endpoint(//
				this.config.graphqlUrl(), //
				HttpMethod.POST, //
				CONNECT_TIMEOUT_MS, //
				READ_TIMEOUT_MS, //
				requestBody.toString(), //
				Map.of(//
						"Content-Type", "application/json", //
						"Authorization", "M2M " + this.config.apiToken() //
				) //
		);

		// Execute request
		this.httpBridge.requestJson(endpoint) //
				.whenComplete((response, error) -> {
					if (error != null) {
						this.log.error("Failed to export price to Eaaze: {}", error.getMessage());
						this._setHttpStatusCode(0);
						return;
					}

					var statusCode = response.status().code();
					this._setHttpStatusCode(statusCode);

					if (response.status().isError()) {
						this.log.error("Eaaze API error (HTTP {}): {}", statusCode, response.data());
						return;
					}

					// Check for GraphQL errors in response
					var responseData = response.data();
					if (responseData.isJsonObject()) {
						var responseObj = responseData.getAsJsonObject();
						if (responseObj.has("errors") && !responseObj.get("errors").isJsonNull()) {
							this.log.error("Eaaze GraphQL error: {}", responseObj.get("errors"));
							return;
						}
					}

					this.log.info("Successfully exported price {} €/kWh to Eaaze", priceEurPerKwh);
				});
	}

	/**
	 * Builds the GraphQL mutation for updating a CPO tariff.
	 *
	 * @param tariffId       the UUID of the tariff to update
	 * @param tenantId       the tenant ID
	 * @param tariffName     the name of the tariff
	 * @param pricePerKwh    the energy price in €/kWh
	 * @param taxRate        the VAT tax rate (e.g., 0.19 for 19%)
	 * @return the GraphQL mutation string
	 */
	private static String buildUpdateCpoTariffMutation(String tariffId, String tenantId, String tariffName,
			double pricePerKwh, double taxRate) {
		// Note: GraphQL requires proper escaping of strings and a return selection
		return String.format("""
				mutation UpdateCpoTariff {
				    updateCpoTariff(
				        id: "%s"
				        input: {
				            tenantId: "%s"
				            name: "%s"
				            priceComponents: [
				                {
				                    name: "Energy fee"
				                    feeType: ENERGY_FEE
				                    pricePerUnit: %.4f
				                    tax: %.2f
				                }
				            ]
				        }
				    ) {
				        id
				    }
				}
				""", //
				escapeGraphqlString(tariffId), //
				escapeGraphqlString(tenantId), //
				escapeGraphqlString(tariffName), //
				pricePerKwh, //
				taxRate //
		);
	}

	/**
	 * Escapes special characters in a string for use in GraphQL.
	 *
	 * @param input the input string to escape
	 * @return the escaped string safe for use in GraphQL
	 */
	private static String escapeGraphqlString(String input) {
		if (input == null) {
			return "";
		}
		return input //
				.replace("\\", "\\\\") //
				.replace("\"", "\\\"") //
				.replace("\n", "\\n") //
				.replace("\r", "\\r") //
				.replace("\t", "\\t");
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}
}
