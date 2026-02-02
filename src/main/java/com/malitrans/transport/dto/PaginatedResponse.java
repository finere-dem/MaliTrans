package com.malitrans.transport.dto;

import java.util.List;

public class PaginatedResponse<T> {
    private List<T> data;
    private Meta meta;

    public PaginatedResponse() {
    }

    public PaginatedResponse(List<T> data, Meta meta) {
        this.data = data;
        this.meta = meta;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public static class Meta {
        private long totalItems;
        private int currentPage;
        private int totalPages;
        private int pageSize;

        public Meta() {
        }

        public Meta(long totalItems, int currentPage, int totalPages, int pageSize) {
            this.totalItems = totalItems;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.pageSize = pageSize;
        }

        // Legacy constructor for backward compatibility
        public Meta(long totalItems, int currentPage, int totalPages) {
            this.totalItems = totalItems;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
        }

        public long getTotalItems() {
            return totalItems;
        }

        public void setTotalItems(long totalItems) {
            this.totalItems = totalItems;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }
}

