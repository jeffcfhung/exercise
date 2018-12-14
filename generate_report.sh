#!/bin/bash

javac -Xlint:unchecked Report.java
java Report $*
