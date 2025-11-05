package org.walrex.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

/**
 * Clase genérica para encapsular resultados paginados con metadata
 * Contiene los datos de la página actual y metadatos de paginación (total elementos, páginas, etc.)
 *
 * @param <T> Tipo de elemento contenido en la lista
 */
@RegisterForReflection
public class PagedResult<T> {

    private final List<T> data;
    private final long totalElements;
    private final int page;
    private final int size;
    private final long totalPages;

    @JsonCreator
    public PagedResult(
            @JsonProperty("data") List<T> data,
            @JsonProperty("totalElements") long totalElements,
            @JsonProperty("page") int page,
            @JsonProperty("size") int size) {
        this.data = data;
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;
        this.totalPages = size > 0 ? (long) Math.ceil((double) totalElements / size) : 0;
    }

    /**
     * Constructor para resultados sin paginación
     */
    public PagedResult(List<T> data) {
        this.data = data;
        this.totalElements = data.size();
        this.page = 0;
        this.size = data.size();
        this.totalPages = 1;
    }

    public List<T> getData() {
        return data;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public boolean hasNext() {
        return page < totalPages - 1;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    @Override
    public String toString() {
        return "PagedResult{" +
                "data size=" + data.size() +
                ", totalElements=" + totalElements +
                ", page=" + page +
                ", size=" + size +
                ", totalPages=" + totalPages +
                '}';
    }
}
