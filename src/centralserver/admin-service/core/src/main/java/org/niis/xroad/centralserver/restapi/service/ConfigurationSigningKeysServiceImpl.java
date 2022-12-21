/*
 * The MIT License
 * <p>
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.centralserver.restapi.service;

import ee.ria.xroad.signer.protocol.dto.TokenInfo;

import lombok.RequiredArgsConstructor;
import org.niis.xroad.centralserver.restapi.service.exception.NotFoundException;
import org.niis.xroad.centralserver.restapi.service.exception.SigningKeyException;
import org.niis.xroad.cs.admin.api.domain.ConfigurationSigningKey;
import org.niis.xroad.cs.admin.api.facade.SignerProxyFacade;
import org.niis.xroad.cs.admin.api.service.ConfigurationSigningKeysService;
import org.niis.xroad.cs.admin.core.entity.mapper.ConfigurationSigningKeyMapper;
import org.niis.xroad.cs.admin.core.repository.ConfigurationSigningKeyRepository;
import org.niis.xroad.restapi.config.audit.AuditDataHelper;
import org.niis.xroad.restapi.config.audit.AuditEventHelper;
import org.niis.xroad.restapi.config.audit.RestApiAuditProperty;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static org.niis.xroad.centralserver.restapi.service.exception.ErrorMessage.ACTIVE_SIGNING_KEY_CANNOT_BE_DELETED;
import static org.niis.xroad.centralserver.restapi.service.exception.ErrorMessage.ERROR_DELETING_SIGNING_KEY;
import static org.niis.xroad.centralserver.restapi.service.exception.ErrorMessage.SIGNING_KEY_NOT_FOUND;
import static org.niis.xroad.cs.admin.api.domain.ConfigurationSourceType.EXTERNAL;
import static org.niis.xroad.cs.admin.api.domain.ConfigurationSourceType.INTERNAL;
import static org.niis.xroad.restapi.config.audit.RestApiAuditEvent.DELETE_EXTERNAL_CONFIGURATION_SIGNING_KEY;
import static org.niis.xroad.restapi.config.audit.RestApiAuditEvent.DELETE_INTERNAL_CONFIGURATION_SIGNING_KEY;

@Service
@Transactional
@RequiredArgsConstructor
public class ConfigurationSigningKeysServiceImpl implements ConfigurationSigningKeysService {

    private final ConfigurationSigningKeyRepository configurationSigningKeyRepository;
    private final ConfigurationSigningKeyMapper configurationSigningKeyMapper;
    private final AuditEventHelper auditEventHelper;
    private final AuditDataHelper auditDataHelper;
    private final SignerProxyFacade signerProxyFacade;

    @Override
    public List<ConfigurationSigningKey> findByTokenIdentifier(String tokenIdentifier) {
        return configurationSigningKeyRepository.findByTokenIdentifier(tokenIdentifier).stream()
                .map(configurationSigningKeyMapper::toTarget).collect(Collectors.toList());
    }

    @Override
    public void deleteKey(String identifier) {
        ConfigurationSigningKey signingKey = configurationSigningKeyRepository.findByKeyIdentifier(identifier)
                .map(configurationSigningKeyMapper::toTarget)
                .orElseThrow(() -> new NotFoundException(SIGNING_KEY_NOT_FOUND));

        if (signingKey.isActiveSourceSigningKey()) {
            throw new SigningKeyException(ACTIVE_SIGNING_KEY_CANNOT_BE_DELETED);
        }

        if (signingKey.getSourceType() == INTERNAL) {
            auditEventHelper.changeRequestScopedEvent(DELETE_INTERNAL_CONFIGURATION_SIGNING_KEY);
        } else if (signingKey.getSourceType() == EXTERNAL) {
            auditEventHelper.changeRequestScopedEvent(DELETE_EXTERNAL_CONFIGURATION_SIGNING_KEY);
        }
        auditDataHelper.put(RestApiAuditProperty.TOKEN_ID, signingKey.getTokenIdentifier());
        auditDataHelper.put(RestApiAuditProperty.KEY_ID, signingKey.getKeyIdentifier());
        try {
            TokenInfo tokenInfo = signerProxyFacade.getToken(signingKey.getTokenIdentifier());
            auditDataHelper.put(RestApiAuditProperty.TOKEN_SERIAL_NUMBER, tokenInfo.getSerialNumber());
            auditDataHelper.put(RestApiAuditProperty.TOKEN_FRIENDLY_NAME, tokenInfo.getFriendlyName());

            configurationSigningKeyRepository.deleteByKeyIdentifier(identifier);
            signerProxyFacade.deleteKey(signingKey.getKeyIdentifier(), true);
        } catch (Exception e) {
            throw new SigningKeyException(ERROR_DELETING_SIGNING_KEY, e);
        }

    }


}
