package com.hospital.management.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

final class ApiClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private String base = "http://localhost:8088";
    private String auth;

    CompletableFuture<String> login(String host, String username, String password) {
        try {
            String rawHost = host == null ? "" : host.trim();
            if (rawHost.isEmpty()) {
                rawHost = "http://localhost:8088";
            }
            if (!rawHost.startsWith("http://") && !rawHost.startsWith("https://")) {
                if (rawHost.contains("trycloudflare.com") || rawHost.contains("onrender.com") || rawHost.contains(".app") || rawHost.contains(".com")) {
                    rawHost = "https://" + rawHost;
                } else {
                    rawHost = "http://" + rawHost;
                }
            }
            URI endpoint = URI.create(rawHost).normalize();
            base = endpoint.toString().replaceAll("/$", "");
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Please enter a valid clinic server address (e.g. http://localhost:8088).", e));
        }
        setCredentials(username,password);
        return get("/api/me");
    }
    void clearCredentials() { auth=null; }
    void setCredentials(String username,String password) { auth="Basic "+Base64.getEncoder().encodeToString((username.trim()+":"+password).getBytes(StandardCharsets.UTF_8)); }
    CompletableFuture<String> get(String path) { return send("GET", path, null); }
    CompletableFuture<String> post(String path, Object body) { return sendJson("POST",path,body); }
    CompletableFuture<String> sendPatch(String path, Object body) { return sendJson("PATCH",path,body); }
    CompletableFuture<String> put(String path, Object body) { return sendJson("PUT",path,body); }

    private CompletableFuture<String> sendJson(String method,String path,Object body) {
        try { return send(method,path,json.writeValueAsString(body)); }
        catch (Exception e) { return CompletableFuture.failedFuture(new IllegalStateException("Could not prepare request",e)); }
    }
    private CompletableFuture<String> send(String method, String path, String body) {
        try {
            HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(12)).header("Authorization",auth).header("Accept","application/json");
            if(body==null) builder.method(method,HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(body));
            return client.sendAsync(builder.build(),HttpResponse.BodyHandlers.ofString()).thenApply(response->{
                if(response.statusCode()<200||response.statusCode()>=300) throw new IllegalStateException(response.body());
                return response.body();
            });
        } catch(Exception e) { return CompletableFuture.failedFuture(new IllegalStateException("Could not start server request",e)); }
    }
}
