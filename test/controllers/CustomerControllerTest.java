package controllers;

import beans.request.CustomerOnboardRequest;
import beans.request.TransferRequestBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import models.Account;
import models.Account.CurrencyEnum;
import models.Customer;
import models.TransferLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.db.jpa.JPAApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import play.test.WithApplication;
import startup.InMemoryDbInitialiser;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;

public class CustomerControllerTest extends WithApplication {
    private static final String ONBOARD_ROUTE = "/customer/onboard";

    private ObjectMapper mapper = new ObjectMapper();
    private Gson gson = new Gson();

    private Application app;

    /**
     * DB SNAPSNOT AFTER INITIALISATION
     *
     * ID  	      BALANCE  	CURRENCY  	CUSTOMER_ID
     * 19283746	  10000.00	   SGD	        1
     * 19283747	  20000.00	   USD	        2
     * 19283748	  30000.00	   EUR	        3
     * 19283749	  40000.00	   USD	        4
     * 19283750	  22000.00	   SGD	        4
     * 19283751	  50000.00	   SGD	        5
     * 19283752	  67000.00	   SGD	        5
     */
    @Before
    public void setup() {
        app = new GuiceApplicationBuilder().build();

        Helpers.start(app);

        // init DB
        InMemoryDbInitialiser inMemoryDbInitialiser = app.injector().instanceOf(InMemoryDbInitialiser.class);
        inMemoryDbInitialiser.init();
    }

    @After
    public void teardown() {
        Helpers.stop(app);
    }

    @Test
    public void Given_NoTransferDone_When_GettingTransferLogs_Then_ReturnNoLogs() {
        // GIVEN
        String customerId = String.valueOf(5L);
        String accountId = String.valueOf(19283751L);
        String url = "/customer/$1/logs/$2".replace("$1", customerId).replace("$2", accountId);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(Helpers.GET).uri(url);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(OK);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("[]");
    }

    @Test
    public void Given_TransferDoneButForDifferentAccount_When_GettingTransferLogs_Then_ReturnNoLogs() {
        // GIVEN
        String customerId = String.valueOf(5L);
        String accountId = String.valueOf(19283751L);
        String url = "/customer/$1/logs/$2".replace("$1", customerId).replace("$2", accountId);

        // do a transfer
        doTransfer(19283746L, 19283752L, 2000, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(Helpers.GET).uri(url);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(OK);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("[]");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void Given_TransferDone_When_GettingTransferLogs_Then_ReturnLogs() {
        // GIVEN
        String customerId = String.valueOf(5L);
        String accountId = String.valueOf(19283751L);
        String url = "/customer/$1/logs/$2".replace("$1", customerId).replace("$2", accountId);

        // do a transfer
        doTransfer(19283746L, 19283751L, 2000, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(Helpers.GET).uri(url);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(OK);
        assertThat(resultString).isNotNull();

        List<TransferLog> logs = gson.fromJson(resultString, List.class);
        assertThat(logs.size()).isEqualTo(1);
    }

    @Test
    public void Given_NoSuchCustomer_When_GettingCustomerAccounts_Then_ThrowError() {
        // GIVEN
        String customerId = String.valueOf(21L);
        String url = "/customer/$1/accounts".replace("$1", customerId);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(Helpers.GET).uri(url);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("customer not found");
    }

    @Test
    public void Given_CustomerFound_When_GettingCustomerAccounts_Then_ReturnAllAccounts() {
        // GIVEN
        String customerId = String.valueOf(5L);
        String url = "/customer/$1/accounts".replace("$1", customerId);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(Helpers.GET).uri(url);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(OK);
        assertThat(resultString).isNotNull();

        Customer customer = gson.fromJson(resultString, Customer.class);
        assertThat(customer.getAccounts().size()).isEqualTo(2);
    }

    @Test
    public void Given_EmptyOnboardRequest_When_CustomerOnboarding_Then_OnboardFail() {
        // GIVEN
        // empty body

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(ONBOARD_ROUTE);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(BAD_REQUEST);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("onboard request cannot be empty");
    }

    @Test
    public void Given_NoFirstName_When_CustomerOnboarding_Then_OnboardFail() {
        // GIVEN
        JsonNode body = buildOnboardRequest(" ", "Andrew", 10, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(ONBOARD_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("first name or last name cannot be empty");
    }

    @Test
    public void Given_NoLastName_When_CustomerOnboarding_Then_OnboardFail() {
        // GIVEN
        JsonNode body = buildOnboardRequest("John", " ", 40, CurrencyEnum.USD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(ONBOARD_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("first name or last name cannot be empty");
    }

    @Test
    public void Given_NoAccountsInformation_When_CustomerOnboarding_Then_OnboardFail() {
        // GIVEN
        CustomerOnboardRequest bean = CustomerOnboardRequest.builder().firstName("Leo").lastName("Travis").build();
        JsonNode body = mapper.convertValue(bean, JsonNode.class);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(ONBOARD_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("No accounts found to onboard customer");
    }

    @Test
    public void Given_OnboardedAccountWithNegativeBalance_When_CustomerOnboarding_Then_OnboardFail() {
        // GIVEN
        JsonNode body = buildOnboardRequest("Leo", "Travis", -1, CurrencyEnum.EUR);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(ONBOARD_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Account with negative currency cannot be onboarded");
    }

    @Test
    public void Given_OnboardedAccountWithZeroBalance_When_CustomerOnboarding_Then_OnboardSuccess() {
        // GIVEN
        JsonNode body = buildOnboardRequest("Leo", "Travis", 0, CurrencyEnum.EUR);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(ONBOARD_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(OK);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("customer account created");
    }

    @Test
    public void Given_CorrectCustomerDetailsToOnboard_When_CustomerOnboarding_Then_VerifySuccessOnboard() {
        // GIVEN
        JsonNode body = buildOnboardRequest("Leo", "Travis", 10, CurrencyEnum.EUR);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(ONBOARD_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(OK);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("customer account created");

        JPAApi jpaApi = app.injector().instanceOf(JPAApi.class);
        Customer customer = jpaApi.withTransaction(em ->
                em.createQuery("select c from Customer c JOIN FETCH c.accounts a" +
                        " where c.firstName = 'Leo'", Customer.class)
                        .getSingleResult());
        assertThat(customer.getFirstName()).isEqualTo("Leo");
        assertThat(customer.getLastName()).isEqualTo("Travis");
        assertThat(customer.getAccounts().size()).isEqualTo(1);
        assertThat(customer.getAccounts().get(0).getCurrency()).isEqualTo(CurrencyEnum.EUR);
    }

    private void doTransfer(Long from, Long to, double amount, CurrencyEnum currency) {
        String TRANSFER_ROUTE = "/transfer";
        TransferRequestBean transferRequestBean = TransferRequestBean.builder()
                .amount(BigDecimal.valueOf(amount))
                .currency(currency)
                .fromAccountId(from)
                .toAccountId(to)
                .build();

        JsonNode body = mapper.convertValue(transferRequestBean, JsonNode.class);

        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        route(app, request);
    }

    private JsonNode buildOnboardRequest(String firstName, String lastName, double balance, CurrencyEnum currency) {
        CustomerOnboardRequest bean = CustomerOnboardRequest.builder()
                .firstName(firstName).lastName(lastName)
                .accounts(Collections.singletonList(Account.builder().balance(BigDecimal.valueOf(balance)).currency(currency).build()))
                .build();

        return mapper.convertValue(bean, JsonNode.class);
    }
}
