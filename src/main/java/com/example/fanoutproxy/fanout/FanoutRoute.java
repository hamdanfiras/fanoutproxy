package com.example.fanoutproxy.fanout;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class FanoutRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:fanout")
                .routeId("fanout-rule-dispatch")
                .bean(FanoutService.class, "fanout");
    }
}
