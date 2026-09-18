package ru.practicum.event.util.specification;

import org.springframework.data.jpa.domain.Specification;

import java.util.function.Supplier;

public class SpecBuilder<T> {

	private Specification<T> spec;

	private SpecBuilder() {
		this.spec = (root, query, cb) -> cb.conjunction();
	}

	public static <T> SpecBuilder<T> builder() {
		return new SpecBuilder<>();
	}

	public SpecBuilder<T> andIf(boolean condition, Supplier<Specification<T>> supplier) {
		if (condition) {
			spec = spec.and(supplier.get());
		}
		return this;
	}

	public SpecBuilder<T> and(Specification<T> other) {
		if (other != null) {
			spec = spec.and(other);
		}
		return this;
	}

	public Specification<T> build() {
		return spec;
	}
}
