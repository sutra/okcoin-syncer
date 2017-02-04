#!/bin/sh
baseDirForScriptSelf="$(cd "$(dirname "$0")"; pwd)"

output="/tmp/okcoin-net-asset-`date '+%F %T %Z'`.pdf"
dbname="okcoin"
user="okcoin"
password=""
host=""
port=5432
days=365

usage() {
	echo "Usage: $0 [-o <filename>] [-d <dbname>] [-u <user>] [-P <password>] [-h <host>] [-p <port>] [-D <days>]"
	echo "  -o\t output file"
	echo "  -d\t database name to connect to (default: \"okcoin\")"
	echo "  -u\t database user name (default: \"okcoin\")"
	echo "  -P\t database password"
	echo "  -h\t database server host"
	echo "  -p\t database server port (default: \"5432\")"
	echo "  -D\t number of days to go back in the net asset chart"
}

while getopts ":o:d:u:P:h:p:D:" o; do
	case "${o}" in
		o)
			output=${OPTARG}
			;;
		d)
			dbname=${OPTARG}
			;;
		u)
			user=${OPTARG}
			;;
		P)
			password=${OPTARG}
			;;
		h)
			host=${OPTARG}
			;;
		p)
			port=${OPTARG}
			;;
		D)
			days=${OPTARG}
			;;
		*)
			usage
			exit
			;;
	esac
done
shift $((OPTIND-1))

Rscript --vanilla \
	"${baseDirForScriptSelf}/net-asset.R" \
	"${output}" \
	"${dbname}" "${user}" "${password}" "${host}" ${port} ${days}
