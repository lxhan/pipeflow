package org.schabi.newpipe.player.mediasource;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * A queue item couldn't be resolved because the device had no usable network.
 */
public final class OfflineSkipException extends FailedMediaSource.FailedMediaSourceException {
    public OfflineSkipException(final Throwable cause) {
        super(cause);
    }

    public OfflineSkipException(final String message) {
        super(message);
    }

    public static boolean onlyOfflineSkips(@NonNull final List<Exception> errors) {
        if (errors.isEmpty()) {
            return false;
        }
        for (final Exception error : errors) {
            if (!(error instanceof OfflineSkipException)) {
                return false;
            }
        }
        return true;
    }
}
