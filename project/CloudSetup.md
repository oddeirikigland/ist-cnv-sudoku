# How to setup the project on AWS

Follow the steps on <http://grupos.ist.utl.pt/meic-cnv/labs/labs-aws/cnv-aws-guide-19-20.pdf>

Before crating the image, clone this repo at root(~). And replace the content of `/etc/rc.local` with following:

```bash
#!/bin/bash
# THIS FILE IS ADDED FOR COMPATIBILITY PURPOSES
#
# It is highly advisable to create own systemd services or udev rules
# to run scripts during boot instead of using this file.
#
# In contrast to previous versions due to parallel execution during boot
# this script will NOT be run after all other services.
#
# Please note that you must run 'chmod +x /etc/rc.d/rc.local' to ensure
# that this script will be executed during boot.

touch /var/lock/subsys/local

# set classpath ( former classpath-config.sh and config-bit.sh )
export CLASSPATH=$CLASSPATH:/home/ec2-user/ist-cnv-sudoku/instrumented:/home/ec2-user/ist-cnv-sudoku/project:/home/ec2-user/ist-cnv-sudoku/BIT:/home/ec2-user/ist-cnv-sudoku/BIT/samples:.
export CLASSPATH=$CLASSPATH:/tmp/cnv/BIT:/tmp/cnv/BIT/samples:./

# export java variables (former java-config-rnl-vm.sh)l
export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
export JAVA_ROOT=/usr/lib/jvm/java-7-openjdk-amd64/
export JDK_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
export JRE_HOME=/usr/lib/jvm/java-7-openjdk-amd64/jre
export PATH=/usr/lib/jvm/java-7-openjdk-amd64/bin/:$PATH
export SDK_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
export _JAVA_OPTIONS="-XX:-UseSplitVerifier "$_JAVA_OPTIONS

java pt.ulisboa.tecnico.cnv.server.WebServer
```

When defining the load balancer, make sure the health check has ping path `/test`
