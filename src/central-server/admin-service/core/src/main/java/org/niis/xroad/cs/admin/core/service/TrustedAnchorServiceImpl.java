/*
 * The MIT License
 *
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
package org.niis.xroad.cs.admin.core.service;

import ee.ria.xroad.common.conf.globalconf.ConfigurationAnchorV2;
import ee.ria.xroad.common.conf.globalconf.ConfigurationLocation;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.niis.xroad.common.exception.NotFoundException;
import org.niis.xroad.common.exception.ValidationFailureException;
import org.niis.xroad.cs.admin.api.domain.TrustedAnchor;
import org.niis.xroad.cs.admin.api.service.TrustedAnchorService;
import org.niis.xroad.cs.admin.core.entity.TrustedAnchorEntity;
import org.niis.xroad.cs.admin.core.entity.mapper.TrustedAnchorMapper;
import org.niis.xroad.cs.admin.core.repository.TrustedAnchorRepository;
import org.niis.xroad.restapi.config.audit.AuditDataHelper;
import org.niis.xroad.restapi.service.ConfigurationVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static ee.ria.xroad.common.util.CryptoUtils.DEFAULT_ANCHOR_HASH_ALGORITHM_ID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.niis.xroad.cs.admin.api.exception.ErrorMessage.MALFORMED_ANCHOR;
import static org.niis.xroad.cs.admin.api.exception.ErrorMessage.TRUSTED_ANCHOR_NOT_FOUND;
import static org.niis.xroad.cs.admin.api.exception.ErrorMessage.TRUSTED_ANCHOR_VERIFICATION_FAILED;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.ANCHOR_FILE_HASH;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.ANCHOR_FILE_HASH_ALGORITHM;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.ANCHOR_URLS;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.GENERATED_AT;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.INSTANCE_IDENTIFIER;

@Service
@RequiredArgsConstructor
@Transactional
class TrustedAnchorServiceImpl implements TrustedAnchorService {

    private final TrustedAnchorRepository trustedAnchorRepository;
    private final TrustedAnchorMapper trustedAnchorMapper;
    private final AuditDataHelper auditDataHelper;
    private final ConfigurationVerifier configurationVerifier;

    @Value("${script.external-configuration-verifier.path}")
    private final String configVerifierScriptPath;

    @Override
    public List<TrustedAnchor> findAll() {
        return trustedAnchorRepository.findAll().stream()
                .map(trustedAnchorMapper::toTarget)
                .collect(toList());
    }

    @Override
    public TrustedAnchor preview(byte[] trustedAnchorFile) {
        try {
            final ConfigurationAnchorV2 anchorV2 = new ConfigurationAnchorV2(trustedAnchorFile);
            return trustedAnchorMapper.map(anchorV2, trustedAnchorFile);
        } catch (Exception e) {
            throw new ValidationFailureException(MALFORMED_ANCHOR);
        }
    }

    @Override
    public TrustedAnchor upload(byte[] trustedAnchor) {
        auditDataHelper.putAnchorHash(trustedAnchor);

        final ConfigurationAnchorV2 anchorV2 = new ConfigurationAnchorV2(trustedAnchor);

        auditDataHelper.put(INSTANCE_IDENTIFIER, anchorV2.getInstanceIdentifier());
        auditDataHelper.putDate(GENERATED_AT, anchorV2.getGeneratedAt());
        auditDataHelper.put(ANCHOR_URLS, anchorV2.getLocations().stream()
                .map(ConfigurationLocation::getDownloadURL)
                .collect(toSet()));

        validateTrustedAnchor(trustedAnchor);

        final TrustedAnchorEntity entity = trustedAnchorRepository.findFirstByInstanceIdentifier(anchorV2.getInstanceIdentifier())
                .map(existing -> trustedAnchorMapper.toEntity(anchorV2, trustedAnchor, existing))
                .orElse(trustedAnchorMapper.toEntity(anchorV2, trustedAnchor, new TrustedAnchorEntity()));

        final TrustedAnchorEntity saved = trustedAnchorRepository.save(entity);
        return trustedAnchorMapper.toTarget(saved);
    }

    @Override
    public TrustedAnchor findByHash(String hash) {
        return trustedAnchorRepository.findFirstByTrustedAnchorHash(hash)
                .map(trustedAnchorMapper::toTarget)
                .orElseThrow(() -> new NotFoundException(TRUSTED_ANCHOR_NOT_FOUND));
    }

    @Override
    public void delete(String hash) {
        final TrustedAnchorEntity trustedAnchor = trustedAnchorRepository.findFirstByTrustedAnchorHash(hash)
                .orElseThrow(() -> new NotFoundException(TRUSTED_ANCHOR_NOT_FOUND));

        auditDataHelper.put(INSTANCE_IDENTIFIER, trustedAnchor.getInstanceIdentifier());
        auditDataHelper.put(ANCHOR_FILE_HASH, trustedAnchor.getTrustedAnchorHash());
        auditDataHelper.put(ANCHOR_FILE_HASH_ALGORITHM, DEFAULT_ANCHOR_HASH_ALGORITHM_ID);

        trustedAnchorRepository.delete(trustedAnchor);
    }

    private void validateTrustedAnchor(byte[] trustedAnchor) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("temp-trusted-anchor-", ".xml");
            Files.write(tempFile, trustedAnchor);
            configurationVerifier.verifyConfiguration(configVerifierScriptPath, tempFile.toAbsolutePath().toString());
        } catch (Exception e) {
            throw new ValidationFailureException(TRUSTED_ANCHOR_VERIFICATION_FAILED, e);
        } finally {
            if (tempFile != null) {
                FileUtils.deleteQuietly(tempFile.toFile());
            }
        }
    }
}
