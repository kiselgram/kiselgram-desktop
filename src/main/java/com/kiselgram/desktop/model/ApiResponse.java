package com.kiselgram.desktop.model;

public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ApiError error;

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public ApiError getError() { return error; }
}
