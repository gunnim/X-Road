/**
 * The MIT License
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
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
 */
package org.niis.xroad.securityserver.restapi.openapi;

import org.junit.Before;
import org.junit.Test;
import org.niis.xroad.restapi.domain.Role;
import org.niis.xroad.restapi.openapi.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.niis.xroad.securityserver.restapi.util.TestUtils.addApiKeyAuthorizationHeader;

/**
 * test user api with real rest requests
 *
 * If data source is altered with TestRestTemplate (e.g. POST, PUT or DELETE) in this test class,
 * please remember to mark the context dirty with the following annotation:
 * <code>@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)</code>
 */
public class UserApiControllerRestTemplateTest extends AbstractApiControllerTestContext {

    @Autowired
    TestRestTemplate restTemplate;

    @Before
    public void setup() {
        addApiKeyAuthorizationHeader(restTemplate);
    }

    @Test
    @WithMockUser(authorities = "VIEW_CLIENTS")
    public void testGetUser() {
        ResponseEntity<User> response = restTemplate.getForEntity("/api/v1/user", User.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("api-key-1", response.getBody().getUsername());
        assertEquals(Role.values().length, response.getBody().getRoles().size());
        List<String> allRoleNames = Arrays.stream(Role.values())
                .map(Role::getGrantedAuthorityName).collect(Collectors.toList());
        assertTrue(response.getBody().getRoles().containsAll(allRoleNames));
        assertTrue(response.getBody().getPermissions().contains("VIEW_CLIENTS"));
    }
}
