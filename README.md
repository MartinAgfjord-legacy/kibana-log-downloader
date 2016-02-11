# Kibana Log Downloader

# Run with Bash-script
```
mvn package
./run.sh <origin_host>
```

The result is in `kibana.log`

## Optional run command
```
mvn package
./run.sh <origin_host> <hours to go back>
```

# Run with Java (Not recommended)
```
mvn package
java -Dkibana_host=<kibana_host> -Dorigin_host=<origin_host> -jar ./target/console-application-1.0-SNAPSHOT-jar-with-dependencies.jar > server.log
```

The log entries will be written to STDOUT if running java command directly.