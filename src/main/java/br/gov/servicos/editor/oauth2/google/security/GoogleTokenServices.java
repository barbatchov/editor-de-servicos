/*******************************************************************************
 * Cloud Foundry
 * Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 * <p>
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 * <p>
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package br.gov.servicos.editor.oauth2.google.security;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;
import static org.springframework.security.crypto.codec.Base64.encode;

/**
 * Copied from Spring Security OAuth2 to support the custom format for a Google Token which is different from what Spring supports
 */
@Slf4j
@FieldDefaults(level = PRIVATE)
public class GoogleTokenServices extends RemoteTokenServices {

    RestOperations restTemplate;

    @Setter
    String checkTokenEndpointUrl;

    @Setter
    String clientId;

    @Setter
    String clientSecret;

    AccessTokenConverter tokenConverter = new GoogleAccessTokenConverter();

    public GoogleTokenServices() {
        restTemplate = new RestTemplate();
        ((RestTemplate) restTemplate).setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            // Ignore 400
            public void handleError(ClientHttpResponse response) throws IOException {
                if (response.getRawStatusCode() != 400) {
                    super.handleError(response);
                }
            }
        });
    }

    public void setAccessTokenConverter(AccessTokenConverter accessTokenConverter) {
        this.tokenConverter = accessTokenConverter;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        Map<String, Object> checkTokenResponse = checkToken(accessToken);

        if (checkTokenResponse.containsKey("error")) {
            logger.debug("check_token returned error: " + checkTokenResponse.get("error"));
            throw new InvalidTokenException(accessToken);
        }

        transformNonStandardValuesToStandardValues(checkTokenResponse);

        Assert.state(checkTokenResponse.containsKey("client_id"), "Client id must be present in response from auth server");
        return tokenConverter.extractAuthentication(checkTokenResponse);
    }

    private Map<String, Object> checkToken(String accessToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("token", accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader(clientId, clientSecret));

        return postForMap(checkTokenEndpointUrl + "?access_token=" + accessToken, formData, headers);
    }

    private void transformNonStandardValuesToStandardValues(Map<String, Object> map) {
        map.put("client_id", map.get("issued_to")); // Google sends 'client_id' as 'issued_to'
        map.put("user_name", map.get("user_id")); // Google sends 'user_name' as 'user_id'
    }

    @SneakyThrows
    private String authorizationHeader(String clientId, String clientSecret) {
        return "Basic " + new String(encode(format("%s:%s", clientId, clientSecret).getBytes("UTF-8")));
    }

    private Map<String, Object> postForMap(String path, MultiValueMap<String, String> formData, HttpHeaders headers) {
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }

        ParameterizedTypeReference<Map<String, Object>> map = new ParameterizedTypeReference<Map<String, Object>>() {
        };
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(formData, headers), map).getBody();
    }
}