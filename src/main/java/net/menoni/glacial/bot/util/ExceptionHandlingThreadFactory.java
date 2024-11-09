package net.menoni.glacial.bot.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ExceptionHandlingThreadFactory implements ThreadFactory {

	private final String format;
	private final Consumer<Throwable> onError;

	private AtomicInteger counter = new AtomicInteger(1);

	public ExceptionHandlingThreadFactory(String format, Consumer<Throwable> onError) {
		this.format = format;
		this.onError = onError;
	}

	@Override
	public Thread newThread(@NotNull Runnable r) {
		if (counter.get() > 9999) {
			counter.set(0);
		}
		return new Thread(() -> {
			try {
				r.run();
			} catch (Throwable ex) {
				if (this.onError != null) {
					this.onError.accept(ex);
				}
			}
		}, format.formatted(counter.getAndIncrement()));
	}
}
