package top.rookiestwo.wheatmarket.service.result;

public class ServiceResult<T> {
    private final boolean success;
    private final T value;
    private final String messageKey;
    private final String[] messageArgs;

    private ServiceResult(boolean success, T value, String messageKey, String[] messageArgs) {
        this.success = success;
        this.value = value;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public static <T> ServiceResult<T> success(T value) {
        return new ServiceResult<>(true, value, null, new String[0]);
    }

    public static <T> ServiceResult<T> failure(String messageKey, String... messageArgs) {
        return new ServiceResult<>(false, null, messageKey, messageArgs);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getValue() {
        return value;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String[] getMessageArgs() {
        return messageArgs;
    }
}
