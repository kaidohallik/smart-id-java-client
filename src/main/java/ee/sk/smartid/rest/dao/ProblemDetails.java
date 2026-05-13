package ee.sk.smartid.rest.dao;

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

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents an error response in RFC 9457 Problem Details for HTTP APIs format.
 * <p>
 * type - URI reference that identifies the problem type.
 * title - Short, human-readable summary of the problem type.
 * status - HTTP status code.
 * detail - Human-readable explanation specific to this occurrence of the problem.
 * instance - URI reference that identifies the specific occurrence of the problem.
 * errors - List of individual errors that contributed to this problem.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProblemDetails implements Serializable {

    private String type;
    private String title;
    private Integer status;
    private String detail;
    private String instance;
    private List<ProblemError> errors;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public List<ProblemError> getErrors() {
        return errors;
    }

    public void setErrors(List<ProblemError> errors) {
        this.errors = errors;
    }

    /**
     * Represents an individual error entry within a Problem Details response.
     * <p>
     * code - Application-specific error code, e.g. "NO_SUITABLE_ACCOUNT_FOUND".
     * detail - Human-readable explanation of this specific error.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProblemError implements Serializable {

        private String code;
        private String detail;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }
}
