package ee.sk.smartid.exception.useraccount;

/*-
 * #%L
 * Smart ID sample Java client
 * %%
 * Copyright (C) 2018 - 2026 SK ID Solutions AS
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.stream.Collectors;

import ee.sk.smartid.exception.UserAccountException;
import ee.sk.smartid.rest.dao.ProblemDetails;

/**
 * Thrown when user account does not exist with the given identifier or document number.
 * <p>
 * When the server returns an RFC 9457 problem-details payload along with the 404 response,
 * the parsed {@link ProblemDetails} is exposed via {@link #getProblemDetails()} so callers
 * can inspect the error codes (e.g. {@code NO_SUITABLE_ACCOUNT_FOUND}) returned by the server.
 */
public class UserAccountNotFoundException extends UserAccountException {

    private final transient ProblemDetails problemDetails;

    /**
     * Constructs the exception without problem details.
     */
    public UserAccountNotFoundException() {
        this(null);
    }

    /**
     * Constructs the exception with the problem details parsed from the server response.
     *
     * @param problemDetails problem details parsed from the response body, or {@code null} if unavailable.
     */
    public UserAccountNotFoundException(ProblemDetails problemDetails) {
        super(buildMessage(problemDetails));
        this.problemDetails = problemDetails;
    }

    /**
     * @return problem details from the server response, or {@code null} if the response had no parseable payload.
     */
    public ProblemDetails getProblemDetails() {
        return problemDetails;
    }

    private static String buildMessage(ProblemDetails problemDetails) {
        if (problemDetails == null || problemDetails.getErrors() == null || problemDetails.getErrors().isEmpty()) {
            return "User account not found";
        }
        String errors = problemDetails.getErrors().stream()
                .map(error -> error.getCode() + " - " + error.getDetail())
                .collect(Collectors.joining("; "));
        return "User account not found: " + errors;
    }
}
