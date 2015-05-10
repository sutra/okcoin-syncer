package org.oxerr.okcoin.syncer.service;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IOExceptionRetryService {

	public static interface Retriable<T> {

		T retry() throws IOException;

	}

	private final Logger log = Logger
			.getLogger(IOExceptionRetryService.class.getName());

	private int maxRetryTimes;
	private long retryInterval;

	public IOExceptionRetryService(int maxRetryTimes, long retryInterval) {
		this.maxRetryTimes = maxRetryTimes;
		this.retryInterval = retryInterval;
	}

	public <T> T retry(Retriable<T> retriable) throws IOException {
		int times = 0;
		IOException previousException = null;
		while (!Thread.interrupted() && ++times <= maxRetryTimes) {
			try {
				return retriable.retry();
			} catch (IOException e) {
				log.log(Level.WARNING, "Retried: {0}({1}). Error: {2}.",
						new Object[] { times, maxRetryTimes, e.getMessage(), });
				previousException = e;
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException io) {
					Thread.currentThread().interrupt();
				}
			}
		}
		throw previousException;
	}

}
