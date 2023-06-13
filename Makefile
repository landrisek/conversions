.PHONY: help build run test clean

help:
	@echo "make help                        Show this help message"
	@echo "make build                       Build scala functionality on localhost"
	@echo "make run                         Run scala functionality on localhost"
	@echo "make test                        Run test of scala functionality on localhost"
	@echo "make run-docker                  Run scala functionality in docker"
	@echo "make msg                         Send json message to server"

build:
	./scripts/build.sh

run:
	./scripts/run.sh

test:
	sbt test

run-docker:
	./scripts/run-docker.sh

msg:
	./scripts/msg.sh

clean:
	rm -rf build

