/*
 * The MIT License
 *
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.niis.xroad.centralserver.restapi.service.exception.NotFoundException;
import org.niis.xroad.centralserver.restapi.validation.UrlValidator;
import org.niis.xroad.cs.admin.api.domain.ApprovedTsa;
import org.niis.xroad.cs.admin.core.entity.ApprovedTsaEntity;
import org.niis.xroad.cs.admin.core.entity.mapper.ApprovedTsaMapper;
import org.niis.xroad.cs.admin.core.repository.ApprovedTsaRepository;
import org.niis.xroad.restapi.config.audit.AuditDataHelper;

import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;

import static ee.ria.xroad.common.util.CryptoUtils.DEFAULT_CERT_HASH_ALGORITHM_ID;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.TSA_CERT_HASH;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.TSA_CERT_HASH_ALGORITHM;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.TSA_ID;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.TSA_NAME;
import static org.niis.xroad.restapi.config.audit.RestApiAuditProperty.TSA_URL;

@ExtendWith(MockitoExtension.class)
class TimestampingServicesServiceImplTest {

    private static final String URL = "http://test.url";
    private static final X509Certificate CERTIFICATE = TestCertUtil.getTspCert();
    private static final Integer ID = 123;
    private static final String NAME = "test TSA name";

    @Mock
    private ApprovedTsaRepository approvedTsaRepository;
    @Mock
    private ApprovedTsaMapper approvedTsaMapper;
    @Mock
    private AuditDataHelper auditDataHelper;
    @Mock
    private UrlValidator urlValidator;
    @Mock
    private ApprovedTsaEntity approvedTsaEntity;
    @Mock
    private ApprovedTsa approvedTsa;

    @InjectMocks
    private TimestampingServicesServiceImpl timestampingServicesService;

    @Test
    void getTimestampingServices() {
        when(approvedTsaRepository.findAll()).thenReturn(of(mock(ApprovedTsaEntity.class), mock(ApprovedTsaEntity.class)));
        when(approvedTsaMapper.toTarget(isA(ApprovedTsaEntity.class))).thenReturn(mock(ApprovedTsa.class), mock(ApprovedTsa.class));

        final Set<ApprovedTsa> timestampingServices = timestampingServicesService.getTimestampingServices();

        assertThat(timestampingServices.size()).isEqualTo(2);
        verify(approvedTsaMapper, times(2)).toTarget(isA(ApprovedTsaEntity.class));
    }

    @Test
    void add() throws Exception {
        when(approvedTsaMapper.toEntity(URL, CERTIFICATE.getEncoded())).thenReturn(approvedTsaEntity);
        when(approvedTsaRepository.save(approvedTsaEntity)).thenReturn(approvedTsaEntity);
        when(approvedTsaEntity.getId()).thenReturn(ID);
        when(approvedTsaEntity.getName()).thenReturn(NAME);
        when(approvedTsaEntity.getUrl()).thenReturn(URL);
        when(approvedTsaEntity.getCert()).thenReturn(CERTIFICATE.getEncoded());

        timestampingServicesService.add(URL, CERTIFICATE.getEncoded());

        verify(urlValidator).validateUrl(URL);

        verify(auditDataHelper).put(TSA_ID, ID);
        verify(auditDataHelper).put(TSA_NAME, NAME);
        verify(auditDataHelper).put(TSA_URL, URL);
        verify(auditDataHelper).put(TSA_CERT_HASH, "05:A1:0E:EB:DB:0C:D9:67:9E:4C:85:A7:88:48:14:5E:F1:F0:0B:EA");
        verify(auditDataHelper).put(TSA_CERT_HASH_ALGORITHM, DEFAULT_CERT_HASH_ALGORITHM_ID);
    }

    @Test
    void get() {
        when(approvedTsaRepository.findById(ID)).thenReturn(Optional.of(approvedTsaEntity));
        when(approvedTsaMapper.toTarget(approvedTsaEntity)).thenReturn(approvedTsa);

        final ApprovedTsa timestampingService = timestampingServicesService.get(ID);

        verify(approvedTsaRepository).findById(ID);

        assertEquals(approvedTsa, timestampingService);
    }

    @Test
    void delete() {
        when(approvedTsaRepository.findById(ID)).thenReturn(Optional.of(approvedTsaEntity));
        when(approvedTsaEntity.getId()).thenReturn(ID);
        when(approvedTsaEntity.getName()).thenReturn(NAME);
        when(approvedTsaEntity.getUrl()).thenReturn(URL);

        timestampingServicesService.delete(ID);

        verify(approvedTsaRepository).delete(approvedTsaEntity);

        verify(auditDataHelper).put(TSA_ID, ID);
        verify(auditDataHelper).put(TSA_NAME, NAME);
        verify(auditDataHelper).put(TSA_URL, URL);
    }

    @Test
    void deleteShouldThrowNotFoundException() {
        when(approvedTsaRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timestampingServicesService.delete(ID)).isInstanceOf(NotFoundException.class);
        verifyNoMoreInteractions(approvedTsaRepository, auditDataHelper);
    }

}
