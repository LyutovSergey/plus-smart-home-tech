package ru.yandex.practicum.order.client.dto;


import lombok.Getter;

@Getter
public class ServiceCallResult<T> {
	private final T data;
	private final boolean success;
	private final boolean businessError;
	private final String errorMessage;

	private ServiceCallResult(T data, boolean success, boolean businessError, String errorMessage) {
		this.data = data;
		this.success = success;
		this.businessError = businessError;
		this.errorMessage = errorMessage;
	}

	public static <T> ServiceCallResult<T> success(T data) {
		return new ServiceCallResult<>(data, true, false, null);
	}

	public static <T> ServiceCallResult<T> businessError(String message) {
		return new ServiceCallResult<>(null, false, true, message);
	}

	public static <T> ServiceCallResult<T> technicalError(String message) {
		return new ServiceCallResult<>(null, false, false, message);
	}

	public boolean isSuccess() {
		return success;
	}

	public boolean isBusinessError() {
		return businessError;
	}
}