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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class YourController {

    // Create a logger instance
    private static final Logger logger = LoggerFactory.getLogger(YourController.class);

    // Injecting properties from application.properties
    @Value("${DD_GIT_COMMIT_SHA}")
    private String gitCommitSha;

    @Value("${DD_GIT_REPOSITORY_URL}")
    private String gitRepositoryUrl;

    @Value("${spring.application.name}")
    private String serviceName;

    @GetMapping("/hello")
    @Trace(operationName = "hello.request")
    public String hello(@RequestParam(required = false) String name) {
        try {
            // Inject Datadog trace ID and span ID into the logs
            ThreadContext.put("dd.trace_id", CorrelationIdentifier.getTraceId());
            ThreadContext.put("dd.span_id", CorrelationIdentifier.getSpanId());

            // Log request details
            logger.info("Received request for /hello endpoint with name: {}", name);

            // Set Git commit SHA and repository URL for telemetry
            setGitTelemetryTags();

            // Validate the input
            if (!"hello".equalsIgnoreCase(name)) {
                // Log an error if the input is invalid
                String errorMessage = "Invalid input: expected 'hello', but received: " + name;
                logger.error("An error occurred: {}", errorMessage);
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

    // Method to set Git commit SHA, repository URL, environment, and version tags
    @Trace(operationName = "set.git.telemetry.tags")
    public void setGitTelemetryTags() {
        // Get the current tracer
        Tracer tracer = (Tracer) GlobalTracer.get();

        // Check if the tracer is available
        if (tracer != null) {
            // Get the active span
            Span activeSpan = tracer.activeSpan();
            if (activeSpan != null) {
                // Set Git commit SHA and repository URL tags
                activeSpan.setTag("git.commit.sha", "9d2877b24555dbbeab72704d2bce203fd6603513");
                activeSpan.setTag("git.repository_url", "https://github.com/srishti1123/java-datadog.git");
                activeSpan.setTag("service.name", "java-hello");

                // Add environment and version as tags
//                activeSpan.setTag("env", "production");
//                activeSpan.setTag("version", "v2");

                logger.info("Git telemetry tags set successfully: commit SHA={}, repository URL={}, service name={}",
                        gitCommitSha, gitRepositoryUrl, serviceName);
            } else {
                logger.warn("No active span available to set Git telemetry tags.");
            }
        } else {
            logger.warn("Tracer is not available.");
        }
    }

}
