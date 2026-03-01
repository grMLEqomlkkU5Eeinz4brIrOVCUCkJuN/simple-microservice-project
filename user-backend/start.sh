#!/bin/bash
env $(cat .env | xargs) sbt ~run