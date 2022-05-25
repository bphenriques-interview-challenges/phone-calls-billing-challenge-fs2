.PHONY: all clean compile lint test package

all: clean compile format test package

clean:
	sbt clean

compile:
	sbt compile

format:
	sbt scalafmtAll

test:
	sbt test

# Generates a distributable ZIP file under target/universal/*.zip
package:
	sbt "Universal / packageBin"

