package utils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Configuration;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {

    @Inject
    public ErrorHandler(Configuration configuration, Environment environment, OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        super(configuration, environment, sourceMapper, routes);
    }

    @Override
    public CompletionStage<Result> onServerError(Http.RequestHeader request, Throwable exception) {
        ObjectNode jsonError = Json.newObject();

        jsonError.set("cause", exceptionToJson(exception));

        return CompletableFuture.completedFuture(Results.internalServerError(jsonError));
    }

    private ArrayNode exceptionToJson(Throwable throwable) {
        ArrayNode causesNode = JsonNodeFactory.instance.arrayNode();

        while (throwable != null) {
            ObjectNode causeNode = causesNode.addObject();
            causeNode.put("message", throwable.getMessage());
            causeNode.put("type", throwable.getClass().getName());

            throwable = throwable.getCause();
        }

        return causesNode;
    }
}