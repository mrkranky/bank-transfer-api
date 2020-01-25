package controllers;

import beans.request.TransferRequestBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import services.TransferService;
import utils.JsonUtil;

public class TransferController extends Controller {
    private final TransferService transferService;

    @Inject
    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @Transactional
    public Result transfer() {
        final JsonNode requestBodyJson = request().body().asJson();

        if (requestBodyJson == null)
            return Results.badRequest("Transfer request cannot be empty");

        TransferRequestBean transferRequestBean = JsonUtil.parseJson(requestBodyJson, TransferRequestBean.class);

        try {
            transferService.transfer(transferRequestBean);
            return ok("Transfer success");
        } catch (Exception e) {
            return internalServerError("Transfer failed - " + e);
        }
    }
}
