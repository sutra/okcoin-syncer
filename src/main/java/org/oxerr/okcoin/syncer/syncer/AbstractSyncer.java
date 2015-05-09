package org.oxerr.okcoin.syncer.syncer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractSyncer implements Syncer {

	private final Logger log = Logger.getLogger(AbstractSyncer.class.getName());
	private long interval;

	public AbstractSyncer(long interval) {
		this.interval = interval;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		log.info("Running...");
		init();

		while (!Thread.interrupted()) {
			try {
				sync();
				log.log(Level.FINEST, "Sleeping {0} milliseconds.", interval);
				Thread.sleep(interval);
				log.log(Level.FINEST, "Being awake.");
			} catch (IOException e) {
				log.log(Level.WARNING, e.getMessage());
			} catch (InterruptedException e) {
				log.fine(e.getMessage());
				Thread.currentThread().interrupt();
			}
		}
		log.info("exit.");
	}

	protected void init() {
	}

	protected abstract void sync() throws IOException;

}
