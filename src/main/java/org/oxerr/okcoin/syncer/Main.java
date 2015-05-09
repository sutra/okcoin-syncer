package org.oxerr.okcoin.syncer;

import org.apache.commons.daemon.support.DaemonLoader;

public class Main {

	public static void main(String[] args) throws Exception {
		DaemonLoader.load(SyncerDaemon.class.getName(), args);
		DaemonLoader.start();
	}

}
