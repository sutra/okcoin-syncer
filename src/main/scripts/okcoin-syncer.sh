#!/bin/sh
basedir=$(cd "$(dirname "$0")"; pwd)/..
java \
	-cp "${basedir}/etc:${basedir}/lib/*" \
	-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager \
	-Dlog.dir="${basedir}/log" \
	org.oxerr.okcoin.syncer.Main $@
