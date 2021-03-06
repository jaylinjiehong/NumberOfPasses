/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package jdk.internal.net.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Utils;

class RedirectFilter implements HeaderFilter {

    HttpRequestImpl request;
    HttpClientImpl client;
    HttpClient.Redirect policy;
    String method;
    MultiExchange<?> exchange;
    static final int DEFAULT_MAX_REDIRECTS = 5;
    URI uri;

    /*
     * NOT_MODIFIED status code results from a conditional GET where
     * the server does not (must not) return a response body because
     * the condition specified in the request disallows it
     */
    static final int HTTP_NOT_MODIFIED = 304;

    static final int max_redirects = Utils.getIntegerNetProperty(
            "jdk.httpclient.redirects.retrylimit", DEFAULT_MAX_REDIRECTS
    );

    // A public no-arg constructor is required by FilterFactory
    public RedirectFilter() {}

    @Override
    public synchronized void request(HttpRequestImpl r, MultiExchange<?> e) throws IOException {
        this.request = r;
        this.client = e.client();
        this.policy = client.followRedirects();

        this.method = r.method();
        this.uri = r.uri();
        this.exchange = e;
    }

    @Override
    public synchronized HttpRequestImpl response(Response r) throws IOException {
        return handleResponse(r);
    }

    private static String redirectedMethod(int statusCode, String orig) {
        switch (statusCode) {
            case 301:
            case 302:
                return orig.equals("POST") ? "GET" : orig;
            case 303:
                return "GET";
            case 307:
            case 308:
                return orig;
            default:
                // unexpected but return orig
                return orig;
        }
    }

    private static boolean isRedirecting(int statusCode) {
        // < 300: not a redirect codes
        if (statusCode < 300) return false;
        // 309-399 Unassigned => don't follow
        // > 399: not a redirect code
        if (statusCode > 308) return false;
        switch (statusCode) {
            // 300: MultipleChoice => don't follow
            case 300:
                return false;
            // 304: Not Modified => don't follow
            case 304:
                return false;
            // 305: Proxy Redirect => don't follow.
            case 305:
                return false;
            // 306: Unused => don't follow
            case 306:
                return false;
            // 301, 302, 303, 307, 308: OK to follow.
            default:
                return true;
        }
    }

    /**
     * Checks to see if a new request is needed and returns it.
     * Null means response is ok to return to user.
     */
    private HttpRequestImpl handleResponse(Response r) {
        int rcode = r.statusCode();
        if (rcode == 200 || policy == HttpClient.Redirect.NEVER) {
            return null;
        }

        if (rcode == HTTP_NOT_MODIFIED)
            return null;

        if (isRedirecting(rcode)) {
            URI redir = getRedirectedURI(r.headers());
            String newMethod = redirectedMethod(rcode, method);
            Log.logTrace("response code: {0}, redirected URI: {1}", rcode, redir);
            if (canRedirect(redir) && ++exchange.numberOfRedirects < max_redirects) {
                Log.logTrace("redirect to: {0} with method: {1}", redir, newMethod);
                return HttpRequestImpl.newInstanceForRedirection(redir, newMethod, request, rcode != 303);
            } else {
                Log.logTrace("not redirecting");
                return null;
            }
        }
        return null;
    }

    private URI getRedirectedURI(HttpHeaders headers) {
        URI redirectedURI;
        redirectedURI = headers.firstValue("Location")
                .map(URI::create)
                .orElseThrow(() -> new UncheckedIOException(
                        new IOException("Invalid redirection")));

        // redirect could be relative to original URL, but if not
        // then redirect is used.
        redirectedURI = uri.resolve(redirectedURI);
        return redirectedURI;
    }

    private boolean canRedirect(URI redir) {
        String newScheme = redir.getScheme();
        String oldScheme = uri.getScheme();
        switch (policy) {
            case ALWAYS:
                return true;
            case NEVER:
                return false;
            case NORMAL:
                return newScheme.equalsIgnoreCase(oldScheme)
                        || newScheme.equalsIgnoreCase("https");
            default:
                throw new InternalError();
        }
    }
}
