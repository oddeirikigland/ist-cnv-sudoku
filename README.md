# ist-cnv-sudoku

The cloud architecture for the checkpoint consists of an *Amazon EC2* instance which runs the web server as well as the tools needed for the instrumentation and collection of metrics. Additionally, a load balancer and an auto-scaling group are in use, which, for now, are created from standard *Amazon* services and have not been enhanced with their own code. As MSS we currently use a simple log file.

The load balancer sends the incoming requests to port 8000 where we run our webserver. The auto scaler spawn instances from an AMI image. When the CPU utilization is above 60% for a minute a new instance is launched, and when the utilization is below 40% one instance is terminated. The instances use 300 seconds to warm up.

## Attention

use "source setup.sh XXX" instead of "./setup.sh XXX" to avoid mistakes with the environment variables"

## Configure environment

```bash
chmod +x setup.sh
source setup.sh configure
```

To configure with aws java sdk

```bash
source setup.sh configure aws-cli
```

## Compile sources

```bash
source setup.sh compile
```

## Run WebServer

```bash
source setup.sh WebServer
```

In case of `Adress already in use` error:

```bash
sudo lsof -i:8000
sudo kill <pid>
```

## Simple Tests

Use one of these commands to see a simple use case

```bash
source setup.sh InstrumentationToolHello
source setup.sh InstrumentationToolSolver
```

To test DynamoDB connection

```bash
java awsclient.AmazonDynamoDBSample
```

Check your DynamoDB console, you should now have a table with an item.
