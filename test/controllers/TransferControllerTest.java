package controllers;

import beans.request.TransferRequestBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Account;
import org.junit.Test;
import play.Application;
import play.db.jpa.JPAApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import startup.InMemoryDbInitialiser;

import java.math.BigDecimal;

import static models.Account.CurrencyEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;
import static play.test.Helpers.OK;
import static play.test.Helpers.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;

public class TransferControllerTest extends WithApplication {

    private static final String TRANSFER_ROUTE = "/transfer";
    private ObjectMapper mapper = new ObjectMapper();

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
    @Override
    protected Application provideApplication() {
        Application app = new GuiceApplicationBuilder().build();

        // init DB
        InMemoryDbInitialiser inMemoryDbInitialiser = app.injector().instanceOf(InMemoryDbInitialiser.class);
        inMemoryDbInitialiser.init();

        return app;
    }

    @Test
    public void Given_EmptyTransferRequest_When_AccountTransfer_Then_TransferFail() {
        // GIVEN
        // empty body

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(BAD_REQUEST);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Transfer request cannot be empty");
    }

    @Test
    public void Given_ZeroTransferAmount_When_AccountTransfer_Then_TransferFail() {
        // GIVEN
        JsonNode body = buildTransferRequest(19283746L, 19283750L, 0, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Invalid amount to transfer");
    }

    @Test
    public void Given_NegativeTransferAmount_When_AccountTransfer_Then_TransferFail() {
        // GIVEN
        JsonNode body = buildTransferRequest(19283746L, 19283750L, -0.01, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Invalid amount to transfer");
    }

    @Test
    public void Given_InvalidFromAccount_When_AccountTransfer_Then_TransferFail() {
        // GIVEN
        JsonNode body = buildTransferRequest(19283746L, 22222L, 7435, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Transfer failed - exception.NoAccountFoundException: Account number not found = 22222");
    }

    @Test
    public void Given_InvalidToAccount_When_AccountTransfer_Then_TransferFail() {
        // GIVEN
        JsonNode body = buildTransferRequest(11111L, 19283750L, 7435, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Transfer failed - exception.NoAccountFoundException: Account number not found = 11111");
    }

    @Test
    public void Given_FromAndToAreSameAccounts_When_AccountTransfer_Then_TransferFail() {
        // GIVEN
        JsonNode body = buildTransferRequest(19283750L, 19283750L, 7435, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Transfer failed - exception.InvalidTransferRequest: Cannot transfer funds within the same bank account = 19283750");
    }

    @Test
    public void Given_FromAndToAccountsHaveDifferentCurrencies_When_AccountTransfer_Then_TransferFail() {
        // GIVEN
        JsonNode body = buildTransferRequest(19283748L, 19283749L, 7435, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Transfer failed - exception.InvalidCurrencyTransfer: Given accounts have different currencies of EUR and USD");
    }

    @Test
    public void Given_AccountAndTransferCurrencyAreDifferent_When_AccountTransfer_Then_TransferFail() {
        // GIVEN
        JsonNode body = buildTransferRequest(19283751L, 19283752L, 7435, CurrencyEnum.USD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Transfer failed - exception.InvalidCurrencyTransfer: Transfer currency USD and account currrency SGD are different");
    }

    @Test
    public void Given_InsufficientAccountBalance_When_AccountTransfer_Then_TransferFail() {
        // GIVEN
        JsonNode body = buildTransferRequest(19283751L, 19283752L, 50000.01, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Transfer failed - exception.InsufficientBalance: The balance in the account not sufficient for this transfer");
    }

    @Test
    public void Given_ExactAccountBalance_When_AccountTransfer_Then_TransferSuccess() {
        // GIVEN
        JsonNode body = buildTransferRequest(19283751L, 19283752L, 50000, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(OK);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Transfer success");
    }

    @Test
    public void Given_SufficientBalance_AccountExists_CorrectCurrency_When_AccountTransfer_Then_TransferSuccess() {
        // GIVEN
        JsonNode body = buildTransferRequest(19283751L, 19283752L, 49999.99, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        Result result = route(app, request);
        String resultString = contentAsString(result);

        // THEN
        assertThat(result.status()).isEqualTo(OK);
        assertThat(resultString).isNotNull();
        assertThat(resultString).contains("Transfer success");
    }

    @Test
    public void Given_SuccessTransfer_When_AccountTransfer_Then_VerifyBalance() {
        // GIVEN
        JPAApi jpaApi = app.injector().instanceOf(JPAApi.class);
        JsonNode body = buildTransferRequest(19283751L, 19283752L, 49999.99, CurrencyEnum.SGD);

        // WHEN
        Http.RequestBuilder request = new Http.RequestBuilder().method(POST).uri(TRANSFER_ROUTE).bodyJson(body);
        route(app, request);

        // THEN
        Account sender = jpaApi.withTransaction(em -> em.find(Account.class, 19283751L));
        Account receiver = jpaApi.withTransaction(em -> em.find(Account.class, 19283752L));

        assertThat(sender.getBalance()).isPositive();
        assertThat(sender.getBalance()).isEqualTo(BigDecimal.valueOf(0.01)); // 50000 - 49999.99

        assertThat(receiver.getBalance()).isPositive();
        assertThat(receiver.getBalance()).isEqualTo(BigDecimal.valueOf(116999.99)); // 67000 + 49999.99
    }

    private JsonNode buildTransferRequest(Long from, Long to, double amount, CurrencyEnum currency) {
        TransferRequestBean transferRequestBean = TransferRequestBean.builder()
                .amount(BigDecimal.valueOf(amount))
                .currency(currency)
                .fromAccountId(from)
                .toAccountId(to)
                .build();

        return mapper.convertValue(transferRequestBean, JsonNode.class);
    }
}
