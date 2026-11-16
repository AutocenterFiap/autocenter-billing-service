package br.com.autocenterfiap.billing.application.dto;

public class PaginationRequest {

	private final int pageNumber;
	private final int pageSize;
	private final String sortBy;
	private final String sortDirection;

	public PaginationRequest(int pageNumber, int pageSize) {
		this(pageNumber, pageSize, "id", "ASC");
	}

	public PaginationRequest(int pageNumber, int pageSize, String sortBy, String sortDirection) {
		this.pageNumber = Math.max(pageNumber, 0);
		this.pageSize = pageSize <= 0 ? 20 : pageSize;
		this.sortBy = sortBy != null ? sortBy : "id";
		this.sortDirection = sortDirection != null ? sortDirection : "ASC";
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public int getPageSize() {
		return pageSize;
	}

	public String getSortBy() {
		return sortBy;
	}

	public String getSortDirection() {
		return sortDirection;
	}
}

