package controllers;

import beans.request.CustomerOnboardRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import dao.CustomerDao;
import models.Customer;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import utils.JsonUtil;

import java.util.Optional;

import static play.libs.Json.toJson;

public class CustomerController extends Controller {
    private final CustomerDao customerDao;

    @Inject
    public CustomerController(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    @Transactional(readOnly = true)
    public Result getTransferLogs(Long customerId, Long accountId) {
        return ok(toJson(customerDao.getTransferLogs(customerId, accountId)));
    }

    @Transactional(readOnly = true)
    public Result getAccounts(Long customerId) {
        Optional<Customer> customer = customerDao.getCustomerById(customerId);
        return customer.map(c -> ok(toJson(c))).orElseGet(() -> internalServerError("customer not found"));
    }

    @Transactional
    public Result onboard() {
        final JsonNode requestBodyJson = request().body().asJson();

        if (requestBodyJson == null)
            return Results.badRequest("onboard request cannot be empty");

        CustomerOnboardRequest customerOnboardRequest = JsonUtil.parseJson(requestBodyJson, CustomerOnboardRequest.class);

        try {
            customerDao.onboardCustomer(customerOnboardRequest.buildRequest());
            return ok(toJson("customer account created"));
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
    }
}
