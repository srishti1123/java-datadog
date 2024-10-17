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

    @Value("${DD_GIT_COMMIT_SHA}")
    private String gitCommitSha;

    @Value("${DD_GIT_REPOSITORY_URL}")
    private String gitRepositoryUrl;

    @Value("${spring.application.name}")
    private String serviceName;

    @GetMapping("/hello")
    @Trace(operationName = "hello.request")
    public ResponseEntity<String> hello(@RequestParam(required = false) String name) {
        try {
            // Attach Datadog trace and span ids to the logs
            ThreadContext.put("dd.trace_id", CorrelationIdentifier.getTraceId());
            ThreadContext.put("dd.span_id", CorrelationIdentifier.getSpanId());

            logger.info("Received request for /hello endpoint with name: {}", name);

            // Set Git telemetry tags
            setGitTelemetryTags();

            if (name == null || !"hello".equalsIgnoreCase(name)) {
                String errorMessage = "Invalid input: expected 'hello', but received: " + name;
                logger.error("Error: {}", errorMessage);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
            }

            logger.info("Processing the request...");
            String response = "Hello, Datadog!";
            logger.info("Generated response: {}", response);
            logger.info("Completed processing for /hello");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("An internal error occurred while processing the request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error: Please try again later.");
        } finally {
            // Clean up trace and span ids from the ThreadContext
            ThreadContext.remove("dd.trace_id");
            ThreadContext.remove("dd.span_id");
        }
    }

    @Trace(operationName = "set.git.telemetry.tags")
    public void setGitTelemetryTags() {
        Tracer tracer = (Tracer) GlobalTracer.get();

        if (tracer != null) {
            Span activeSpan = tracer.activeSpan();
            if (activeSpan != null) {
                activeSpan.setTag("git.commit.sha", "9d2877b24555dbbeab72704d2bce203fd6603513");
                activeSpan.setTag("git.repository_url", "https://github.com/srishti1123/java-datadog.git");
                activeSpan.setTag("service.name", "java-hello-0.0.1-SNAPSHOT");

                logger.info("Git telemetry tags set successfully: commit SHA={}, repository URL={}, service name={}",
                        gitCommitSha, gitRepositoryUrl, serviceName);
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
