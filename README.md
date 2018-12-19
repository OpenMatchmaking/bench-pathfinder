# bench-pathfinder
Performance benchmark for Pathfinder application

This repository contains performance / load tests for [Pathfinder application](https://github.com/OpenMatchmaking/pathfinder), that have 
written in Scala for Gatling tool.

# How to run tests
1. Clone this repository via `git clone https://github.com/OpenMatchmaking/bench-pathfinder.git`
2. Override the `REMOTE_SERVER` and the `JAVA_OPTS` environment variables on another values if you need it in `docker-compose.yml`  for `gatling` service
3. Start up the minimal development cluster via `docker-compose up -d` from the root of the cloned repository
4. Connect to the container with Gatling via `docker-compose exec gatling bash` command. By default you will moved to `/opt/gatling` as the base work directory  
5. Start simulation tests with `./bin/gatling.sh` command
6. Select the simulated test from a list and wait for it completion
7. The results will be saved in `/gatling/results` directory relatively to the root of this repository

# Repository structure  
- `gatling` stores a group of files for running simulations
  - `conf` - a bunch of configuration files for Gatling tool, e.g. Gatling itself / logging / Akka
  - `results` - special folder which is used as a storage for Gatling reports 
  - `user-files` - code with performance / load tests in Scala which are simulate a user's behaviour    
-  `microservice-echo` stores the code for a small microservice that returns data to the caller as is, without any changes

# License
The bench-pathfinder code is published under BSD license. For more details read the [LICENSE](https://github.com/OpenMatchmaking/bench-pathfinder/blob/master/LICENSE) file.
