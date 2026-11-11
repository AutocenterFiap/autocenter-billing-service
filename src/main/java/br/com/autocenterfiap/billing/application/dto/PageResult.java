package br.com.autocenterfiap.billing.application.dto;

import java.util.List;
import java.util.function.Function;

public class PageResult<T> {

	private final List<T> content;
	private final int pageNumber;
	private final int pageSize;
	private final long totalElements;
	private final long totalPages;

	public PageResult(List<T> content, int pageNumber, int pageSize, long totalElements, long totalPages) {
		this.content = content;
		this.pageNumber = pageNumber;
		this.pageSize = pageSize;
		this.totalElements = totalElements;
		this.totalPages = totalPages;
	}

	public List<T> getContent() {
		return content;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public int getPageSize() {
		return pageSize;
	}

	public long getTotalElements() {
		return totalElements;
	}

	public long getTotalPages() {
		return totalPages;
	}

	public boolean hasNextPage() {
		return pageNumber < totalPages - 1;
	}

	public boolean hasPreviousPage() {
		return pageNumber > 0;
	}

	public int getNumberOfElements() {
		return content.size();
	}

	public <R> PageResult<R> map(Function<T, R> mapper) {
		return new PageResult<>(
				this.content.stream().map(mapper).toList(),
				this.pageNumber,
				this.pageSize,
				this.totalElements,
				this.totalPages
		);
	}

}

