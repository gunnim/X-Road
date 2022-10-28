/**
 * The MIT License
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

import ee.ria.xroad.common.TestCertUtil;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.niis.xroad.centralserver.restapi.dto.converter.CaInfoConverter;
import org.niis.xroad.centralserver.restapi.dto.converter.KeyUsageConverter;
import org.niis.xroad.centralserver.restapi.dto.converter.OcspResponderConverter;
import org.niis.xroad.cs.admin.api.dto.CertificateDetails;
import org.niis.xroad.cs.admin.api.dto.OcspResponder;
import org.niis.xroad.cs.admin.api.dto.OcspResponderRequest;
import org.niis.xroad.cs.admin.core.entity.ApprovedCaEntity;
import org.niis.xroad.cs.admin.core.entity.CaInfoEntity;
import org.niis.xroad.cs.admin.core.entity.OcspInfoEntity;
import org.niis.xroad.cs.admin.core.repository.ApprovedCaRepository;
import org.niis.xroad.cs.admin.core.repository.OcspInfoRepository;
import org.niis.xroad.restapi.config.audit.AuditDataHelper;

import java.time.Instant;
import java.util.Optional;

import static ee.ria.xroad.common.util.CryptoUtils.DEFAULT_CERT_HASH_ALGORITHM_ID;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.niis.xroad.cs.admin.api.dto.KeyUsageEnum.DIGITAL_SIGNATURE;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.OCSP_CERT_HASH;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.OCSP_CERT_HASH_ALGORITHM;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.OCSP_ID;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.OCSP_URL;

@ExtendWith(MockitoExtension.class)
class OcspRespondersServiceImplTest {
    private static final Integer ID = 123;
    private static final Instant VALID_FROM = Instant.now().minus(1, DAYS);
    private static final Instant VALID_TO = Instant.now().plus(1, DAYS);
    private static final String CA_NAME = "X-Road Test CA";
    private static final String CERT_PROFILE = "ee.ria.xroad.common.certificateprofile.impl.FiVRKCertificateProfileInfoProvider";

    @Mock
    private OcspInfoRepository ocspInfoRepository;
    @Mock
    private AuditDataHelper auditDataHelper;

    @Spy
    private OcspResponderConverter ocspResponderConverter = new OcspResponderConverter(mock(ApprovedCaRepository.class));

    @Spy
    private CaInfoConverter caInfoConverter = new CaInfoConverter(new KeyUsageConverter());

    @InjectMocks
    private OcspRespondersServiceImpl service;

    @Test
    void getCertificateDetails() {
        when(ocspInfoRepository.findById(ID)).thenReturn(Optional.of(ocspInfo()));

        final CertificateDetails certificateDetails = service.getOcspResponderCertificateDetails(ID);

        assertNotNull(certificateDetails);
        assertThat(certificateDetails.getKeyUsages()).contains(DIGITAL_SIGNATURE);
        assertEquals("Subject", certificateDetails.getSubjectCommonName());
        assertEquals("CN=Subject", certificateDetails.getSubjectDistinguishedName());
        assertEquals("Cyber", certificateDetails.getIssuerCommonName());
        assertEquals("1", certificateDetails.getSerial());
        assertEquals("SHA256withRSA", certificateDetails.getSignatureAlgorithm());
        assertEquals("EMAILADDRESS=aaa@bbb.ccc, CN=Cyber, OU=ITO, O=Cybernetica, C=EE",
                certificateDetails.getIssuerDistinguishedName());
    }

    @Test
    void update() throws Exception {
        final byte[] cert = TestCertUtil.getOcspSigner().certChain[0].getEncoded();
        final String newUrl = "http://new.url";
        final OcspResponderRequest request = new OcspResponderRequest()
                .setId(ID)
                .setUrl(newUrl)
                .setCertificate(cert);

        final OcspInfoEntity ocspInfo = ocspInfo();

        when(ocspInfoRepository.findById(ID)).thenReturn(Optional.of(ocspInfo));
        when(ocspInfoRepository.save(isA(OcspInfoEntity.class))).thenReturn(ocspInfo);

        final OcspResponder result = service.update(request);

        ArgumentCaptor<OcspInfoEntity> captor = ArgumentCaptor.forClass(OcspInfoEntity.class);
        verify(ocspInfoRepository).save(captor.capture());
        assertEquals(newUrl, captor.getValue().getUrl());
        assertEquals(cert, captor.getValue().getCert());

        assertEquals(newUrl, result.getUrl());

        assertAuditMessages(ocspInfo, newUrl);
    }

    @Test
    void updateOnlyUrl() {
        final String newUrl = "http://new.url";
        final OcspResponderRequest request = new OcspResponderRequest()
                .setId(ID)
                .setUrl(newUrl);

        final OcspInfoEntity ocspInfo = ocspInfo();
        final byte[] cert = ocspInfo.getCert();

        when(ocspInfoRepository.findById(ID)).thenReturn(Optional.of(ocspInfo));
        when(ocspInfoRepository.save(isA(OcspInfoEntity.class))).thenReturn(ocspInfo);

        final OcspResponder result = service.update(request);

        ArgumentCaptor<OcspInfoEntity> captor = ArgumentCaptor.forClass(OcspInfoEntity.class);
        verify(ocspInfoRepository).save(captor.capture());
        assertEquals(newUrl, captor.getValue().getUrl());
        assertEquals(cert, captor.getValue().getCert());

        assertEquals(newUrl, result.getUrl());

        assertAuditMessages(ocspInfo, newUrl);
    }

    private void assertAuditMessages(OcspInfoEntity ocspInfo, String url) {
        verify(auditDataHelper).put(OCSP_ID, ocspInfo.getId());
        verify(auditDataHelper).put(OCSP_URL, url);
        verify(auditDataHelper).put(eq(OCSP_CERT_HASH), isA(String.class));
        verify(auditDataHelper).put(OCSP_CERT_HASH_ALGORITHM, DEFAULT_CERT_HASH_ALGORITHM_ID);
    }

    @SneakyThrows
    private OcspInfoEntity ocspInfo() {
        CaInfoEntity caInfo = new CaInfoEntity();
        caInfo.setValidFrom(VALID_FROM);
        caInfo.setValidTo(VALID_TO);
        caInfo.setCert(TestCertUtil.generateAuthCert());
        ApprovedCaEntity ca = new ApprovedCaEntity();
        ca.setName(CA_NAME);
        ca.setAuthenticationOnly(true);
        ca.setCertProfileInfo(CERT_PROFILE);
        ca.setCaInfo(caInfo);
        return new OcspInfoEntity(caInfo, "https://flakyocsp:666", new byte[0]);
    }

}
