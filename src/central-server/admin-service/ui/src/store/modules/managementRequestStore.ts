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

import { defineStore } from 'pinia';
import { DataOptions } from 'vuetify';
import axios, { AxiosRequestConfig } from 'axios';
import {
  ManagementRequest,
  ManagementRequestListView,
  ManagementRequestsFilter,
  PagedManagementRequests,
  PagingMetadata,
} from '@/openapi-types';

export interface State {
  currentManagementRequest: ManagementRequest | null;
  items: ManagementRequestListView[];
  pagingOptions: PagingMetadata;
}

export const managementRequestsStore = defineStore('managementRequests', {
  state: (): State => ({
    currentManagementRequest: null,
    items: [],
    pagingOptions: {
      total_items: 0,
      items: 0,
      limit: 25,
      offset: 0,
    },
  }),
  getters: {},
  actions: {
    async find(dataOptions: DataOptions, filter: ManagementRequestsFilter) {
      const offset = dataOptions?.page == null ? 0 : dataOptions.page - 1;
      const params: unknown = {
        limit: dataOptions.itemsPerPage,
        offset: offset,
        sort: dataOptions.sortBy[0],
        desc: dataOptions.sortDesc[0],
        ...filter,
      };

      const axiosParams: AxiosRequestConfig = { params };

      return axios
        .get<PagedManagementRequests>('/management-requests/', axiosParams)
        .then((resp) => {
          this.items = resp.data.items || [];
          this.pagingOptions = resp.data.paging_metadata;
        });
    },
    loadById(requestId: number) {
      return axios
        .get<ManagementRequest>(`/management-requests/${requestId}`)
        .then((resp) => {
          this.currentManagementRequest = resp.data;
        })
        .catch((error) => {
          throw error;
        });
    },
    approve(id: number) {
      return axios.post<ManagementRequest>(
        `/management-requests/${id}/approval`,
      );
    },
    decline(id: number) {
      return axios.delete(`/management-requests/${id}`);
    },
  },
});
