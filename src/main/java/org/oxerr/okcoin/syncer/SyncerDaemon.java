package org.oxerr.okcoin.syncer;

import java.util.logging.Logger;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SyncerDaemon implements Daemon {

	private final Logger log = Logger.getLogger(SyncerDaemon.class.getName());
	private ClassPathXmlApplicationContext ctx;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(DaemonContext context) throws DaemonInitException,
			Exception {
		log.info("Initializing...");

		ctx = new ClassPathXmlApplicationContext(
			"classpath:META-INF/spring/applicationContext.xml");

		log.info("Initialized.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() throws Exception {
		log.info("Starting...");

		ctx.start();

		log.info("Started.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() throws Exception {
		log.info("Stopping...");

		ctx.stop();

		log.info("Stopped.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void destroy() {
		log.info("Destroying...");

		ctx.close();

		log.info("Destroyed.");
	}

}
