package datadog.datadog.integration.controller;

import datadog.trace.api.CorrelationIdentifier;
import datadog.trace.api.GlobalTracer;
import datadog.trace.api.Trace;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class YourController {

    private static final Logger logger = LoggerFactory.getLogger(YourController.class);

    @GetMapping("/hello")
    public String hello(@RequestParam(required = false) String name) {
        try {
            // Add Datadog tracing information to the log context
            ThreadContext.put("dd.trace_id", CorrelationIdentifier.getTraceId());
            ThreadContext.put("dd.span_id", CorrelationIdentifier.getSpanId());
            ThreadContext.put("dd.service", "java-hello-0.0.1-SNAPSHOT");
            ThreadContext.put("dd.env", "production");
            ThreadContext.put("dd.version", "1.0.0");

            logger.info("Received request for /hello endpoint with name: {}", name);

            if (name == null || !"hello".equalsIgnoreCase(name)) {
                String errorMessage = "Invalid input: expected 'hello', but received: " + name;
                logger.error("Error: {}", errorMessage);
                return errorMessage;
            }

            logger.info("Processing the request...");
            return "Hello, Datadog!";

        } finally {
            // Clean up the thread context
            ThreadContext.clearAll();
        }
    }


    @Trace(operationName = "set.git.telemetry.tags")
    public void setGitTelemetryTags() {
        Tracer tracer = (Tracer) GlobalTracer.get();
        logger.info("Tracer instance: {}", tracer);  // Add this line to log the tracer instance

        if (tracer != null) {
            Span activeSpan = tracer.activeSpan();
            logger.info("Active span: {}", activeSpan);  // Add this line to log the active span

            if (activeSpan != null) {
                activeSpan.setTag("git.commit.sha", "9d2877b24555dbbeab72704d2bce203fd6603513");
                activeSpan.setTag("git.repository_url", "https://github.com/srishti1123/java-datadog.git");
                activeSpan.setTag("service.name", "java-hello-0.0.1-SNAPSHOT");
                logger.info("Git telemetry tags set successfully.");
            } else {
                logger.warn("No active span available to set Git telemetry tags.");
            }
        } else {
            logger.warn("Tracer is not available.");
        }
    }


    // Centralized error handling method
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        logger.error("An unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal Server Error: Please contact support.");
    }
}
