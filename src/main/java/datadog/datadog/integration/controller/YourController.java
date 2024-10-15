package datadog.datadog.integration.controller;

import datadog.trace.api.CorrelationIdentifier;
import datadog.trace.api.Trace;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class YourController {

    // Create a logger instance
    private static final Logger logger = LoggerFactory.getLogger(YourController.class);

    @GetMapping("/hello")
    @Trace(operationName = "hello.request")
    public String hello(@RequestParam(required = false) String name) {
        try {
            // Inject Datadog trace ID and span ID into the logs
            ThreadContext.put("dd.trace_id", CorrelationIdentifier.getTraceId());
            ThreadContext.put("dd.span_id", CorrelationIdentifier.getSpanId());

            // Log request details
            logger.info("Received request for /hello endpoint with name: {}", name);

            // Validate the input
            if (name == null || !name.equalsIgnoreCase("hello")) {
                // Log an error if the input is invalid and pass the exception as the last argument
                String errorMessage = "Invalid input: expected 'hello', but received: " + name;
                logger.error("An error occurred: {}", errorMessage);

                try {
                    throw new Exception(errorMessage);
                } catch (Exception e) {
                    // Log the stack trace of the exception
                    logger.error("An exception occurred", e);
                }

                // Return error response
                return "Error: " + errorMessage;
            }

            // Simulate some processing
            logger.info("Processing the request...");

            // Return the response
            String response = "Hello, Datadog!";
            logger.info("Generated response: {}", response);

            // Log completion of request processing
            logger.info("Completed processing for /hello");

            return response;
        } finally {
            // Clean up the trace and span ID after logging
            ThreadContext.remove("dd.trace_id");
            ThreadContext.remove("dd.span_id");
        }
    }
}
