package org.shield.webserver.offer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;


@Configuration
public class BondMonitorRouter {

    @Bean
    public RouterFunction<ServerResponse> route(OfferController offerController) {
        return RouterFunctions.route(GET("/hello").and(accept(MediaType.ALL)), offerController::hello);
    }


}
