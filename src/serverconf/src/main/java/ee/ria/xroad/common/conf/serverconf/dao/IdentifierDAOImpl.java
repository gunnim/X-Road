/*
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
package ee.ria.xroad.common.conf.serverconf.dao;

import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.XRoadId;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;

import java.util.List;

/**
 * Identifier data access object implementation.
 */
public class IdentifierDAOImpl extends AbstractDAOImpl<XRoadId.Conf> {

    /**
     * Finds a (local) client identifier corresponding the example or null if none exits
     */
    public ClientId.Conf findClientId(Session session, ClientId example) {
        final CriteriaBuilder cb = session.getCriteriaBuilder();
        final CriteriaQuery<ClientId.Conf> query = cb.createQuery(ClientId.Conf.class);
        final Root<ClientId.Conf> from = query.from(ClientId.Conf.class);

        Predicate pred = cb.and(
                cb.equal(from.get("xRoadInstance"), example.getXRoadInstance()),
                cb.equal(from.get("memberClass"), example.getMemberClass()),
                cb.equal(from.get("memberCode"), example.getMemberCode()));
        if (example.getSubsystemCode() == null) {
            pred = cb.and(pred, cb.isNull(from.get("subsystemCode")));
        } else {
            pred = cb.and(pred, cb.equal(from.get("subsystemCode"), example.getSubsystemCode()));
        }

        final List<ClientId.Conf> list = session.createQuery(query.select(from).where(pred))
                .setMaxResults(1)
                .setCacheable(true)
                .getResultList();

        return list.isEmpty() ? null : list.get(0);
    }
}
