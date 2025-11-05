package org.walrex.domain.model;

/**
 * Objeto de dominio para manejar paginación
 * Independiente de cualquier framework de infraestructura
 */
public class Pageable {

    private final int page;
    private final int size;

    private Pageable(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public static Pageable of(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        return new Pageable(page, size);
    }

    public static Pageable unpaged() {
        return new Pageable(0, Integer.MAX_VALUE);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getOffset() {
        return page * size;
    }

    public boolean isPaged() {
        return size != Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return "Pageable{" +
                "page=" + page +
                ", size=" + size +
                '}';
    }
}
