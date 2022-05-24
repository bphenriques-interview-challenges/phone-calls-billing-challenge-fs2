# Contributing Guide

This project is developed in Scala and [scala-sbt](https://www.scala-sbt.org/) as the build tool.

### Requirements

* Sbt 1.2.x.
* Scala 2.12.8.

### Quick Guide

Deploy the application:
```shell
$ make package
$ tmp_dir=$(mktemp -d)
$ cp target/universal/*.zip $tmp_dir
$ unzip $tmp_dir/*.zip -d $tmp_dir
```

Run it:
```shell
$ $tmp_dir/**/bin/billing-fs2 src/test/resources/valid-samples/sample.csv
```

### Commands

* Compile: `make compile`
* Format Code: `make format`
* Test: `make test`
* Package: `make package`. A zip file will available at `target/universal`.

### Other

Ensure that your global gitignore contains:
```
.bsp
.idea
```
