package controllers;

import com.google.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller {

    @Inject
    public Result index() {
        return ok("Your new application is ready.");
    }
}
