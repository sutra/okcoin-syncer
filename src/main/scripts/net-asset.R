args <- commandArgs(TRUE)

filename <- ifelse(is.na(args[1]), "/tmp/okcoin-net-asset.pdf", args[1])
dbname   <- ifelse(is.na(args[2]), "okcoin", args[2])
user     <- ifelse(is.na(args[3]), "okcoin", args[3])
password <- ifelse(is.na(args[4]), "", args[4])
host     <- ifelse(is.na(args[5]), "", args[5])
port     <- ifelse(is.na(args[6]), 5432, as.numeric(args[6]))
days     <- ifelse(is.na(args[7]), 365, as.numeric(args[7]))

# Use the following command to install the package "lubridate":
# install.packages("lubridate")
library(lubridate)

# Use the following command to install the package "logging":
# install.packages("logging")
require(logging)
logReset()
basicConfig(level='FINEST')
addHandler(writeToFile, file="/tmp/okcoin-net-asset.log", level='DEBUG')

pdf(file=filename, width=11.2, height=7)

# Use the following command to install the package "RPostgreSQL":
# install.packages("RPostgreSQL")
require(RPostgreSQL)
drv = dbDriver("PostgreSQL")
con = dbConnect(drv, dbname = dbname, user = user, password = password, host = host, port = port)

date = as.POSIXct(Sys.time() %m-% days(days), origin = "1970-01-01")
sql <- paste0("select date as \"Date\", net as \"Net asset\" from asset where date >= '", date, "' order by date")
logdebug(sql)

rs = dbSendQuery(con, statement = sql)
df = fetch(rs, n = -1)

plot(df, type = "l")

dbDisconnect(con)
dbUnloadDriver(drv)

dev.off()
