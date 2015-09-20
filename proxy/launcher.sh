#!/bin/bash

export CLASSPATH=$CLASSPATH:/home/ubuntu/jsch-0.1.52.jar
javac MasterInter.java
javac MasterImpl.java
javac ContentServerIntf.java
javac NodeData.java
java MasterImpl
